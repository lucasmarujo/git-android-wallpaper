import Foundation

public struct WallClock: Sendable {
    private let nowProvider: @Sendable () -> Date
    private let timeZoneProvider: @Sendable () -> TimeZone

    public init(now: @escaping @Sendable () -> Date, timeZone: @escaping @Sendable () -> TimeZone) {
        nowProvider = now
        timeZoneProvider = timeZone
    }

    public static let system = WallClock(now: { Date() }, timeZone: { TimeZone.autoupdatingCurrent })

    public var now: Date { nowProvider() }

    public var timeZone: TimeZone { timeZoneProvider() }

    public func today() -> LocalDate {
        LocalDate.today(now: now, timeZone: timeZone)
    }
}
