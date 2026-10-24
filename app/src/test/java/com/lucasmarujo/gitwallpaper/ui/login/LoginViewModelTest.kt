package com.lucasmarujo.gitwallpaper.ui.login

import com.lucasmarujo.gitwallpaper.data.auth.AuthRepository
import com.lucasmarujo.gitwallpaper.data.auth.DeviceFlowClient
import com.lucasmarujo.gitwallpaper.data.auth.SessionRepository
import com.lucasmarujo.gitwallpaper.data.contributions.ContributionRepository
import com.lucasmarujo.gitwallpaper.data.github.GitHubException
import com.lucasmarujo.gitwallpaper.data.settings.UpdateStatusRepository
import com.lucasmarujo.gitwallpaper.testing.FakeContributionsSource
import com.lucasmarujo.gitwallpaper.testing.FakeSecretCipher
import com.lucasmarujo.gitwallpaper.testing.FakeWallpaperScheduler
import com.lucasmarujo.gitwallpaper.testing.MainDispatcherRule
import com.lucasmarujo.gitwallpaper.testing.preferencesDataStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.OkHttpClient
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

@OptIn(ExperimentalCoroutinesApi::class)
class LoginViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule(StandardTestDispatcher())

    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private val server = MockWebServer()
    private val source = FakeContributionsSource()
    private lateinit var sessionRepository: SessionRepository

    @Before
    fun setUp() {
        server.start()
    }

    @After
    fun tearDown() {
        server.close()
    }

    @Test
    fun `device flow shows the user code and signs in once authorized`() = runTest(mainDispatcherRule.testDispatcher) {
        server.enqueue(MockResponse(code = 200, body = DEVICE_CODE))
        server.enqueue(MockResponse(code = 200, body = """{"error":"authorization_pending"}"""))
        server.enqueue(MockResponse(code = 200, body = """{"access_token":"gho_device"}"""))
        val viewModel = viewModel()

        viewModel.startDeviceFlow()
        runCurrent()

        assertEquals(
            LoginUiState.AwaitingAuthorization("ABCD-1234", "https://github.com/login/device"),
            viewModel.uiState.value,
        )

        advanceUntilIdle()

        assertEquals(LoginUiState.Idle, viewModel.awaitSettledState())
        assertEquals("gho_device", sessionRepository.currentSession()?.token)
        assertEquals(listOf("gho_device"), source.requestedTokens)
    }

    @Test
    fun `denied authorization shows an error`() = runTest(mainDispatcherRule.testDispatcher) {
        server.enqueue(MockResponse(code = 200, body = DEVICE_CODE))
        server.enqueue(MockResponse(code = 200, body = """{"error":"access_denied"}"""))
        val viewModel = viewModel()

        viewModel.startDeviceFlow()
        advanceUntilIdle()

        assertEquals(LoginUiState.Error(LoginError.ACCESS_DENIED), viewModel.uiState.value)
        assertNull(sessionRepository.currentSession())
    }

    @Test
    fun `network failures while requesting a code show a network error`() = runTest(mainDispatcherRule.testDispatcher) {
        val viewModel = viewModel()
        server.close()

        viewModel.startDeviceFlow()
        advanceUntilIdle()

        assertEquals(LoginUiState.Error(LoginError.NETWORK), viewModel.uiState.value)
    }

    @Test
    fun `cancelling stops waiting for authorization`() = runTest(mainDispatcherRule.testDispatcher) {
        server.enqueue(MockResponse(code = 200, body = DEVICE_CODE))
        val viewModel = viewModel()
        viewModel.startDeviceFlow()
        runCurrent()

        viewModel.cancel()
        advanceUntilIdle()

        assertEquals(LoginUiState.Idle, viewModel.uiState.value)
        assertEquals(1, server.requestCount)
        assertNull(sessionRepository.currentSession())
    }

    @Test
    fun `personal access tokens sign in directly`() = runTest(mainDispatcherRule.testDispatcher) {
        val viewModel = viewModel()

        viewModel.signInWithToken("ghp_personal")
        advanceUntilIdle()

        assertEquals(LoginUiState.Idle, viewModel.awaitSettledState())
        assertEquals("ghp_personal", sessionRepository.currentSession()?.token)
    }

    @Test
    fun `rejected personal access tokens show an error`() = runTest(mainDispatcherRule.testDispatcher) {
        source.handler = { throw GitHubException.Unauthorized() }
        val viewModel = viewModel()

        viewModel.signInWithToken("ghp_invalid")
        advanceUntilIdle()

        assertEquals(LoginUiState.Error(LoginError.INVALID_TOKEN), viewModel.uiState.value)
    }

    @Test
    fun `blank tokens are ignored`() = runTest(mainDispatcherRule.testDispatcher) {
        val viewModel = viewModel()

        viewModel.signInWithToken("   ")
        advanceUntilIdle()

        assertEquals(LoginUiState.Idle, viewModel.uiState.value)
        assertEquals(emptyList<String>(), source.requestedTokens)
    }

    private suspend fun LoginViewModel.awaitSettledState(): LoginUiState =
        uiState.first { it is LoginUiState.Idle || it is LoginUiState.Error }

    private fun TestScope.viewModel(): LoginViewModel {
        sessionRepository = SessionRepository(temporaryFolder.preferencesDataStore(backgroundScope), FakeSecretCipher())
        val authRepository = AuthRepository(
            deviceFlowClient = DeviceFlowClient(
                httpClient = OkHttpClient.Builder().retryOnConnectionFailure(false).build(),
                clientId = "client-123",
                baseUrl = server.url("/"),
                ioDispatcher = Dispatchers.Unconfined,
            ),
            contributionsSource = source,
            sessionRepository = sessionRepository,
            contributionRepository = ContributionRepository(
                temporaryFolder.preferencesDataStore(backgroundScope),
                source,
                sessionRepository,
            ),
            updateStatusRepository = UpdateStatusRepository(temporaryFolder.preferencesDataStore(backgroundScope)),
            wallpaperScheduler = FakeWallpaperScheduler(),
        )
        return LoginViewModel(authRepository)
    }

    private companion object {
        const val DEVICE_CODE =
            """{"device_code":"device-123","user_code":"ABCD-1234","verification_uri":"https://github.com/login/device","expires_in":900,"interval":5}"""
    }
}
