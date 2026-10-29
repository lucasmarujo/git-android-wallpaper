import CoreGraphics
import Foundation
import GitWallpaperKit
import XCTest

final class AuthRepositoryTests: XCTestCase {
    private let stores = TestStores()
    private let source = FakeContributionsSource()
    private let widgetUpdater = FakeWidgetUpdater()
    private let server = StubServer()
    private lazy var sessionRepository = SessionRepository(defaults: stores.defaults, secretStore: FakeSecretStore(), notificationCenter: NotificationCenter())
    private lazy var contributionRepository = ContributionRepository(
        fileURL: stores.file("contributions.json"),
        contributionsSource: source,
        sessionRepository: sessionRepository
    )
    private lazy var updateStatusRepository = UpdateStatusRepository(defaults: stores.defaults)
    private lazy var stagedWallpaper = StagedWallpaperStore(fileURL: stores.file("wallpaper.png"))

    override func tearDown() {
        stores.tearDown()
        super.tearDown()
    }

    func testSignInValidatesTheTokenAndStoresSessionAndCalendar() async throws {
        let repository = makeRepository()

        try await repository.signIn(token: "  gho_token \n")

        XCTAssertEqual(["gho_token"], source.requestedTokens)
        XCTAssertEqual(Session(login: "octocat", token: "gho_token"), sessionRepository.currentSession())
        let cached = await contributionRepository.cachedCalendar()
        XCTAssertEqual(TestCalendars.calendar(), cached)
        XCTAssertEqual(1, widgetUpdater.updateAllCount)
    }

    func testRejectedTokensDoNotCreateASession() async {
        source.handler = { _ in throw GitHubError.unauthorized }
        let repository = makeRepository()

        await XCTAssertThrowsErrorAsync(try await repository.signIn(token: "bad")) { error in
            XCTAssertEqual(.unauthorized, error as? GitHubError)
        }
        XCTAssertNil(sessionRepository.currentSession())
        let cached = await contributionRepository.cachedCalendar()
        XCTAssertNil(cached)
    }

    func testSignOutClearsLocalDataAndTheStagedWallpaper() async throws {
        let repository = makeRepository()
        try await repository.signIn(token: "gho_token")
        updateStatusRepository.markApplied(fingerprint: "abc", at: Date())
        try stagedWallpaper.apply(solidImage(color: ARGBColor(argb: 0xFF00_0000), size: 4), target: .lockScreen)

        await repository.signOut()

        XCTAssertNil(sessionRepository.currentSession())
        let cached = await contributionRepository.cachedCalendar()
        XCTAssertNil(cached)
        XCTAssertEqual(UpdateStatus(), updateStatusRepository.current())
        XCTAssertFalse(stagedWallpaper.hasImage)
        XCTAssertEqual(2, widgetUpdater.updateAllCount)
    }

    func testDeviceFlowAvailabilityFollowsTheClientConfiguration() {
        XCTAssertTrue(makeRepository(clientId: "client").isDeviceFlowAvailable)
        XCTAssertFalse(makeRepository(clientId: "").isDeviceFlowAvailable)
    }

    private func makeRepository(clientId: String = "client") -> AuthRepository {
        AuthRepository(
            deviceFlowClient: DeviceFlowClient(session: server.makeSession(), clientId: clientId, baseURL: server.url("/")),
            contributionsSource: source,
            sessionRepository: sessionRepository,
            contributionRepository: contributionRepository,
            updateStatusRepository: updateStatusRepository,
            stagedWallpaper: stagedWallpaper,
            widgetUpdater: widgetUpdater
        )
    }
}
