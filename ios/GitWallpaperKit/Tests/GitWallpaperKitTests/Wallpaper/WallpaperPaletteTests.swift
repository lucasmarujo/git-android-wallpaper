import GitWallpaperKit
import XCTest

final class WallpaperPaletteTests: XCTestCase {
    private let theme = WallpaperPalette.of(.githubDark)

    func testWithoutACustomColorTheThemePaletteIsKept() {
        XCTAssertEqual(theme, theme.withCommitColor(nil))
    }

    func testCustomColorDrivesTheBusiestLevelAndTheAccent() {
        let color = ARGBColor(argb: 0xFF8A_2BE2)

        let palette = theme.withCommitColor(color)

        XCTAssertEqual(color, palette.level4)
        XCTAssertEqual(color, palette.accent)
    }

    func testCustomColorKeepsTheThemeBackgroundEmptyDaysAndText() {
        let palette = theme.withCommitColor(ARGBColor(argb: 0xFF8A_2BE2))

        XCTAssertEqual(theme.background, palette.background)
        XCTAssertEqual(theme.empty, palette.empty)
        XCTAssertEqual(theme.future, palette.future)
        XCTAssertEqual(theme.primaryText, palette.primaryText)
        XCTAssertEqual(theme.secondaryText, palette.secondaryText)
    }

    func testLowerLevelsFadeFromTheEmptyColorTowardsTheCustomColor() {
        let palette = theme.withCommitColor(ARGBColor(argb: 0xFFFF_0000))

        let reds = [theme.empty, palette.level1, palette.level2, palette.level3, palette.level4].map(\.red)
        XCTAssertEqual(reds.sorted(), reds)
        XCTAssertEqual(reds.count, Set(reds).count)
    }

    func testTransparentColorsAreDrawnOpaque() {
        let palette = theme.withCommitColor(ARGBColor(argb: 0x0000_FF00))

        XCTAssertEqual(ARGBColor(argb: 0xFF00_FF00), palette.level4)
        XCTAssertTrue([palette.level1, palette.level2, palette.level3].allSatisfy { $0.alpha == 255 })
    }

    func testEveryThemeHasItsOwnBackground() {
        let backgrounds = WallpaperTheme.allCases.map { WallpaperPalette.of($0).background }

        XCTAssertEqual(backgrounds.count, Set(backgrounds).count)
    }

    func testMonochromeLevelsGetBrighterWithMoreContributions() {
        let alphas = ContributionLevel.allCases.map { WallpaperPalette.monochrome.color(for: $0).alpha }

        XCTAssertEqual(alphas.sorted(), alphas)
        XCTAssertEqual(0, WallpaperPalette.monochrome.background.alpha)
    }
}
