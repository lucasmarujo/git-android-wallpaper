public enum ContributionLevel: String, CaseIterable, Codable, Sendable {
    case empty = "NONE"
    case firstQuartile = "FIRST_QUARTILE"
    case secondQuartile = "SECOND_QUARTILE"
    case thirdQuartile = "THIRD_QUARTILE"
    case fourthQuartile = "FOURTH_QUARTILE"
}
