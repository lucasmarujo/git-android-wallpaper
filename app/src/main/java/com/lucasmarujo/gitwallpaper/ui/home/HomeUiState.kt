package com.lucasmarujo.gitwallpaper.ui.home

import androidx.compose.ui.graphics.ImageBitmap
import com.lucasmarujo.gitwallpaper.domain.ContributionStats
import com.lucasmarujo.gitwallpaper.domain.WallpaperSettings

data class HomeUiState(
    val login: String,
    val settings: WallpaperSettings = WallpaperSettings(),
    val preview: ImageBitmap? = null,
    val previewAspectRatio: Float = DEFAULT_PREVIEW_ASPECT_RATIO,
    val stats: ContributionStats? = null,
    val hasCalendar: Boolean = false,
    val isLoadingCalendar: Boolean = true,
    val lastAppliedAtMillis: Long? = null,
    val isApplying: Boolean = false,
    val isRefreshing: Boolean = false,
) {
    companion object {
        const val DEFAULT_PREVIEW_ASPECT_RATIO = 9f / 20f
    }
}

enum class HomeMessage {
    APPLIED,
    REFRESHED,
    NETWORK_ERROR,
    GITHUB_ERROR,
    WALLPAPER_ERROR,
    SESSION_EXPIRED,
}
