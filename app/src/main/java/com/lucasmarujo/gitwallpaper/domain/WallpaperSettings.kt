package com.lucasmarujo.gitwallpaper.domain

data class WallpaperSettings(
    val target: WallpaperTarget = WallpaperTarget.LOCK_SCREEN,
    val theme: WallpaperTheme = WallpaperTheme.GITHUB_DARK,
    val period: ContributionPeriod = ContributionPeriod.CURRENT_YEAR,
    val cellShape: CellShape = CellShape.ROUNDED_SQUARE,
    val showStats: Boolean = true,
    val autoUpdate: Boolean = true,
    val updateInterval: UpdateInterval = UpdateInterval.ONE_HOUR,
)
