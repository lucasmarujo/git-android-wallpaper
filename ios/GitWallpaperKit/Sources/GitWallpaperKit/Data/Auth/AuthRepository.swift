import Foundation

public final class AuthRepository: Sendable {
    private let deviceFlowClient: DeviceFlowClient
    private let contributionsSource: ContributionsSource
    private let sessionRepository: SessionRepository
    private let contributionRepository: ContributionRepository
    private let updateStatusRepository: UpdateStatusRepository
    private let stagedWallpaper: StagedWallpaperStore
    private let widgetUpdater: WidgetUpdater

    public init(
        deviceFlowClient: DeviceFlowClient,
        contributionsSource: ContributionsSource,
        sessionRepository: SessionRepository,
        contributionRepository: ContributionRepository,
        updateStatusRepository: UpdateStatusRepository,
        stagedWallpaper: StagedWallpaperStore,
        widgetUpdater: WidgetUpdater
    ) {
        self.deviceFlowClient = deviceFlowClient
        self.contributionsSource = contributionsSource
        self.sessionRepository = sessionRepository
        self.contributionRepository = contributionRepository
        self.updateStatusRepository = updateStatusRepository
        self.stagedWallpaper = stagedWallpaper
        self.widgetUpdater = widgetUpdater
    }

    public var isDeviceFlowAvailable: Bool {
        deviceFlowClient.isConfigured
    }

    public func requestDeviceCode() async throws -> DeviceCode {
        try await deviceFlowClient.requestDeviceCode()
    }

    public func awaitDeviceAuthorization(_ deviceCode: DeviceCode) async throws -> String {
        try await deviceFlowClient.awaitAccessToken(deviceCode)
    }

    public func signIn(token: String) async throws {
        let sanitizedToken = token.trimmingCharacters(in: .whitespacesAndNewlines)
        let calendar = try await contributionsSource.fetchCalendar(token: sanitizedToken)
        await contributionRepository.save(calendar)
        try sessionRepository.save(Session(login: calendar.login, token: sanitizedToken))
        await widgetUpdater.updateAll()
    }

    public func signOut() async {
        sessionRepository.clear()
        await contributionRepository.clear()
        updateStatusRepository.clear()
        stagedWallpaper.clear()
        await widgetUpdater.updateAll()
    }
}
