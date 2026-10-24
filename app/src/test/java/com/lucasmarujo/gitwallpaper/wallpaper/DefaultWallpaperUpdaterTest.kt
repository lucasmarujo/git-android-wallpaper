package com.lucasmarujo.gitwallpaper.wallpaper

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.lucasmarujo.gitwallpaper.data.auth.Session
import com.lucasmarujo.gitwallpaper.data.auth.SessionRepository
import com.lucasmarujo.gitwallpaper.data.contributions.ContributionRepository
import com.lucasmarujo.gitwallpaper.data.github.GitHubException
import com.lucasmarujo.gitwallpaper.data.settings.SettingsRepository
import com.lucasmarujo.gitwallpaper.data.settings.UpdateStatusRepository
import com.lucasmarujo.gitwallpaper.domain.SyncError
import com.lucasmarujo.gitwallpaper.domain.UpdateResult
import com.lucasmarujo.gitwallpaper.domain.WallpaperTarget
import com.lucasmarujo.gitwallpaper.testing.FakeContributionsSource
import com.lucasmarujo.gitwallpaper.testing.FakeSecretCipher
import com.lucasmarujo.gitwallpaper.testing.RecordingWallpaperApplier
import com.lucasmarujo.gitwallpaper.testing.TestCalendars
import com.lucasmarujo.gitwallpaper.testing.preferencesDataStore
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
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
class DefaultWallpaperUpdaterTest {

    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val clock = Clock.fixed(Instant.parse("2026-10-24T12:00:00Z"), ZoneOffset.UTC)
    private val source = FakeContributionsSource()
    private val applier = RecordingWallpaperApplier()
    private val screenSize = ScreenSize(270, 600)

    private lateinit var sessionRepository: SessionRepository
    private lateinit var contributionRepository: ContributionRepository
    private lateinit var settingsRepository: SettingsRepository
    private lateinit var updateStatusRepository: UpdateStatusRepository

    @Test
    fun `first update renders, applies and records the status`() = runTest {
        val updater = updater()

        val result = updater.update(force = false)

        assertEquals(UpdateResult.Applied, result)
        assertEquals(
            listOf(RecordingWallpaperApplier.Applied(screenSize.width, screenSize.height, WallpaperTarget.LOCK_SCREEN)),
            applier.applied,
        )
        val status = updateStatusRepository.current()
        assertNotNull(status.fingerprint)
        assertEquals(clock.millis(), status.lastAppliedAtMillis)
    }

    @Test
    fun `unchanged contributions do not reapply the wallpaper`() = runTest {
        val updater = updater()
        updater.update(force = false)

        val result = updater.update(force = false)

        assertEquals(UpdateResult.Unchanged, result)
        assertEquals(1, applier.applied.size)
    }

    @Test
    fun `forced updates always reapply`() = runTest {
        val updater = updater()
        updater.update(force = false)

        val result = updater.update(force = true)

        assertEquals(UpdateResult.Applied, result)
        assertEquals(2, applier.applied.size)
    }

    @Test
    fun `new contributions reapply the wallpaper`() = runTest {
        val updater = updater()
        updater.update(force = false)
        source.handler = { TestCalendars.calendar(counts = List(371) { 4 }) }

        val result = updater.update(force = false)

        assertEquals(UpdateResult.Applied, result)
        assertEquals(2, applier.applied.size)
    }

    @Test
    fun `wallpaper target follows the settings`() = runTest {
        val updater = updater()
        settingsRepository.update { it.copy(target = WallpaperTarget.BOTH) }

        updater.update(force = false)

        assertEquals(WallpaperTarget.BOTH, applier.applied.single().target)
    }

    @Test
    fun `signed out users are not updated`() = runTest {
        val updater = updater(signedIn = false)

        val result = updater.update(force = false)

        assertEquals(UpdateResult.Failure(SyncError.NOT_SIGNED_IN), result)
        assertTrue(applier.applied.isEmpty())
    }

    @Test
    fun `network failures are reported for background updates`() = runTest {
        val updater = updater()
        contributionRepository.save(TestCalendars.calendar())
        source.handler = { throw GitHubException.Network(IOException("offline")) }

        val result = updater.update(force = false)

        assertEquals(UpdateResult.Failure(SyncError.NETWORK), result)
        assertTrue(applier.applied.isEmpty())
    }

    @Test
    fun `forced updates fall back to the cached calendar when offline`() = runTest {
        val updater = updater()
        contributionRepository.save(TestCalendars.calendar())
        source.handler = { throw GitHubException.Network(IOException("offline")) }

        val result = updater.update(force = true)

        assertEquals(UpdateResult.Applied, result)
        assertEquals(1, applier.applied.size)
    }

    @Test
    fun `forced updates without cache report the network failure`() = runTest {
        val updater = updater()
        source.handler = { throw GitHubException.Network(IOException("offline")) }

        assertEquals(UpdateResult.Failure(SyncError.NETWORK), updater.update(force = true))
    }

    @Test
    fun `revoked tokens end the session`() = runTest {
        val updater = updater()
        source.handler = { throw GitHubException.Unauthorized() }

        val result = updater.update(force = true)

        assertEquals(UpdateResult.Failure(SyncError.SESSION_EXPIRED), result)
        assertNull(sessionRepository.currentSession())
        assertTrue(applier.applied.isEmpty())
    }

    @Test
    fun `wallpaper failures are reported and not recorded as applied`() = runTest {
        val updater = updater()
        applier.failure = IOException("disk full")

        val result = updater.update(force = false)

        assertEquals(UpdateResult.Failure(SyncError.WALLPAPER), result)
        assertNull(updateStatusRepository.current().lastAppliedAtMillis)
    }

    @Test
    fun `refused wallpaper changes are reported`() = runTest {
        val updater = updater()
        applier.failure = SecurityException("not allowed")

        assertEquals(UpdateResult.Failure(SyncError.WALLPAPER), updater.update(force = true))
    }

    private suspend fun TestScope.updater(signedIn: Boolean = true): DefaultWallpaperUpdater {
        sessionRepository = SessionRepository(temporaryFolder.preferencesDataStore(backgroundScope), FakeSecretCipher())
        if (signedIn) sessionRepository.save(Session(login = "octocat", token = "gho_token"))
        contributionRepository = ContributionRepository(
            temporaryFolder.preferencesDataStore(backgroundScope),
            source,
            sessionRepository,
        )
        settingsRepository = SettingsRepository(temporaryFolder.preferencesDataStore(backgroundScope))
        updateStatusRepository = UpdateStatusRepository(temporaryFolder.preferencesDataStore(backgroundScope))
        return DefaultWallpaperUpdater(
            contributionRepository = contributionRepository,
            settingsRepository = settingsRepository,
            updateStatusRepository = updateStatusRepository,
            renderer = ContributionWallpaperRenderer(context.resources),
            applier = applier,
            screenSizeProvider = { screenSize },
            clock = clock,
            ioDispatcher = StandardTestDispatcher(testScheduler),
        )
    }
}
