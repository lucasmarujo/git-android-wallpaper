package com.lucasmarujo.gitwallpaper.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.lucasmarujo.gitwallpaper.domain.SyncError
import com.lucasmarujo.gitwallpaper.domain.UpdateResult
import com.lucasmarujo.gitwallpaper.wallpaper.WallpaperUpdater
import com.lucasmarujo.gitwallpaper.widget.WidgetUpdater

class WallpaperUpdateWorker(
    context: Context,
    params: WorkerParameters,
    private val wallpaperUpdater: WallpaperUpdater,
    private val wallpaperScheduler: WallpaperScheduler,
    private val widgetUpdater: WidgetUpdater,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val result = wallpaperUpdater.update(force = false)
        widgetUpdater.updateAll()
        return result.toWorkResult()
    }

    private fun UpdateResult.toWorkResult(): Result = when (this) {
        UpdateResult.Applied, UpdateResult.Unchanged -> Result.success()
        is UpdateResult.Failure -> when (error) {
            SyncError.NETWORK, SyncError.GITHUB -> retryOrFail()
            SyncError.NOT_SIGNED_IN, SyncError.SESSION_EXPIRED -> {
                wallpaperScheduler.cancel()
                Result.failure()
            }
            SyncError.WALLPAPER -> Result.failure()
        }
    }

    private fun retryOrFail(): Result =
        if (runAttemptCount < MAX_RETRY_ATTEMPTS) Result.retry() else Result.failure()

    private companion object {
        const val MAX_RETRY_ATTEMPTS = 3
    }
}
