import Foundation

public struct LocalDate: Hashable, Comparable, Codable, Sendable, CustomStringConvertible {
    public let epochDay: Int

    public init(epochDay: Int) {
        self.epochDay = epochDay
    }

    public init(year: Int, month: Int, day: Int) {
        precondition((1...12).contains(month), "month must be between 1 and 12")
        precondition((1...Self.daysIn(month: month, year: year)).contains(day), "day is outside the month")
        let shiftedYear = month <= 2 ? year - 1 : year
        let era = (shiftedYear >= 0 ? shiftedYear : shiftedYear - 399) / 400
        let yearOfEra = shiftedYear - era * 400
        let dayOfYear = (153 * (month + (month > 2 ? -3 : 9)) + 2) / 5 + day - 1
        let dayOfEra = yearOfEra * 365 + yearOfEra / 4 - yearOfEra / 100 + dayOfYear
        epochDay = era * 146_097 + dayOfEra - Self.unixEpochOffset
    }

    public static func today(now: Date, timeZone: TimeZone) -> LocalDate {
        var calendar = Calendar(identifier: .gregorian)
        calendar.timeZone = timeZone
        let components = calendar.dateComponents([.year, .month, .day], from: now)
        return LocalDate(year: components.year ?? 1970, month: components.month ?? 1, day: components.day ?? 1)
    }

    public static func parse(_ text: String) -> LocalDate? {
        let parts = text.split(separator: "-", omittingEmptySubsequences: false)
        guard parts.count == 3,
              parts[0].count == 4, parts[1].count == 2, parts[2].count == 2,
              let year = Int(parts[0]), let month = Int(parts[1]), let day = Int(parts[2]),
              (1...12).contains(month), (1...daysIn(month: month, year: year)).contains(day)
        else { return nil }
        return LocalDate(year: year, month: month, day: day)
    }

    public var year: Int { civil.year }

    public var month: Int { civil.month }

    public var day: Int { civil.day }

    public var dayOfWeek: Int {
        ((epochDay + 3) % Self.daysPerWeek + Self.daysPerWeek) % Self.daysPerWeek + 1
    }

    public var dayOfYear: Int {
        epochDay - LocalDate(year: year, month: 1, day: 1).epochDay + 1
    }

    public var firstDayOfYear: LocalDate {
        LocalDate(year: year, month: 1, day: 1)
    }

    public var lastDayOfYear: LocalDate {
        LocalDate(year: year, month: 12, day: 31)
    }

    public var previousOrSameSunday: LocalDate {
        plusDays(-(dayOfWeek % Self.daysPerWeek))
    }

    public func plusDays(_ days: Int) -> LocalDate {
        LocalDate(epochDay: epochDay + days)
    }

    public func minusDays(_ days: Int) -> LocalDate {
        plusDays(-days)
    }

    public func plusYears(_ years: Int) -> LocalDate {
        let civil = civil
        let targetYear = civil.year + years
        return LocalDate(year: targetYear, month: civil.month, day: min(civil.day, Self.daysIn(month: civil.month, year: targetYear)))
    }

    public func minusYears(_ years: Int) -> LocalDate {
        plusYears(-years)
    }

    public func daysUntil(_ other: LocalDate) -> Int {
        other.epochDay - epochDay
    }

    public func startOfDay(in timeZone: TimeZone) -> Date {
        var calendar = Calendar(identifier: .gregorian)
        calendar.timeZone = timeZone
        let components = DateComponents(year: year, month: month, day: day)
        return calendar.date(from: components) ?? Date(timeIntervalSince1970: TimeInterval(epochDay * Self.secondsPerDay))
    }

    public var description: String {
        let civil = civil
        return String(format: "%04d-%02d-%02d", civil.year, civil.month, civil.day)
    }

    public static func < (lhs: LocalDate, rhs: LocalDate) -> Bool {
        lhs.epochDay < rhs.epochDay
    }

    public init(from decoder: Decoder) throws {
        let container = try decoder.singleValueContainer()
        let text = try container.decode(String.self)
        guard let date = LocalDate.parse(text) else {
            throw DecodingError.dataCorruptedError(in: container, debugDescription: "Invalid ISO date: \(text)")
        }
        self = date
    }

    public func encode(to encoder: Encoder) throws {
        var container = encoder.singleValueContainer()
        try container.encode(description)
    }

    public static func isLeapYear(_ year: Int) -> Bool {
        year % 4 == 0 && (year % 100 != 0 || year % 400 == 0)
    }

    public static func daysIn(month: Int, year: Int) -> Int {
        switch month {
        case 2: isLeapYear(year) ? 29 : 28
        case 4, 6, 9, 11: 30
        default: 31
        }
    }

    private var civil: (year: Int, month: Int, day: Int) {
        let shifted = epochDay + Self.unixEpochOffset
        let era = (shifted >= 0 ? shifted : shifted - 146_096) / 146_097
        let dayOfEra = shifted - era * 146_097
        let yearOfEra = (dayOfEra - dayOfEra / 1460 + dayOfEra / 36524 - dayOfEra / 146_096) / 365
        let dayOfYear = dayOfEra - (365 * yearOfEra + yearOfEra / 4 - yearOfEra / 100)
        let shiftedMonth = (5 * dayOfYear + 2) / 153
        let day = dayOfYear - (153 * shiftedMonth + 2) / 5 + 1
        let month = shiftedMonth < 10 ? shiftedMonth + 3 : shiftedMonth - 9
        let year = yearOfEra + era * 400 + (month <= 2 ? 1 : 0)
        return (year, month, day)
    }

    private static let unixEpochOffset = 719_468
    private static let daysPerWeek = 7
    private static let secondsPerDay = 86_400
}
