import GitWallpaperKit
import SwiftUI

extension WallpaperTarget {
    var labelKey: LocalizedStringKey {
        switch self {
        case .lockScreen: "target_lock_screen"
        case .homeScreen: "target_home_screen"
        case .both: "target_both"
        }
    }

    var shortcutLocationKey: String.LocalizationValue {
        switch self {
        case .lockScreen: "shortcut_location_lock_screen"
        case .homeScreen: "shortcut_location_home_screen"
        case .both: "shortcut_location_both"
        }
    }
}

extension WallpaperTheme {
    var labelKey: LocalizedStringKey {
        switch self {
        case .githubDark: "theme_github_dark"
        case .githubLight: "theme_github_light"
        case .amoled: "theme_amoled"
        case .ocean: "theme_ocean"
        case .sunset: "theme_sunset"
        }
    }
}

extension ContributionPeriod {
    var labelKey: LocalizedStringKey {
        switch self {
        case .currentYear: "period_current_year"
        case .last12Months: "period_last_12_months"
        }
    }
}

extension CellShape {
    var labelKey: LocalizedStringKey {
        switch self {
        case .roundedSquare: "shape_rounded_square"
        case .circle: "shape_circle"
        }
    }
}

extension UpdateInterval {
    var labelKey: LocalizedStringKey {
        switch self {
        case .thirtyMinutes: "interval_thirty_minutes"
        case .oneHour: "interval_one_hour"
        case .threeHours: "interval_three_hours"
        case .sixHours: "interval_six_hours"
        case .twelveHours: "interval_twelve_hours"
        }
    }
}

extension HomeMessage {
    var textKey: String.LocalizationValue {
        switch self {
        case .applied: "message_applied"
        case .refreshed: "message_refreshed"
        case .networkError: "message_network_error"
        case .gitHubError: "message_github_error"
        case .wallpaperError: "message_wallpaper_error"
        case .sessionExpired: "message_session_expired"
        case .shortcutFailed: "message_shortcut_failed"
        }
    }
}
