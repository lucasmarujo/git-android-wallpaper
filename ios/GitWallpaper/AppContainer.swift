import Foundation
import GitWallpaperKit

final class AppContainer: @unchecked Sendable {
    static let shared = AppContainer()

    let core: SharedContainer
    let widgetUpdater: WidgetUpdater
    let screenSizeProvider: ScreenSizeProvider
    let wallpaperShortcut: WallpaperShortcut
    let wallpaperUpdater: WallpaperUpdater
    let authRepository: AuthRepository

    private init() {
        let core = SharedContainer(userAgent: "\(Self.userAgentProduct)/\(Self.appVersion)")
        let widgetUpdater = WidgetCenterWidgetUpdater()
        let screenSizeProvider = DisplayScreenSizeProvider()
        self.core = core
        self.widgetUpdater = widgetUpdater
        self.screenSizeProvider = screenSizeProvider
        wallpaperShortcut = WallpaperShortcut(defaults: .standard)
        wallpaperUpdater = DefaultWallpaperUpdater(
            contributionRepository: core.contributionRepository,
            settingsRepository: core.settingsRepository,
            updateStatusRepository: core.updateStatusRepository,
            avatarSource: core.avatarSource,
            renderer: core.renderer,
            applier: core.stagedWallpaper,
            screenSizeProvider: screenSizeProvider,
            clock: core.clock
        )
        authRepository = AuthRepository(
            deviceFlowClient: DeviceFlowClient(session: core.urlSession, clientId: Self.gitHubClientId),
            contributionsSource: core.gitHubClient,
            sessionRepository: core.sessionRepository,
            contributionRepository: core.contributionRepository,
            updateStatusRepository: core.updateStatusRepository,
            stagedWallpaper: core.stagedWallpaper,
            widgetUpdater: widgetUpdater
        )
    }

    @MainActor
    func makeHomeViewModel(login: String) -> HomeViewModel {
        let screenSize = screenSizeProvider.screenSize()
        return HomeViewModel(
            login: login,
            contributionRepository: core.contributionRepository,
            settingsRepository: core.settingsRepository,
            updateStatusRepository: core.updateStatusRepository,
            wallpaperUpdater: wallpaperUpdater,
            widgetUpdater: widgetUpdater,
            authRepository: authRepository,
            avatarSource: core.avatarSource,
            renderer: core.renderer,
            stagedWallpaper: core.stagedWallpaper,
            wallpaperShortcut: wallpaperShortcut,
            urlOpener: SystemURLOpener(),
            previewSize: screenSize.scaled(Self.previewScale),
            adjustmentSize: screenSize,
            clock: core.clock
        )
    }

    @MainActor
    func makeLoginViewModel() -> LoginViewModel {
        LoginViewModel(authRepository: authRepository)
    }

    private static let previewScale = 0.4
    private static let userAgentProduct = "GitWallpaper-iOS"
    private static let gitHubClientIdKey = "GitHubClientID"
    private static let versionKey = "CFBundleShortVersionString"

    private static var appVersion: String {
        Bundle.main.object(forInfoDictionaryKey: versionKey) as? String ?? "0"
    }

    private static var gitHubClientId: String {
        Bundle.main.object(forInfoDictionaryKey: gitHubClientIdKey) as? String ?? ""
    }
}
