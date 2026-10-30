public extension ContributionCalendar {
    static func sample(login: String, endingAt today: LocalDate, dayCount: Int = ContributionCalendar.sampleDayCount) -> ContributionCalendar {
        var generator = SampleGenerator(seed: sampleSeed)
        let days = (0..<dayCount).map { index in
            let date = today.minusDays(dayCount - 1 - index)
            let isWeekend = date.dayOfWeek >= saturday
            let isRecentStreak = index >= dayCount - sampleStreakDays
            let roll = generator.next(below: percent)
            let restChance = isWeekend ? weekendRestChance : weekdayRestChance
            let count = !isRecentStreak && roll < restChance ? 0 : 1 + generator.next(below: maxDailyContributions)
            return ContributionDay(date: date, count: count, level: level(for: count))
        }
        return ContributionCalendar(login: login, totalContributions: days.reduce(0) { $0 + $1.count }, days: days)
    }

    static let sampleDayCount = 371

    private static let sampleSeed: UInt64 = 0x2545_F491_4F6C_DD1D
    private static let sampleStreakDays = 23
    private static let saturday = 6
    private static let percent = 100
    private static let weekendRestChance = 55
    private static let weekdayRestChance = 18
    private static let maxDailyContributions = 14

    private static func level(for count: Int) -> ContributionLevel {
        switch count {
        case 0: .empty
        case 1...3: .firstQuartile
        case 4...6: .secondQuartile
        case 7...9: .thirdQuartile
        default: .fourthQuartile
        }
    }
}

private struct SampleGenerator {
    private var state: UInt64

    init(seed: UInt64) {
        state = seed
    }

    mutating func next(below bound: Int) -> Int {
        state = state &* 6_364_136_223_846_793_005 &+ 1_442_695_040_888_963_407
        return Int((state >> 33) % UInt64(bound))
    }
}
