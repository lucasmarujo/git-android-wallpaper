package com.lucasmarujo.gitwallpaper.work

import android.content.Context
import androidx.work.ListenableWorker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import com.lucasmarujo.gitwallpaper.AppContainer

class WallpaperWorkerFactory(private val container: AppContainer) : WorkerFactory() {

    override fun createWorker(
        appContext: Context,
        workerClassName: String,
        workerParameters: WorkerParameters,
    ): ListenableWorker? = when (workerClassName) {
        WallpaperUpdateWorker::class.java.name -> WallpaperUpdateWorker(
            context = appContext,
            params = workerParameters,
            wallpaperUpdater = container.wallpaperUpdater,
            wallpaperScheduler = container.wallpaperScheduler,
            widgetUpdater = container.widgetUpdater,
        )
        else -> null
    }
}
