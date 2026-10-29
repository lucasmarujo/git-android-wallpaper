import GitWallpaperKit

enum TestCalendars {
    static let today = LocalDate(year: 2026, month: 10, day: 24)

    static func daysEndingAt(_ endDate: LocalDate, counts: [Int]) -> [ContributionDay] {
        counts.enumerated().map { index, count in
            ContributionDay(date: endDate.minusDays(counts.count - 1 - index), count: count, level: levelFor(count))
        }
    }

    static func calendar(
        login: String = "octocat",
        endDate: LocalDate = today,
        counts: [Int] = (0..<371).map { $0 % 5 },
        avatarUrl: String? = nil
    ) -> ContributionCalendar {
        let days = daysEndingAt(endDate, counts: counts)
        return ContributionCalendar(
            login: login,
            totalContributions: days.reduce(0) { $0 + $1.count },
            days: days,
            avatarUrl: avatarUrl
        )
    }

    private static func levelFor(_ count: Int) -> ContributionLevel {
        switch count {
        case ...0: .empty
        case 1: .firstQuartile
        case 2: .secondQuartile
        case 3: .thirdQuartile
        default: .fourthQuartile
        }
    }
}
