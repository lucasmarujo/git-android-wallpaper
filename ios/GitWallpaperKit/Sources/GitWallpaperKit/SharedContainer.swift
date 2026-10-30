import Foundation
import os

public final class SharedContainer: @unchecked Sendable {
    public static let appGroupIdentifier = "group.dev.marujo.gitwallpaper"

    public let urlSession: URLSession
    public let contributionsSource: ContributionsSource
    public let sessionRepository: SessionRepository
    public let settingsRepository: SettingsRepository
    public let updateStatusRepository: UpdateStatusRepository
    public let contributionRepository: ContributionRepository
    public let avatarSource: AvatarSource
    public let stagedWallpaper: StagedWallpaperStore
    public let renderer: ContributionWallpaperRenderer
    public let clock: WallClock

    public convenience init(userAgent: String, clock: WallClock = .system) {
        let configuration = URLSessionConfiguration.ephemeral
        configuration.timeoutIntervalForRequest = Self.requestTimeoutSeconds
        configuration.timeoutIntervalForResource = Self.resourceTimeoutSeconds
        configuration.httpAdditionalHeaders = [Self.userAgentHeader: userAgent]
        configuration.urlCache = nil
        let session = URLSession(configuration: configuration)
        self.init(
            urlSession: session,
            contributionsSource: GitHubGraphQLClient(session: session),
            secretStore: KeychainSecretStore(
                service: Self.keychainService,
                account: Self.keychainAccount,
                accessGroup: Self.appGroupIdentifier
            ),
            defaults: UserDefaults(suiteName: Self.appGroupIdentifier) ?? .standard,
            directory: Self.sharedDirectory(),
            clock: clock
        )
    }

    public init(
        urlSession: URLSession,
        contributionsSource: ContributionsSource,
        secretStore: SecretStore,
        defaults: UserDefaults,
        directory: URL,
        clock: WallClock
    ) {
        let sessionRepository = SessionRepository(defaults: defaults, secretStore: secretStore)
        self.urlSession = urlSession
        self.contributionsSource = contributionsSource
        self.sessionRepository = sessionRepository
        settingsRepository = SettingsRepository(defaults: defaults)
        updateStatusRepository = UpdateStatusRepository(defaults: defaults)
        contributionRepository = ContributionRepository(
            fileURL: directory.appendingPathComponent(Self.contributionsFileName),
            contributionsSource: contributionsSource,
            sessionRepository: sessionRepository
        )
        avatarSource = CachedAvatarRepository(
            session: urlSession,
            cacheDirectory: directory.appendingPathComponent(Self.avatarDirectoryName, isDirectory: true)
        )
        stagedWallpaper = StagedWallpaperStore(fileURL: directory.appendingPathComponent(Self.stagedWallpaperFileName))
        renderer = ContributionWallpaperRenderer()
        self.clock = clock
    }

    private static func sharedDirectory() -> URL {
        let fileManager = FileManager.default
        if let container = fileManager.containerURL(forSecurityApplicationGroupIdentifier: appGroupIdentifier) {
            return container.appendingPathComponent(sharedDirectoryName, isDirectory: true)
        }
        logger.error("The app group container is unavailable, falling back to the app sandbox")
        let applicationSupport = fileManager.urls(for: .applicationSupportDirectory, in: .userDomainMask)[0]
        return applicationSupport.appendingPathComponent(sharedDirectoryName, isDirectory: true)
    }

    private static let requestTimeoutSeconds: TimeInterval = 30
    private static let resourceTimeoutSeconds: TimeInterval = 45
    private static let userAgentHeader = "User-Agent"
    private static let keychainService = "dev.marujo.gitwallpaper.session"
    private static let keychainAccount = "github_token"
    private static let sharedDirectoryName = "GitWallpaper"
    private static let contributionsFileName = "contributions.json"
    private static let avatarDirectoryName = "avatar"
    private static let stagedWallpaperFileName = "wallpaper.png"
    private static let logger = Logger(subsystem: "dev.marujo.gitwallpaper", category: "SharedContainer")
}
