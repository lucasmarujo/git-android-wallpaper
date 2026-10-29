import Foundation
import GitWallpaperKit
import XCTest

final class LocalDateTests: XCTestCase {
    func testUnixEpochIsDayZero() {
        let epoch = LocalDate(year: 1970, month: 1, day: 1)

        XCTAssertEqual(0, epoch.epochDay)
        XCTAssertEqual("1970-01-01", epoch.description)
    }

    func testCivilDatesRoundTripAcrossCenturies() {
        for epochDay in stride(from: -800_000, through: 800_000, by: 997) {
            let date = LocalDate(epochDay: epochDay)

            XCTAssertEqual(date, LocalDate(year: date.year, month: date.month, day: date.day))
        }
    }

    func testDayOfWeekFollowsIso() {
        XCTAssertEqual(4, LocalDate(year: 1970, month: 1, day: 1).dayOfWeek)
        XCTAssertEqual(6, TestCalendars.today.dayOfWeek)
        XCTAssertEqual(7, LocalDate(year: 2026, month: 10, day: 25).dayOfWeek)
        XCTAssertEqual(1, LocalDate(year: 2026, month: 10, day: 26).dayOfWeek)
    }

    func testPreviousOrSameSundayStartsTheGitHubWeek() {
        XCTAssertEqual(LocalDate(year: 2026, month: 10, day: 18), TestCalendars.today.previousOrSameSunday)
        XCTAssertEqual(LocalDate(year: 2026, month: 10, day: 25), LocalDate(year: 2026, month: 10, day: 25).previousOrSameSunday)
    }

    func testLeapYearsHaveTheTwentyNinthOfFebruary() {
        XCTAssertTrue(LocalDate.isLeapYear(2028))
        XCTAssertTrue(LocalDate.isLeapYear(2000))
        XCTAssertFalse(LocalDate.isLeapYear(1900))
        XCTAssertFalse(LocalDate.isLeapYear(2026))
        XCTAssertEqual(LocalDate(year: 2028, month: 3, day: 1), LocalDate(year: 2028, month: 2, day: 29).plusDays(1))
    }

    func testMinusYearsClampsTheLeapDay() {
        XCTAssertEqual(LocalDate(year: 2027, month: 2, day: 28), LocalDate(year: 2028, month: 2, day: 29).minusYears(1))
        XCTAssertEqual(LocalDate(year: 2025, month: 10, day: 24), TestCalendars.today.minusYears(1))
    }

    func testYearBoundaries() {
        XCTAssertEqual(LocalDate(year: 2026, month: 1, day: 1), TestCalendars.today.firstDayOfYear)
        XCTAssertEqual(LocalDate(year: 2026, month: 12, day: 31), TestCalendars.today.lastDayOfYear)
        XCTAssertEqual(297, TestCalendars.today.dayOfYear)
    }

    func testParsingAcceptsOnlyIsoDates() {
        XCTAssertEqual(TestCalendars.today, LocalDate.parse("2026-10-24"))
        XCTAssertNil(LocalDate.parse("2026-02-30"))
        XCTAssertNil(LocalDate.parse("2026-13-01"))
        XCTAssertNil(LocalDate.parse("26-10-24"))
        XCTAssertNil(LocalDate.parse("not a date"))
    }

    func testCodableUsesTheIsoString() throws {
        let encoded = try JSONEncoder().encode([TestCalendars.today])

        XCTAssertEqual("[\"2026-10-24\"]", String(data: encoded, encoding: .utf8))
        XCTAssertEqual([TestCalendars.today], try JSONDecoder().decode([LocalDate].self, from: encoded))
    }

    func testTodayUsesTheGivenTimeZone() {
        let instant = ISO8601DateFormatter().date(from: "2026-10-24T23:30:00Z")!

        XCTAssertEqual(TestCalendars.today, LocalDate.today(now: instant, timeZone: TimeZone(identifier: "UTC")!))
        XCTAssertEqual(
            LocalDate(year: 2026, month: 10, day: 25),
            LocalDate.today(now: instant, timeZone: TimeZone(identifier: "Asia/Tokyo")!)
        )
    }
}
