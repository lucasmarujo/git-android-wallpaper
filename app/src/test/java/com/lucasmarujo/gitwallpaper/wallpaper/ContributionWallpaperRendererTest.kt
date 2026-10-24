package com.lucasmarujo.gitwallpaper.wallpaper

import android.content.Context
import android.graphics.Bitmap
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.lucasmarujo.gitwallpaper.domain.CellShape
import com.lucasmarujo.gitwallpaper.domain.ContributionLevel
import com.lucasmarujo.gitwallpaper.domain.ContributionPeriod
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
    private val size = ScreenSize(540, 1200)

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
    fun `cells are painted with the color of their contribution level`() {
        val settings = WallpaperSettings(theme = WallpaperTheme.GITHUB_DARK)
        val bitmap = render(settings)
        val palette = WallpaperPalette.of(settings.theme)

        ContributionLevel.entries.forEach { level ->
            val (x, y) = cellPoint(settings, level, fractionX = 0.5f, fractionY = 0.5f)
            assertEquals(level.name, palette.colorFor(level), bitmap.getPixel(x, y))
        }
    }

    @Test
    fun `circle cells leave their corners with the background color`() {
        val square = WallpaperSettings(cellShape = CellShape.ROUNDED_SQUARE, period = ContributionPeriod.THREE_MONTHS)
        val circle = square.copy(cellShape = CellShape.CIRCLE)
        val palette = WallpaperPalette.of(square.theme)
        val level = ContributionLevel.FOURTH_QUARTILE
        val largeSize = ScreenSize(1080, 2400)

        val (squareX, squareY) = cellPoint(square, level, CORNER_FRACTION, CORNER_FRACTION, largeSize)
        val (circleX, circleY) = cellPoint(circle, level, CORNER_FRACTION, CORNER_FRACTION, largeSize)

        assertEquals(palette.colorFor(level), render(square, largeSize).getPixel(squareX, squareY))
        assertEquals(palette.background, render(circle, largeSize).getPixel(circleX, circleY))
    }

    @Test
    fun `stats text is drawn only when enabled`() {
        val withStats = WallpaperSettings(showStats = true)
        val withoutStats = withStats.copy(showStats = false)
        val statsLayout = requireNotNull(layoutFor(withStats).stats)
        val background = WallpaperPalette.of(withStats.theme).background
        val statsTop = statsLayout.top.roundToInt()
        val statsBottom = (statsLayout.top + statsLayout.titleTextSize + 3 * statsLayout.lineHeight).roundToInt()
        val gridBottom = layoutFor(withoutStats).bottom.roundToInt() + 1

        val pixelsWithStats = countPixelsDifferentFrom(render(withStats), background, statsTop, statsBottom)
        val pixelsBelowGridWithoutStats =
            countPixelsDifferentFrom(render(withoutStats), background, gridBottom, size.height)

        assertTrue(pixelsWithStats > 0)
        assertEquals(0, pixelsBelowGridWithoutStats)
    }

    private fun render(settings: WallpaperSettings, screenSize: ScreenSize = size): Bitmap =
        renderer.render(calendar, settings, screenSize, TODAY)

    private fun layoutFor(settings: WallpaperSettings, screenSize: ScreenSize = size): WallpaperLayout =
        WallpaperLayout.calculate(
            width = screenSize.width,
            height = screenSize.height,
            weekCount = settings.period.weeks,
            verticalPosition = settings.verticalPosition,
            showStats = settings.showStats,
        )

    private fun cellPoint(
        settings: WallpaperSettings,
        level: ContributionLevel,
        fractionX: Float,
        fractionY: Float,
        screenSize: ScreenSize = size,
    ): Pair<Int, Int> {
        val grid = ContributionGrid.from(calendar.days, settings.period.weeks, TODAY)
        val layout = layoutFor(settings, screenSize)
        val cell = grid.cells.first { it.level == level }
        val segment = requireNotNull(layout.segmentFor(cell.column))
        val x = layout.cellLeft(segment, cell.column) + layout.cellSize * fractionX
        val y = layout.cellTop(segment, cell.row) + layout.cellSize * fractionY
        return x.roundToInt() to y.roundToInt()
    }

    private fun countPixelsDifferentFrom(bitmap: Bitmap, color: Int, top: Int, bottom: Int): Int {
        var count = 0
        for (y in top.coerceAtLeast(0) until bottom.coerceAtMost(bitmap.height)) {
            for (x in 0 until bitmap.width) {
                if (bitmap.getPixel(x, y) != color) count++
            }
        }
        return count
    }

    private companion object {
        const val CORNER_FRACTION = 0.105f
    }
}
