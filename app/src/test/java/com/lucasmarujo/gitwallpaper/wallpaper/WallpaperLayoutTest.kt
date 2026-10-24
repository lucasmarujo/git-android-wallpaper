package com.lucasmarujo.gitwallpaper.wallpaper

import com.lucasmarujo.gitwallpaper.domain.ContributionPeriod
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WallpaperLayoutTest {

    private val width = 1080
    private val height = 2400

    @Test
    fun `twelve months are split into two stacked segments`() {
        val layout = layout(weeks = ContributionPeriod.TWELVE_MONTHS.weeks)

        assertEquals(2, layout.segments.size)
        assertEquals(listOf(0, 27), layout.segments.map { it.startColumn })
        assertEquals(listOf(27, 26), layout.segments.map { it.columnCount })
        assertTrue(layout.segments[1].top > layout.segments[0].top + layout.segmentHeight)
    }

    @Test
    fun `shorter periods use a single segment`() {
        assertEquals(1, layout(weeks = ContributionPeriod.SIX_MONTHS.weeks).segments.size)
        assertEquals(1, layout(weeks = ContributionPeriod.THREE_MONTHS.weeks).segments.size)
    }

    @Test
    fun `grid is horizontally centered inside the screen`() {
        ContributionPeriod.entries.forEach { period ->
            val layout = layout(weeks = period.weeks)
            val segment = layout.segments.first()
            val right = layout.cellLeft(segment, segment.startColumn + segment.columnCount - 1) + layout.cellSize

            assertTrue(segment.left > 0f)
            assertEquals(segment.left, width - right, 0.5f)
        }
    }

    @Test
    fun `fewer weeks produce bigger cells`() {
        val threeMonths = layout(weeks = ContributionPeriod.THREE_MONTHS.weeks)
        val sixMonths = layout(weeks = ContributionPeriod.SIX_MONTHS.weeks)

        assertTrue(threeMonths.cellSize > sixMonths.cellSize)
    }

    @Test
    fun `block stays inside the safe area for every position`() {
        listOf(0f, 0.25f, 0.55f, 0.8f, 1f).forEach { position ->
            val layout = layout(weeks = ContributionPeriod.TWELVE_MONTHS.weeks, position = position)

            assertTrue("top for $position", layout.top >= height * 0.08f - 0.5f)
            assertTrue("bottom for $position", layout.bottom <= height * 0.92f + 0.5f)
        }
    }

    @Test
    fun `vertical position moves the block`() {
        val upper = layout(weeks = ContributionPeriod.SIX_MONTHS.weeks, position = 0.3f)
        val lower = layout(weeks = ContributionPeriod.SIX_MONTHS.weeks, position = 0.7f)

        assertTrue(upper.top < lower.top)
    }

    @Test
    fun `stats are placed below the grid only when enabled`() {
        val withStats = layout(weeks = ContributionPeriod.TWELVE_MONTHS.weeks, showStats = true)
        val withoutStats = layout(weeks = ContributionPeriod.TWELVE_MONTHS.weeks, showStats = false)

        val stats = requireNotNull(withStats.stats)
        val lastSegment = withStats.segments.last()
        assertTrue(stats.top > lastSegment.top + withStats.segmentHeight)
        assertTrue(withStats.bottom <= height)
        assertNull(withoutStats.stats)
    }

    @Test
    fun `segment lookup finds the segment that owns a column`() {
        val layout = layout(weeks = ContributionPeriod.TWELVE_MONTHS.weeks)

        assertEquals(0, layout.segmentFor(26)?.startColumn)
        assertEquals(27, layout.segmentFor(27)?.startColumn)
        assertNull(layout.segmentFor(53))
    }

    @Test(expected = IllegalArgumentException::class)
    fun `invalid sizes are rejected`() {
        WallpaperLayout.calculate(0, height, 26, 0.5f, true)
    }

    private fun layout(weeks: Int, position: Float = 0.55f, showStats: Boolean = true) =
        WallpaperLayout.calculate(width, height, weeks, position, showStats)
}
