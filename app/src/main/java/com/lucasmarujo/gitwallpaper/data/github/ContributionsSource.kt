package com.lucasmarujo.gitwallpaper.data.github

import com.lucasmarujo.gitwallpaper.domain.ContributionCalendar

fun interface ContributionsSource {
    suspend fun fetchCalendar(token: String): ContributionCalendar
}
