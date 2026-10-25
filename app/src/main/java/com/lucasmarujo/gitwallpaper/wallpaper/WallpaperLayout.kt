package com.lucasmarujo.gitwallpaper.wallpaper

import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min

data class StatsLayout(
    val centerX: Float,
    val titleBaseline: Float,
    val titleTextSize: Float,
    val bodyBaseline: Float,
    val bodyTextSize: Float,
    val userRowCenterY: Float,
    val userTextSize: Float,
    val avatarSize: Float,
    val avatarGap: Float,
)

data class WallpaperLayout(
    val columns: Int,
    val rows: Int,
    val cellSize: Float,
    val cellGap: Float,
    val gridLeft: Float,
    val gridTop: Float,
    val stats: StatsLayout?,
    val top: Float,
    val bottom: Float,
) {
    val gridWidth: Float
        get() = columns * cellSize + (columns - 1) * cellGap

    val gridHeight: Float
        get() = rows * cellSize + (rows - 1) * cellGap

    fun cellLeft(index: Int): Float = gridLeft + (index % columns) * (cellSize + cellGap)

    fun cellTop(index: Int): Float = gridTop + (index / columns) * (cellSize + cellGap)

    companion object {
        const val SYSTEM_WALLPAPER_ZOOM = 1.1f
        const val VISIBLE_SAFE_TOP_RATIO = 0.27f
        const val VISIBLE_SAFE_BOTTOM_RATIO = 0.8f
        const val VISIBLE_MAX_GRID_WIDTH_RATIO = 0.7f
        private const val CENTER_RATIO = 0.5f
        private const val MIN_COLUMNS = 10
        private const val MAX_COLUMNS = 30
        private const val CELL_GAP_RATIO = 0.22f
        private const val MIN_GRID_HEIGHT_RATIO = 0.2f
        private const val STATS_SPACING_WIDTH_RATIO = 0.045f
        private const val TITLE_TEXT_WIDTH_RATIO = 0.044f
        private const val BODY_TEXT_WIDTH_RATIO = 0.032f
        private const val USER_TEXT_WIDTH_RATIO = 0.034f
        private const val USER_ROW_SPACING_WIDTH_RATIO = 0.025f
        private const val AVATAR_TEXT_RATIO = 1.8f
        private const val AVATAR_GAP_TEXT_RATIO = 0.5f
        private const val LINE_HEIGHT_RATIO = 1.5f

        fun calculate(width: Int, height: Int, dayCount: Int, showStats: Boolean): WallpaperLayout {
            require(width > 0 && height > 0) { "Wallpaper size must be positive" }
            require(dayCount > 0) { "dayCount must be positive" }
            val safeTop = height * toImageRatio(VISIBLE_SAFE_TOP_RATIO)
            val safeHeight = height * toImageRatio(VISIBLE_SAFE_BOTTOM_RATIO) - safeTop

            val titleTextSize = width * TITLE_TEXT_WIDTH_RATIO
            val bodyTextSize = width * BODY_TEXT_WIDTH_RATIO
            val userTextSize = width * USER_TEXT_WIDTH_RATIO
            val avatarSize = userTextSize * AVATAR_TEXT_RATIO
            val userRowHeight = max(avatarSize, userTextSize * LINE_HEIGHT_RATIO)
            val statsSpacing = width * STATS_SPACING_WIDTH_RATIO
            val userRowSpacing = width * USER_ROW_SPACING_WIDTH_RATIO
            val statsHeight = if (showStats) {
                statsSpacing + titleTextSize + bodyTextSize * LINE_HEIGHT_RATIO + userRowSpacing + userRowHeight
            } else {
                0f
            }

            val maxGridWidth = width * VISIBLE_MAX_GRID_WIDTH_RATIO / SYSTEM_WALLPAPER_ZOOM
            val maxGridHeight = max(safeHeight - statsHeight, height * MIN_GRID_HEIGHT_RATIO)
            val maxColumns = min(MAX_COLUMNS, dayCount)
            val minColumns = min(MIN_COLUMNS, maxColumns)
            val (columns, cellSize) = (minColumns..maxColumns)
                .map { columns -> columns to cellSizeFor(columns, rowsFor(dayCount, columns), maxGridWidth, maxGridHeight) }
                .maxBy { (_, cellSize) -> cellSize }
            val rows = rowsFor(dayCount, columns)
            val cellGap = cellSize * CELL_GAP_RATIO
            val gridWidth = columns * cellSize + (columns - 1) * cellGap
            val gridHeight = rows * cellSize + (rows - 1) * cellGap

            val blockHeight = gridHeight + statsHeight
            val top = safeTop + (safeHeight - blockHeight) / 2f
            val gridLeft = (width - gridWidth) / 2f
            val stats = if (showStats) {
                val titleBaseline = top + gridHeight + statsSpacing + titleTextSize
                val bodyBaseline = titleBaseline + bodyTextSize * LINE_HEIGHT_RATIO
                StatsLayout(
                    centerX = width / 2f,
                    titleBaseline = titleBaseline,
                    titleTextSize = titleTextSize,
                    bodyBaseline = bodyBaseline,
                    bodyTextSize = bodyTextSize,
                    userRowCenterY = bodyBaseline + userRowSpacing + userRowHeight / 2f,
                    userTextSize = userTextSize,
                    avatarSize = avatarSize,
                    avatarGap = userTextSize * AVATAR_GAP_TEXT_RATIO,
                )
            } else {
                null
            }
            return WallpaperLayout(
                columns = columns,
                rows = rows,
                cellSize = cellSize,
                cellGap = cellGap,
                gridLeft = gridLeft,
                gridTop = top,
                stats = stats,
                top = top,
                bottom = top + blockHeight,
            )
        }

        fun toImageRatio(visibleRatio: Float): Float =
            CENTER_RATIO + (visibleRatio - CENTER_RATIO) / SYSTEM_WALLPAPER_ZOOM

        private fun rowsFor(dayCount: Int, columns: Int): Int = ceil(dayCount / columns.toFloat()).toInt()

        private fun cellSizeFor(columns: Int, rows: Int, maxWidth: Float, maxHeight: Float): Float = min(
            maxWidth / (columns + (columns - 1) * CELL_GAP_RATIO),
            maxHeight / (rows + (rows - 1) * CELL_GAP_RATIO),
        )
    }
}
