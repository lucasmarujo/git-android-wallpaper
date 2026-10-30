import Foundation
import GitWallpaperKit
import XCTest
@testable import GitWallpaper

@MainActor
final class HomeViewModelTests: XCTestCase {
    private let stores = TestStores()
    private let source = FakeContributionsSource()
    private let wallpaperUpdater = FakeWallpaperUpdater()
    private let widgetUpdater = FakeWidgetUpdater()
    private let urlOpener = RecordingURLOpener()
    private let server = StubServer()
    private let previewSize = ScreenSize(width: 108, height: 234)
    private let adjustmentSize = ScreenSize(width: 270, height: 585)
    private let clock = WallClock(
        now: { ISO8601DateFormatter().date(from: "2026-10-24T12:00:00Z")! },
        timeZone: { TimeZone(identifier: "UTC")! }
    )

    private var sessionRepository: SessionRepository!
    private var settingsRepository: SettingsRepository!
    private var updateStatusRepository: UpdateStatusRepository!
    private var contributionRepository: ContributionRepository!
    private var stagedWallpaper: StagedWallpaperStore!
    private var wallpaperShortcut: WallpaperShortcut!

    override func setUp() async throws {
        sessionRepository = SessionRepository(defaults: stores.defaults, secretStore: FakeSecretStore(), notificationCenter: NotificationCenter())
        try sessionRepository.save(Session(login: "octocat", token: "gho_token"))
        settingsRepository = SettingsRepository(defaults: stores.defaults)
        updateStatusRepository = UpdateStatusRepository(defaults: stores.defaults)
        contributionRepository = ContributionRepository(
            fileURL: stores.file("contributions.json"),
            contributionsSource: source,
            sessionRepository: sessionRepository
        )
        stagedWallpaper = StagedWallpaperStore(fileURL: stores.file("wallpaper.png"))
        wallpaperShortcut = WallpaperShortcut(defaults: stores.defaults)
    }

    override func tearDown() async throws {
        stores.tearDown()
    }

    func testOpeningTheScreenRefreshesContributionsAndRendersThePreview() async {
        let viewModel = makeViewModel()

        await viewModel.start()

        XCTAssertEqual(["gho_token"], source.requestedTokens)
        XCTAssertTrue(viewModel.uiState.hasCalendar)
        XCTAssertFalse(viewModel.uiState.isLoadingCalendar)
        XCTAssertEqual(previewSize.width, viewModel.uiState.preview?.width)
        XCTAssertEqual(previewSize.height, viewModel.uiState.preview?.height)
        XCTAssertEqual(ContributionStats.from(TestCalendars.calendar().days, periodStart: LocalDate(year: 2026, month: 1, day: 1)), viewModel.uiState.stats)
        XCTAssertGreaterThan(widgetUpdater.updateAllCount, 0)
    }

    func testTappingApplyOpensTheAdjustmentWithAFullSizePreview() async {
        let viewModel = makeViewModel()
        await viewModel.start()

        await viewModel.startAdjusting()

        XCTAssertTrue(viewModel.uiState.isAdjusting)
        XCTAssertEqual(adjustmentSize.width, viewModel.uiState.adjustmentPreview?.width)
        XCTAssertEqual(adjustmentSize.height, viewModel.uiState.adjustmentPreview?.height)
    }

    func testApplyingFromTheAdjustmentSavesTheFramingAndClosesIt() async {
        let viewModel = makeViewModel()
        await viewModel.start()
        await viewModel.startAdjusting()
        let framing = WallpaperFraming(zoom: 1.4, offsetX: 0.1, offsetY: -0.05)

        await viewModel.applyWallpaper(framing)

        XCTAssertEqual([true], wallpaperUpdater.forcedCalls)
        XCTAssertEqual(framing, settingsRepository.current().framing)
        XCTAssertEqual(framing, viewModel.uiState.settings.framing)
        XCTAssertFalse(viewModel.uiState.isAdjusting)
        XCTAssertFalse(viewModel.uiState.isApplying)
        XCTAssertNil(viewModel.uiState.adjustmentPreview)
    }

