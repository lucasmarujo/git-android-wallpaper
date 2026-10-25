package com.lucasmarujo.gitwallpaper.wallpaper

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.lucasmarujo.gitwallpaper.domain.ContributionLevel
import com.lucasmarujo.gitwallpaper.domain.ContributionPeriod
import com.lucasmarujo.gitwallpaper.domain.WallpaperSettings
import com.lucasmarujo.gitwallpaper.domain.WallpaperTheme
import com.lucasmarujo.gitwallpaper.testing.TestCalendars
import com.lucasmarujo.gitwallpaper.testing.TestCalendars.TODAY
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.GraphicsMode
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters
import kotlin.math.roundToInt

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ContributionWidgetRendererTest {

    private val renderer = ContributionWallpaperRenderer(ApplicationProvider.getApplicationContext<Context>().resources)
    private val calendar = TestCalendars.calendar(endDate = TODAY)

    @Test
    fun `bitmap matches the widget size with rounded transparent corners`() {
        val bitmap = render(WallpaperSettings(), SHORT_SIZE)

        assertEquals(SHORT_SIZE.width, bitmap.width)
        assertEquals(SHORT_SIZE.height, bitmap.height)
        assertEquals(Color.TRANSPARENT, bitmap.getPixel(0, 0))
        assertEquals(WallpaperPalette.of(WallpaperTheme.GITHUB_DARK).background, bitmap.getPixel(SHORT_SIZE.width / 2, 2))
    }

    @Test
    fun `background follows the selected theme`() {
        WallpaperTheme.entries.forEach { theme ->
            val bitmap = render(WallpaperSettings(theme = theme), SHORT_SIZE)

            assertEquals(theme.name, WallpaperPalette.of(theme).background, bitmap.getPixel(SHORT_SIZE.width / 2, 2))
        }
    }

    @Test
    fun `short widgets draw week columns and tall widgets draw the wallpaper day grid`() {
        assertEquals(WidgetGridStyle.WEEK_COLUMNS, placement(WallpaperSettings(), SHORT_SIZE).layout.style)
        assertEquals(WidgetGridStyle.DAY_ROWS, placement(WallpaperSettings(), TALL_SIZE).layout.style)
    }

    @Test
    fun `tall widgets show the whole year from january first`() {
        val placement = placement(WallpaperSettings(), TALL_SIZE)

        assertTrue(placement.isVisible(LocalDate.of(2026, 1, 1)))
        assertTrue(placement.isVisible(LocalDate.of(2026, 12, 31)))
        assertEquals(placement.layout.gridLeft, placement.left(LocalDate.of(2026, 1, 1)))
        assertEquals(placement.layout.gridTop, placement.top(LocalDate.of(2026, 1, 1)))
    }

    @Test
    fun `visible days are painted with the color of their contribution level in both styles`() {
        val settings = WallpaperSettings()
        val palette = WallpaperPalette.of(settings.theme)
        listOf(SHORT_SIZE, TALL_SIZE).forEach { size ->
            val bitmap = render(settings, size)
            val placement = placement(settings, size)

            ContributionLevel.entries.forEach { level ->
                val cell = placement.grid.cells.first {
                    it.state == DayState.PAST && it.level == level && placement.isVisible(it.date)
                }
                val (x, y) = placement.center(cell.date)
                assertEquals("$size $level", palette.colorFor(level), bitmap.getPixel(x, y))
            }
        }
    }

    @Test
    fun `future days are dimmed in the day grid`() {
        val settings = WallpaperSettings()
        val placement = placement(settings, TALL_SIZE)
        val future = placement.grid.cells.first { it.state == DayState.FUTURE }

        val (x, y) = placement.center(future.date)

        assertEquals(WallpaperPalette.of(settings.theme).future, render(settings, TALL_SIZE).getPixel(x, y))
    }

    @Test
    fun `the current week stays visible at any widget size`() {
        listOf(ScreenSize(80, 80), ScreenSize(220, 80), ScreenSize(80, 500), SHORT_SIZE, TALL_SIZE).forEach { size ->
            assertTrue(size.toString(), placement(WallpaperSettings(), size).isVisible(TODAY))
        }
    }

    @Test
    fun `today is highlighted with an accent ring in both styles`() {
        val settings = WallpaperSettings()
        listOf(SHORT_SIZE, TALL_SIZE).forEach { size ->
            val sharpSize = ScreenSize(size.width * 2, size.height * 2)
            val placement = placement(settings, sharpSize, SHARP_DENSITY)
            val bitmap = renderer.renderWidget(calendar, settings, sharpSize, SHARP_DENSITY, TODAY)

            val ringX = (placement.left(TODAY) - placement.layout.cellGap * 0.5f).roundToInt()
            val ringY = (placement.top(TODAY) + placement.layout.cellSize / 2f).roundToInt()

            assertEquals(sharpSize.toString(), WallpaperPalette.of(settings.theme).accent, bitmap.getPixel(ringX, ringY))
        }
    }

    @Test
    fun `a custom commit color recolors the busiest days`() {
        val commitColor = 0xFFFF0080.toInt()
        val settings = WallpaperSettings(commitColor = commitColor)
        val placement = placement(settings, TALL_SIZE)
        val cell = placement.grid.cells.first {
            it.state == DayState.PAST && it.level == ContributionLevel.FOURTH_QUARTILE && placement.isVisible(it.date)
        }

        val (x, y) = placement.center(cell.date)

        assertEquals(commitColor, render(settings, TALL_SIZE).getPixel(x, y))
    }

    @Test
    fun `stats can be hidden from the widget`() {
        val withStats = render(WallpaperSettings(showStats = true), TALL_SIZE)
        val withoutStats = render(WallpaperSettings(showStats = false), TALL_SIZE)

        assertFalse(withStats.sameAs(withoutStats))
        assertNull(placement(WallpaperSettings(showStats = false), TALL_SIZE).layout.header)
    }

    @Test
    fun `the last 12 months period ends at today`() {
        val settings = WallpaperSettings(period = ContributionPeriod.LAST_12_MONTHS)
        listOf(ScreenSize(2400, 220), TALL_SIZE).forEach { size ->
            val placement = placement(settings, size)

            assertEquals(TODAY, placement.grid.cells.last().date)
            assertTrue(size.toString(), placement.isVisible(TODAY))
            assertTrue(size.toString(), placement.isVisible(placement.grid.startDate))
        }
    }

    private fun render(settings: WallpaperSettings, size: ScreenSize): Bitmap =
        renderer.renderWidget(calendar, settings, size, DENSITY, TODAY)

    private fun placement(settings: WallpaperSettings, size: ScreenSize, density: Float = DENSITY): Placement {
        val grid = DayGrid.from(calendar.days, settings.period, TODAY)
        val firstWeekStart = grid.startDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY))
        val weekCount = weekIndex(firstWeekStart, grid.cells.last().date) + 1
        val layout = WidgetLayout.calculate(size.width, size.height, density, weekCount, grid.cells.size, settings.showStats)
        val firstVisible = when (layout.style) {
            WidgetGridStyle.WEEK_COLUMNS ->
                WidgetLayout.firstVisible(weekCount, weekIndex(firstWeekStart, grid.today), layout.visibleCells)
            WidgetGridStyle.DAY_ROWS ->
                WidgetLayout.firstVisible(grid.cells.size, dayIndex(grid.startDate, grid.today), layout.visibleCells)
        }
        return Placement(grid, layout, firstWeekStart, firstVisible)
    }

    private class Placement(
        val grid: DayGrid,
        val layout: WidgetLayout,
        private val firstWeekStart: LocalDate,
        private val firstVisible: Int,
    ) {
        fun isVisible(date: LocalDate): Boolean = slot(date) in 0 until layout.visibleCells

        fun left(date: LocalDate): Float = when (layout.style) {
            WidgetGridStyle.WEEK_COLUMNS -> layout.cellLeft(slot(date))
            WidgetGridStyle.DAY_ROWS -> layout.cellLeft(slot(date) % layout.columns)
        }

        fun top(date: LocalDate): Float = when (layout.style) {
            WidgetGridStyle.WEEK_COLUMNS -> layout.cellTop(date.dayOfWeek.value % WidgetLayout.DAYS_PER_WEEK)
            WidgetGridStyle.DAY_ROWS -> layout.cellTop(slot(date) / layout.columns)
        }

        fun center(date: LocalDate): Pair<Int, Int> =
            (left(date) + layout.cellSize / 2f).roundToInt() to (top(date) + layout.cellSize / 2f).roundToInt()

        private fun slot(date: LocalDate): Int = when (layout.style) {
            WidgetGridStyle.WEEK_COLUMNS -> weekIndex(firstWeekStart, date) - firstVisible
            WidgetGridStyle.DAY_ROWS -> dayIndex(grid.startDate, date) - firstVisible
        }
    }

    private companion object {
        const val DENSITY = 2f
        const val SHARP_DENSITY = 4f
        val SHORT_SIZE = ScreenSize(500, 160)
        val TALL_SIZE = ScreenSize(692, 410)

        fun weekIndex(firstWeekStart: LocalDate, date: LocalDate): Int =
            (ChronoUnit.DAYS.between(firstWeekStart, date) / WidgetLayout.DAYS_PER_WEEK).toInt()

        fun dayIndex(startDate: LocalDate, date: LocalDate): Int = ChronoUnit.DAYS.between(startDate, date).toInt()
    }
}
