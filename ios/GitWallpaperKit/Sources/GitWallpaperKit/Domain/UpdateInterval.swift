public enum UpdateInterval: String, CaseIterable, Sendable {
    case thirtyMinutes = "THIRTY_MINUTES"
    case oneHour = "ONE_HOUR"
    case threeHours = "THREE_HOURS"
    case sixHours = "SIX_HOURS"
    case twelveHours = "TWELVE_HOURS"

    public var minutes: Int {
        switch self {
        case .thirtyMinutes: 30
        case .oneHour: 60
        case .threeHours: 180
        case .sixHours: 360
        case .twelveHours: 720
        }
    }
}
