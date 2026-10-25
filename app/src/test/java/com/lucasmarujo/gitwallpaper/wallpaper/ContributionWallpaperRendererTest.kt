package com.lucasmarujo.gitwallpaper.wallpaper

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import androidx.core.graphics.createBitmap
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.lucasmarujo.gitwallpaper.domain.CellShape
import com.lucasmarujo.gitwallpaper.domain.ContributionLevel
import com.lucasmarujo.gitwallpaper.domain.WallpaperFraming
import com.lucasmarujo.gitwallpaper.domain.WallpaperSettings
import com.lucasmarujo.gitwallpaper.domain.WallpaperTheme
import com.lucasmarujo.gitwallpaper.testing.TestCalendars
import com.lucasmarujo.gitwallpaper.testing.TestCalendars.TODAY
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.GraphicsMode
import kotlin.math.roundToInt

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ContributionWallpaperRendererTest {

    private val renderer = ContributionWallpaperRenderer(ApplicationProvider.getApplicationContext<Context>().resources)
    private val calendar = TestCalendars.calendar(endDate = TODAY)
    private val size = ScreenSize(1080, 2340)

    @Test
    fun `bitmap matches the requested screen size`() {
        val bitmap = render(WallpaperSettings())

        assertEquals(size.width, bitmap.width)
        assertEquals(size.height, bitmap.height)
    }

    @Test
    fun `background uses the selected theme color`() {
        WallpaperTheme.entries.forEach { theme ->
            val bitmap = render(WallpaperSettings(theme = theme))

            assertEquals(theme.name, WallpaperPalette.of(theme).background, bitmap.getPixel(2, 2))
        }
    }

    @Test
    fun `past days are painted with the color of their contribution level`() {
        val settings = WallpaperSettings()
        val bitmap = render(settings)
        val palette = WallpaperPalette.of(settings.theme)
        val grid = grid(settings)

        ContributionLevel.entries.forEach { level ->
            val index = grid.cells.indexOfFirst { it.state == DayState.PAST && it.level == level }
            val (x, y) = cellPoint(settings, index, 0.5f, 0.5f)
            assertEquals(level.name, palette.colorFor(level), bitmap.getPixel(x, y))
        }
    }

    @Test
    fun `future days are dimmed`() {
        val settings = WallpaperSettings()
        val index = grid(settings).cells.indexOfFirst { it.state == DayState.FUTURE }

        val (x, y) = cellPoint(settings, index, 0.5f, 0.5f)

        assertEquals(WallpaperPalette.of(settings.theme).future, render(settings).getPixel(x, y))
    }

    @Test
    fun `today is highlighted with an accent ring`() {
        val settings = WallpaperSettings()
        val layout = layoutFor(settings)
        val index = grid(settings).cells.indexOfFirst { it.state == DayState.TODAY }

        val x = (layout.cellLeft(index) - layout.cellGap * 0.5f).roundToInt()
        val y = (layout.cellTop(index) + layout.cellSize / 2f).roundToInt()

        assertEquals(WallpaperPalette.of(settings.theme).accent, render(settings).getPixel(x, y))
    }

    @Test
    fun `circle cells leave their corners with the background color`() {
        val square = WallpaperSettings(cellShape = CellShape.ROUNDED_SQUARE)
        val circle = square.copy(cellShape = CellShape.CIRCLE)
        val palette = WallpaperPalette.of(square.theme)
        val level = ContributionLevel.FOURTH_QUARTILE
        val index = grid(square).cells.indexOfFirst { it.state == DayState.PAST && it.level == level }

        val (x, y) = cellPoint(square, index, CORNER_FRACTION, CORNER_FRACTION)

        assertEquals(palette.colorFor(level), render(square).getPixel(x, y))
        assertEquals(palette.background, render(circle).getPixel(x, y))
    }

    @Test
    fun `stats text is drawn below the grid only when enabled`() {
        val withStats = WallpaperSettings(showStats = true)
        val withoutStats = withStats.copy(showStats = false)
        val background = WallpaperPalette.of(withStats.theme).background
        val belowGridWithStats = layoutFor(withStats).let { (it.gridTop + it.gridHeight).roundToInt() + 2 }
        val belowGridWithoutStats = layoutFor(withoutStats).let { (it.gridTop + it.gridHeight).roundToInt() + 2 }

        assertTrue(countPixelsDifferentFrom(render(withStats), background, belowGridWithStats, size.height) > 0)
        assertEquals(0, countPixelsDifferentFrom(render(withoutStats), background, belowGridWithoutStats, size.height))
    }

    @Test
    fun `avatar is drawn next to the login when available`() {
        val settings = WallpaperSettings()
        val avatar = createBitmap(AVATAR_SIZE, AVATAR_SIZE).apply { eraseColor(Color.RED) }
        val stats = requireNotNull(layoutFor(settings).stats)
        val top = (stats.userRowCenterY - stats.avatarSize).roundToInt()
        val bottom = (stats.userRowCenterY + stats.avatarSize).roundToInt()

        val withAvatar = renderer.render(calendar, settings, size, TODAY, avatar)
        val withoutAvatar = render(settings)

        assertTrue(countPixels(withAvatar, Color.RED, top, bottom) > 0)
        assertEquals(0, countPixels(withoutAvatar, Color.RED, top, bottom))
    }

    @Test
    fun `custom commit color paints the busiest days and keeps the theme background`() {
        val commitColor = 0xFF8A2BE2.toInt()
        val settings = WallpaperSettings(theme = WallpaperTheme.OCEAN, commitColor = commitColor)
        val level = ContributionLevel.FOURTH_QUARTILE
        val index = grid(settings).cells.indexOfFirst { it.state == DayState.PAST && it.level == level }
        val (x, y) = cellPoint(settings, index, 0.5f, 0.5f)

        val bitmap = render(settings)

        assertEquals(commitColor, bitmap.getPixel(x, y))
        assertEquals(WallpaperPalette.of(WallpaperTheme.OCEAN).background, bitmap.getPixel(2, 2))
    }

    @Test
    fun `framing moves the graph to the chosen position`() {
        val settings = WallpaperSettings()
        val framed = settings.copy(framing = WallpaperFraming(offsetX = 0.1f, offsetY = -0.2f))
        val palette = WallpaperPalette.of(settings.theme)
        val level = ContributionLevel.FOURTH_QUARTILE
        val index = grid(settings).cells.indexOfFirst { it.state == DayState.PAST && it.level == level }
        val (x, y) = cellPoint(settings, index, 0.5f, 0.5f)

        val movedX = x + (size.width * 0.1f).roundToInt()
        val movedY = y - (size.height * 0.2f).roundToInt()

        assertEquals(palette.colorFor(level), render(framed).getPixel(movedX, movedY))
    }

    @Test
    fun `framing zooms the graph around the screen center`() {
        val settings = WallpaperSettings()
        val zoom = 1.5f
        val framed = settings.copy(framing = WallpaperFraming(zoom = zoom))
        val palette = WallpaperPalette.of(settings.theme)
        val level = ContributionLevel.FOURTH_QUARTILE
        val index = grid(settings).cells.indexOfFirst { it.state == DayState.PAST && it.level == level }
        val (x, y) = cellPoint(settings, index, 0.5f, 0.5f)
        val centerX = size.width / 2f
        val centerY = size.height / 2f

        val zoomedX = (centerX + (x - centerX) * zoom).roundToInt()
        val zoomedY = (centerY + (y - centerY) * zoom).roundToInt()

        val bitmap = render(framed)
        val outsideZoomedGrid = (centerX + (layoutFor(settings).gridLeft - centerX) * zoom - 2f).roundToInt()
        assertEquals(palette.colorFor(level), bitmap.getPixel(zoomedX, zoomedY))
        assertEquals(palette.background, bitmap.getPixel(outsideZoomedGrid, zoomedY))
    }

    @Test
    fun `framing keeps the background filling the whole screen`() {
        val settings = WallpaperSettings(framing = WallpaperFraming(zoom = WallpaperFraming.MIN_ZOOM, offsetY = 0.5f))
        val bitmap = render(settings)
        val background = WallpaperPalette.of(settings.theme).background

        assertEquals(background, bitmap.getPixel(2, 2))
        assertEquals(background, bitmap.getPixel(size.width - 2, size.height - 2))
    }

    private fun render(settings: WallpaperSettings): Bitmap = renderer.render(calendar, settings, size, TODAY, null)

    private fun grid(settings: WallpaperSettings): DayGrid = DayGrid.from(calendar.days, settings.period, TODAY)

    private fun layoutFor(settings: WallpaperSettings): WallpaperLayout =
        WallpaperLayout.calculate(size.width, size.height, grid(settings).cells.size, settings.showStats)

    private fun cellPoint(settings: WallpaperSettings, index: Int, fractionX: Float, fractionY: Float): Pair<Int, Int> {
        val layout = layoutFor(settings)
        val x = layout.cellLeft(index) + layout.cellSize * fractionX
        val y = layout.cellTop(index) + layout.cellSize * fractionY
        return x.roundToInt() to y.roundToInt()
    }

    private fun countPixelsDifferentFrom(bitmap: Bitmap, color: Int, top: Int, bottom: Int): Int =
        countMatching(bitmap, top, bottom) { it != color }

    private fun countPixels(bitmap: Bitmap, color: Int, top: Int, bottom: Int): Int =
        countMatching(bitmap, top, bottom) { it == color }

    private fun countMatching(bitmap: Bitmap, top: Int, bottom: Int, predicate: (Int) -> Boolean): Int {
        var count = 0
        for (y in top.coerceAtLeast(0) until bottom.coerceAtMost(bitmap.height)) {
            for (x in 0 until bitmap.width) {
                if (predicate(bitmap.getPixel(x, y))) count++
            }
        }
        return count
    }

    private companion object {
        const val CORNER_FRACTION = 0.105f
        const val AVATAR_SIZE = 64
    }
}