    func testTheFirstApplyAsksToSetUpTheShortcut() async {
        let viewModel = makeViewModel()
        await viewModel.start()

        await viewModel.applyWallpaper(WallpaperFraming())

        XCTAssertFalse(viewModel.isShortcutSetupPresented)
        viewModel.adjustmentDismissed()
        XCTAssertTrue(viewModel.isShortcutSetupPresented)
        XCTAssertTrue(urlOpener.openedURLs.isEmpty)
    }

    func testApplyingRunsTheShortcutOnceItWorked() async {
        wallpaperShortcut.isReady = true
        let viewModel = makeViewModel()
        await viewModel.start()

        await viewModel.applyWallpaper(WallpaperFraming())
        viewModel.adjustmentDismissed()

        XCTAssertFalse(viewModel.isShortcutSetupPresented)
        XCTAssertEqual([wallpaperShortcut.runURL], urlOpener.openedURLs)
    }

    func testASuccessfulShortcutRunNotifiesAndIsRemembered() async {
        let viewModel = makeViewModel()
        await viewModel.start()

        viewModel.handleShortcutCallback(.success)

        XCTAssertEqual(.applied, viewModel.messageEvent?.message)
        XCTAssertTrue(viewModel.uiState.isShortcutReady)
        XCTAssertTrue(WallpaperShortcut(defaults: stores.defaults).isReady)
    }

    func testAFailedShortcutRunShowsTheSetupAgain() async {
        wallpaperShortcut.isReady = true
        let viewModel = makeViewModel()
        await viewModel.start()

        viewModel.handleShortcutCallback(.error)

        XCTAssertEqual(.shortcutFailed, viewModel.messageEvent?.message)
        XCTAssertTrue(viewModel.isShortcutSetupPresented)
        XCTAssertFalse(viewModel.uiState.isShortcutReady)
    }

    func testACancelledShortcutRunChangesNothing() async {
        wallpaperShortcut.isReady = true
        let viewModel = makeViewModel()
        await viewModel.start()

        viewModel.handleShortcutCallback(.cancel)

        XCTAssertNil(viewModel.messageEvent)
        XCTAssertFalse(viewModel.isShortcutSetupPresented)
        XCTAssertTrue(viewModel.uiState.isShortcutReady)
    }

    func testRunningTheShortcutFromTheSetupClosesIt() async {
        let viewModel = makeViewModel()
        viewModel.presentShortcutSetup()

        viewModel.runShortcut()

        XCTAssertFalse(viewModel.isShortcutSetupPresented)
        XCTAssertEqual([wallpaperShortcut.runURL], urlOpener.openedURLs)
    }

    func testCancellingTheAdjustmentKeepsTheWallpaperAndTheFraming() async {
        settingsRepository.update { $0.framing = WallpaperFraming(zoom: 1.2) }
        let viewModel = makeViewModel()
        await viewModel.start()
        await viewModel.startAdjusting()

        viewModel.cancelAdjusting()

        XCTAssertFalse(viewModel.uiState.isAdjusting)
        XCTAssertTrue(wallpaperUpdater.forcedCalls.isEmpty)
        XCTAssertEqual(WallpaperFraming(zoom: 1.2), settingsRepository.current().framing)
    }

    func testChangingSettingsPersistsThemRedrawsTheWidgetsAndThePreview() async {
        let viewModel = makeViewModel()
        await viewModel.start()
        let widgetUpdatesBefore = widgetUpdater.updateAllCount
        let previewBefore = viewModel.uiState.preview?.dataProvider?.data as Data?

        await viewModel.updateSettings { $0.theme = .sunset }.value

        XCTAssertEqual(.sunset, settingsRepository.current().theme)
        XCTAssertEqual(.sunset, viewModel.uiState.settings.theme)
        XCTAssertEqual(widgetUpdatesBefore + 1, widgetUpdater.updateAllCount)
        XCTAssertNotEqual(previewBefore, viewModel.uiState.preview?.dataProvider?.data as Data?)
    }

