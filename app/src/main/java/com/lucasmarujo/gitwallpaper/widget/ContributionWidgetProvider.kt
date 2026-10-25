package com.lucasmarujo.gitwallpaper.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.os.Bundle
import com.lucasmarujo.gitwallpaper.AppContainer
import com.lucasmarujo.gitwallpaper.GitWallpaperApplication
import kotlinx.coroutines.launch

class ContributionWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        runAsync(context) { container -> container.widgetUpdater.update(appWidgetIds) }
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: Bundle,
    ) {
        runAsync(context) { container -> container.widgetUpdater.update(intArrayOf(appWidgetId)) }
    }

    override fun onEnabled(context: Context) {
        runAsync(context) { container -> container.autoUpdateSync.sync() }
    }

    override fun onDisabled(context: Context) {
        runAsync(context) { container -> container.autoUpdateSync.sync() }
    }

    private fun runAsync(context: Context, block: suspend (AppContainer) -> Unit) {
        val container = (context.applicationContext as GitWallpaperApplication).container
        val pendingResult: PendingResult? = goAsync()
        container.backgroundScope.launch {
            try {
                block(container)
            } finally {
                pendingResult?.finish()
            }
        }
    }
}
