package com.lucasmarujo.gitwallpaper.domain

import java.time.LocalDate

data class ContributionStats(
    val totalContributions: Int,
    val currentStreak: Int,
    val longestStreak: Int,
) {
    companion object {
        fun from(days: List<ContributionDay>, periodStart: LocalDate): ContributionStats {
            val sortedDays = days.sortedBy { it.date }
            return ContributionStats(
                totalContributions = sortedDays
                    .filter { !it.date.isBefore(periodStart) }
                    .sumOf { it.count },
                currentStreak = currentStreak(sortedDays),
                longestStreak = longestStreak(sortedDays),
            )
        }

        private fun currentStreak(sortedDays: List<ContributionDay>): Int {
            val lastDay = sortedDays.lastOrNull() ?: return 0
            var expectedDate = if (lastDay.count > 0) lastDay.date else lastDay.date.minusDays(1)
            var streak = 0
            for (day in sortedDays.asReversed()) {
                if (day.date.isAfter(expectedDate)) continue
                if (day.date != expectedDate || day.count == 0) break
                streak++
                expectedDate = expectedDate.minusDays(1)
            }
            return streak
        }

        private fun longestStreak(sortedDays: List<ContributionDay>): Int {
            var longest = 0
            var current = 0
            var previousDate: LocalDate? = null
            for (day in sortedDays) {
                current = when {
                    day.count == 0 -> 0
                    previousDate != null && day.date == previousDate.plusDays(1) -> current + 1
                    else -> 1
                }
                longest = maxOf(longest, current)
                previousDate = day.date
            }
            return longest
        }
    }
}
