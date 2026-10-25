package com.lucasmarujo.gitwallpaper.wallpaper

import com.lucasmarujo.gitwallpaper.domain.ContributionDay
import com.lucasmarujo.gitwallpaper.domain.ContributionLevel
import com.lucasmarujo.gitwallpaper.domain.ContributionPeriod
import com.lucasmarujo.gitwallpaper.testing.TestCalendars
import com.lucasmarujo.gitwallpaper.testing.TestCalendars.TODAY
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class DayGridTest {

    private val days = TestCalendars.calendar(endDate = TODAY).days

    @Test
    fun `current year covers every day from january to december`() {
        val grid = DayGrid.from(days, ContributionPeriod.CURRENT_YEAR, TODAY)

        assertEquals(365, grid.cells.size)
        assertEquals(LocalDate.of(2026, 1, 1), grid.startDate)
        assertEquals(LocalDate.of(2026, 12, 31), grid.cells.last().date)
    }

    @Test
    fun `leap years have one extra day`() {
        val today = LocalDate.of(2028, 3, 1)

        val grid = DayGrid.from(TestCalendars.daysEndingAt(today, List(10) { 1 }), ContributionPeriod.CURRENT_YEAR, today)

        assertEquals(366, grid.cells.size)
    }

    @Test
    fun `days are split into past, today and future`() {
        val grid = DayGrid.from(days, ContributionPeriod.CURRENT_YEAR, TODAY)

        val today = grid.cells.single { it.state == DayState.TODAY }
        assertEquals(TODAY, today.date)
        assertTrue(grid.cells.filter { it.date.isBefore(TODAY) }.all { it.state == DayState.PAST })
        assertTrue(grid.cells.filter { it.date.isAfter(TODAY) }.all { it.state == DayState.FUTURE })
        assertTrue(grid.cells.filter { it.state == DayState.FUTURE }.all { it.level == ContributionLevel.NONE })
    }

    @Test
    fun `levels come from the matching contribution day`() {
        val grid = DayGrid.from(days, ContributionPeriod.CURRENT_YEAR, TODAY)
        val byDate = days.associateBy { it.date }

        grid.cells.filter { it.state != DayState.FUTURE }.forEach { cell ->
            assertEquals(cell.date.toString(), byDate.getValue(cell.date).level, cell.level)
        }
    }

    @Test
    fun `missing days are drawn without contributions`() {
        val sparse = listOf(ContributionDay(TODAY.minusDays(3), 4, ContributionLevel.FOURTH_QUARTILE))

        val grid = DayGrid.from(sparse, ContributionPeriod.CURRENT_YEAR, TODAY)

        assertEquals(1, grid.cells.count { it.level != ContributionLevel.NONE })
        assertEquals(ContributionLevel.FOURTH_QUARTILE, grid.cells.single { it.date == TODAY.minusDays(3) }.level)
    }

    @Test
    fun `last twelve months end today without future days`() {
        val grid = DayGrid.from(days, ContributionPeriod.LAST_12_MONTHS, TODAY)

        assertEquals(365, grid.cells.size)
        assertEquals(TODAY.minusYears(1).plusDays(1), grid.startDate)
        assertEquals(DayState.TODAY, grid.cells.last().state)
        assertTrue(grid.cells.none { it.state == DayState.FUTURE })
    }

    @Test
    fun `a github day ahead of the device clock becomes today`() {
        val tomorrow = TODAY.plusDays(1)
        val ahead = TestCalendars.daysEndingAt(tomorrow, listOf(1, 2))

        val grid = DayGrid.from(ahead, ContributionPeriod.CURRENT_YEAR, TODAY)

        assertEquals(tomorrow, grid.today)
        assertEquals(DayState.TODAY, grid.cells.single { it.date == tomorrow }.state)
    }

    @Test
    fun `empty history still renders the whole year`() {
        val grid = DayGrid.from(emptyList(), ContributionPeriod.CURRENT_YEAR, TODAY)

        assertEquals(365, grid.cells.size)
        assertEquals(TODAY, grid.today)
    }
}
