public struct ContributionStats: Equatable, Sendable {
    public var totalContributions: Int
    public var currentStreak: Int
    public var longestStreak: Int

    public init(totalContributions: Int, currentStreak: Int, longestStreak: Int) {
        self.totalContributions = totalContributions
        self.currentStreak = currentStreak
        self.longestStreak = longestStreak
    }

    public static func from(_ days: [ContributionDay], periodStart: LocalDate) -> ContributionStats {
        let sortedDays = days.sorted { $0.date < $1.date }
        return ContributionStats(
            totalContributions: sortedDays
                .filter { $0.date >= periodStart }
                .reduce(0) { $0 + $1.count },
            currentStreak: currentStreak(sortedDays),
            longestStreak: longestStreak(sortedDays)
        )
    }

    private static func currentStreak(_ sortedDays: [ContributionDay]) -> Int {
        guard let lastDay = sortedDays.last else { return 0 }
        var expectedDate = lastDay.count > 0 ? lastDay.date : lastDay.date.minusDays(1)
        var streak = 0
        for day in sortedDays.reversed() {
            if day.date > expectedDate { continue }
            if day.date != expectedDate || day.count == 0 { break }
            streak += 1
            expectedDate = expectedDate.minusDays(1)
        }
        return streak
    }

    private static func longestStreak(_ sortedDays: [ContributionDay]) -> Int {
        var longest = 0
        var current = 0
        var previousDate: LocalDate?
        for day in sortedDays {
            if day.count == 0 {
                current = 0
            } else if let previousDate, day.date == previousDate.plusDays(1) {
                current += 1
            } else {
                current = 1
            }
            longest = max(longest, current)
            previousDate = day.date
        }
        return longest
    }
}
