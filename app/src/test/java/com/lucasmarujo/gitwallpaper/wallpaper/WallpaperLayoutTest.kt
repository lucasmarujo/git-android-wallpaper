package com.lucasmarujo.gitwallpaper.wallpaper

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.ceil

class WallpaperLayoutTest {

    private val width = 1080
    private val height = 2340
    private val dayCount = 365

    @Test
    fun `grid is vertical with more rows than columns on phones`() {
        val layout = layout()

        assertTrue(layout.rows > layout.columns)
        assertEquals(ceil(dayCount / layout.columns.toFloat()).toInt(), layout.rows)
        assertTrue(layout.gridHeight > layout.gridWidth)
    }

    @Test
    fun `every day has a cell inside the grid`() {
        val layout = layout()

        assertTrue(layout.rows * layout.columns >= dayCount)
        assertTrue((layout.rows - 1) * layout.columns < dayCount)
        val lastIndex = dayCount - 1
        assertTrue(layout.cellLeft(lastIndex) + layout.cellSize <= layout.gridLeft + layout.gridWidth + 0.5f)
        assertTrue(layout.cellTop(lastIndex) + layout.cellSize <= layout.gridTop + layout.gridHeight + 0.5f)
    }

    @Test
    fun `cells flow left to right then top to bottom`() {
        val layout = layout()

        assertEquals(layout.gridLeft, layout.cellLeft(0), 0.01f)
        assertEquals(layout.gridTop, layout.cellTop(0), 0.01f)
        assertEquals(layout.cellTop(0), layout.cellTop(layout.columns - 1), 0.01f)
        assertEquals(layout.gridLeft, layout.cellLeft(layout.columns), 0.01f)
        assertEquals(layout.cellTop(0) + layout.cellSize + layout.cellGap, layout.cellTop(layout.columns), 0.01f)
    }

    @Test
    fun `visible content stays between the lock screen clock and bottom buttons`() {
        listOf(true, false).forEach { showStats ->
            val layout = layout(showStats = showStats)

            assertTrue("top with stats=$showStats", visibleRatio(layout.top) >= WallpaperLayout.VISIBLE_SAFE_TOP_RATIO - 0.001f)
            assertTrue("bottom with stats=$showStats", visibleRatio(layout.bottom) <= WallpaperLayout.VISIBLE_SAFE_BOTTOM_RATIO + 0.001f)
        }
    }

    @Test
    fun `image ratios undo the system zoom`() {
        assertEquals(0.5f, WallpaperLayout.toImageRatio(0.5f), 0.0001f)
        assertEquals(0.27f, visibleRatio(height * WallpaperLayout.toImageRatio(0.27f)), 0.0001f)
    }

    @Test
    fun `grid is horizontally centered and narrower than the limit`() {
        val layout = layout()

        assertEquals(layout.gridLeft, width - (layout.gridLeft + layout.gridWidth), 0.5f)
        val visibleGridWidth = layout.gridWidth * WallpaperLayout.SYSTEM_WALLPAPER_ZOOM
        assertTrue(visibleGridWidth <= width * WallpaperLayout.VISIBLE_MAX_GRID_WIDTH_RATIO + 0.5f)
    }

    @Test
    fun `grid stays clear of the edges when the system zooms the wallpaper`() {
        val systemZoom = 1.1f
        val croppedPerSide = (width - width / systemZoom) / 2f

        val visibleMargin = (layout().gridLeft - croppedPerSide) * systemZoom

        assertTrue(visibleMargin >= width * 0.05f)
    }

    @Test
    fun `stats sit below the grid in reading order`() {
        val layout = layout()
        val stats = requireNotNull(layout.stats)

        assertTrue(stats.titleBaseline > layout.gridTop + layout.gridHeight)
        assertTrue(stats.bodyBaseline > stats.titleBaseline)
        assertTrue(stats.userRowCenterY > stats.bodyBaseline)
        assertEquals(width / 2f, stats.centerX, 0.01f)
    }

    @Test
    fun `hiding stats leaves room for bigger cells`() {
        val withStats = layout(showStats = true)
        val withoutStats = layout(showStats = false)

        assertNull(withoutStats.stats)
        assertTrue(withoutStats.cellSize >= withStats.cellSize)
    }

    @Test
    fun `wider screens use more columns`() {
        val phone = layout()
        val tablet = WallpaperLayout.calculate(1600, 2560, dayCount, showStats = true)

        assertTrue(tablet.columns > phone.columns)
    }

    @Test
    fun `scaled previews keep the same proportions`() {
        val full = layout()
        val preview = WallpaperLayout.calculate(432, 936, dayCount, showStats = true)

        assertEquals(full.columns, preview.columns)
        assertEquals(full.cellSize * 0.4f, preview.cellSize, 0.5f)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `invalid sizes are rejected`() {
        WallpaperLayout.calculate(0, height, dayCount, showStats = true)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `empty grids are rejected`() {
        WallpaperLayout.calculate(width, height, 0, showStats = true)
    }

    private fun visibleRatio(imageY: Float): Float =
        0.5f + (imageY / height - 0.5f) * WallpaperLayout.SYSTEM_WALLPAPER_ZOOM

    private fun layout(showStats: Boolean = true) = WallpaperLayout.calculate(width, height, dayCount, showStats)
}
