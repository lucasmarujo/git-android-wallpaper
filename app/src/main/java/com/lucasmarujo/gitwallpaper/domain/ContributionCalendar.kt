package com.lucasmarujo.gitwallpaper.domain

import kotlinx.serialization.Serializable
import java.time.LocalDate

@Serializable
data class ContributionDay(
    @Serializable(with = LocalDateSerializer::class)
    val date: LocalDate,
    val count: Int,
    val level: ContributionLevel,
)

@Serializable
data class ContributionCalendar(
    val login: String,
    val totalContributions: Int,
    val days: List<ContributionDay>,
    val avatarUrl: String? = null,
)
