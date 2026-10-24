package com.lucasmarujo.gitwallpaper.data.auth

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.runTest
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import mockwebserver3.SocketEffect
import okhttp3.OkHttpClient
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DeviceFlowClientTest {

    private val server = MockWebServer()
    private lateinit var client: DeviceFlowClient

    private val deviceCode = DeviceCode(
        deviceCode = "device-123",
        userCode = "ABCD-1234",
        verificationUri = "https://github.com/login/device",
        expiresInSeconds = 900,
        intervalSeconds = 5,
    )

    @Before
    fun setUp() {
        server.start()
        client = DeviceFlowClient(
            httpClient = OkHttpClient.Builder().retryOnConnectionFailure(false).build(),
            clientId = CLIENT_ID,
            baseUrl = server.url("/"),
            ioDispatcher = Dispatchers.Unconfined,
        )
    }

    @After
    fun tearDown() {
        server.close()
    }

    @Test
    fun `requests a device code with the client id and read user scope`() = runTest {
        server.enqueue(
            json(
                """{"device_code":"device-123","user_code":"ABCD-1234","verification_uri":"https://github.com/login/device","expires_in":900,"interval":5}""",
            ),
        )

        val result = client.requestDeviceCode()

        val request = server.takeRequest()
        assertEquals("/login/device/code", request.url.encodedPath)
        assertEquals("application/json", request.headers["Accept"])
        val body = request.body!!.utf8()
        assertTrue(body.contains("client_id=$CLIENT_ID"))
        assertTrue(body.contains("scope=read%3Auser"))
        assertEquals(deviceCode, result)
    }

    @Test
    fun `device code errors are mapped to device flow errors`() = runTest {
        server.enqueue(json("""{"error":"device_flow_disabled"}"""))

        val exception = runCatching { client.requestDeviceCode() }.exceptionOrNull()

        assertEquals(DeviceFlowError.DEVICE_FLOW_DISABLED, (exception as DeviceFlowException).error)
    }

    @Test
    fun `polls until the user authorizes the device`() = runTest {
        server.enqueue(json(PENDING))
        server.enqueue(json(PENDING))
        server.enqueue(json("""{"access_token":"gho_token","token_type":"bearer","scope":"read:user"}"""))

        val token = client.awaitAccessToken(deviceCode)

        assertEquals("gho_token", token)
        assertEquals(3, server.requestCount)
        assertEquals(15_000L, currentTime)
        val request = server.takeRequest()
        assertEquals("/login/oauth/access_token", request.url.encodedPath)
        assertTrue(request.body!!.utf8().contains("device_code=device-123"))
    }

    @Test
    fun `slow down responses increase the polling interval`() = runTest {
        server.enqueue(json("""{"error":"slow_down","interval":10}"""))
        server.enqueue(json("""{"access_token":"gho_token"}"""))

        client.awaitAccessToken(deviceCode)

        assertEquals(15_000L, currentTime)
    }

    @Test
    fun `denied authorizations stop polling`() = runTest {
        server.enqueue(json("""{"error":"access_denied"}"""))

        val exception = runCatching { client.awaitAccessToken(deviceCode) }.exceptionOrNull()

        assertEquals(DeviceFlowError.ACCESS_DENIED, (exception as DeviceFlowException).error)
        assertEquals(1, server.requestCount)
    }

    @Test
    fun `expired tokens reported by github stop polling`() = runTest {
        server.enqueue(json("""{"error":"expired_token"}"""))

        val exception = runCatching { client.awaitAccessToken(deviceCode) }.exceptionOrNull()

        assertEquals(DeviceFlowError.EXPIRED, (exception as DeviceFlowException).error)
    }

    @Test
    fun `polling stops when the device code lifetime is over`() = runTest {
        server.enqueue(json(PENDING))
        server.enqueue(json(PENDING))

        val exception = runCatching {
            client.awaitAccessToken(deviceCode.copy(expiresInSeconds = 10))
        }.exceptionOrNull()

        assertEquals(DeviceFlowError.EXPIRED, (exception as DeviceFlowException).error)
        assertEquals(2, server.requestCount)
    }

    @Test
    fun `transient network failures while polling are retried`() = runTest {
        server.enqueue(MockResponse.Builder().onRequestStart(SocketEffect.CloseSocket()).build())
        server.enqueue(json("""{"access_token":"gho_token"}"""))

        val token = client.awaitAccessToken(deviceCode)

        assertEquals("gho_token", token)
    }

    @Test
    fun `client without id is not configured`() {
        val unconfigured = DeviceFlowClient(OkHttpClient(), clientId = " ")

        assertFalse(unconfigured.isConfigured)
        assertTrue(client.isConfigured)
    }

    private fun json(body: String) = MockResponse(code = 200, body = body)

    private companion object {
        const val CLIENT_ID = "client-123"
        const val PENDING = """{"error":"authorization_pending"}"""
    }
}
