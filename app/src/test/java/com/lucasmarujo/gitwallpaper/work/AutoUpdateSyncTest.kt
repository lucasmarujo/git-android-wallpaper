package com.lucasmarujo.gitwallpaper.work

import com.lucasmarujo.gitwallpaper.data.settings.SettingsRepository
import com.lucasmarujo.gitwallpaper.data.settings.UpdateStatusRepository
import com.lucasmarujo.gitwallpaper.domain.UpdateInterval
import com.lucasmarujo.gitwallpaper.domain.WallpaperSettings
import com.lucasmarujo.gitwallpaper.testing.FakeWallpaperScheduler
import com.lucasmarujo.gitwallpaper.testing.FakeWidgetUpdater
import com.lucasmarujo.gitwallpaper.testing.preferencesDataStore
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class AutoUpdateSyncTest {

    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private val scheduler = FakeWallpaperScheduler()
    private val widgetUpdater = FakeWidgetUpdater()
    private lateinit var settingsRepository: SettingsRepository
    private lateinit var updateStatusRepository: UpdateStatusRepository

    @Test
    fun `an applied wallpaper keeps the saved interval scheduled`() = runTest {
        val sync = autoUpdateSync()
        updateStatusRepository.markApplied("fingerprint", 1_000L)
        settingsRepository.update { it.copy(updateInterval = UpdateInterval.THREE_HOURS) }

        sync.sync()

        assertEquals(listOf(UpdateInterval.THREE_HOURS), scheduler.scheduledIntervals)
        assertEquals(0, scheduler.cancelCount)
    }

    @Test
    fun `home screen widgets are kept updated without an applied wallpaper`() = runTest {
        val sync = autoUpdateSync()
        widgetUpdater.widgetCount = 2

        sync.sync()

        assertEquals(listOf(UpdateInterval.ONE_HOUR), scheduler.scheduledIntervals)
    }

    @Test
    fun `nothing to update cancels the schedule`() = runTest {
        val sync = autoUpdateSync()

        sync.sync()

        assertTrue(scheduler.scheduledIntervals.isEmpty())
        assertEquals(1, scheduler.cancelCount)
    }

    @Test
    fun `disabled automatic updates cancel the schedule even with widgets and a wallpaper`() = runTest {
        val sync = autoUpdateSync()
        widgetUpdater.widgetCount = 1
        updateStatusRepository.markApplied("fingerprint", 1_000L)

        sync.sync(WallpaperSettings(autoUpdate = false))

        assertTrue(scheduler.scheduledIntervals.isEmpty())
        assertEquals(1, scheduler.cancelCount)
    }

    @Test
    fun `the given settings take precedence over the stored ones`() = runTest {
        val sync = autoUpdateSync()
        widgetUpdater.widgetCount = 1

        sync.sync(WallpaperSettings(updateInterval = UpdateInterval.TWELVE_HOURS))

        assertEquals(listOf(UpdateInterval.TWELVE_HOURS), scheduler.scheduledIntervals)
    }

    private fun TestScope.autoUpdateSync(): AutoUpdateSync {
        settingsRepository = SettingsRepository(temporaryFolder.preferencesDataStore(backgroundScope))
        updateStatusRepository = UpdateStatusRepository(temporaryFolder.preferencesDataStore(backgroundScope))
        return AutoUpdateSync(settingsRepository, updateStatusRepository, widgetUpdater, scheduler)
    }
}
