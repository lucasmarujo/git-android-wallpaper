package com.lucasmarujo.gitwallpaper.wallpaper

import android.util.Log
import com.lucasmarujo.gitwallpaper.data.contributions.ContributionRepository
import com.lucasmarujo.gitwallpaper.data.contributions.RefreshResult
import com.lucasmarujo.gitwallpaper.data.settings.SettingsRepository
import com.lucasmarujo.gitwallpaper.data.settings.UpdateStatusRepository
import com.lucasmarujo.gitwallpaper.domain.ContributionCalendar
import com.lucasmarujo.gitwallpaper.domain.SyncError
import com.lucasmarujo.gitwallpaper.domain.UpdateResult
import com.lucasmarujo.gitwallpaper.domain.WallpaperSettings
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.IOException
import java.time.Clock
import java.time.LocalDate
import java.util.Locale

interface WallpaperUpdater {
    suspend fun update(force: Boolean): UpdateResult
}

class DefaultWallpaperUpdater(
    private val contributionRepository: ContributionRepository,
    private val settingsRepository: SettingsRepository,
    private val updateStatusRepository: UpdateStatusRepository,
    private val renderer: ContributionWallpaperRenderer,
    private val applier: WallpaperApplier,
    private val screenSizeProvider: ScreenSizeProvider,
    private val clock: Clock,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : WallpaperUpdater {
    private val mutex = Mutex()

    override suspend fun update(force: Boolean): UpdateResult = mutex.withLock {
        val calendar = when (val refresh = contributionRepository.refresh()) {
            is RefreshResult.Success -> refresh.calendar
            is RefreshResult.Failure -> fallbackCalendar(refresh.error, force)
                ?: return@withLock UpdateResult.Failure(refresh.error)
        }
        val settings = settingsRepository.current()
        val screenSize = screenSizeProvider.screenSize()
        val fingerprint = WallpaperFingerprint.of(calendar, settings, screenSize, Locale.getDefault())
        val now = clock.millis()
        if (!force && fingerprint == updateStatusRepository.current().fingerprint) {
            updateStatusRepository.markChecked(now)
            return@withLock UpdateResult.Unchanged
        }
        val applied = withContext(ioDispatcher) { renderAndApply(calendar, settings, screenSize) }
        if (!applied) return@withLock UpdateResult.Failure(SyncError.WALLPAPER)
        updateStatusRepository.markApplied(fingerprint, now)
        UpdateResult.Applied
    }

    private suspend fun fallbackCalendar(error: SyncError, force: Boolean): ContributionCalendar? {
        val canUseCache = force && (error == SyncError.NETWORK || error == SyncError.GITHUB)
        return if (canUseCache) contributionRepository.cachedCalendar() else null
    }

    private fun renderAndApply(
        calendar: ContributionCalendar,
        settings: WallpaperSettings,
        screenSize: ScreenSize,
    ): Boolean {
        val bitmap = renderer.render(calendar, settings, screenSize, LocalDate.now(clock))
        return try {
            applier.apply(bitmap, settings.target)
            true
        } catch (exception: IOException) {
            Log.e(TAG, "Failed to write the wallpaper", exception)
            false
        } catch (exception: SecurityException) {
            Log.e(TAG, "The system refused the wallpaper change", exception)
            false
        } finally {
            bitmap.recycle()
        }
    }

    private companion object {
        const val TAG = "WallpaperUpdater"
    }
}
