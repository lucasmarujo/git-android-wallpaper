package com.lucasmarujo.gitwallpaper.data.contributions

import com.lucasmarujo.gitwallpaper.domain.ContributionCalendar
import com.lucasmarujo.gitwallpaper.domain.SyncError

sealed interface RefreshResult {
    data class Success(val calendar: ContributionCalendar) : RefreshResult

    data class Failure(val error: SyncError) : RefreshResult
}
