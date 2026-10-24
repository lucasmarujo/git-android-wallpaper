package com.lucasmarujo.gitwallpaper.wallpaper

import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import androidx.annotation.StringRes
import androidx.core.graphics.createBitmap
import com.lucasmarujo.gitwallpaper.R
import com.lucasmarujo.gitwallpaper.domain.CellShape
import com.lucasmarujo.gitwallpaper.domain.ContributionCalendar
import com.lucasmarujo.gitwallpaper.domain.ContributionPeriod
import com.lucasmarujo.gitwallpaper.domain.ContributionStats
import com.lucasmarujo.gitwallpaper.domain.WallpaperSettings
import com.lucasmarujo.gitwallpaper.util.formatMessage
import com.lucasmarujo.gitwallpaper.util.primaryLocale
import java.time.LocalDate
import java.time.Month
import java.time.format.TextStyle
import java.util.Locale

class ContributionWallpaperRenderer(private val resources: Resources) {

    fun render(
        calendar: ContributionCalendar,
        settings: WallpaperSettings,
        size: ScreenSize,
        today: LocalDate,
    ): Bitmap {
        val palette = WallpaperPalette.of(settings.theme)
        val grid = ContributionGrid.from(calendar.days, settings.period.weeks, today)
        val layout = WallpaperLayout.calculate(
            width = size.width,
            height = size.height,
            weekCount = grid.weekCount,
            verticalPosition = settings.verticalPosition,
            showStats = settings.showStats,
        )
        val bitmap = createBitmap(size.width, size.height)
        val canvas = Canvas(bitmap)
        canvas.drawColor(palette.background)
        drawCells(canvas, grid, layout, palette, settings.cellShape)
        drawMonthLabels(canvas, grid, layout, palette)
        layout.stats?.let { statsLayout ->
            val stats = ContributionStats.from(calendar.days, grid.startDate)
            drawStats(canvas, statsLayout, calendar.login, stats, settings.period, palette)
        }
        return bitmap
    }

    private fun drawCells(
        canvas: Canvas,
        grid: ContributionGrid,
        layout: WallpaperLayout,
        palette: WallpaperPalette,
        cellShape: CellShape,
    ) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val bounds = RectF()
        val cornerRadius = layout.cellSize * CORNER_RADIUS_RATIO
        val radius = layout.cellSize / 2f
        grid.cells.forEach { cell ->
            val segment = layout.segmentFor(cell.column) ?: return@forEach
            val left = layout.cellLeft(segment, cell.column)
            val top = layout.cellTop(segment, cell.row)
            paint.color = palette.colorFor(cell.level)
            when (cellShape) {
                CellShape.ROUNDED_SQUARE -> {
                    bounds.set(left, top, left + layout.cellSize, top + layout.cellSize)
                    canvas.drawRoundRect(bounds, cornerRadius, cornerRadius, paint)
                }
                CellShape.CIRCLE -> canvas.drawCircle(left + radius, top + radius, radius, paint)
            }
        }
    }

    private fun drawMonthLabels(
        canvas: Canvas,
        grid: ContributionGrid,
        layout: WallpaperLayout,
        palette: WallpaperPalette,
    ) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = palette.secondaryText
            textSize = layout.monthLabelTextSize
            typeface = Typeface.DEFAULT
        }
        val locale = resources.primaryLocale
        layout.segments.forEach { segment ->
            grid.monthLabels(segment.startColumn, segment.startColumn + segment.columnCount).forEach { label ->
                canvas.drawText(
                    label.month.shortName(locale),
                    layout.cellLeft(segment, label.column),
                    layout.monthLabelBaseline(segment),
                    paint,
                )
            }
        }
    }

    private fun drawStats(
        canvas: Canvas,
        statsLayout: StatsLayout,
        login: String,
        stats: ContributionStats,
        period: ContributionPeriod,
        palette: WallpaperPalette,
    ) {
        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = palette.primaryText
            textSize = statsLayout.titleTextSize
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = palette.secondaryText
            textSize = statsLayout.bodyTextSize
            typeface = Typeface.DEFAULT
        }
        val totalLine = resources.formatMessage(
            R.string.wallpaper_total_contributions,
            COUNT_ARGUMENT to stats.totalContributions,
            PERIOD_ARGUMENT to resources.getString(period.phraseRes()),
        )
        val streakLine = resources.getString(
            R.string.wallpaper_streaks,
            resources.formatMessage(R.string.wallpaper_days, COUNT_ARGUMENT to stats.currentStreak),
            resources.formatMessage(R.string.wallpaper_days, COUNT_ARGUMENT to stats.longestStreak),
        )
        var baseline = statsLayout.top + statsLayout.titleTextSize
        canvas.drawText(resources.getString(R.string.wallpaper_login, login), statsLayout.left, baseline, titlePaint)
        baseline += statsLayout.lineHeight + (statsLayout.titleTextSize - statsLayout.bodyTextSize)
        canvas.drawText(totalLine, statsLayout.left, baseline, bodyPaint)
        baseline += statsLayout.lineHeight
        canvas.drawText(streakLine, statsLayout.left, baseline, bodyPaint)
    }

    private fun Month.shortName(locale: Locale): String =
        getDisplayName(TextStyle.SHORT_STANDALONE, locale)
            .removeSuffix(".")
            .replaceFirstChar { it.titlecase(locale) }

    @StringRes
    private fun ContributionPeriod.phraseRes(): Int = when (this) {
        ContributionPeriod.THREE_MONTHS -> R.string.wallpaper_period_three_months
        ContributionPeriod.SIX_MONTHS -> R.string.wallpaper_period_six_months
        ContributionPeriod.TWELVE_MONTHS -> R.string.wallpaper_period_twelve_months
    }

    private companion object {
        const val CORNER_RADIUS_RATIO = 0.22f
        const val COUNT_ARGUMENT = "count"
        const val PERIOD_ARGUMENT = "period"
    }
}
