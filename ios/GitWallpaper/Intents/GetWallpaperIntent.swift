import AppIntents
import GitWallpaperKit
import UniformTypeIdentifiers

struct GetWallpaperIntent: AppIntent {
    static let title: LocalizedStringResource = "Get Git Wallpaper"
    static let description = IntentDescription("Draws your latest GitHub contribution graph as a wallpaper image for the Set Wallpaper Photo action.")
    static let openAppWhenRun = false

    func perform() async throws -> some IntentResult & ReturnsValue<IntentFile> {
        let container = AppContainer.shared
        let stagedWallpaper = container.core.stagedWallpaper
        let result = await container.wallpaperUpdater.update(force: !stagedWallpaper.hasImage)
        await container.widgetUpdater.updateAll()
        if let error = WallpaperIntentError(result: result, hasImage: stagedWallpaper.hasImage) {
            throw error
        }
        guard let data = stagedWallpaper.imageData() else { throw WallpaperIntentError.unavailable }
        return .result(value: IntentFile(data: data, filename: Self.fileName, type: .png))
    }

    private static let fileName = "git-wallpaper.png"
}

enum WallpaperIntentError: Error, Equatable, CustomLocalizedStringResourceConvertible {
    case notSignedIn
    case offline
    case unavailable

    init?(result: UpdateResult, hasImage: Bool) {
        switch result {
        case .applied, .unchanged:
            return nil
        case .failure(.notSignedIn), .failure(.sessionExpired):
            self = .notSignedIn
        case .failure(.network), .failure(.github):
            guard !hasImage else { return nil }
            self = .offline
        case .failure(.wallpaper):
            guard !hasImage else { return nil }
            self = .unavailable
        }
    }

    var localizedStringResource: LocalizedStringResource {
        switch self {
        case .notSignedIn: "Open Git Wallpaper and sign in with GitHub."
        case .offline: "Could not reach GitHub. Try again."
        case .unavailable: "Open Git Wallpaper and apply your wallpaper once."
        }
    }
}
