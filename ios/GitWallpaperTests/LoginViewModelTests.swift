import Combine
import Foundation
import GitWallpaperKit
import XCTest
@testable import GitWallpaper

@MainActor
final class LoginViewModelTests: XCTestCase {
    private let stores = TestStores()
    private let source = FakeContributionsSource()
    private let widgetUpdater = FakeWidgetUpdater()
    private let server = StubServer()
    private var sessionRepository: SessionRepository!
    private var cancellables: Set<AnyCancellable> = []

    override func setUp() async throws {
        sessionRepository = SessionRepository(defaults: stores.defaults, secretStore: FakeSecretStore(), notificationCenter: NotificationCenter())
    }

    override func tearDown() async throws {
        cancellables.removeAll()
        stores.tearDown()
    }

    func testDeviceFlowShowsTheUserCodeAndSignsInOnceAuthorized() async {
        server.enqueue(.json(Self.deviceCodeResponse))
        server.enqueue(.json(#"{"error":"authorization_pending"}"#))
        server.enqueue(.json(#"{"access_token":"gho_device"}"#))
        let viewModel = makeViewModel()
        var states: [LoginUiState] = []
        viewModel.$uiState.sink { states.append($0) }.store(in: &cancellables)

        await viewModel.startDeviceFlow().value

        XCTAssertTrue(states.contains(.requestingCode))
        XCTAssertTrue(states.contains(.awaitingAuthorization(userCode: "ABCD-1234", verificationUri: "https://github.com/login/device")))
        XCTAssertTrue(states.contains(.signingIn))
        XCTAssertEqual(.idle, viewModel.uiState)
        XCTAssertEqual(["gho_device"], source.requestedTokens)
        XCTAssertEqual(Session(login: "octocat", token: "gho_device"), sessionRepository.currentSession())
    }

    func testDeniedAuthorizationShowsAnError() async {
        server.enqueue(.json(Self.deviceCodeResponse))
        server.enqueue(.json(#"{"error":"access_denied"}"#))
        let viewModel = makeViewModel()

        await viewModel.startDeviceFlow().value

        XCTAssertEqual(.error(.accessDenied), viewModel.uiState)
        XCTAssertNil(sessionRepository.currentSession())
    }

    func testDisabledDeviceFlowSuggestsAPersonalAccessToken() async {
        server.enqueue(.json(#"{"error":"device_flow_disabled"}"#))
        let viewModel = makeViewModel()

        await viewModel.startDeviceFlow().value

        XCTAssertEqual(.error(.deviceFlowUnavailable), viewModel.uiState)
    }

    func testNetworkFailuresWhileRequestingACodeShowANetworkError() async {
        server.enqueue(.networkFailure())
        let viewModel = makeViewModel()

        await viewModel.startDeviceFlow().value

        XCTAssertEqual(.error(.network), viewModel.uiState)
    }

    func testCancellingStopsWaitingForAuthorization() async {
        server.enqueue(.json(Self.deviceCodeResponse))
        let viewModel = makeViewModel(sleep: { _ in try await Task.sleep(nanoseconds: 60_000_000_000) })

        let task = viewModel.startDeviceFlow()
        await waitUntil { if case .awaitingAuthorization = viewModel.uiState { true } else { false } }
        viewModel.cancel()
        await task.value

        XCTAssertEqual(.idle, viewModel.uiState)
        XCTAssertNil(sessionRepository.currentSession())
    }

    func testPersonalAccessTokensSignInDirectly() async {
        let viewModel = makeViewModel()

        await viewModel.signInWithToken("  gho_pat  ")?.value

        XCTAssertEqual(.idle, viewModel.uiState)
        XCTAssertEqual(["gho_pat"], source.requestedTokens)
        XCTAssertEqual("octocat", sessionRepository.currentSession()?.login)
    }

    func testRejectedPersonalAccessTokensShowAnError() async {
        source.handler = { _ in throw GitHubError.unauthorized }
        let viewModel = makeViewModel()

        await viewModel.signInWithToken("gho_bad")?.value

        XCTAssertEqual(.error(.invalidToken), viewModel.uiState)
        XCTAssertNil(sessionRepository.currentSession())
    }

    func testBlankTokensAreIgnored() {
        let viewModel = makeViewModel()

        XCTAssertNil(viewModel.signInWithToken("   "))
        XCTAssertEqual(.idle, viewModel.uiState)
        XCTAssertTrue(source.requestedTokens.isEmpty)
    }

    func testDeviceFlowAvailabilityFollowsTheClientId() {
        XCTAssertTrue(makeViewModel().isDeviceFlowAvailable)
        XCTAssertFalse(makeViewModel(clientId: "").isDeviceFlowAvailable)
    }

    private func makeViewModel(
        clientId: String = "client",
        sleep: @escaping DeviceFlowClient.Sleeper = { _ in }
    ) -> LoginViewModel {
        let contributionRepository = ContributionRepository(
            fileURL: stores.file("contributions.json"),
            contributionsSource: source,
            sessionRepository: sessionRepository
        )
        return LoginViewModel(
            authRepository: AuthRepository(
                deviceFlowClient: DeviceFlowClient(session: server.makeSession(), clientId: clientId, baseURL: server.url("/"), sleep: sleep),
                contributionsSource: source,
                sessionRepository: sessionRepository,
                contributionRepository: contributionRepository,
                updateStatusRepository: UpdateStatusRepository(defaults: stores.defaults),
                stagedWallpaper: StagedWallpaperStore(fileURL: stores.file("wallpaper.png")),
                widgetUpdater: widgetUpdater
            )
        )
    }

    private static let deviceCodeResponse = #"{"device_code":"device-123","user_code":"ABCD-1234","verification_uri":"https://github.com/login/device","expires_in":900,"interval":5}"#
}
