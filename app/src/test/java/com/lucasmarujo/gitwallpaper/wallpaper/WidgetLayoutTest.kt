package com.lucasmarujo.gitwallpaper.wallpaper

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class WidgetLayoutTest {

    @Test
    fun `short widgets show the github week columns in a single row`() {
        val layout = layout(width = 250, height = 80)

        assertEquals(WidgetGridStyle.WEEK_COLUMNS, layout.style)
        assertEquals(WidgetLayout.DAYS_PER_WEEK, layout.rows)
        assertEquals(26, layout.visibleCells)
        assertEquals(layout.visibleCells, layout.columns)
    }

    @Test
    fun `stretching a short widget sideways shows more weeks until the whole period fits`() {
        val visibleWeeks = (60..1200 step 20).map { width -> layout(width = width, height = 80).visibleCells }

        assertEquals(visibleWeeks.sorted(), visibleWeeks)
        assertEquals(YEAR_WEEKS, visibleWeeks.last())
        (60..1200 step 20).forEach { width ->
            assertEquals(WidgetLayout.DAYS_PER_WEEK, layout(width = width, height = 80).rows)
        }
    }

    @Test
    fun `wide landscape widgets keep the week columns with the header`() {
        val layout = layout(width = 600, height = 130)

        assertEquals(WidgetGridStyle.WEEK_COLUMNS, layout.style)
        assertEquals(48, layout.visibleCells)
        assertNotNull(layout.header)
    }

    @Test
    fun `stretching the widget vertically switches to the wallpaper day grid`() {
        val layout = layout(width = 346, height = 205)

        assertEquals(WidgetGridStyle.DAY_ROWS, layout.style)
        assertEquals(YEAR_DAYS, layout.visibleCells)
        assertTrue(layout.rows > WidgetLayout.DAYS_PER_WEEK)
        assertTrue(layout.columns * layout.rows >= YEAR_DAYS)
        assertTrue(layout.columns * (layout.rows - 1) < YEAR_DAYS)
    }

    @Test
    fun `taller widgets get bigger day cells`() {
        val cellSizes = listOf(205, 300, 420, 560).map { height -> layout(width = 346, height = height).cellSize }

        assertEquals(cellSizes.sorted(), cellSizes)
        assertTrue(cellSizes.first() < cellSizes.last())
    }

    @Test
    fun `narrow tall widgets show the latest days that keep the cells readable`() {
        val layout = layout(width = 70, height = 300)

        assertEquals(WidgetGridStyle.DAY_ROWS, layout.style)
        assertEquals(266, layout.visibleCells)
        assertTrue(layout.cellSize >= MIN_CELL_DP)
        assertNull(layout.header)
    }

    @Test
    fun `the grid fits inside the widget at any size and density`() {
        val sizes = listOf(40 to 40, 110 to 40, 40 to 250, 250 to 110, 346 to 205, 400 to 200, 320 to 520, 600 to 90)
        listOf(1f, 2.75f).forEach { density ->
            sizes.forEach { (widthDp, heightDp) ->
                val width = (widthDp * density).toInt()
                val height = (heightDp * density).toInt()
                val layout = WidgetLayout.calculate(width, height, density, YEAR_WEEKS, YEAR_DAYS, showHeader = true)
                val label = "${widthDp}x$heightDp@$density"
                val lastSlot = layout.visibleCells - 1
                val (lastColumn, lastRow) = when (layout.style) {
                    WidgetGridStyle.WEEK_COLUMNS -> lastSlot to WidgetLayout.DAYS_PER_WEEK - 1
                    WidgetGridStyle.DAY_ROWS -> lastSlot % layout.columns to lastSlot / layout.columns
                }

                assertTrue(label, layout.cellSize > 0f)
                assertTrue(label, layout.gridLeft >= 0f && layout.gridTop >= 0f)
                assertTrue(label, layout.gridLeft + layout.gridWidth <= width + TOLERANCE)
                assertTrue(label, layout.gridTop + layout.gridHeight <= height + TOLERANCE)
                assertTrue(label, layout.cellLeft(lastColumn) + layout.cellSize <= width + TOLERANCE)
                assertTrue(label, layout.cellTop(lastRow) + layout.cellSize <= height + TOLERANCE)
            }
        }
    }

    @Test
    fun `the header needs stats enabled and enough room`() {
        val layout = layout(width = 346, height = 205)

        assertTrue(requireNotNull(layout.header).baseline < layout.gridTop)
        assertNull(layout(width = 346, height = 205, showHeader = false).header)
        assertNull(layout(width = 250, height = 80).header)
        assertNull(layout(width = 100, height = 300).header)
    }

    @Test
    fun `visible cells end at today`() {
        assertEquals(23, WidgetLayout.firstVisible(total = YEAR_WEEKS, todayIndex = 42, visible = 20))
        assertEquals(0, WidgetLayout.firstVisible(total = YEAR_WEEKS, todayIndex = 42, visible = YEAR_WEEKS))
        assertEquals(0, WidgetLayout.firstVisible(total = YEAR_WEEKS, todayIndex = 2, visible = 20))
        assertEquals(33, WidgetLayout.firstVisible(total = YEAR_WEEKS, todayIndex = 52, visible = 20))
        assertEquals(31, WidgetLayout.firstVisible(total = YEAR_DAYS, todayIndex = 296, visible = 266))
    }

    @Test
    fun `invalid sizes are rejected`() {
        assertThrows(IllegalArgumentException::class.java) { WidgetLayout.calculate(0, 110, 1f, YEAR_WEEKS, YEAR_DAYS, true) }
        assertThrows(IllegalArgumentException::class.java) { WidgetLayout.calculate(250, 110, 0f, YEAR_WEEKS, YEAR_DAYS, true) }
        assertThrows(IllegalArgumentException::class.java) { WidgetLayout.calculate(250, 110, 1f, 0, YEAR_DAYS, true) }
        assertThrows(IllegalArgumentException::class.java) { WidgetLayout.calculate(250, 110, 1f, YEAR_WEEKS, 0, true) }
    }

    private fun layout(width: Int, height: Int, showHeader: Boolean = true): WidgetLayout =
        WidgetLayout.calculate(width, height, density = 1f, weekCount = YEAR_WEEKS, dayCount = YEAR_DAYS, showHeader = showHeader)

    private companion object {
        const val YEAR_WEEKS = 53
        const val YEAR_DAYS = 365
        const val MIN_CELL_DP = 6f
        const val TOLERANCE = 0.01f
    }
}
