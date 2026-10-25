package com.lucasmarujo.gitwallpaper.ui.home

import androidx.annotation.StringRes
import com.lucasmarujo.gitwallpaper.R
import com.lucasmarujo.gitwallpaper.domain.CellShape
import com.lucasmarujo.gitwallpaper.domain.ContributionPeriod
import com.lucasmarujo.gitwallpaper.domain.UpdateInterval
import com.lucasmarujo.gitwallpaper.domain.WallpaperTarget
import com.lucasmarujo.gitwallpaper.domain.WallpaperTheme

@StringRes
fun WallpaperTarget.labelRes(): Int = when (this) {
    WallpaperTarget.LOCK_SCREEN -> R.string.target_lock_screen
    WallpaperTarget.HOME_SCREEN -> R.string.target_home_screen
    WallpaperTarget.BOTH -> R.string.target_both
}

@StringRes
fun WallpaperTheme.labelRes(): Int = when (this) {
    WallpaperTheme.GITHUB_DARK -> R.string.theme_github_dark
    WallpaperTheme.GITHUB_LIGHT -> R.string.theme_github_light
    WallpaperTheme.AMOLED -> R.string.theme_amoled
    WallpaperTheme.OCEAN -> R.string.theme_ocean
    WallpaperTheme.SUNSET -> R.string.theme_sunset
}

@StringRes
fun ContributionPeriod.labelRes(): Int = when (this) {
    ContributionPeriod.CURRENT_YEAR -> R.string.period_current_year
    ContributionPeriod.LAST_12_MONTHS -> R.string.period_last_12_months
}

@StringRes
fun CellShape.labelRes(): Int = when (this) {
    CellShape.ROUNDED_SQUARE -> R.string.shape_rounded_square
    CellShape.CIRCLE -> R.string.shape_circle
}

@StringRes
fun UpdateInterval.labelRes(): Int = when (this) {
    UpdateInterval.THIRTY_MINUTES -> R.string.interval_thirty_minutes
    UpdateInterval.ONE_HOUR -> R.string.interval_one_hour
    UpdateInterval.THREE_HOURS -> R.string.interval_three_hours
    UpdateInterval.SIX_HOURS -> R.string.interval_six_hours
    UpdateInterval.TWELVE_HOURS -> R.string.interval_twelve_hours
}

@StringRes
fun HomeMessage.textRes(): Int = when (this) {
    HomeMessage.APPLIED -> R.string.message_applied
    HomeMessage.REFRESHED -> R.string.message_refreshed
    HomeMessage.NETWORK_ERROR -> R.string.message_network_error
    HomeMessage.GITHUB_ERROR -> R.string.message_github_error
    HomeMessage.WALLPAPER_ERROR -> R.string.message_wallpaper_error
    HomeMessage.SESSION_EXPIRED -> R.string.message_session_expired
}
