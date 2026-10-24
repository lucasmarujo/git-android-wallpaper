package com.lucasmarujo.gitwallpaper.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.lucasmarujo.gitwallpaper.domain.SyncError
import com.lucasmarujo.gitwallpaper.domain.UpdateResult
import com.lucasmarujo.gitwallpaper.wallpaper.WallpaperUpdater

class WallpaperUpdateWorker(
    context: Context,
    params: WorkerParameters,
    private val wallpaperUpdater: WallpaperUpdater,
    private val wallpaperScheduler: WallpaperScheduler,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = when (val result = wallpaperUpdater.update(force = false)) {
        UpdateResult.Applied, UpdateResult.Unchanged -> Result.success()
        is UpdateResult.Failure -> when (result.error) {
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
