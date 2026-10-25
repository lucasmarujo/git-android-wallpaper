package com.lucasmarujo.gitwallpaper.wallpaper

import androidx.annotation.ColorInt
import androidx.core.graphics.ColorUtils
import com.lucasmarujo.gitwallpaper.domain.ContributionLevel
import com.lucasmarujo.gitwallpaper.domain.WallpaperTheme

data class WallpaperPalette(
    @ColorInt val background: Int,
    @ColorInt val empty: Int,
    @ColorInt val future: Int,
    @ColorInt val level1: Int,
    @ColorInt val level2: Int,
    @ColorInt val level3: Int,
    @ColorInt val level4: Int,
    @ColorInt val accent: Int,
    @ColorInt val primaryText: Int,
    @ColorInt val secondaryText: Int,
) {
    @ColorInt
    fun colorFor(level: ContributionLevel): Int = when (level) {
        ContributionLevel.NONE -> empty
        ContributionLevel.FIRST_QUARTILE -> level1
        ContributionLevel.SECOND_QUARTILE -> level2
        ContributionLevel.THIRD_QUARTILE -> level3
        ContributionLevel.FOURTH_QUARTILE -> level4
    }

    fun withCommitColor(@ColorInt commitColor: Int?): WallpaperPalette {
        if (commitColor == null) return this
        val opaque = commitColor or OPAQUE_ALPHA
        return copy(
            level1 = ColorUtils.blendARGB(empty, opaque, FIRST_QUARTILE_RATIO),
            level2 = ColorUtils.blendARGB(empty, opaque, SECOND_QUARTILE_RATIO),
            level3 = ColorUtils.blendARGB(empty, opaque, THIRD_QUARTILE_RATIO),
            level4 = opaque,
            accent = opaque,
        )
    }

    companion object {
        private const val OPAQUE_ALPHA = 0xFF000000.toInt()
        private const val FIRST_QUARTILE_RATIO = 0.3f
        private const val SECOND_QUARTILE_RATIO = 0.55f
        private const val THIRD_QUARTILE_RATIO = 0.8f

        fun of(theme: WallpaperTheme): WallpaperPalette = when (theme) {
            WallpaperTheme.GITHUB_DARK -> GITHUB_DARK
            WallpaperTheme.GITHUB_LIGHT -> GITHUB_LIGHT
            WallpaperTheme.AMOLED -> AMOLED
            WallpaperTheme.OCEAN -> OCEAN
            WallpaperTheme.SUNSET -> SUNSET
        }

        private val GITHUB_DARK = WallpaperPalette(
            background = 0xFF0D1117.toInt(),
            empty = 0xFF1A2029.toInt(),
            future = 0xFF12171E.toInt(),
            level1 = 0xFF0E4429.toInt(),
            level2 = 0xFF006D32.toInt(),
            level3 = 0xFF26A641.toInt(),
            level4 = 0xFF39D353.toInt(),
            accent = 0xFF39D353.toInt(),
            primaryText = 0xFFE6EDF3.toInt(),
            secondaryText = 0xFF8B949E.toInt(),
        )

        private val GITHUB_LIGHT = WallpaperPalette(
            background = 0xFFFFFFFF.toInt(),
            empty = 0xFFEBEDF0.toInt(),
            future = 0xFFF6F7F9.toInt(),
            level1 = 0xFF9BE9A8.toInt(),
            level2 = 0xFF40C463.toInt(),
            level3 = 0xFF30A14E.toInt(),
            level4 = 0xFF216E39.toInt(),
            accent = 0xFF1A7F37.toInt(),
            primaryText = 0xFF1F2328.toInt(),
            secondaryText = 0xFF656D76.toInt(),
        )

        private val AMOLED = WallpaperPalette(
            background = 0xFF000000.toInt(),
            empty = 0xFF161616.toInt(),
            future = 0xFF0B0B0B.toInt(),
            level1 = 0xFF0E4429.toInt(),
            level2 = 0xFF006D32.toInt(),
            level3 = 0xFF26A641.toInt(),
            level4 = 0xFF39D353.toInt(),
            accent = 0xFF39D353.toInt(),
            primaryText = 0xFFF0F6FC.toInt(),
            secondaryText = 0xFF8B949E.toInt(),
        )

        private val OCEAN = WallpaperPalette(
            background = 0xFF0A1628.toInt(),
            empty = 0xFF15263F.toInt(),
            future = 0xFF0F1D33.toInt(),
            level1 = 0xFF0C4A6E.toInt(),
            level2 = 0xFF0369A1.toInt(),
            level3 = 0xFF0EA5E9.toInt(),
            level4 = 0xFF7DD3FC.toInt(),
            accent = 0xFF7DD3FC.toInt(),
            primaryText = 0xFFE0F2FE.toInt(),
            secondaryText = 0xFF7896B4.toInt(),
        )

        private val SUNSET = WallpaperPalette(
            background = 0xFF1B1020.toInt(),
            empty = 0xFF2C1C32.toInt(),
            future = 0xFF221627.toInt(),
            level1 = 0xFF5B2150.toInt(),
            level2 = 0xFF9D3462.toInt(),
            level3 = 0xFFE0565B.toInt(),
            level4 = 0xFFFFB267.toInt(),
            accent = 0xFFFFB267.toInt(),
            primaryText = 0xFFFFEFE6.toInt(),
            secondaryText = 0xFFB08E9E.toInt(),
        )
    }
}
