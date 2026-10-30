public struct ContributionDay: Hashable, Codable, Sendable {
    public var date: LocalDate
    public var count: Int
    public var level: ContributionLevel

    public init(date: LocalDate, count: Int, level: ContributionLevel) {
        self.date = date
        self.count = count
        self.level = level
    }
}

public struct ContributionCalendar: Hashable, Codable, Sendable {
    public var login: String
    public var totalContributions: Int
    public var days: [ContributionDay]
    public var avatarUrl: String?

    public init(login: String, totalContributions: Int, days: [ContributionDay], avatarUrl: String? = nil) {
        self.login = login
        self.totalContributions = totalContributions
        self.days = days
        self.avatarUrl = avatarUrl
    }
}
