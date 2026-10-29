import Foundation

public struct UpdateStatus: Equatable, Sendable {
    public var fingerprint: String?
    public var lastAppliedAt: Date?
    public var lastCheckedAt: Date?

    public init(fingerprint: String? = nil, lastAppliedAt: Date? = nil, lastCheckedAt: Date? = nil) {
        self.fingerprint = fingerprint
        self.lastAppliedAt = lastAppliedAt
        self.lastCheckedAt = lastCheckedAt
    }
}
