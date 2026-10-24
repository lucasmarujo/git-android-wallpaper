package com.lucasmarujo.gitwallpaper.testing

import com.lucasmarujo.gitwallpaper.domain.ContributionCalendar
import com.lucasmarujo.gitwallpaper.domain.ContributionDay
import com.lucasmarujo.gitwallpaper.domain.ContributionLevel
import java.time.LocalDate

object TestCalendars {
    val TODAY: LocalDate = LocalDate.of(2026, 10, 24)

    fun daysEndingAt(endDate: LocalDate, counts: List<Int>): List<ContributionDay> =
        counts.mapIndexed { index, count ->
            ContributionDay(
                date = endDate.minusDays((counts.size - 1 - index).toLong()),
                count = count,
                level = levelFor(count),
            )
        }

    fun calendar(
        login: String = "octocat",
        endDate: LocalDate = TODAY,
        counts: List<Int> = List(371) { index -> index % 5 },
    ): ContributionCalendar {
        val days = daysEndingAt(endDate, counts)
        return ContributionCalendar(login = login, totalContributions = days.sumOf { it.count }, days = days)
    }

    private fun levelFor(count: Int): ContributionLevel = when {
        count <= 0 -> ContributionLevel.NONE
        count == 1 -> ContributionLevel.FIRST_QUARTILE
        count == 2 -> ContributionLevel.SECOND_QUARTILE
        count == 3 -> ContributionLevel.THIRD_QUARTILE
        else -> ContributionLevel.FOURTH_QUARTILE
    }
}
