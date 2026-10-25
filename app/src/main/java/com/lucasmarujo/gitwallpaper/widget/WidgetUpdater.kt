package com.lucasmarujo.gitwallpaper.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.widget.RemoteViews
import com.lucasmarujo.gitwallpaper.MainActivity
import com.lucasmarujo.gitwallpaper.R
import com.lucasmarujo.gitwallpaper.data.contributions.ContributionRepository
import com.lucasmarujo.gitwallpaper.data.settings.SettingsRepository
import com.lucasmarujo.gitwallpaper.domain.ContributionCalendar
import com.lucasmarujo.gitwallpaper.domain.WallpaperSettings
import com.lucasmarujo.gitwallpaper.wallpaper.ContributionWallpaperRenderer
import com.lucasmarujo.gitwallpaper.wallpaper.ScreenSize
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Clock
import java.time.LocalDate
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sqrt

interface WidgetUpdater {
    fun hasWidgets(): Boolean

    suspend fun updateAll()

    suspend fun update(appWidgetIds: IntArray)
}

class ContributionWidgetUpdater(
    private val context: Context,
    private val appWidgetManager: AppWidgetManager,
    private val contributionRepository: ContributionRepository,
    private val settingsRepository: SettingsRepository,
    private val renderer: ContributionWallpaperRenderer,
    private val clock: Clock,
    private val renderDispatcher: CoroutineDispatcher = Dispatchers.Default,
) : WidgetUpdater {
    private val provider = ComponentName(context, ContributionWidgetProvider::class.java)

    override fun hasWidgets(): Boolean = appWidgetIds().isNotEmpty()

    override suspend fun updateAll() {
        update(appWidgetIds())
    }

    override suspend fun update(appWidgetIds: IntArray) {
        if (appWidgetIds.isEmpty()) return
        val calendar = contributionRepository.cachedCalendar()
        val settings = settingsRepository.current()
        val today = LocalDate.now(clock)
        withContext(renderDispatcher) {
            appWidgetIds.forEach { appWidgetId ->
                val views = if (calendar == null) {
                    messageViews()
                } else {
                    contributionViews(renderWidget(appWidgetId, calendar, settings, today))
                }
                appWidgetManager.updateAppWidget(appWidgetId, views)
            }
        }
    }

    private fun appWidgetIds(): IntArray = appWidgetManager.getAppWidgetIds(provider)

    private fun renderWidget(
        appWidgetId: Int,
        calendar: ContributionCalendar,
        settings: WallpaperSettings,
        today: LocalDate,
    ): Bitmap {
        val options = appWidgetManager.getAppWidgetOptions(appWidgetId)
        val widthDp = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH).takeIf { it > 0 } ?: DEFAULT_WIDTH_DP
        val heightDp = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT).takeIf { it > 0 } ?: DEFAULT_HEIGHT_DP
        val density = min(
            context.resources.displayMetrics.density,
            sqrt(MAX_BITMAP_PIXELS / (widthDp.toFloat() * heightDp)),
        )
        val size = ScreenSize(
            width = (widthDp * density).roundToInt().coerceAtLeast(1),
            height = (heightDp * density).roundToInt().coerceAtLeast(1),
        )
        return renderer.renderWidget(calendar, settings, size, density, today)
    }

    private fun contributionViews(bitmap: Bitmap): RemoteViews =
        RemoteViews(context.packageName, R.layout.widget_contribution).apply {
            setImageViewBitmap(R.id.widget_image, bitmap)
            setOnClickPendingIntent(android.R.id.background, openAppIntent())
        }

    private fun messageViews(): RemoteViews =
        RemoteViews(context.packageName, R.layout.widget_message).apply {
            setTextViewText(R.id.widget_message, context.getString(R.string.widget_empty))
            setOnClickPendingIntent(android.R.id.background, openAppIntent())
        }

    private fun openAppIntent(): PendingIntent = PendingIntent.getActivity(
        context,
        OPEN_APP_REQUEST_CODE,
        Intent(context, MainActivity::class.java),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    private companion object {
        const val DEFAULT_WIDTH_DP = 250
        const val DEFAULT_HEIGHT_DP = 80
        const val MAX_BITMAP_PIXELS = 1_000_000f
        const val OPEN_APP_REQUEST_CODE = 0
    }
}
