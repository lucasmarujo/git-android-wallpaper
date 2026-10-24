package com.lucasmarujo.gitwallpaper.domain

data class WallpaperSettings(
    val target: WallpaperTarget = WallpaperTarget.LOCK_SCREEN,
    val theme: WallpaperTheme = WallpaperTheme.GITHUB_DARK,
    val period: ContributionPeriod = ContributionPeriod.TWELVE_MONTHS,
    val cellShape: CellShape = CellShape.ROUNDED_SQUARE,
    val verticalPosition: Float = DEFAULT_VERTICAL_POSITION,
    val showStats: Boolean = true,
    val autoUpdate: Boolean = true,
    val updateInterval: UpdateInterval = UpdateInterval.ONE_HOUR,
) {
    companion object {
        const val MIN_VERTICAL_POSITION = 0.25f
        const val MAX_VERTICAL_POSITION = 0.8f
        const val DEFAULT_VERTICAL_POSITION = 0.55f
    }
}
