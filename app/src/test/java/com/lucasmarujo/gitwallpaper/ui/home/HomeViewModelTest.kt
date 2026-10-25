package com.lucasmarujo.gitwallpaper.ui.home

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.lucasmarujo.gitwallpaper.data.auth.AuthRepository
import com.lucasmarujo.gitwallpaper.data.auth.DeviceFlowClient
import com.lucasmarujo.gitwallpaper.data.auth.Session
import com.lucasmarujo.gitwallpaper.data.auth.SessionRepository
import com.lucasmarujo.gitwallpaper.data.contributions.ContributionRepository
import com.lucasmarujo.gitwallpaper.data.github.GitHubException
import com.lucasmarujo.gitwallpaper.data.settings.SettingsRepository
import com.lucasmarujo.gitwallpaper.data.settings.UpdateStatusRepository
import com.lucasmarujo.gitwallpaper.domain.UpdateInterval
import com.lucasmarujo.gitwallpaper.testing.FakeAvatarSource
import com.lucasmarujo.gitwallpaper.testing.FakeContributionsSource
import com.lucasmarujo.gitwallpaper.testing.FakeSecretCipher
import com.lucasmarujo.gitwallpaper.testing.FakeWallpaperScheduler
import com.lucasmarujo.gitwallpaper.testing.MainDispatcherRule
import com.lucasmarujo.gitwallpaper.testing.RecordingWallpaperApplier
import com.lucasmarujo.gitwallpaper.testing.TestCalendars
import com.lucasmarujo.gitwallpaper.testing.preferencesDataStore
import com.lucasmarujo.gitwallpaper.wallpaper.ContributionWallpaperRenderer
import com.lucasmarujo.gitwallpaper.wallpaper.DefaultWallpaperUpdater
import com.lucasmarujo.gitwallpaper.wallpaper.ScreenSize
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import okhttp3.OkHttpClient
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import java.io.IOException
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class HomeViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule(StandardTestDispatcher())

    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val clock = Clock.fixed(Instant.parse("2026-10-24T12:00:00Z"), ZoneOffset.UTC)
    private val source = FakeContributionsSource()
    private val applier = RecordingWallpaperApplier()
    private val scheduler = FakeWallpaperScheduler()
    private val avatarSource = FakeAvatarSource()
    private lateinit var sessionRepository: SessionRepository
    private lateinit var settingsRepository: SettingsRepository
    private lateinit var updateStatusRepository: UpdateStatusRepository

    @Test
    fun `opening the screen refreshes contributions and renders the preview`() = runTest(mainDispatcherRule.testDispatcher) {
        val viewModel = viewModel()

        val state = awaitInitialLoad(viewModel)

        assertEquals("octocat", state.login)
        assertTrue(state.hasCalendar)
        assertFalse(state.isLoadingCalendar)
        assertNotNull(viewModel.uiState.first { it.preview != null && it.stats != null }.preview)
        assertEquals(listOf(TOKEN), source.requestedTokens)
    }

    @Test
    fun `applying the wallpaper notifies and schedules automatic updates`() = runTest(mainDispatcherRule.testDispatcher) {
        val viewModel = viewModel()
        awaitInitialLoad(viewModel)

        viewModel.applyWallpaper()

        assertEquals(HomeMessage.APPLIED, viewModel.messages.first())
        assertEquals(1, applier.applied.size)
        assertEquals(setOf(UpdateInterval.ONE_HOUR), scheduler.scheduledIntervals.toSet())
        assertNotNull(updateStatusRepository.current().lastAppliedAtMillis)
    }

    @Test
    fun `automatic updates are not scheduled before the first apply`() = runTest(mainDispatcherRule.testDispatcher) {
        val viewModel = viewModel()
        awaitInitialLoad(viewModel)

        viewModel.updateSettings { it.copy(updateInterval = UpdateInterval.SIX_HOURS) }
        settingsRepository.settings.first { it.updateInterval == UpdateInterval.SIX_HOURS }
        waitRealTime(SETTLE_MILLIS)

        assertTrue(scheduler.scheduledIntervals.isEmpty())
    }

    @Test
    fun `changing the interval reschedules after the wallpaper was applied`() = runTest(mainDispatcherRule.testDispatcher) {
        val viewModel = viewModel()
        awaitInitialLoad(viewModel)
        viewModel.applyWallpaper()
        viewModel.messages.first()

        viewModel.updateSettings { it.copy(updateInterval = UpdateInterval.SIX_HOURS) }
        awaitUntil { UpdateInterval.SIX_HOURS in scheduler.scheduledIntervals }

        assertEquals(UpdateInterval.SIX_HOURS, scheduler.scheduledIntervals.last())
        assertEquals(UpdateInterval.SIX_HOURS, settingsRepository.current().updateInterval)
    }

    @Test
    fun `disabling automatic updates cancels the schedule`() = runTest(mainDispatcherRule.testDispatcher) {
        val viewModel = viewModel()
        awaitInitialLoad(viewModel)

        viewModel.updateSettings { it.copy(autoUpdate = false) }
        awaitUntil { scheduler.cancelCount > 0 }

        assertEquals(1, scheduler.cancelCount)
        assertFalse(settingsRepository.current().autoUpdate)
    }

    @Test
    fun `wallpaper failures are reported`() = runTest(mainDispatcherRule.testDispatcher) {
        applier.failure = IOException("disk full")
        val viewModel = viewModel()
        awaitInitialLoad(viewModel)

        viewModel.applyWallpaper()

        assertEquals(HomeMessage.WALLPAPER_ERROR, viewModel.messages.first())
        assertTrue(scheduler.scheduledIntervals.isEmpty())
    }

    @Test
    fun `manual refresh failures are reported`() = runTest(mainDispatcherRule.testDispatcher) {
        val viewModel = viewModel()
        awaitInitialLoad(viewModel)
        source.handler = { throw GitHubException.Network(IOException("offline")) }

        viewModel.refresh()

        assertEquals(HomeMessage.NETWORK_ERROR, viewModel.messages.first())
    }

    @Test
    fun `signing out clears the session and cancels updates`() = runTest(mainDispatcherRule.testDispatcher) {
        val viewModel = viewModel()
        awaitInitialLoad(viewModel)

        viewModel.signOut()

        assertNull(sessionRepository.session.first { it == null })
        assertTrue(scheduler.cancelCount >= 1)
    }

    private suspend fun TestScope.awaitInitialLoad(viewModel: HomeViewModel): HomeUiState {
        backgroundScope.launch { viewModel.uiState.collect {} }
        return viewModel.uiState.first { it.hasCalendar && !it.isRefreshing && !it.isLoadingCalendar }
    }

    private suspend fun awaitUntil(condition: () -> Boolean) {
        withContext(Dispatchers.Default) {
            withTimeout(AWAIT_TIMEOUT_MILLIS) {
                while (!condition()) delay(POLL_INTERVAL_MILLIS)
            }
        }
    }

    private suspend fun waitRealTime(millis: Long) {
        withContext(Dispatchers.Default) { delay(millis) }
    }

    private suspend fun TestScope.viewModel(): HomeViewModel {
        sessionRepository = SessionRepository(temporaryFolder.preferencesDataStore(backgroundScope), FakeSecretCipher())
        sessionRepository.save(Session(login = "octocat", token = TOKEN))
        settingsRepository = SettingsRepository(temporaryFolder.preferencesDataStore(backgroundScope))
        updateStatusRepository = UpdateStatusRepository(temporaryFolder.preferencesDataStore(backgroundScope))
        source.handler = { TestCalendars.calendar(login = "octocat") }
        val contributionRepository = ContributionRepository(
            temporaryFolder.preferencesDataStore(backgroundScope),
            source,
            sessionRepository,
        )
        val renderer = ContributionWallpaperRenderer(context.resources)
        val dispatcher = StandardTestDispatcher(testScheduler)
        return HomeViewModel(
            login = "octocat",
            contributionRepository = contributionRepository,
            settingsRepository = settingsRepository,
            updateStatusRepository = updateStatusRepository,
            wallpaperUpdater = DefaultWallpaperUpdater(
                contributionRepository = contributionRepository,
                settingsRepository = settingsRepository,
                updateStatusRepository = updateStatusRepository,
                avatarSource = avatarSource,
                renderer = renderer,
                applier = applier,
                screenSizeProvider = { ScreenSize(270, 600) },
                clock = clock,
                ioDispatcher = dispatcher,
            ),
            wallpaperScheduler = scheduler,
            authRepository = AuthRepository(
                deviceFlowClient = DeviceFlowClient(OkHttpClient(), clientId = "client"),
                contributionsSource = source,
                sessionRepository = sessionRepository,
                contributionRepository = contributionRepository,
                updateStatusRepository = updateStatusRepository,
                wallpaperScheduler = scheduler,
            ),
            avatarSource = avatarSource,
            renderer = renderer,
            previewSize = ScreenSize(108, 240),
            clock = clock,
            renderDispatcher = dispatcher,
        )
    }

    private companion object {
        const val TOKEN = "gho_home"
        const val AWAIT_TIMEOUT_MILLIS = 5_000L
        const val POLL_INTERVAL_MILLIS = 10L
        const val SETTLE_MILLIS = 300L
    }
}
