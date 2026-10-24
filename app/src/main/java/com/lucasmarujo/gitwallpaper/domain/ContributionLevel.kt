package com.lucasmarujo.gitwallpaper.domain

import kotlinx.serialization.Serializable

@Serializable
enum class ContributionLevel {
    NONE,
    FIRST_QUARTILE,
    SECOND_QUARTILE,
    THIRD_QUARTILE,
    FOURTH_QUARTILE,
}
