import Foundation
import os

public final class SharedContainer: @unchecked Sendable {
    public static let appGroupIdentifier = "group.dev.marujo.gitwallpaper"

    public let urlSession: URLSession
    public let gitHubClient: GitHubGraphQLClient
    public let sessionRepository: SessionRepository
    public let settingsRepository: SettingsRepository
    public let updateStatusRepository: UpdateStatusRepository
    public let contributionRepository: ContributionRepository
    public let avatarSource: AvatarSource
    public let stagedWallpaper: StagedWallpaperStore
    public let renderer: ContributionWallpaperRenderer
    public let clock: WallClock

    public init(userAgent: String, clock: WallClock = .system) {
        let configuration = URLSessionConfiguration.ephemeral
        configuration.timeoutIntervalForRequest = Self.requestTimeoutSeconds
        configuration.timeoutIntervalForResource = Self.resourceTimeoutSeconds
        configuration.httpAdditionalHeaders = [Self.userAgentHeader: userAgent]
        configuration.urlCache = nil
        let session = URLSession(configuration: configuration)
        let directory = Self.sharedDirectory()
        let defaults = UserDefaults(suiteName: Self.appGroupIdentifier) ?? .standard
        let sessionRepository = SessionRepository(
            defaults: defaults,
            secretStore: KeychainSecretStore(
                service: Self.keychainService,
                account: Self.keychainAccount,
                accessGroup: Self.appGroupIdentifier
            )
        )
        let gitHubClient = GitHubGraphQLClient(session: session)
        urlSession = session
        self.gitHubClient = gitHubClient
        self.sessionRepository = sessionRepository
        settingsRepository = SettingsRepository(defaults: defaults)
        updateStatusRepository = UpdateStatusRepository(defaults: defaults)
        contributionRepository = ContributionRepository(
            fileURL: directory.appendingPathComponent(Self.contributionsFileName),
            contributionsSource: gitHubClient,
            sessionRepository: sessionRepository
        )
        avatarSource = CachedAvatarRepository(
            session: session,
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
