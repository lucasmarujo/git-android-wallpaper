import GitWallpaperKit
import XCTest

final class ContributionStatsTests: XCTestCase {
    private let today = TestCalendars.today

    func testEmptyHistoryHasNoContributionsOrStreaks() {
        let stats = ContributionStats.from([], periodStart: today)

        XCTAssertEqual(ContributionStats(totalContributions: 0, currentStreak: 0, longestStreak: 0), stats)
    }

    func testCurrentStreakCountsConsecutiveActiveDaysEndingToday() {
        let days = TestCalendars.daysEndingAt(today, counts: [3, 0, 1, 2, 5])

        XCTAssertEqual(3, ContributionStats.from(days, periodStart: today.minusDays(10)).currentStreak)
    }

    func testADayWithoutContributionsTodayDoesNotBreakTheCurrentStreak() {
        let days = TestCalendars.daysEndingAt(today, counts: [1, 1, 1, 0])

        XCTAssertEqual(3, ContributionStats.from(days, periodStart: today.minusDays(10)).currentStreak)
    }

    func testCurrentStreakIsZeroWhenTodayAndYesterdayHaveNoContributions() {
        let days = TestCalendars.daysEndingAt(today, counts: [4, 4, 0, 0])

        XCTAssertEqual(0, ContributionStats.from(days, periodStart: today.minusDays(10)).currentStreak)
    }

    func testMissingDatesBreakStreaks() {
        let days = [
            ContributionDay(date: today.minusDays(3), count: 2, level: .secondQuartile),
            ContributionDay(date: today.minusDays(1), count: 2, level: .secondQuartile),
            ContributionDay(date: today, count: 2, level: .secondQuartile),
        ]

        let stats = ContributionStats.from(days, periodStart: today.minusDays(10))

        XCTAssertEqual(2, stats.currentStreak)
        XCTAssertEqual(2, stats.longestStreak)
    }

    func testLongestStreakIsTheLongestRunInTheWholeHistory() {
        let days = TestCalendars.daysEndingAt(today, counts: [1, 1, 1, 1, 0, 2, 2, 0, 1])

        let stats = ContributionStats.from(days, periodStart: today.minusDays(30))

        XCTAssertEqual(4, stats.longestStreak)
        XCTAssertEqual(1, stats.currentStreak)
    }

    func testTotalOnlyCountsContributionsInsideThePeriod() {
        let days = TestCalendars.daysEndingAt(today, counts: [10, 10, 1, 2, 3])

        XCTAssertEqual(6, ContributionStats.from(days, periodStart: today.minusDays(2)).totalContributions)
    }

    func testUnsortedInputProducesTheSameResultAsSortedInput() {
        let days = TestCalendars.daysEndingAt(today, counts: [1, 2, 0, 3, 4])

        XCTAssertEqual(
            ContributionStats.from(days, periodStart: today.minusDays(30)),
            ContributionStats.from([days[3], days[0], days[4], days[2], days[1]], periodStart: today.minusDays(30))
        )
    }
}
