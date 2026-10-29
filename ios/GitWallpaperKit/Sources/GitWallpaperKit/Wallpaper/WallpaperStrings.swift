import Foundation

public struct WallpaperStrings: Sendable {
    public static let standard = WallpaperStrings(bundle: .module, locale: .autoupdatingCurrent)

    private let bundle: Bundle
    public let locale: Locale

    public init(bundle: Bundle, locale: Locale) {
        self.bundle = bundle
        self.locale = locale
    }

    public init?(localization: String) {
        let match = Bundle.module.localizations.first { $0.caseInsensitiveCompare(localization) == .orderedSame }
        guard let match,
              let path = Bundle.module.path(forResource: match, ofType: Self.localizationFolderType),
              let bundle = Bundle(path: path)
        else { return nil }
        self.init(bundle: bundle, locale: Locale(identifier: localization))
    }

    public var languageTag: String {
        "\(bundle.preferredLocalizations.first ?? Self.fallbackLocalization)|\(locale.identifier)"
    }

    public func login(_ login: String) -> String {
        Self.loginPrefix + login
    }

    public func days(_ count: Int) -> String {
        count == 0 ? string("wallpaper_days_zero") : plural("wallpaper_days", count)
    }

    public func totalHighlight(_ count: Int) -> String {
        count == 0 ? string("wallpaper_total_highlight_zero") : plural("wallpaper_total_highlight", count)
    }

    public func totalSuffix(period: ContributionPeriod, year: Int) -> String {
        switch period {
        case .currentYear: String(format: string("wallpaper_total_suffix_year"), String(year))
        case .last12Months: string("wallpaper_total_suffix_last_12_months")
        }
    }

    public func streaks(current: Int, longest: Int) -> String {
        String(format: string("wallpaper_streaks"), locale: locale, days(current), days(longest))
    }

    public func number(_ value: Int) -> String {
        let formatter = NumberFormatter()
        formatter.locale = locale
        formatter.numberStyle = .decimal
        return formatter.string(from: NSNumber(value: value)) ?? String(value)
    }

    private func string(_ key: String) -> String {
        bundle.localizedString(forKey: key, value: nil, table: nil)
    }

    private func plural(_ key: String, _ count: Int) -> String {
        String(format: string(key), locale: locale, count)
            .replacingOccurrences(of: Self.countPlaceholder, with: number(count))
    }

    private static let loginPrefix = "@"
    private static let countPlaceholder = "{count}"
    private static let fallbackLocalization = "en"
    private static let localizationFolderType = "lproj"
}
