public struct WallpaperPalette: Equatable, Sendable {
    public var background: ARGBColor
    public var empty: ARGBColor
    public var future: ARGBColor
    public var level1: ARGBColor
    public var level2: ARGBColor
    public var level3: ARGBColor
    public var level4: ARGBColor
    public var accent: ARGBColor
    public var primaryText: ARGBColor
    public var secondaryText: ARGBColor

    public func color(for level: ContributionLevel) -> ARGBColor {
        switch level {
        case .empty: empty
        case .firstQuartile: level1
        case .secondQuartile: level2
        case .thirdQuartile: level3
        case .fourthQuartile: level4
        }
    }

    public func withCommitColor(_ commitColor: ARGBColor?) -> WallpaperPalette {
        guard let commitColor else { return self }
        let opaque = commitColor.opaque
        var palette = self
        palette.level1 = empty.blended(with: opaque, ratio: Self.firstQuartileRatio)
        palette.level2 = empty.blended(with: opaque, ratio: Self.secondQuartileRatio)
        palette.level3 = empty.blended(with: opaque, ratio: Self.thirdQuartileRatio)
        palette.level4 = opaque
        palette.accent = opaque
        return palette
    }

    public static func of(_ theme: WallpaperTheme) -> WallpaperPalette {
        switch theme {
        case .githubDark: githubDark
        case .githubLight: githubLight
        case .amoled: amoled
        case .ocean: ocean
        case .sunset: sunset
        }
    }

    public static let monochrome = WallpaperPalette(
        background: ARGBColor(argb: 0x0000_0000),
        empty: ARGBColor(argb: 0x33FF_FFFF),
        future: ARGBColor(argb: 0x14FF_FFFF),
        level1: ARGBColor(argb: 0x66FF_FFFF),
        level2: ARGBColor(argb: 0x99FF_FFFF),
        level3: ARGBColor(argb: 0xCCFF_FFFF),
        level4: ARGBColor(argb: 0xFFFF_FFFF),
        accent: ARGBColor(argb: 0xFFFF_FFFF),
        primaryText: ARGBColor(argb: 0xFFFF_FFFF),
        secondaryText: ARGBColor(argb: 0xB3FF_FFFF)
    )

    private static let firstQuartileRatio: Float = 0.3
    private static let secondQuartileRatio: Float = 0.55
    private static let thirdQuartileRatio: Float = 0.8

    private static let githubDark = WallpaperPalette(
        background: ARGBColor(argb: 0xFF0D_1117),
        empty: ARGBColor(argb: 0xFF1A_2029),
        future: ARGBColor(argb: 0xFF12_171E),
        level1: ARGBColor(argb: 0xFF0E_4429),
        level2: ARGBColor(argb: 0xFF00_6D32),
        level3: ARGBColor(argb: 0xFF26_A641),
        level4: ARGBColor(argb: 0xFF39_D353),
        accent: ARGBColor(argb: 0xFF39_D353),
        primaryText: ARGBColor(argb: 0xFFE6_EDF3),
        secondaryText: ARGBColor(argb: 0xFF8B_949E)
    )

    private static let githubLight = WallpaperPalette(
        background: ARGBColor(argb: 0xFFFF_FFFF),
        empty: ARGBColor(argb: 0xFFEB_EDF0),
        future: ARGBColor(argb: 0xFFF6_F7F9),
        level1: ARGBColor(argb: 0xFF9B_E9A8),
        level2: ARGBColor(argb: 0xFF40_C463),
        level3: ARGBColor(argb: 0xFF30_A14E),
        level4: ARGBColor(argb: 0xFF21_6E39),
        accent: ARGBColor(argb: 0xFF1A_7F37),
        primaryText: ARGBColor(argb: 0xFF1F_2328),
        secondaryText: ARGBColor(argb: 0xFF65_6D76)
    )

    private static let amoled = WallpaperPalette(
        background: ARGBColor(argb: 0xFF00_0000),
        empty: ARGBColor(argb: 0xFF16_1616),
        future: ARGBColor(argb: 0xFF0B_0B0B),
        level1: ARGBColor(argb: 0xFF0E_4429),
        level2: ARGBColor(argb: 0xFF00_6D32),
        level3: ARGBColor(argb: 0xFF26_A641),
        level4: ARGBColor(argb: 0xFF39_D353),
        accent: ARGBColor(argb: 0xFF39_D353),
        primaryText: ARGBColor(argb: 0xFFF0_F6FC),
        secondaryText: ARGBColor(argb: 0xFF8B_949E)
    )

    private static let ocean = WallpaperPalette(
        background: ARGBColor(argb: 0xFF0A_1628),
        empty: ARGBColor(argb: 0xFF15_263F),
        future: ARGBColor(argb: 0xFF0F_1D33),
        level1: ARGBColor(argb: 0xFF0C_4A6E),
        level2: ARGBColor(argb: 0xFF03_69A1),
        level3: ARGBColor(argb: 0xFF0E_A5E9),
        level4: ARGBColor(argb: 0xFF7D_D3FC),
        accent: ARGBColor(argb: 0xFF7D_D3FC),
        primaryText: ARGBColor(argb: 0xFFE0_F2FE),
        secondaryText: ARGBColor(argb: 0xFF78_96B4)
    )

    private static let sunset = WallpaperPalette(
        background: ARGBColor(argb: 0xFF1B_1020),
        empty: ARGBColor(argb: 0xFF2C_1C32),
        future: ARGBColor(argb: 0xFF22_1627),
        level1: ARGBColor(argb: 0xFF5B_2150),
        level2: ARGBColor(argb: 0xFF9D_3462),
        level3: ARGBColor(argb: 0xFFE0_565B),
        level4: ARGBColor(argb: 0xFFFF_B267),
        accent: ARGBColor(argb: 0xFFFF_B267),
        primaryText: ARGBColor(argb: 0xFFFF_EFE6),
        secondaryText: ARGBColor(argb: 0xFFB0_8E9E)
    )
}
