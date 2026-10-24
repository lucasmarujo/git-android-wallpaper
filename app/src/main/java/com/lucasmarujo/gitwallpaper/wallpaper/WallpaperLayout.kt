package com.lucasmarujo.gitwallpaper.wallpaper

import com.lucasmarujo.gitwallpaper.domain.WallpaperSettings
import com.lucasmarujo.gitwallpaper.wallpaper.ContributionGrid.Companion.DAYS_PER_WEEK
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min

data class SegmentLayout(
    val startColumn: Int,
    val columnCount: Int,
    val left: Float,
    val top: Float,
) {
    fun contains(column: Int): Boolean = column >= startColumn && column < startColumn + columnCount
}

data class StatsLayout(
    val left: Float,
    val top: Float,
    val titleTextSize: Float,
    val bodyTextSize: Float,
    val lineHeight: Float,
)

data class WallpaperLayout(
    val cellSize: Float,
    val cellGap: Float,
    val monthLabelTextSize: Float,
    val segments: List<SegmentLayout>,
    val stats: StatsLayout?,
    val top: Float,
    val bottom: Float,
) {
    val segmentHeight: Float
        get() = DAYS_PER_WEEK * cellSize + (DAYS_PER_WEEK - 1) * cellGap

    fun segmentFor(column: Int): SegmentLayout? = segments.firstOrNull { it.contains(column) }

    fun cellLeft(segment: SegmentLayout, column: Int): Float =
        segment.left + (column - segment.startColumn) * (cellSize + cellGap)

    fun cellTop(segment: SegmentLayout, row: Int): Float = segment.top + row * (cellSize + cellGap)

    fun monthLabelBaseline(segment: SegmentLayout): Float = segment.top - monthLabelTextSize * MONTH_LABEL_BASELINE_RATIO

    companion object {
        const val MAX_COLUMNS_PER_SEGMENT = 27
        private const val HORIZONTAL_MARGIN_RATIO = 0.07f
        private const val CELL_GAP_RATIO = 0.22f
        private const val MONTH_LABEL_CELL_RATIO = 0.6f
        private const val MIN_MONTH_LABEL_WIDTH_RATIO = 0.024f
        private const val MONTH_LABEL_BAND_RATIO = 1.7f
        private const val MONTH_LABEL_BASELINE_RATIO = 0.45f
        private const val SEGMENT_SPACING_CELL_RATIO = 1.2f
        private const val STATS_SPACING_WIDTH_RATIO = 0.05f
        private const val TITLE_TEXT_WIDTH_RATIO = 0.05f
        private const val BODY_TEXT_WIDTH_RATIO = 0.034f
        private const val LINE_HEIGHT_RATIO = 1.45f
        private const val STATS_BODY_LINES = 2
        private const val SAFE_AREA_HEIGHT_RATIO = 0.08f

        fun calculate(
            width: Int,
            height: Int,
            weekCount: Int,
            verticalPosition: Float,
            showStats: Boolean,
        ): WallpaperLayout {
            require(width > 0 && height > 0) { "Wallpaper size must be positive" }
            require(weekCount > 0) { "weekCount must be positive" }
            val segmentCount = ceil(weekCount / MAX_COLUMNS_PER_SEGMENT.toFloat()).toInt()
            val columnsPerSegment = ceil(weekCount / segmentCount.toFloat()).toInt()
            val availableWidth = width * (1f - 2f * HORIZONTAL_MARGIN_RATIO)
            val cellSize = availableWidth / (columnsPerSegment + (columnsPerSegment - 1) * CELL_GAP_RATIO)
            val cellGap = cellSize * CELL_GAP_RATIO
            val gridWidth = columnsPerSegment * cellSize + (columnsPerSegment - 1) * cellGap
            val gridLeft = (width - gridWidth) / 2f
            val monthLabelTextSize = max(cellSize * MONTH_LABEL_CELL_RATIO, width * MIN_MONTH_LABEL_WIDTH_RATIO)
            val monthLabelBand = monthLabelTextSize * MONTH_LABEL_BAND_RATIO
            val segmentHeight = DAYS_PER_WEEK * cellSize + (DAYS_PER_WEEK - 1) * cellGap
            val segmentSpacing = cellSize * SEGMENT_SPACING_CELL_RATIO
            val gridHeight = segmentCount * (monthLabelBand + segmentHeight) + (segmentCount - 1) * segmentSpacing

            val titleTextSize = width * TITLE_TEXT_WIDTH_RATIO
            val bodyTextSize = width * BODY_TEXT_WIDTH_RATIO
            val lineHeight = bodyTextSize * LINE_HEIGHT_RATIO
            val statsSpacing = width * STATS_SPACING_WIDTH_RATIO
            val statsHeight = if (showStats) {
                statsSpacing + titleTextSize * LINE_HEIGHT_RATIO + STATS_BODY_LINES * lineHeight
            } else {
                0f
            }

            val blockHeight = gridHeight + statsHeight
            val safeArea = height * SAFE_AREA_HEIGHT_RATIO
            val position = verticalPosition.coerceIn(
                WallpaperSettings.MIN_VERTICAL_POSITION,
                WallpaperSettings.MAX_VERTICAL_POSITION,
            )
            val top = if (blockHeight > height - 2f * safeArea) {
                (height - blockHeight) / 2f
            } else {
                (height * position - blockHeight / 2f).coerceIn(safeArea, height - safeArea - blockHeight)
            }

            val segments = (0 until segmentCount).map { index ->
                val startColumn = index * columnsPerSegment
                SegmentLayout(
                    startColumn = startColumn,
                    columnCount = min(columnsPerSegment, weekCount - startColumn),
                    left = gridLeft,
                    top = top + index * (monthLabelBand + segmentHeight + segmentSpacing) + monthLabelBand,
                )
            }
            val stats = if (showStats) {
                StatsLayout(
                    left = gridLeft,
                    top = top + gridHeight + statsSpacing,
                    titleTextSize = titleTextSize,
                    bodyTextSize = bodyTextSize,
                    lineHeight = lineHeight,
                )
            } else {
                null
            }
            return WallpaperLayout(
                cellSize = cellSize,
                cellGap = cellGap,
                monthLabelTextSize = monthLabelTextSize,
                segments = segments,
                stats = stats,
                top = top,
                bottom = top + blockHeight,
            )
        }
    }
}
