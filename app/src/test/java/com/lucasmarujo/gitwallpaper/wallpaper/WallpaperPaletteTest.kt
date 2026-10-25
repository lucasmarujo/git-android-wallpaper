package com.lucasmarujo.gitwallpaper.wallpaper

import android.graphics.Color
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.lucasmarujo.gitwallpaper.domain.WallpaperTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class WallpaperPaletteTest {

    private val theme = WallpaperPalette.of(WallpaperTheme.GITHUB_DARK)

    @Test
    fun `without a custom color the theme palette is kept`() {
        assertSame(theme, theme.withCommitColor(null))
    }

    @Test
    fun `custom color drives the busiest level and the accent`() {
        val color = 0xFF8A2BE2.toInt()

        val palette = theme.withCommitColor(color)

        assertEquals(color, palette.level4)
        assertEquals(color, palette.accent)
    }

    @Test
    fun `custom color keeps the theme background, empty days and text`() {
        val palette = theme.withCommitColor(0xFF8A2BE2.toInt())

        assertEquals(theme.background, palette.background)
        assertEquals(theme.empty, palette.empty)
        assertEquals(theme.future, palette.future)
        assertEquals(theme.primaryText, palette.primaryText)
        assertEquals(theme.secondaryText, palette.secondaryText)
    }

    @Test
    fun `lower levels fade from the empty color towards the custom color`() {
        val color = 0xFFFF0000.toInt()

        val palette = theme.withCommitColor(color)

        val reds = listOf(theme.empty, palette.level1, palette.level2, palette.level3, palette.level4).map(Color::red)
        assertEquals(reds.sorted(), reds)
        assertEquals(reds.size, reds.toSet().size)
    }

    @Test
    fun `transparent colors are drawn opaque`() {
        val palette = theme.withCommitColor(0x0000FF00)

        assertEquals(0xFF00FF00.toInt(), palette.level4)
        assertTrue(listOf(palette.level1, palette.level2, palette.level3).all { Color.alpha(it) == 255 })
    }
}
