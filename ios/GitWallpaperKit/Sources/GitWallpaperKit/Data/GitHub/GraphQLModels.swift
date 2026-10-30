struct GraphQLRequest: Encodable {
    let query: String
}

struct GraphQLResponse<T: Decodable>: Decodable {
    let data: T?
    let errors: [GraphQLError]?
}

struct GraphQLError: Decodable {
    let message: String
}

struct ViewerData: Decodable {
    let viewer: ViewerDTO
}

struct ViewerDTO: Decodable {
    let login: String
    let avatarUrl: String?
    let contributionsCollection: ContributionsCollectionDTO

    func toCalendar() -> ContributionCalendar {
        let calendar = contributionsCollection.contributionCalendar
        return ContributionCalendar(
            login: login,
            totalContributions: calendar.totalContributions,
            days: calendar.weeks
                .flatMap(\.contributionDays)
                .map { ContributionDay(date: $0.date, count: $0.contributionCount, level: $0.contributionLevel) }
                .sorted { $0.date < $1.date },
            avatarUrl: avatarUrl
        )
    }
}

struct ContributionsCollectionDTO: Decodable {
    let contributionCalendar: ContributionCalendarDTO
}

struct ContributionCalendarDTO: Decodable {
    let totalContributions: Int
    let weeks: [ContributionWeekDTO]
}

struct ContributionWeekDTO: Decodable {
    let contributionDays: [ContributionDayDTO]
}

struct ContributionDayDTO: Decodable {
    let date: LocalDate
    let contributionCount: Int
    let contributionLevel: ContributionLevel

    private enum CodingKeys: String, CodingKey {
        case date
        case contributionCount
        case contributionLevel
    }

    init(from decoder: Decoder) throws {
        let container = try decoder.container(keyedBy: CodingKeys.self)
        date = try container.decode(LocalDate.self, forKey: .date)
        contributionCount = try container.decode(Int.self, forKey: .contributionCount)
        let rawLevel = try container.decodeIfPresent(String.self, forKey: .contributionLevel)
        contributionLevel = rawLevel.flatMap(ContributionLevel.init(rawValue:)) ?? .empty
    }
}
