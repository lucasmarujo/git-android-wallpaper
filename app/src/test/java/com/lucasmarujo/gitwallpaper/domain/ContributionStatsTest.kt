package com.lucasmarujo.gitwallpaper.domain

import com.lucasmarujo.gitwallpaper.testing.TestCalendars
import com.lucasmarujo.gitwallpaper.testing.TestCalendars.TODAY
import org.junit.Assert.assertEquals
import org.junit.Test

class ContributionStatsTest {

    @Test
    fun `empty history has no contributions or streaks`() {
        val stats = ContributionStats.from(emptyList(), TODAY)

        assertEquals(ContributionStats(totalContributions = 0, currentStreak = 0, longestStreak = 0), stats)
    }

    @Test
    fun `current streak counts consecutive active days ending today`() {
        val days = TestCalendars.daysEndingAt(TODAY, listOf(3, 0, 1, 2, 5))

        val stats = ContributionStats.from(days, TODAY.minusDays(10))

        assertEquals(3, stats.currentStreak)
    }

    @Test
    fun `a day without contributions today does not break the current streak`() {
        val days = TestCalendars.daysEndingAt(TODAY, listOf(1, 1, 1, 0))

        val stats = ContributionStats.from(days, TODAY.minusDays(10))

        assertEquals(3, stats.currentStreak)
    }

    @Test
    fun `current streak is zero when today and yesterday have no contributions`() {
        val days = TestCalendars.daysEndingAt(TODAY, listOf(4, 4, 0, 0))

        val stats = ContributionStats.from(days, TODAY.minusDays(10))

        assertEquals(0, stats.currentStreak)
    }

    @Test
    fun `missing dates break streaks`() {
        val days = listOf(
            ContributionDay(TODAY.minusDays(3), 2, ContributionLevel.SECOND_QUARTILE),
            ContributionDay(TODAY.minusDays(1), 2, ContributionLevel.SECOND_QUARTILE),
            ContributionDay(TODAY, 2, ContributionLevel.SECOND_QUARTILE),
        )

        val stats = ContributionStats.from(days, TODAY.minusDays(10))

        assertEquals(2, stats.currentStreak)
        assertEquals(2, stats.longestStreak)
    }

    @Test
    fun `longest streak is the longest run in the whole history`() {
        val days = TestCalendars.daysEndingAt(TODAY, listOf(1, 1, 1, 1, 0, 2, 2, 0, 1))

        val stats = ContributionStats.from(days, TODAY.minusDays(30))

        assertEquals(4, stats.longestStreak)
        assertEquals(1, stats.currentStreak)
    }

    @Test
    fun `total only counts contributions inside the period`() {
        val days = TestCalendars.daysEndingAt(TODAY, listOf(10, 10, 1, 2, 3))

        val stats = ContributionStats.from(days, TODAY.minusDays(2))

        assertEquals(6, stats.totalContributions)
    }

    @Test
    fun `unsorted input produces the same result as sorted input`() {
        val days = TestCalendars.daysEndingAt(TODAY, listOf(1, 2, 0, 3, 4))

        assertEquals(
            ContributionStats.from(days, TODAY.minusDays(30)),
            ContributionStats.from(days.shuffled(java.util.Random(7)), TODAY.minusDays(30)),
        )
    }
}
