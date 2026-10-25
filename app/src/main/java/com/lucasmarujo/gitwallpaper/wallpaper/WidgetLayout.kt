package com.lucasmarujo.gitwallpaper.wallpaper

import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min

enum class WidgetGridStyle {
    WEEK_COLUMNS,
    DAY_ROWS,
}

data class WidgetHeaderLayout(
    val centerX: Float,
    val baseline: Float,
    val textSize: Float,
    val maxTextWidth: Float,
)

data class WidgetLayout(
    val style: WidgetGridStyle,
    val visibleCells: Int,
    val columns: Int,
    val rows: Int,
    val cellSize: Float,
    val cellGap: Float,
    val gridLeft: Float,
    val gridTop: Float,
    val header: WidgetHeaderLayout?,
) {
    val gridWidth: Float
        get() = columns * cellSize + (columns - 1) * cellGap

    val gridHeight: Float
        get() = rows * cellSize + (rows - 1) * cellGap

    fun cellLeft(column: Int): Float = gridLeft + column * (cellSize + cellGap)

    fun cellTop(row: Int): Float = gridTop + row * (cellSize + cellGap)

    private data class Grid(val visibleCells: Int, val columns: Int, val rows: Int, val cellSize: Float)

    companion object {
        const val DAYS_PER_WEEK = 7
        private const val CELL_GAP_RATIO = 0.22f
        private const val PADDING_DP = 12f
        private const val MAX_PADDING_RATIO = 0.12f
        private const val MIN_CELL_DP = 6f
        private const val WEEK_COLUMNS_MAX_GRID_HEIGHT_DP = 100f
        private const val HEADER_TEXT_DP = 13f
        private const val HEADER_HEIGHT_TEXT_RATIO = 1.9f
        private const val HEADER_BASELINE_TEXT_RATIO = 0.95f
        private const val HEADER_MIN_GRID_HEIGHT_DP = 56f
        private const val HEADER_MIN_WIDTH_DP = 120f

        fun calculate(
            width: Int,
            height: Int,
            density: Float,
            weekCount: Int,
            dayCount: Int,
            showHeader: Boolean,
        ): WidgetLayout {
            require(width > 0 && height > 0) { "Widget size must be positive" }
            require(density > 0f) { "density must be positive" }
            require(weekCount > 0 && dayCount > 0) { "weekCount and dayCount must be positive" }
            val padding = min(PADDING_DP * density, min(width, height) * MAX_PADDING_RATIO)
            val contentWidth = width - 2 * padding
            val contentHeight = height - 2 * padding
            val headerTextSize = HEADER_TEXT_DP * density
            val headerHeight = headerTextSize * HEADER_HEIGHT_TEXT_RATIO
            val hasHeader = showHeader &&
                contentWidth >= HEADER_MIN_WIDTH_DP * density &&
                contentHeight - headerHeight >= HEADER_MIN_GRID_HEIGHT_DP * density
            val gridAreaHeight = if (hasHeader) contentHeight - headerHeight else contentHeight

            val style = if (gridAreaHeight <= WEEK_COLUMNS_MAX_GRID_HEIGHT_DP * density) {
                WidgetGridStyle.WEEK_COLUMNS
            } else {
                WidgetGridStyle.DAY_ROWS
            }
            val grid = when (style) {
                WidgetGridStyle.WEEK_COLUMNS -> weekColumns(contentWidth, gridAreaHeight, weekCount)
                WidgetGridStyle.DAY_ROWS -> dayRows(contentWidth, gridAreaHeight, dayCount, MIN_CELL_DP * density)
            }

            val cellGap = grid.cellSize * CELL_GAP_RATIO
            val gridWidth = grid.columns * grid.cellSize + (grid.columns - 1) * cellGap
            val gridHeight = grid.rows * grid.cellSize + (grid.rows - 1) * cellGap
            val blockHeight = gridHeight + if (hasHeader) headerHeight else 0f
            val top = padding + (contentHeight - blockHeight) / 2f
            val header = if (hasHeader) {
                WidgetHeaderLayout(
                    centerX = width / 2f,
                    baseline = top + headerTextSize * HEADER_BASELINE_TEXT_RATIO,
                    textSize = headerTextSize,
                    maxTextWidth = contentWidth,
                )
            } else {
                null
            }
            return WidgetLayout(
                style = style,
                visibleCells = grid.visibleCells,
                columns = grid.columns,
                rows = grid.rows,
                cellSize = grid.cellSize,
                cellGap = cellGap,
                gridLeft = (width - gridWidth) / 2f,
                gridTop = if (hasHeader) top + headerHeight else top,
                header = header,
            )
        }

        fun firstVisible(total: Int, todayIndex: Int, visible: Int): Int =
            (todayIndex + 1).coerceIn(visible, total) - visible

        private fun weekColumns(width: Float, height: Float, weekCount: Int): Grid {
            val cellByHeight = height / units(DAYS_PER_WEEK)
            val weeksThatFit = fittingCount(width, cellByHeight)
            val weeks = min(weekCount, weeksThatFit)
            return Grid(
                visibleCells = weeks,
                columns = weeks,
                rows = DAYS_PER_WEEK,
                cellSize = min(cellByHeight, width / units(weeks)),
            )
        }

        private fun dayRows(width: Float, height: Float, dayCount: Int, minCellSize: Float): Grid {
            val capacity = fittingCount(width, minCellSize) * fittingCount(height, minCellSize)
            val days = min(dayCount, capacity)
            return (1..days)
                .map { columns ->
                    val rows = ceil(days / columns.toFloat()).toInt()
                    Grid(days, columns, rows, min(width / units(columns), height / units(rows)))
                }
                .maxBy { it.cellSize }
        }

        private fun units(cells: Int): Float = cells + (cells - 1) * CELL_GAP_RATIO

        private fun fittingCount(length: Float, cellSize: Float): Int =
            max(1, floor((length / cellSize + CELL_GAP_RATIO) / (1 + CELL_GAP_RATIO)).toInt())
    }
}
