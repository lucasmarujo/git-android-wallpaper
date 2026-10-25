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
import androidx.core.graphics.withMatrix
import com.lucasmarujo.gitwallpaper.R
import com.lucasmarujo.gitwallpaper.domain.CellShape
import com.lucasmarujo.gitwallpaper.domain.ContributionCalendar
import com.lucasmarujo.gitwallpaper.domain.ContributionPeriod
import com.lucasmarujo.gitwallpaper.domain.ContributionStats
import com.lucasmarujo.gitwallpaper.domain.WallpaperFraming
import com.lucasmarujo.gitwallpaper.domain.WallpaperSettings
import com.lucasmarujo.gitwallpaper.util.formatMessage
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters
import kotlin.math.min

class ContributionWallpaperRenderer(private val resources: Resources) {

    fun render(
        calendar: ContributionCalendar,
        settings: WallpaperSettings,
        size: ScreenSize,
        today: LocalDate,
        avatar: Bitmap?,
    ): Bitmap {
        val palette = WallpaperPalette.of(settings.theme).withCommitColor(settings.commitColor)
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
        canvas.withMatrix(framingMatrix(settings.framing, size)) {
            drawCells(this, grid, layout, palette, settings.cellShape)
            layout.stats?.let { statsLayout ->
                drawStats(
                    canvas = this,
                    statsLayout = statsLayout,
                    maxTextWidth = size.width * MAX_TEXT_WIDTH_RATIO,
                    calendar = calendar,
                    grid = grid,
                    period = settings.period,
                    palette = palette,
                    avatar = avatar,
                )
            }
        }
        return bitmap
    }

