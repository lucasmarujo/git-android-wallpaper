public struct WallpaperSettings: Equatable, Sendable {
    public var target: WallpaperTarget
    public var theme: WallpaperTheme
    public var commitColor: ARGBColor?
    public var period: ContributionPeriod
    public var cellShape: CellShape
    public var showStats: Bool
    public var framing: WallpaperFraming
    public var autoUpdate: Bool
    public var updateInterval: UpdateInterval

    public init(
        target: WallpaperTarget = .lockScreen,
        theme: WallpaperTheme = .githubDark,
        commitColor: ARGBColor? = nil,
        period: ContributionPeriod = .currentYear,
        cellShape: CellShape = .roundedSquare,
        showStats: Bool = true,
        framing: WallpaperFraming = WallpaperFraming(),
        autoUpdate: Bool = true,
        updateInterval: UpdateInterval = .oneHour
    ) {
        self.target = target
        self.theme = theme
        self.commitColor = commitColor
        self.period = period
        self.cellShape = cellShape
        self.showStats = showStats
        self.framing = framing
        self.autoUpdate = autoUpdate
        self.updateInterval = updateInterval
    }
}
