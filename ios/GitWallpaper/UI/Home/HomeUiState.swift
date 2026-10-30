import CoreGraphics
import Foundation
import GitWallpaperKit

struct HomeUiState {
    static let defaultPreviewAspectRatio = 9.0 / 19.5

    var login: String
    var settings = WallpaperSettings()
    var preview: CGImage?
    var previewAspectRatio = HomeUiState.defaultPreviewAspectRatio
    var stats: ContributionStats?
    var hasCalendar = false
    var isLoadingCalendar = true
    var lastAppliedAt: Date?
    var isAdjusting = false
    var adjustmentPreview: CGImage?
    var isApplying = false
    var isRefreshing = false
    var isShortcutReady = false
    var isAutomationAdded = false
    var canInstallAutomation = false

    var needsShortcutSetup: Bool {
        !isShortcutReady || (canInstallAutomation && !isAutomationAdded)
    }
}

enum HomeMessage: Equatable {
    case applied
    case refreshed
    case networkError
    case gitHubError
    case wallpaperError
    case sessionExpired
}

struct ShortcutFailure: Equatable {
    let reason: String?
}

struct HomeMessageEvent: Identifiable, Equatable {
    let id = UUID()
    let message: HomeMessage
}
