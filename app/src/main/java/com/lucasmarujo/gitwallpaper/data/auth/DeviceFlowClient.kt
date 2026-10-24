package com.lucasmarujo.gitwallpaper.data.auth

import com.lucasmarujo.gitwallpaper.data.github.GitHubException
import com.lucasmarujo.gitwallpaper.data.github.GitHubJson
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.FormBody
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException

class DeviceFlowClient(
    private val httpClient: OkHttpClient,
    private val clientId: String,
    private val baseUrl: HttpUrl = DEFAULT_BASE_URL.toHttpUrl(),
    private val json: Json = GitHubJson,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) {
    val isConfigured: Boolean
        get() = clientId.isNotBlank()

    suspend fun requestDeviceCode(): DeviceCode {
        val form = FormBody.Builder()
            .add(CLIENT_ID_FIELD, clientId)
            .add(SCOPE_FIELD, REQUIRED_SCOPE)
            .build()
        val response = json.decodeFromString(DeviceCodeResponse.serializer(), post(DEVICE_CODE_PATH, form))
        response.error?.let { throw DeviceFlowException(it.toDeviceFlowError()) }
        return DeviceCode(
            deviceCode = response.deviceCode ?: throw DeviceFlowException(DeviceFlowError.UNKNOWN),
            userCode = response.userCode ?: throw DeviceFlowException(DeviceFlowError.UNKNOWN),
            verificationUri = response.verificationUri ?: DEFAULT_VERIFICATION_URI,
            expiresInSeconds = response.expiresIn ?: DEFAULT_EXPIRES_IN_SECONDS,
            intervalSeconds = response.interval ?: MIN_INTERVAL_SECONDS,
        )
    }

    suspend fun awaitAccessToken(deviceCode: DeviceCode): String {
        val form = FormBody.Builder()
            .add(CLIENT_ID_FIELD, clientId)
            .add(DEVICE_CODE_FIELD, deviceCode.deviceCode)
            .add(GRANT_TYPE_FIELD, DEVICE_CODE_GRANT_TYPE)
            .build()
        var intervalSeconds = deviceCode.intervalSeconds.coerceAtLeast(MIN_INTERVAL_SECONDS)
        var elapsedSeconds = 0
        while (elapsedSeconds < deviceCode.expiresInSeconds) {
            delay(intervalSeconds * MILLIS_PER_SECOND)
            elapsedSeconds += intervalSeconds
            val response = try {
                json.decodeFromString(AccessTokenResponse.serializer(), post(ACCESS_TOKEN_PATH, form))
            } catch (exception: GitHubException.Network) {
                continue
            }
            response.accessToken?.let { return it }
            when (response.error) {
                AUTHORIZATION_PENDING -> Unit
                SLOW_DOWN -> intervalSeconds = response.interval ?: (intervalSeconds + SLOW_DOWN_INCREMENT_SECONDS)
                else -> throw DeviceFlowException(response.error.toDeviceFlowError())
            }
        }
        throw DeviceFlowException(DeviceFlowError.EXPIRED)
    }

    private suspend fun post(path: String, form: FormBody): String = withContext(ioDispatcher) {
        val request = Request.Builder()
            .url(baseUrl.newBuilder().addPathSegments(path).build())
            .header(ACCEPT_HEADER, JSON_CONTENT_TYPE)
            .post(form)
            .build()
        try {
            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) throw GitHubException.Http(response.code)
                response.body.string()
            }
        } catch (exception: IOException) {
            throw GitHubException.Network(exception)
        }
    }

    private fun String?.toDeviceFlowError(): DeviceFlowError = when (this) {
        EXPIRED_TOKEN -> DeviceFlowError.EXPIRED
        ACCESS_DENIED -> DeviceFlowError.ACCESS_DENIED
        DEVICE_FLOW_DISABLED -> DeviceFlowError.DEVICE_FLOW_DISABLED
        INCORRECT_CLIENT_CREDENTIALS -> DeviceFlowError.INVALID_CLIENT
        else -> DeviceFlowError.UNKNOWN
    }

    companion object {
        const val REQUIRED_SCOPE = "read:user"
        private const val DEFAULT_BASE_URL = "https://github.com/"
        private const val DEFAULT_VERIFICATION_URI = "https://github.com/login/device"
        private const val DEVICE_CODE_PATH = "login/device/code"
        private const val ACCESS_TOKEN_PATH = "login/oauth/access_token"
        private const val CLIENT_ID_FIELD = "client_id"
        private const val SCOPE_FIELD = "scope"
        private const val DEVICE_CODE_FIELD = "device_code"
        private const val GRANT_TYPE_FIELD = "grant_type"
        private const val DEVICE_CODE_GRANT_TYPE = "urn:ietf:params:oauth:grant-type:device_code"
        private const val ACCEPT_HEADER = "Accept"
        private const val JSON_CONTENT_TYPE = "application/json"
        private const val AUTHORIZATION_PENDING = "authorization_pending"
        private const val SLOW_DOWN = "slow_down"
        private const val EXPIRED_TOKEN = "expired_token"
        private const val ACCESS_DENIED = "access_denied"
        private const val DEVICE_FLOW_DISABLED = "device_flow_disabled"
        private const val INCORRECT_CLIENT_CREDENTIALS = "incorrect_client_credentials"
        private const val MIN_INTERVAL_SECONDS = 5
        private const val SLOW_DOWN_INCREMENT_SECONDS = 5
        private const val DEFAULT_EXPIRES_IN_SECONDS = 900
        private const val MILLIS_PER_SECOND = 1_000L
    }
}
