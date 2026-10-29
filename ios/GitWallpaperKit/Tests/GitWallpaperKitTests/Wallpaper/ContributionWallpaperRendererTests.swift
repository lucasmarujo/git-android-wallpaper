import CoreGraphics
import GitWallpaperKit
import XCTest

final class ContributionWallpaperRendererTests: XCTestCase {
    private let renderer = ContributionWallpaperRenderer(strings: WallpaperStrings(localization: "en")!)
    private let calendar = TestCalendars.calendar()
    private let size = ScreenSize(width: 1080, height: 2340)
    private let today = TestCalendars.today
    private let cornerFraction: CGFloat = 0.105

    func testImageMatchesTheRequestedScreenSize() throws {
        let image = try render(WallpaperSettings())

        XCTAssertEqual(size.width, image.width)
        XCTAssertEqual(size.height, image.height)
    }

    func testBackgroundUsesTheSelectedThemeColor() throws {
        for theme in WallpaperTheme.allCases {
            let pixels = Pixels(try render(WallpaperSettings(theme: theme)))

            XCTAssertEqual(WallpaperPalette.of(theme).background, pixels.color(2, 2), theme.rawValue)
        }
    }

    func testPastDaysArePaintedWithTheColorOfTheirContributionLevel() throws {
        let settings = WallpaperSettings()
        let pixels = Pixels(try render(settings))
        let palette = WallpaperPalette.of(settings.theme)

        for level in ContributionLevel.allCases {
            let index = try XCTUnwrap(grid(settings).cells.firstIndex { $0.state == .past && $0.level == level })
            let point = try cellPoint(settings, index, 0.5, 0.5)
            XCTAssertEqual(palette.color(for: level), pixels.color(point.x, point.y), level.rawValue)
        }
    }

    func testFutureDaysAreDimmed() throws {
        let settings = WallpaperSettings()
        let index = try XCTUnwrap(grid(settings).cells.firstIndex { $0.state == .future })

        let point = try cellPoint(settings, index, 0.5, 0.5)

        XCTAssertEqual(WallpaperPalette.of(settings.theme).future, Pixels(try render(settings)).color(point.x, point.y))
    }

    func testTodayIsHighlightedWithAnAccentRing() throws {
        let settings = WallpaperSettings()
        let layout = try layoutFor(settings)
        let index = try XCTUnwrap(grid(settings).cells.firstIndex { $0.state == .today })

        let x = Int((layout.cellLeft(index) - layout.cellGap * 0.5).rounded())
        let y = Int((layout.cellTop(index) + layout.cellSize / 2).rounded())

        XCTAssertEqual(WallpaperPalette.of(settings.theme).accent, Pixels(try render(settings)).color(x, y))
    }

    func testCircleCellsLeaveTheirCornersWithTheBackgroundColor() throws {
        let square = WallpaperSettings(cellShape: .roundedSquare)
        let circle = WallpaperSettings(cellShape: .circle)
        let palette = WallpaperPalette.of(square.theme)
        let index = try XCTUnwrap(grid(square).cells.firstIndex { $0.state == .past && $0.level == .fourthQuartile })

        let point = try cellPoint(square, index, cornerFraction, cornerFraction)

        XCTAssertEqual(palette.level4, Pixels(try render(square)).color(point.x, point.y))
        XCTAssertEqual(palette.background, Pixels(try render(circle)).color(point.x, point.y))
    }

    func testStatsTextIsDrawnBelowTheGridOnlyWhenEnabled() throws {
        let withStats = WallpaperSettings(showStats: true)
        let withoutStats = WallpaperSettings(showStats: false)
        let background = WallpaperPalette.of(withStats.theme).background
        let belowGridWithStats = try layoutFor(withStats).bottomOfGrid + 2
        let belowGridWithoutStats = try layoutFor(withoutStats).bottomOfGrid + 2

        XCTAssertGreaterThan(Pixels(try render(withStats)).count(top: belowGridWithStats, bottom: size.height) { $0 != background }, 0)
        XCTAssertEqual(0, Pixels(try render(withoutStats)).count(top: belowGridWithoutStats, bottom: size.height) { $0 != background })
    }

    func testTheTotalIsHighlightedWithTheAccentColor() throws {
        let settings = WallpaperSettings()
        let stats = try XCTUnwrap(try layoutFor(settings).stats)
        let top = Int(stats.titleBaseline - stats.titleTextSize)
        let bottom = Int(stats.titleBaseline)

        let accentPixels = Pixels(try render(settings)).count(top: top, bottom: bottom) { $0 == WallpaperPalette.of(settings.theme).accent }

        XCTAssertGreaterThan(accentPixels, 0)
    }

