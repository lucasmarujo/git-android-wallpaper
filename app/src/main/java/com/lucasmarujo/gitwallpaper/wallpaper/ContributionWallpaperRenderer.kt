package com.lucasmarujo.gitwallpaper.wallpaper

import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import androidx.annotation.ColorInt
import androidx.core.graphics.createBitmap
import com.lucasmarujo.gitwallpaper.R
import com.lucasmarujo.gitwallpaper.domain.CellShape
import com.lucasmarujo.gitwallpaper.domain.ContributionCalendar
import com.lucasmarujo.gitwallpaper.domain.ContributionPeriod
import com.lucasmarujo.gitwallpaper.domain.ContributionStats
import com.lucasmarujo.gitwallpaper.domain.WallpaperSettings
import com.lucasmarujo.gitwallpaper.util.formatMessage
import java.time.LocalDate
import kotlin.math.min

class ContributionWallpaperRenderer(private val resources: Resources) {

    fun render(
        calendar: ContributionCalendar,
        settings: WallpaperSettings,
        size: ScreenSize,
        today: LocalDate,
        avatar: Bitmap?,
    ): Bitmap {
        val palette = WallpaperPalette.of(settings.theme)
        val grid = DayGrid.from(calendar.days, settings.period, today)
        val layout = WallpaperLayout.calculate(
            width = size.width,
            height = size.height,
            dayCount = grid.cells.size,
            showStats = settings.showStats,
        )
        val bitmap = createBitmap(size.width, size.height)
        val canvas = Canvas(bitmap)
        canvas.drawColor(palette.background)
        drawCells(canvas, grid, layout, palette, settings.cellShape)
        layout.stats?.let { statsLayout ->
            drawStats(
                canvas = canvas,
                statsLayout = statsLayout,
                maxTextWidth = size.width * MAX_TEXT_WIDTH_RATIO,
                calendar = calendar,
                grid = grid,
                period = settings.period,
                palette = palette,
                avatar = avatar,
            )
        }
        return bitmap
    }

