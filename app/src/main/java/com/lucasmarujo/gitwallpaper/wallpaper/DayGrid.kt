package com.lucasmarujo.gitwallpaper.wallpaper

import com.lucasmarujo.gitwallpaper.domain.ContributionDay
import com.lucasmarujo.gitwallpaper.domain.ContributionLevel
import com.lucasmarujo.gitwallpaper.domain.ContributionPeriod
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters

enum class DayState {
    PAST,
    TODAY,
    FUTURE,
}

data class DayCell(val date: LocalDate, val level: ContributionLevel, val state: DayState)

data class DayGrid(
    val startDate: LocalDate,
    val today: LocalDate,
    val cells: List<DayCell>,
) {
    companion object {
        fun from(days: List<ContributionDay>, period: ContributionPeriod, today: LocalDate): DayGrid {
            val lastReportedDate = days.maxOfOrNull { it.date }
            val effectiveToday = if (lastReportedDate != null && lastReportedDate.isAfter(today)) lastReportedDate else today
            val startDate = when (period) {
                ContributionPeriod.CURRENT_YEAR -> effectiveToday.withDayOfYear(1)
                ContributionPeriod.LAST_12_MONTHS -> effectiveToday.minusYears(1).plusDays(1)
            }
            val endDate = when (period) {
                ContributionPeriod.CURRENT_YEAR -> effectiveToday.with(TemporalAdjusters.lastDayOfYear())
                ContributionPeriod.LAST_12_MONTHS -> effectiveToday
            }
            val levelsByDate = days.associate { it.date to it.level }
            val dayCount = ChronoUnit.DAYS.between(startDate, endDate).toInt() + 1
            val cells = List(dayCount) { offset ->
                val date = startDate.plusDays(offset.toLong())
                DayCell(
                    date = date,
                    level = levelsByDate[date] ?: ContributionLevel.NONE,
                    state = when {
                        date == effectiveToday -> DayState.TODAY
                        date.isAfter(effectiveToday) -> DayState.FUTURE
                        else -> DayState.PAST
                    },
                )
            }
            return DayGrid(startDate = startDate, today = effectiveToday, cells = cells)
        }
    }
}
