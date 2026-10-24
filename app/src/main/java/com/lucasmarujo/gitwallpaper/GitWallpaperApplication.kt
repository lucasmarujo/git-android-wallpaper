package com.lucasmarujo.gitwallpaper

import android.app.Application
import androidx.work.Configuration
import com.lucasmarujo.gitwallpaper.work.WallpaperWorkerFactory

class GitWallpaperApplication : Application(), Configuration.Provider {

    val container: AppContainer by lazy { AppContainer(this) }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(WallpaperWorkerFactory(container))
            .build()
}