    func testAvatarIsDrawnNextToTheLoginWhenAvailable() throws {
        let settings = WallpaperSettings()
        let red = ARGBColor(argb: 0xFFFF_0000)
        let avatar = solidImage(color: red, size: 64)
        let stats = try XCTUnwrap(try layoutFor(settings).stats)
        let top = Int((stats.userRowCenterY - stats.avatarSize).rounded())
        let bottom = Int((stats.userRowCenterY + stats.avatarSize).rounded())

        let withAvatar = try renderer.render(calendar: calendar, settings: settings, size: size, today: today, avatar: avatar)
        let withoutAvatar = try render(settings)

        XCTAssertGreaterThan(Pixels(withAvatar).count(top: top, bottom: bottom) { $0 == red }, 0)
        XCTAssertEqual(0, Pixels(withoutAvatar).count(top: top, bottom: bottom) { $0 == red })
    }

    func testCustomCommitColorPaintsTheBusiestDaysAndKeepsTheThemeBackground() throws {
        let commitColor = ARGBColor(argb: 0xFF8A_2BE2)
        let settings = WallpaperSettings(theme: .ocean, commitColor: commitColor)
        let index = try XCTUnwrap(grid(settings).cells.firstIndex { $0.state == .past && $0.level == .fourthQuartile })
        let point = try cellPoint(settings, index, 0.5, 0.5)

        let pixels = Pixels(try render(settings))

        XCTAssertEqual(commitColor, pixels.color(point.x, point.y))
        XCTAssertEqual(WallpaperPalette.of(.ocean).background, pixels.color(2, 2))
    }

    func testFramingMovesTheGraphToTheChosenPosition() throws {
        let settings = WallpaperSettings()
        let framed = WallpaperSettings(framing: WallpaperFraming(offsetX: 0.1, offsetY: -0.2))
        let index = try XCTUnwrap(grid(settings).cells.firstIndex { $0.state == .past && $0.level == .fourthQuartile })
        let point = try cellPoint(settings, index, 0.5, 0.5)

        let movedX = point.x + Int((Double(size.width) * 0.1).rounded())
        let movedY = point.y - Int((Double(size.height) * 0.2).rounded())

        XCTAssertEqual(WallpaperPalette.of(settings.theme).level4, Pixels(try render(framed)).color(movedX, movedY))
    }

    func testFramingZoomsTheGraphAroundTheScreenCenter() throws {
        let settings = WallpaperSettings()
        let zoom: CGFloat = 1.5
        let framed = WallpaperSettings(framing: WallpaperFraming(zoom: Float(zoom)))
        let index = try XCTUnwrap(grid(settings).cells.firstIndex { $0.state == .past && $0.level == .fourthQuartile })
        let point = try cellPoint(settings, index, 0.5, 0.5)
        let centerX = CGFloat(size.width) / 2
        let centerY = CGFloat(size.height) / 2

        let zoomedX = Int((centerX + (CGFloat(point.x) - centerX) * zoom).rounded())
        let zoomedY = Int((centerY + (CGFloat(point.y) - centerY) * zoom).rounded())
        let outsideZoomedGrid = Int((centerX + (try layoutFor(settings).gridLeft - centerX) * zoom - 2).rounded())

        let pixels = Pixels(try render(framed))
        XCTAssertEqual(WallpaperPalette.of(settings.theme).level4, pixels.color(zoomedX, zoomedY))
        XCTAssertEqual(WallpaperPalette.of(settings.theme).background, pixels.color(outsideZoomedGrid, zoomedY))
    }

    func testFramingKeepsTheBackgroundFillingTheWholeScreen() throws {
        let settings = WallpaperSettings(framing: WallpaperFraming(zoom: WallpaperFraming.minZoom, offsetY: 0.5))
        let pixels = Pixels(try render(settings))
        let background = WallpaperPalette.of(settings.theme).background

        XCTAssertEqual(background, pixels.color(2, 2))
        XCTAssertEqual(background, pixels.color(size.width - 2, size.height - 2))
    }

    private func render(_ settings: WallpaperSettings) throws -> CGImage {
        try renderer.render(calendar: calendar, settings: settings, size: size, today: today, avatar: nil)
    }

    private func grid(_ settings: WallpaperSettings) -> DayGrid {
        DayGrid.from(calendar.days, period: settings.period, today: today)
    }

    private func layoutFor(_ settings: WallpaperSettings) throws -> WallpaperLayout {
        try WallpaperLayout.calculate(width: size.width, height: size.height, dayCount: grid(settings).cells.count, showStats: settings.showStats)
    }

    private func cellPoint(_ settings: WallpaperSettings, _ index: Int, _ fractionX: CGFloat, _ fractionY: CGFloat) throws -> (x: Int, y: Int) {
        let layout = try layoutFor(settings)
        return (
            Int((layout.cellLeft(index) + layout.cellSize * fractionX).rounded()),
            Int((layout.cellTop(index) + layout.cellSize * fractionY).rounded())
        )
    }
}

private extension WallpaperLayout {
    var bottomOfGrid: Int {
        Int((gridTop + gridHeight).rounded())
    }
}
