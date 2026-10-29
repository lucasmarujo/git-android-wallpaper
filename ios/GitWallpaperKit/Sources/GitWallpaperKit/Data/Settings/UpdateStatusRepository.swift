import Foundation

public final class UpdateStatusRepository: @unchecked Sendable {
    private let defaults: UserDefaults

    public init(defaults: UserDefaults) {
        self.defaults = defaults
    }

    public func current() -> UpdateStatus {
        UpdateStatus(
            fingerprint: defaults.string(forKey: Key.fingerprint),
            lastAppliedAt: date(Key.lastAppliedAt),
            lastCheckedAt: date(Key.lastCheckedAt)
        )
    }

    public func markApplied(fingerprint: String, at date: Date) {
        defaults.set(fingerprint, forKey: Key.fingerprint)
        defaults.set(date.timeIntervalSince1970, forKey: Key.lastAppliedAt)
        defaults.set(date.timeIntervalSince1970, forKey: Key.lastCheckedAt)
    }

    public func markChecked(at date: Date) {
        defaults.set(date.timeIntervalSince1970, forKey: Key.lastCheckedAt)
    }

    public func clear() {
        [Key.fingerprint, Key.lastAppliedAt, Key.lastCheckedAt].forEach(defaults.removeObject(forKey:))
    }

    private func date(_ key: String) -> Date? {
        (defaults.object(forKey: key) as? NSNumber).map { Date(timeIntervalSince1970: $0.doubleValue) }
    }

    private enum Key {
        static let fingerprint = "fingerprint"
        static let lastAppliedAt = "last_applied_at"
        static let lastCheckedAt = "last_checked_at"
    }
}
