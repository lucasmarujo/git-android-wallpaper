package com.lucasmarujo.gitwallpaper.work

import androidx.work.BackoffPolicy
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.lucasmarujo.gitwallpaper.domain.UpdateInterval
import java.util.concurrent.TimeUnit

interface WallpaperScheduler {
    fun schedule(interval: UpdateInterval)

    fun cancel()
}

class WorkManagerWallpaperScheduler(private val workManager: WorkManager) : WallpaperScheduler {

    override fun schedule(interval: UpdateInterval) {
        val request = PeriodicWorkRequestBuilder<WallpaperUpdateWorker>(interval.minutes, TimeUnit.MINUTES)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, BACKOFF_DELAY_MINUTES, TimeUnit.MINUTES)
            .build()
        workManager.enqueueUniquePeriodicWork(UNIQUE_WORK_NAME, ExistingPeriodicWorkPolicy.UPDATE, request)
    }

    override fun cancel() {
        workManager.cancelUniqueWork(UNIQUE_WORK_NAME)
    }

    companion object {
        const val UNIQUE_WORK_NAME = "wallpaper-update"
        private const val BACKOFF_DELAY_MINUTES = 10L
    }
}