    func testWallpaperFailuresAreReported() async {
        wallpaperUpdater.result = .failure(.wallpaper)
        let viewModel = makeViewModel()
        await viewModel.start()

        await viewModel.applyWallpaper(WallpaperFraming())

        viewModel.adjustmentDismissed()

        XCTAssertEqual(.wallpaperError, viewModel.messageEvent?.message)
        XCTAssertFalse(viewModel.isShortcutSetupPresented)
    }

    func testManualRefreshSuccessIsReported() async {
        let viewModel = makeViewModel()
        await viewModel.start()

        await viewModel.refresh()

        XCTAssertEqual(.refreshed, viewModel.messageEvent?.message)
    }

    func testManualRefreshFailuresAreReported() async {
        let viewModel = makeViewModel()
        await viewModel.start()
        source.handler = { _ in throw GitHubError.network(URLError(.notConnectedToInternet)) }

        await viewModel.refresh()

        XCTAssertEqual(.networkError, viewModel.messageEvent?.message)
        XCTAssertTrue(viewModel.uiState.hasCalendar)
    }

    func testTheFirstLoadWithoutNetworkShowsTheRetryState() async {
        source.handler = { _ in throw GitHubError.network(URLError(.notConnectedToInternet)) }
        let viewModel = makeViewModel()

        await viewModel.start()

        XCTAssertFalse(viewModel.uiState.hasCalendar)
        XCTAssertFalse(viewModel.uiState.isLoadingCalendar)
        XCTAssertNil(viewModel.uiState.preview)
        XCTAssertNil(viewModel.messageEvent)
    }

    func testSigningOutClearsTheSessionAndTheStagedWallpaper() async throws {
        try stagedWallpaper.apply(solidImage(), target: .lockScreen)
        let viewModel = makeViewModel()
        await viewModel.start()

        await viewModel.signOut()

        XCTAssertNil(sessionRepository.currentSession())
        XCTAssertFalse(stagedWallpaper.hasImage)
        let cached = await contributionRepository.cachedCalendar()
        XCTAssertNil(cached)
    }

    func testClearingAMessageOnlyRemovesTheSameEvent() async {
        let viewModel = makeViewModel()
        await viewModel.start()
        await viewModel.refresh()
        let first = viewModel.messageEvent
        await viewModel.refresh()

        if let first { viewModel.clearMessage(first) }

        XCTAssertNotNil(viewModel.messageEvent)
    }

    private func makeViewModel() -> HomeViewModel {
        HomeViewModel(
            login: "octocat",
            contributionRepository: contributionRepository,
            settingsRepository: settingsRepository,
            updateStatusRepository: updateStatusRepository,
            wallpaperUpdater: wallpaperUpdater,
            widgetUpdater: widgetUpdater,
            authRepository: AuthRepository(
                deviceFlowClient: DeviceFlowClient(session: server.makeSession(), clientId: "client", baseURL: server.url("/")),
                contributionsSource: source,
                sessionRepository: sessionRepository,
                contributionRepository: contributionRepository,
                updateStatusRepository: updateStatusRepository,
                stagedWallpaper: stagedWallpaper,
                widgetUpdater: widgetUpdater
            ),
            avatarSource: FakeAvatarSource(),
            renderer: ContributionWallpaperRenderer(),
            stagedWallpaper: stagedWallpaper,
            wallpaperShortcut: wallpaperShortcut,
            urlOpener: urlOpener,
            previewSize: previewSize,
            adjustmentSize: adjustmentSize,
            clock: clock
        )
    }

    private func solidImage() -> CGImage {
        let context = CGContext(
            data: nil,
            width: 4,
            height: 4,
            bitsPerComponent: 8,
            bytesPerRow: 0,
            space: CGColorSpace(name: CGColorSpace.sRGB)!,
            bitmapInfo: CGImageAlphaInfo.premultipliedLast.rawValue
        )!
        return context.makeImage()!
    }
}
