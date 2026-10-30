import GitWallpaperKit
import XCTest

final class SampleCalendarTests: XCTestCase {
    private let today = TestCalendars.today

    func testSampleEndsTodayWithOneDayPerDate() {
        let calendar = ContributionCalendar.sample(login: "octocat", endingAt: today)

        XCTAssertEqual(ContributionCalendar.sampleDayCount, calendar.days.count)
        XCTAssertEqual(today, calendar.days.last?.date)
        XCTAssertEqual(calendar.days.map(\.date), calendar.days.map(\.date).sorted())
        XCTAssertEqual(calendar.days.count, Set(calendar.days.map(\.date)).count)
        XCTAssertEqual(calendar.days.reduce(0) { $0 + $1.count }, calendar.totalContributions)
    }

    func testSampleIsDeterministic() {
        XCTAssertEqual(
            ContributionCalendar.sample(login: "octocat", endingAt: today),
            ContributionCalendar.sample(login: "octocat", endingAt: today)
        )
    }

    func testSampleShowsEveryLevelAndARecentStreak() {
        let calendar = ContributionCalendar.sample(login: "octocat", endingAt: today)

        XCTAssertEqual(Set(ContributionLevel.allCases), Set(calendar.days.map(\.level)))
        XCTAssertGreaterThanOrEqual(ContributionStats.from(calendar.days, periodStart: today.firstDayOfYear).currentStreak, 23)
    }

    func testLevelsGrowWithTheCount() {
        let calendar = ContributionCalendar.sample(login: "octocat", endingAt: today)
        let order = Dictionary(uniqueKeysWithValues: ContributionLevel.allCases.enumerated().map { ($1, $0) })

        for first in calendar.days {
            for second in calendar.days where first.count < second.count {
                XCTAssertLessThanOrEqual(order[first.level]!, order[second.level]!)
            }
        }
    }
}
