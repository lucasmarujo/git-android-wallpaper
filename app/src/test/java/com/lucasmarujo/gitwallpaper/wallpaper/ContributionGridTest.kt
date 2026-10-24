package com.lucasmarujo.gitwallpaper.wallpaper

import com.lucasmarujo.gitwallpaper.domain.ContributionLevel
import com.lucasmarujo.gitwallpaper.testing.TestCalendars
import com.lucasmarujo.gitwallpaper.testing.TestCalendars.TODAY
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.Month

class ContributionGridTest {

    private val calendar = TestCalendars.calendar(endDate = TODAY)

    @Test
    fun `grid starts on a sunday covering the requested number of weeks`() {
        val grid = ContributionGrid.from(calendar.days, weekCount = 13, today = TODAY)

        assertEquals(DayOfWeek.SUNDAY, grid.startDate.dayOfWeek)
        assertEquals(LocalDate.of(2026, 7, 26), grid.startDate)
        assertEquals(13, grid.weekCount)
    }

    @Test
    fun `only days inside the visible weeks become cells`() {
        val grid = ContributionGrid.from(calendar.days, weekCount = 13, today = TODAY)

        assertEquals(13 * 7, grid.cells.size)
        assertTrue(grid.cells.all { it.column in 0 until 13 && it.row in 0 until 7 })
    }

    @Test
    fun `cells map dates to week columns and sunday based rows`() {
        val grid = ContributionGrid.from(calendar.days, weekCount = 13, today = TODAY)

        val first = grid.cells.first()
        val last = grid.cells.last()
        assertEquals(0, first.column)
        assertEquals(0, first.row)
        assertEquals(12, last.column)
        assertEquals(DayOfWeek.SATURDAY.value, last.row)
        assertEquals(calendar.days.last().level, last.level)
    }

    @Test
    fun `month labels mark the columns where a new month starts`() {
        val grid = ContributionGrid.from(calendar.days, weekCount = 13, today = TODAY)

        assertEquals(
            listOf(MonthLabel(0, Month.AUGUST), MonthLabel(5, Month.SEPTEMBER), MonthLabel(9, Month.OCTOBER)),
            grid.monthLabels(),
        )
    }

    @Test
    fun `a crowded first month label is dropped`() {
        val grid = ContributionGrid.from(calendar.days, weekCount = 14, today = TODAY)

        val labels = grid.monthLabels()

        assertEquals(MonthLabel(1, Month.AUGUST), labels.first())
    }

    @Test
    fun `month labels can be restricted to a segment`() {
        val grid = ContributionGrid.from(calendar.days, weekCount = 13, today = TODAY)

        assertEquals(
            listOf(MonthLabel(5, Month.SEPTEMBER), MonthLabel(9, Month.OCTOBER)),
            grid.monthLabels(fromColumn = 5, toColumn = 13),
        )
    }

    @Test
    fun `an empty history produces an empty grid anchored on today`() {
        val grid = ContributionGrid.from(emptyList(), weekCount = 26, today = TODAY)

        assertTrue(grid.cells.isEmpty())
        assertEquals(LocalDate.of(2026, 4, 26), grid.startDate)
    }

    @Test
    fun `levels are preserved from the source days`() {
        val grid = ContributionGrid.from(calendar.days, weekCount = 53, today = TODAY)

        val levels = grid.cells.map { it.level }.toSet()
        assertEquals(ContributionLevel.entries.toSet(), levels)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `week count must be positive`() {
        ContributionGrid.from(calendar.days, weekCount = 0, today = TODAY)
    }
}