    fun renderWidget(
        calendar: ContributionCalendar,
        settings: WallpaperSettings,
        size: ScreenSize,
        density: Float,
        today: LocalDate,
    ): Bitmap {
        val palette = WallpaperPalette.of(settings.theme).withCommitColor(settings.commitColor)
        val grid = DayGrid.from(calendar.days, settings.period, today)
        val firstWeekStart = grid.startDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY))
        val weekCount = weekIndex(firstWeekStart, grid.cells.last().date) + 1
        val layout = WidgetLayout.calculate(
            width = size.width,
            height = size.height,
            density = density,
            weekCount = weekCount,
            dayCount = grid.cells.size,
            showHeader = settings.showStats,
        )
        val bitmap = createBitmap(size.width, size.height)
        val canvas = Canvas(bitmap)
        val cornerRadius = min(
            resources.getDimension(R.dimen.widget_corner_radius) / resources.displayMetrics.density * density,
            min(size.width, size.height) / 2f,
        )
        val backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = palette.background }
        canvas.drawRoundRect(0f, 0f, size.width.toFloat(), size.height.toFloat(), cornerRadius, cornerRadius, backgroundPaint)
        val painter = CellPainter(palette, settings.cellShape, layout.cellSize, layout.cellGap)
        when (layout.style) {
            WidgetGridStyle.WEEK_COLUMNS -> drawWeekColumns(canvas, grid, layout, painter, firstWeekStart, weekCount)
            WidgetGridStyle.DAY_ROWS -> drawDayRows(canvas, grid, layout, painter)
        }
        layout.header?.let { header ->
            drawTitle(
                canvas = canvas,
                centerX = header.centerX,
                baseline = header.baseline,
                textSize = header.textSize,
                maxTextWidth = header.maxTextWidth,
                stats = ContributionStats.from(calendar.days, grid.startDate),
                period = settings.period,
                today = grid.today,
                palette = palette,
            )
        }
        return bitmap
    }

    private fun drawWeekColumns(
        canvas: Canvas,
        grid: DayGrid,
        layout: WidgetLayout,
        painter: CellPainter,
        firstWeekStart: LocalDate,
        weekCount: Int,
    ) {
        val firstWeek = WidgetLayout.firstVisible(weekCount, weekIndex(firstWeekStart, grid.today), layout.visibleCells)
        grid.cells.forEach { cell ->
            val column = weekIndex(firstWeekStart, cell.date) - firstWeek
            if (column in 0 until layout.columns) {
                val row = cell.date.dayOfWeek.value % WidgetLayout.DAYS_PER_WEEK
                painter.draw(canvas, cell, layout.cellLeft(column), layout.cellTop(row))
            }
        }
    }

    private fun drawDayRows(canvas: Canvas, grid: DayGrid, layout: WidgetLayout, painter: CellPainter) {
        val todayIndex = ChronoUnit.DAYS.between(grid.startDate, grid.today).toInt()
        val firstDay = WidgetLayout.firstVisible(grid.cells.size, todayIndex, layout.visibleCells)
        repeat(layout.visibleCells) { slot ->
            val left = layout.cellLeft(slot % layout.columns)
            val top = layout.cellTop(slot / layout.columns)
            painter.draw(canvas, grid.cells[firstDay + slot], left, top)
        }
    }

    private fun weekIndex(firstWeekStart: LocalDate, date: LocalDate): Int =
        (ChronoUnit.DAYS.between(firstWeekStart, date) / WidgetLayout.DAYS_PER_WEEK).toInt()

    private fun framingMatrix(framing: WallpaperFraming, size: ScreenSize): Matrix {
        val coerced = framing.coerced()
        return Matrix().apply {
            setScale(coerced.zoom, coerced.zoom, size.width / 2f, size.height / 2f)
            postTranslate(coerced.offsetX * size.width, coerced.offsetY * size.height)
        }
    }

    private fun drawCells(
        canvas: Canvas,
        grid: DayGrid,
        layout: WallpaperLayout,
        palette: WallpaperPalette,
        cellShape: CellShape,
    ) {
        val painter = CellPainter(palette, cellShape, layout.cellSize, layout.cellGap)
        grid.cells.forEachIndexed { index, cell ->
            painter.draw(canvas, cell, layout.cellLeft(index), layout.cellTop(index))
        }
    }

    private class CellPainter(
        private val palette: WallpaperPalette,
        private val cellShape: CellShape,
        private val cellSize: Float,
        cellGap: Float,
    ) {
        private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        private val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            color = palette.accent
            strokeWidth = cellGap * TODAY_RING_WIDTH_RATIO
        }
        private val ringOffset = cellGap * TODAY_RING_OFFSET_RATIO
        private val cornerRadius = cellSize * CORNER_RADIUS_RATIO
        private val bounds = RectF()

        fun draw(canvas: Canvas, cell: DayCell, left: Float, top: Float) {
            bounds.set(left, top, left + cellSize, top + cellSize)
            fillPaint.color = if (cell.state == DayState.FUTURE) palette.future else palette.colorFor(cell.level)
            canvas.drawCell(cornerRadius, fillPaint)
            if (cell.state == DayState.TODAY) {
                bounds.inset(-ringOffset, -ringOffset)
                canvas.drawCell(cornerRadius + ringOffset, ringPaint)
            }
        }

        private fun Canvas.drawCell(cornerRadius: Float, paint: Paint) {
            when (cellShape) {
                CellShape.ROUNDED_SQUARE -> drawRoundRect(bounds, cornerRadius, cornerRadius, paint)
                CellShape.CIRCLE -> drawCircle(bounds.centerX(), bounds.centerY(), bounds.width() / 2f, paint)
            }
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
        drawTitle(
            canvas = canvas,
            centerX = statsLayout.centerX,
            baseline = statsLayout.titleBaseline,
            textSize = statsLayout.titleTextSize,
            maxTextWidth = maxTextWidth,
            stats = stats,
            period = period,
            today = grid.today,
            palette = palette,
        )

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
        centerX: Float,
        baseline: Float,
        textSize: Float,
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
        val highlightPaint = textPaint(palette.accent, textSize, BOLD_TYPEFACE)
        val suffixPaint = textPaint(palette.secondaryText, textSize, Typeface.DEFAULT)
        val naturalWidth = highlightPaint.measureText(highlight) +
            suffixPaint.measureText(WORD_SEPARATOR) +
            suffixPaint.measureText(suffix)
        highlightPaint.shrinkToFit(naturalWidth, maxTextWidth)
        suffixPaint.shrinkToFit(naturalWidth, maxTextWidth)
        val highlightWidth = highlightPaint.measureText(highlight)
        val separatorWidth = suffixPaint.measureText(WORD_SEPARATOR)
        val totalWidth = highlightWidth + separatorWidth + suffixPaint.measureText(suffix)
        val startX = centerX - totalWidth / 2f
        canvas.drawText(highlight, startX, baseline, highlightPaint)
        canvas.drawText(suffix, startX + highlightWidth + separatorWidth, baseline, suffixPaint)
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