    private fun drawCells(
        canvas: Canvas,
        grid: DayGrid,
        layout: WallpaperLayout,
        palette: WallpaperPalette,
        cellShape: CellShape,
    ) {
        val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            color = palette.accent
            strokeWidth = layout.cellGap * TODAY_RING_WIDTH_RATIO
        }
        val ringOffset = layout.cellGap * TODAY_RING_OFFSET_RATIO
        val cornerRadius = layout.cellSize * CORNER_RADIUS_RATIO
        val bounds = RectF()
        grid.cells.forEachIndexed { index, cell ->
            val left = layout.cellLeft(index)
            val top = layout.cellTop(index)
            bounds.set(left, top, left + layout.cellSize, top + layout.cellSize)
            fillPaint.color = if (cell.state == DayState.FUTURE) palette.future else palette.colorFor(cell.level)
            canvas.drawCell(bounds, cellShape, cornerRadius, fillPaint)
            if (cell.state == DayState.TODAY) {
                bounds.inset(-ringOffset, -ringOffset)
                canvas.drawCell(bounds, cellShape, cornerRadius + ringOffset, ringPaint)
            }
        }
    }

    private fun Canvas.drawCell(bounds: RectF, cellShape: CellShape, cornerRadius: Float, paint: Paint) {
        when (cellShape) {
            CellShape.ROUNDED_SQUARE -> drawRoundRect(bounds, cornerRadius, cornerRadius, paint)
            CellShape.CIRCLE -> drawCircle(bounds.centerX(), bounds.centerY(), bounds.width() / 2f, paint)
        }
    }

    private fun drawStats(
        canvas: Canvas,
        statsLayout: StatsLayout,
        maxTextWidth: Float,
        calendar: ContributionCalendar,
        grid: DayGrid,
        period: ContributionPeriod,
        palette: WallpaperPalette,
        avatar: Bitmap?,
    ) {
        val stats = ContributionStats.from(calendar.days, grid.startDate)
        drawTitle(canvas, statsLayout, maxTextWidth, stats, period, grid.today, palette)

        val streakLine = resources.getString(
            R.string.wallpaper_streaks,
            resources.formatMessage(R.string.wallpaper_days, COUNT_ARGUMENT to stats.currentStreak),
            resources.formatMessage(R.string.wallpaper_days, COUNT_ARGUMENT to stats.longestStreak),
        )
        val bodyPaint = textPaint(palette.secondaryText, statsLayout.bodyTextSize, Typeface.DEFAULT)
        bodyPaint.shrinkToFit(bodyPaint.measureText(streakLine), maxTextWidth)
        bodyPaint.textAlign = Paint.Align.CENTER
        canvas.drawText(streakLine, statsLayout.centerX, statsLayout.bodyBaseline, bodyPaint)

        drawUserRow(canvas, statsLayout, maxTextWidth, calendar.login, palette, avatar)
    }

    private fun drawTitle(
        canvas: Canvas,
        statsLayout: StatsLayout,
        maxTextWidth: Float,
        stats: ContributionStats,
        period: ContributionPeriod,
        today: LocalDate,
        palette: WallpaperPalette,
    ) {
        val highlight = resources.formatMessage(
            R.string.wallpaper_total_highlight,
            COUNT_ARGUMENT to stats.totalContributions,
        )
        val suffix = when (period) {
            ContributionPeriod.CURRENT_YEAR -> resources.getString(R.string.wallpaper_total_suffix_year, today.year)
            ContributionPeriod.LAST_12_MONTHS -> resources.getString(R.string.wallpaper_total_suffix_last_12_months)
        }
        val highlightPaint = textPaint(palette.accent, statsLayout.titleTextSize, BOLD_TYPEFACE)
        val suffixPaint = textPaint(palette.secondaryText, statsLayout.titleTextSize, Typeface.DEFAULT)
        val naturalWidth = highlightPaint.measureText(highlight) +
            suffixPaint.measureText(WORD_SEPARATOR) +
            suffixPaint.measureText(suffix)
        highlightPaint.shrinkToFit(naturalWidth, maxTextWidth)
        suffixPaint.shrinkToFit(naturalWidth, maxTextWidth)
        val highlightWidth = highlightPaint.measureText(highlight)
        val separatorWidth = suffixPaint.measureText(WORD_SEPARATOR)
        val totalWidth = highlightWidth + separatorWidth + suffixPaint.measureText(suffix)
        val startX = statsLayout.centerX - totalWidth / 2f
        canvas.drawText(highlight, startX, statsLayout.titleBaseline, highlightPaint)
        canvas.drawText(suffix, startX + highlightWidth + separatorWidth, statsLayout.titleBaseline, suffixPaint)
    }

    private fun drawUserRow(
        canvas: Canvas,
        statsLayout: StatsLayout,
        maxTextWidth: Float,
        login: String,
        palette: WallpaperPalette,
        avatar: Bitmap?,
    ) {
        val userText = resources.getString(R.string.wallpaper_login, login)
        val userPaint = textPaint(palette.secondaryText, statsLayout.userTextSize, Typeface.DEFAULT)
        val avatarWidth = if (avatar != null) statsLayout.avatarSize + statsLayout.avatarGap else 0f
        userPaint.shrinkToFit(userPaint.measureText(userText), maxTextWidth - avatarWidth)
        val rowWidth = avatarWidth + userPaint.measureText(userText)
        val startX = statsLayout.centerX - rowWidth / 2f
        if (avatar != null) {
            val radius = statsLayout.avatarSize / 2f
            canvas.drawAvatar(avatar, startX + radius, statsLayout.userRowCenterY, radius)
        }
        val metrics = userPaint.fontMetrics
        val baseline = statsLayout.userRowCenterY - (metrics.ascent + metrics.descent) / 2f
        canvas.drawText(userText, startX + avatarWidth, baseline, userPaint)
    }

    private fun Canvas.drawAvatar(avatar: Bitmap, centerX: Float, centerY: Float, radius: Float) {
        val scale = radius * 2f / min(avatar.width, avatar.height)
        val shader = BitmapShader(avatar, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP).apply {
            setLocalMatrix(
                Matrix().apply {
                    setScale(scale, scale)
                    postTranslate(centerX - avatar.width * scale / 2f, centerY - avatar.height * scale / 2f)
                },
            )
        }
        drawCircle(centerX, centerY, radius, Paint(Paint.ANTI_ALIAS_FLAG).apply { this.shader = shader })
    }

    private fun textPaint(@ColorInt color: Int, textSize: Float, typeface: Typeface): Paint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color
            this.textSize = textSize
            this.typeface = typeface
        }

    private fun Paint.shrinkToFit(width: Float, maxWidth: Float) {
        if (width > maxWidth && width > 0f) textSize *= maxWidth / width
    }

    private companion object {
        const val CORNER_RADIUS_RATIO = 0.22f
        const val TODAY_RING_OFFSET_RATIO = 0.5f
        const val TODAY_RING_WIDTH_RATIO = 0.5f
        const val MAX_TEXT_WIDTH_RATIO = 0.8f
        const val COUNT_ARGUMENT = "count"
        const val WORD_SEPARATOR = " "
        val BOLD_TYPEFACE: Typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }
}
