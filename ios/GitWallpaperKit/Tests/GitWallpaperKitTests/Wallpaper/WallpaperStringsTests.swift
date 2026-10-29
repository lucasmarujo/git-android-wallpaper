import GitWallpaperKit
import XCTest

final class WallpaperStringsTests: XCTestCase {
    func testEnglishPluralsAndGrouping() throws {
        let strings = try XCTUnwrap(WallpaperStrings(localization: "en"))

        XCTAssertEqual("0 days", strings.days(0))
        XCTAssertEqual("1 day", strings.days(1))
        XCTAssertEqual("12 days", strings.days(12))
        XCTAssertEqual("No contributions", strings.totalHighlight(0))
        XCTAssertEqual("1 contribution", strings.totalHighlight(1))
        XCTAssertEqual("1,234 contributions", strings.totalHighlight(1234))
    }

    func testPortuguesePluralsAndGrouping() throws {
        let strings = try XCTUnwrap(WallpaperStrings(localization: "pt-BR"))

        XCTAssertEqual("0 dias", strings.days(0))
        XCTAssertEqual("1 dia", strings.days(1))
        XCTAssertEqual("12 dias", strings.days(12))
        XCTAssertEqual("Nenhuma contribuição", strings.totalHighlight(0))
        XCTAssertEqual("1 contribuição", strings.totalHighlight(1))
        XCTAssertEqual("1.234 contribuições", strings.totalHighlight(1234))
    }

    func testPeriodSuffixesNameTheYearWithoutGrouping() throws {
        let english = try XCTUnwrap(WallpaperStrings(localization: "en"))
        let portuguese = try XCTUnwrap(WallpaperStrings(localization: "pt-BR"))

        XCTAssertEqual("in 2026", english.totalSuffix(period: .currentYear, year: 2026))
        XCTAssertEqual("in the last 12 months", english.totalSuffix(period: .last12Months, year: 2026))
        XCTAssertEqual("em 2026", portuguese.totalSuffix(period: .currentYear, year: 2026))
        XCTAssertEqual("nos últimos 12 meses", portuguese.totalSuffix(period: .last12Months, year: 2026))
    }

    func testStreakLineCombinesBothStreaks() throws {
        let english = try XCTUnwrap(WallpaperStrings(localization: "en"))
        let portuguese = try XCTUnwrap(WallpaperStrings(localization: "pt-BR"))

        XCTAssertEqual("Current streak 3 days · Longest 1 day", english.streaks(current: 3, longest: 1))
        XCTAssertEqual("Sequência atual 0 dias · Recorde 15 dias", portuguese.streaks(current: 0, longest: 15))
    }

    func testLoginIsPrefixedWithTheAtSign() throws {
        XCTAssertEqual("@octocat", try XCTUnwrap(WallpaperStrings(localization: "en")).login("octocat"))
    }

    func testLanguageTagDiffersBetweenLocalizations() throws {
        let english = try XCTUnwrap(WallpaperStrings(localization: "en"))
        let portuguese = try XCTUnwrap(WallpaperStrings(localization: "pt-BR"))

        XCTAssertNotEqual(english.languageTag, portuguese.languageTag)
    }
}
