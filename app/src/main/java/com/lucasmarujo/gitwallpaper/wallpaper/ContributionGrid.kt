package com.lucasmarujo.gitwallpaper.wallpaper

import com.lucasmarujo.gitwallpaper.domain.ContributionDay
import com.lucasmarujo.gitwallpaper.domain.ContributionLevel
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.Month
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters

data class GridCell(val column: Int, val row: Int, val level: ContributionLevel)

data class MonthLabel(val column: Int, val month: Month)

data class ContributionGrid(
    val startDate: LocalDate,
    val weekCount: Int,
    val cells: List<GridCell>,
) {
    fun monthOfColumn(column: Int): Month =
        startDate.plusWeeks(column.toLong()).plusDays(DAYS_PER_WEEK - 1L).month

    fun monthLabels(fromColumn: Int = 0, toColumn: Int = weekCount): List<MonthLabel> {
        val labels = (fromColumn until toColumn)
            .filter { column -> column == fromColumn || monthOfColumn(column) != monthOfColumn(column - 1) }
            .map { column -> MonthLabel(column = column, month = monthOfColumn(column)) }
        val crowdedStart = labels.size > 1 && labels[1].column - labels[0].column < MIN_MONTH_LABEL_SPACING
        return if (crowdedStart) labels.drop(1) else labels
    }

    companion object {
        const val DAYS_PER_WEEK = 7
        private const val MIN_MONTH_LABEL_SPACING = 3

        fun from(days: List<ContributionDay>, weekCount: Int, today: LocalDate): ContributionGrid {
            require(weekCount > 0) { "weekCount must be positive" }
            val endDate = days.maxOfOrNull { it.date } ?: today
            val startDate = endDate
                .with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY))
                .minusWeeks(weekCount - 1L)
            val cells = days
                .filter { !it.date.isBefore(startDate) }
                .map { day ->
                    GridCell(
                        column = (ChronoUnit.DAYS.between(startDate, day.date) / DAYS_PER_WEEK).toInt(),
                        row = day.date.dayOfWeek.value % DAYS_PER_WEEK,
                        level = day.level,
                    )
                }
            return ContributionGrid(startDate = startDate, weekCount = weekCount, cells = cells)
        }
    }
}
