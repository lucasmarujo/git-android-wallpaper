package com.lucasmarujo.gitwallpaper.wallpaper

import android.graphics.Bitmap
import android.util.Log
import com.lucasmarujo.gitwallpaper.data.contributions.ContributionRepository
import com.lucasmarujo.gitwallpaper.data.contributions.RefreshResult
import com.lucasmarujo.gitwallpaper.data.github.AvatarSource
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
    private val avatarSource: AvatarSource,
    private val renderer: ContributionWallpaperRenderer,
    private val applier: WallpaperApplier,
    private val screenSizeProvider: ScreenSizeProvider,
    private val clock: Clock,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : WallpaperUpdater {
    private val mutex = Mutex()

    override suspend fun update(force: Boolean): UpdateResult = mutex.withLock {
        val refresh = contributionRepository.refresh()
        val staleError = (refresh as? RefreshResult.Failure)?.error
        val calendar = when (refresh) {
            is RefreshResult.Success -> refresh.calendar
            is RefreshResult.Failure -> cachedCalendarFor(refresh.error)
                ?: return@withLock UpdateResult.Failure(refresh.error)
        }
        val settings = settingsRepository.current()
        val screenSize = screenSizeProvider.screenSize()
        val today = LocalDate.now(clock)
        val fingerprint = WallpaperFingerprint.of(calendar, settings, screenSize, Locale.getDefault(), today)
        val now = clock.millis()
        val changed = force || fingerprint != updateStatusRepository.current().fingerprint
        if (changed) {
            val avatar = avatarSource.load(calendar.avatarUrl)
            val applied = withContext(ioDispatcher) { renderAndApply(calendar, settings, screenSize, today, avatar) }
            if (!applied) return@withLock UpdateResult.Failure(SyncError.WALLPAPER)
            updateStatusRepository.markApplied(fingerprint, now)
        } else {
            updateStatusRepository.markChecked(now)
        }
        when {
            staleError != null && !force -> UpdateResult.Failure(staleError)
            changed -> UpdateResult.Applied
            else -> UpdateResult.Unchanged
        }
    }

    private suspend fun cachedCalendarFor(error: SyncError): ContributionCalendar? {
        val isTransient = error == SyncError.NETWORK || error == SyncError.GITHUB
        return if (isTransient) contributionRepository.cachedCalendar() else null
    }

    private fun renderAndApply(
        calendar: ContributionCalendar,
        settings: WallpaperSettings,
        screenSize: ScreenSize,
        today: LocalDate,
        avatar: Bitmap?,
    ): Boolean {
        val bitmap = renderer.render(calendar, settings, screenSize, today, avatar)
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
