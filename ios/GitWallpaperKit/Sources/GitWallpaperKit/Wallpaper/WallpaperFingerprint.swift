import CryptoKit
import Foundation

public enum WallpaperFingerprint {
    private static let renderVersion = 1
    private static let separator = "|"
    private static let missingValue = "null"

    public static func of(
        calendar: ContributionCalendar,
        settings: WallpaperSettings,
        size: ScreenSize,
        languageTag: String,
        today: LocalDate
    ) -> String {
        var content = [
            String(renderVersion),
            calendar.login,
            calendar.avatarUrl ?? missingValue,
            today.description,
            settings.target.rawValue,
            settings.theme.rawValue,
            settings.commitColor.map { String($0.argb) } ?? missingValue,
            settings.period.rawValue,
            settings.cellShape.rawValue,
            String(settings.showStats),
            String(settings.framing.zoom),
            String(settings.framing.offsetX),
            String(settings.framing.offsetY),
            "\(size.width)x\(size.height)",
            languageTag,
        ].joined(separator: separator)
        for day in calendar.days {
            content += "\(separator)\(day.date):\(day.count):\(day.level.rawValue)"
        }
        return SHA256.hash(data: Data(content.utf8))
            .map { String(format: "%02x", $0) }
            .joined()
    }
}
