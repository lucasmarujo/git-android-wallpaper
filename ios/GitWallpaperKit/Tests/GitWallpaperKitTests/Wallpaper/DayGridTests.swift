import GitWallpaperKit
import XCTest

final class DayGridTests: XCTestCase {
    private let today = TestCalendars.today
    private let days = TestCalendars.calendar().days

    func testCurrentYearCoversEveryDayFromJanuaryToDecember() {
        let grid = DayGrid.from(days, period: .currentYear, today: today)

        XCTAssertEqual(365, grid.cells.count)
        XCTAssertEqual(LocalDate(year: 2026, month: 1, day: 1), grid.startDate)
        XCTAssertEqual(LocalDate(year: 2026, month: 12, day: 31), grid.cells.last?.date)
    }

    func testLeapYearsHaveOneExtraDay() {
        let leapToday = LocalDate(year: 2028, month: 3, day: 1)

        let grid = DayGrid.from(TestCalendars.daysEndingAt(leapToday, counts: Array(repeating: 1, count: 10)), period: .currentYear, today: leapToday)

        XCTAssertEqual(366, grid.cells.count)
    }

    func testDaysAreSplitIntoPastTodayAndFuture() {
        let grid = DayGrid.from(days, period: .currentYear, today: today)

        let todayCells = grid.cells.filter { $0.state == .today }
        XCTAssertEqual([today], todayCells.map(\.date))
        XCTAssertTrue(grid.cells.filter { $0.date < today }.allSatisfy { $0.state == .past })
        XCTAssertTrue(grid.cells.filter { $0.date > today }.allSatisfy { $0.state == .future })
        XCTAssertTrue(grid.cells.filter { $0.state == .future }.allSatisfy { $0.level == .empty })
    }

    func testLevelsComeFromTheMatchingContributionDay() {
        let grid = DayGrid.from(days, period: .currentYear, today: today)
        let byDate = Dictionary(uniqueKeysWithValues: days.map { ($0.date, $0.level) })

        for cell in grid.cells where cell.state != .future {
            XCTAssertEqual(byDate[cell.date], cell.level, cell.date.description)
        }
    }

    func testMissingDaysAreDrawnWithoutContributions() {
        let sparse = [ContributionDay(date: today.minusDays(3), count: 4, level: .fourthQuartile)]

        let grid = DayGrid.from(sparse, period: .currentYear, today: today)

        XCTAssertEqual(1, grid.cells.filter { $0.level != .empty }.count)
        XCTAssertEqual(.fourthQuartile, grid.cells.first { $0.date == today.minusDays(3) }?.level)
    }

    func testLastTwelveMonthsEndTodayWithoutFutureDays() {
        let grid = DayGrid.from(days, period: .last12Months, today: today)

        XCTAssertEqual(365, grid.cells.count)
        XCTAssertEqual(today.minusYears(1).plusDays(1), grid.startDate)
        XCTAssertEqual(.today, grid.cells.last?.state)
        XCTAssertFalse(grid.cells.contains { $0.state == .future })
    }

    func testAGitHubDayAheadOfTheDeviceClockBecomesToday() {
        let tomorrow = today.plusDays(1)
        let ahead = TestCalendars.daysEndingAt(tomorrow, counts: [1, 2])

        let grid = DayGrid.from(ahead, period: .currentYear, today: today)

        XCTAssertEqual(tomorrow, grid.today)
        XCTAssertEqual(.today, grid.cells.first { $0.date == tomorrow }?.state)
    }

    func testEmptyHistoryStillRendersTheWholeYear() {
        let grid = DayGrid.from([], period: .currentYear, today: today)

        XCTAssertEqual(365, grid.cells.count)
        XCTAssertEqual(today, grid.today)
    }
}
