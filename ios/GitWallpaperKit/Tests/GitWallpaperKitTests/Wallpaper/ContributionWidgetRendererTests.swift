import CoreGraphics
import GitWallpaperKit
import XCTest

final class ContributionWidgetRendererTests: XCTestCase {
    private let renderer = ContributionWallpaperRenderer(strings: WallpaperStrings(localization: "en")!)
    private let calendar = TestCalendars.calendar()
    private let today = TestCalendars.today
    private let density: CGFloat = 3

    func testImageMatchesTheWidgetSizeAndFillsTheThemeBackground() throws {
        let size = ScreenSize(width: 1014, height: 474)

        let image = try render(WallpaperSettings(), size: size)

        XCTAssertEqual(size.width, image.width)
        XCTAssertEqual(size.height, image.height)
        XCTAssertEqual(WallpaperPalette.of(.githubDark).background, Pixels(image).color(1, 1))
    }

    func testWeekColumnsPutTodayInTheLastVisibleWeekOnItsWeekdayRow() throws {
        let size = ScreenSize(width: 516, height: 228)
        let settings = WallpaperSettings()
        let layout = try widgetLayout(settings, size: size)
        XCTAssertEqual(.weekColumns, layout.style)

        let todayRow = today.dayOfWeek % WidgetLayout.daysPerWeek
        let x = Int((layout.cellLeft(layout.columns - 1) - layout.cellGap * 0.5).rounded())
        let y = Int((layout.cellTop(todayRow) + layout.cellSize / 2).rounded())

        XCTAssertEqual(WallpaperPalette.of(settings.theme).accent, Pixels(try render(settings, size: size)).color(x, y))
    }

    func testWeekColumnsPaintPastDaysWithTheirLevel() throws {
        let size = ScreenSize(width: 516, height: 228)
        let settings = WallpaperSettings()
        let layout = try widgetLayout(settings, size: size)
        let yesterday = today.minusDays(1)
        let level = try XCTUnwrap(calendar.days.first { $0.date == yesterday }?.level)
        let row = yesterday.dayOfWeek % WidgetLayout.daysPerWeek

        let x = Int((layout.cellLeft(layout.columns - 1) + layout.cellSize / 2).rounded())
        let y = Int((layout.cellTop(row) + layout.cellSize / 2).rounded())

        XCTAssertEqual(WallpaperPalette.of(settings.theme).color(for: level), Pixels(try render(settings, size: size)).color(x, y))
    }

    func testDayRowsPaintTheWholeYearAndTheHeader() throws {
        let size = ScreenSize(width: 1014, height: 474)
        let settings = WallpaperSettings()
        let layout = try widgetLayout(settings, size: size)
        let header = try XCTUnwrap(layout.header)
        XCTAssertEqual(.dayRows, layout.style)
        XCTAssertEqual(365, layout.visibleCells)

        let pixels = Pixels(try render(settings, size: size))
        let accent = WallpaperPalette.of(settings.theme).accent
        let headerTop = Int(header.baseline - header.textSize)

        XCTAssertGreaterThan(pixels.count(top: headerTop, bottom: Int(header.baseline)) { $0 == accent }, 0)
    }

    func testMonochromePaletteLeavesTheBackgroundTransparent() throws {
        let size = ScreenSize(width: 516, height: 228)

        let image = try renderer.renderWidget(
            calendar: calendar,
            settings: WallpaperSettings(),
            palette: .monochrome,
            size: size,
            density: density,
            today: today
        )

        XCTAssertEqual(0, Pixels(image).color(1, 1).alpha)
    }

    func testCustomCommitColorIsUsedOnTheWidget() throws {
        let size = ScreenSize(width: 516, height: 228)
        let commitColor = ARGBColor(argb: 0xFF8A_2BE2)
        let settings = WallpaperSettings(commitColor: commitColor)
        let layout = try widgetLayout(settings, size: size)
        let yesterday = today.minusDays(1)
        XCTAssertEqual(.fourthQuartile, calendar.days.first { $0.date == yesterday }?.level)
        XCTAssertEqual(today.previousOrSameSunday, yesterday.previousOrSameSunday)
        let row = yesterday.dayOfWeek % WidgetLayout.daysPerWeek

        let x = Int((layout.cellLeft(layout.columns - 1) + layout.cellSize / 2).rounded())
        let y = Int((layout.cellTop(row) + layout.cellSize / 2).rounded())

        XCTAssertEqual(commitColor, Pixels(try render(settings, size: size)).color(x, y))
    }

    private func render(_ settings: WallpaperSettings, size: ScreenSize) throws -> CGImage {
        try renderer.renderWidget(
            calendar: calendar,
            settings: settings,
            palette: WallpaperPalette.of(settings.theme).withCommitColor(settings.commitColor),
            size: size,
            density: density,
            today: today
        )
    }

    private func widgetLayout(_ settings: WallpaperSettings, size: ScreenSize) throws -> WidgetLayout {
        let grid = DayGrid.from(calendar.days, period: settings.period, today: today)
        let firstWeekStart = grid.startDate.previousOrSameSunday
        let weekCount = firstWeekStart.daysUntil(grid.cells[grid.cells.count - 1].date) / WidgetLayout.daysPerWeek + 1
        return try WidgetLayout.calculate(
            width: size.width,
            height: size.height,
            density: density,
            weekCount: weekCount,
            dayCount: grid.cells.count,
            showHeader: settings.showStats
        )
    }
}
