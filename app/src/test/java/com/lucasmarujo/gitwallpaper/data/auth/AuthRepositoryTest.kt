package com.lucasmarujo.gitwallpaper.data.auth

import com.lucasmarujo.gitwallpaper.data.contributions.ContributionRepository
import com.lucasmarujo.gitwallpaper.data.github.GitHubException
import com.lucasmarujo.gitwallpaper.data.settings.UpdateStatusRepository
import com.lucasmarujo.gitwallpaper.domain.UpdateStatus
import com.lucasmarujo.gitwallpaper.testing.FakeContributionsSource
import com.lucasmarujo.gitwallpaper.testing.FakeSecretCipher
import com.lucasmarujo.gitwallpaper.testing.FakeWallpaperScheduler
import com.lucasmarujo.gitwallpaper.testing.FakeWidgetUpdater
import com.lucasmarujo.gitwallpaper.testing.TestCalendars
import com.lucasmarujo.gitwallpaper.testing.preferencesDataStore
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class AuthRepositoryTest {

    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private val source = FakeContributionsSource()
    private val scheduler = FakeWallpaperScheduler()
    private val widgetUpdater = FakeWidgetUpdater()
    private lateinit var sessionRepository: SessionRepository
    private lateinit var contributionRepository: ContributionRepository
    private lateinit var updateStatusRepository: UpdateStatusRepository

    @Test
    fun `sign in validates the token and stores session and calendar`() = runTest {
        val calendar = TestCalendars.calendar(login = "mona")
        source.handler = { calendar }
        val repository = authRepository()

        repository.signIn("  gho_token \n")

        assertEquals(listOf("gho_token"), source.requestedTokens)
        assertEquals(Session(login = "mona", token = "gho_token"), sessionRepository.currentSession())
        assertEquals(calendar, contributionRepository.cachedCalendar())
        assertEquals(1, widgetUpdater.updateAllCount)
    }

    @Test
    fun `rejected tokens do not create a session`() = runTest {
        source.handler = { throw GitHubException.Unauthorized() }
        val repository = authRepository()

        val exception = runCatching { repository.signIn("bad") }.exceptionOrNull()

        assertEquals(GitHubException.Unauthorized::class, exception!!::class)
        assertNull(sessionRepository.currentSession())
        assertNull(contributionRepository.cachedCalendar())
        assertEquals(0, widgetUpdater.updateAllCount)
    }

    @Test
    fun `sign out clears local data and cancels updates`() = runTest {
        val repository = authRepository()
        repository.signIn("gho_token")
        updateStatusRepository.markApplied("fingerprint", 1_000L)

        repository.signOut()

        assertNull(sessionRepository.currentSession())
        assertNull(contributionRepository.cachedCalendar())
        assertEquals(UpdateStatus(), updateStatusRepository.current())
        assertEquals(1, scheduler.cancelCount)
        assertEquals(2, widgetUpdater.updateAllCount)
    }

    @Test
    fun `device flow availability follows the client configuration`() = runTest {
        assertEquals(true, authRepository(clientId = "abc").isDeviceFlowAvailable)
        assertEquals(false, authRepository(clientId = "").isDeviceFlowAvailable)
    }

    private fun TestScope.authRepository(clientId: String = "client"): AuthRepository {
        sessionRepository = SessionRepository(temporaryFolder.preferencesDataStore(backgroundScope), FakeSecretCipher())
        contributionRepository = ContributionRepository(
            temporaryFolder.preferencesDataStore(backgroundScope),
            source,
            sessionRepository,
        )
        updateStatusRepository = UpdateStatusRepository(temporaryFolder.preferencesDataStore(backgroundScope))
        return AuthRepository(
            deviceFlowClient = DeviceFlowClient(OkHttpClient(), clientId),
            contributionsSource = source,
            sessionRepository = sessionRepository,
            contributionRepository = contributionRepository,
            updateStatusRepository = updateStatusRepository,
            wallpaperScheduler = scheduler,
            widgetUpdater = widgetUpdater,
        )
    }
}
