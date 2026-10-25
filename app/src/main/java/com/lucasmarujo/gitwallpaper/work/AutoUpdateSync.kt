package com.lucasmarujo.gitwallpaper.work

import com.lucasmarujo.gitwallpaper.data.settings.SettingsRepository
import com.lucasmarujo.gitwallpaper.data.settings.UpdateStatusRepository
import com.lucasmarujo.gitwallpaper.domain.WallpaperSettings
import com.lucasmarujo.gitwallpaper.widget.WidgetUpdater

class AutoUpdateSync(
    private val settingsRepository: SettingsRepository,
    private val updateStatusRepository: UpdateStatusRepository,
    private val widgetUpdater: WidgetUpdater,
    private val wallpaperScheduler: WallpaperScheduler,
) {
    suspend fun sync() {
        sync(settingsRepository.current())
    }

    suspend fun sync(settings: WallpaperSettings) {
        val isWallpaperApplied = updateStatusRepository.current().lastAppliedAtMillis != null
        if (settings.autoUpdate && (isWallpaperApplied || widgetUpdater.hasWidgets())) {
            wallpaperScheduler.schedule(settings.updateInterval)
        } else {
            wallpaperScheduler.cancel()
        }
    }
}
