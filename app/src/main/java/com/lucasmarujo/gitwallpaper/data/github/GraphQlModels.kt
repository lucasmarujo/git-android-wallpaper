package com.lucasmarujo.gitwallpaper.data.github

import com.lucasmarujo.gitwallpaper.domain.ContributionCalendar
import com.lucasmarujo.gitwallpaper.domain.ContributionDay
import com.lucasmarujo.gitwallpaper.domain.ContributionLevel
import com.lucasmarujo.gitwallpaper.domain.LocalDateSerializer
import kotlinx.serialization.Serializable
import java.time.LocalDate

@Serializable
internal data class GraphQlRequest(val query: String)

@Serializable
internal data class GraphQlResponse<T>(
    val data: T? = null,
    val errors: List<GraphQlError>? = null,
)

@Serializable
internal data class GraphQlError(val message: String)

@Serializable
internal data class ViewerData(val viewer: ViewerDto)

@Serializable
internal data class ViewerDto(
    val login: String,
    val contributionsCollection: ContributionsCollectionDto,
)

@Serializable
internal data class ContributionsCollectionDto(val contributionCalendar: ContributionCalendarDto)

@Serializable
internal data class ContributionCalendarDto(
    val totalContributions: Int,
    val weeks: List<ContributionWeekDto>,
)

@Serializable
internal data class ContributionWeekDto(val contributionDays: List<ContributionDayDto>)

@Serializable
internal data class ContributionDayDto(
    @Serializable(with = LocalDateSerializer::class)
    val date: LocalDate,
    val contributionCount: Int,
    val contributionLevel: ContributionLevel = ContributionLevel.NONE,
)

internal fun ViewerDto.toCalendar(): ContributionCalendar {
    val calendar = contributionsCollection.contributionCalendar
    return ContributionCalendar(
        login = login,
        totalContributions = calendar.totalContributions,
        days = calendar.weeks
            .flatMap { it.contributionDays }
            .map { ContributionDay(date = it.date, count = it.contributionCount, level = it.contributionLevel) }
            .sortedBy { it.date },
    )
}
