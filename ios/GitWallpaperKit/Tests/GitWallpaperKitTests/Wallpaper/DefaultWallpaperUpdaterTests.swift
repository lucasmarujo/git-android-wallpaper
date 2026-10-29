import CoreGraphics
import Foundation
import GitWallpaperKit
import XCTest

final class DefaultWallpaperUpdaterTests: XCTestCase {
    private let stores = TestStores()
    private let clock = MutableClock(ISO8601DateFormatter().date(from: "2026-10-24T12:00:00Z")!)
    private let source = FakeContributionsSource()
    private let avatarSource = FakeAvatarSource()
    private let applier = RecordingWallpaperApplier()
    private let screenSize = ScreenSize(width: 270, height: 585)
    private let renderer = ContributionWallpaperRenderer(strings: WallpaperStrings(localization: "en")!)

    private var sessionRepository: SessionRepository!
    private var contributionRepository: ContributionRepository!
    private var settingsRepository: SettingsRepository!
    private var updateStatusRepository: UpdateStatusRepository!

    override func tearDown() {
        stores.tearDown()
        super.tearDown()
    }

    func testApplyingRendersAppliesAndRecordsTheStatus() async throws {
        let updater = try makeUpdater()

        let result = await updater.update(force: true)

        XCTAssertEqual(.applied, result)
        XCTAssertEqual(
            [RecordingWallpaperApplier.Applied(width: screenSize.width, height: screenSize.height, target: .lockScreen)],
            applier.applied
        )
        let status = updateStatusRepository.current()
        XCTAssertNotNil(status.fingerprint)
        XCTAssertEqual(clock.now, status.lastAppliedAt)
    }

    func testAutomaticUpdatesNeverStageAWallpaperThatWasNotAppliedYet() async throws {
        let updater = try makeUpdater()

        let result = await updater.update(force: false)

        XCTAssertEqual(.unchanged, result)
        XCTAssertTrue(applier.applied.isEmpty)
        let cached = await contributionRepository.cachedCalendar()
        XCTAssertEqual(TestCalendars.calendar(), cached)
        let status = updateStatusRepository.current()
        XCTAssertNil(status.lastAppliedAt)
        XCTAssertEqual(clock.now, status.lastCheckedAt)
    }

    func testUnchangedContributionsDoNotReapplyTheWallpaper() async throws {
        let updater = try makeUpdater()
        _ = await updater.update(force: true)

        let result = await updater.update(force: false)

        XCTAssertEqual(.unchanged, result)
        XCTAssertEqual(1, applier.applied.count)
    }

    func testForcedUpdatesAlwaysReapply() async throws {
        let updater = try makeUpdater()
        _ = await updater.update(force: true)

        let result = await updater.update(force: true)

        XCTAssertEqual(.applied, result)
        XCTAssertEqual(2, applier.applied.count)
    }

    func testNewContributionsReapplyTheWallpaper() async throws {
        let updater = try makeUpdater()
        _ = await updater.update(force: true)
        source.handler = { _ in TestCalendars.calendar(counts: Array(repeating: 4, count: 371)) }

        let result = await updater.update(force: false)

        XCTAssertEqual(.applied, result)
        XCTAssertEqual(2, applier.applied.count)
    }

    func testANewDayReappliesTheWallpaperToMoveTheHighlight() async throws {
        let updater = try makeUpdater()
        _ = await updater.update(force: true)
        clock.now = clock.now.addingTimeInterval(86_400)

        let result = await updater.update(force: false)

        XCTAssertEqual(.applied, result)
        XCTAssertEqual(2, applier.applied.count)
    }

    func testOfflineUpdatesRedrawTheNewDayFromTheCacheAndReportTheFailure() async throws {
        let updater = try makeUpdater()
        _ = await updater.update(force: true)
        source.handler = { _ in throw GitHubError.network(URLError(.notConnectedToInternet)) }
        clock.now = clock.now.addingTimeInterval(86_400)

        let result = await updater.update(force: false)

        XCTAssertEqual(.failure(.network), result)
        XCTAssertEqual(2, applier.applied.count)
    }

    func testOfflineUpdatesWithoutChangesKeepTheCurrentWallpaper() async throws {
        let updater = try makeUpdater()
        _ = await updater.update(force: true)
        source.handler = { _ in throw GitHubError.network(URLError(.notConnectedToInternet)) }

        let result = await updater.update(force: false)

        XCTAssertEqual(.failure(.network), result)
        XCTAssertEqual(1, applier.applied.count)
    }

    func testUpdatesWithoutCacheReportTheNetworkFailure() async throws {
        let updater = try makeUpdater()
        source.handler = { _ in throw GitHubError.network(URLError(.notConnectedToInternet)) }

        let result = await updater.update(force: false)

        XCTAssertEqual(.failure(.network), result)
        XCTAssertTrue(applier.applied.isEmpty)
    }

    func testForcedUpdatesUseTheCachedCalendarWhenOffline() async throws {
        let updater = try makeUpdater()
        await contributionRepository.save(TestCalendars.calendar())
        source.handler = { _ in throw GitHubError.network(URLError(.notConnectedToInternet)) }

        let result = await updater.update(force: true)

        XCTAssertEqual(.applied, result)
        XCTAssertEqual(1, applier.applied.count)
    }

    func testWallpaperTargetFollowsTheSettings() async throws {
        let updater = try makeUpdater()
        settingsRepository.update { $0.target = .both }

        _ = await updater.update(force: true)

        XCTAssertEqual([.both], applier.applied.map(\.target))
    }

    func testANewFramingReappliesTheWallpaperOnTheNextAutomaticUpdate() async throws {
        let updater = try makeUpdater()
        _ = await updater.update(force: true)
        settingsRepository.update { $0.framing = WallpaperFraming(zoom: 1.4) }

        let result = await updater.update(force: false)

        XCTAssertEqual(.applied, result)
        XCTAssertEqual(2, applier.applied.count)
    }

    func testAutomaticUpdatesDrawTheSavedFraming() async throws {
        let updater = try makeUpdater()
        let framed = WallpaperSettings(framing: WallpaperFraming(zoom: 1.4, offsetY: 0.1))
        settingsRepository.update { $0 = framed }
        updateStatusRepository.markApplied(fingerprint: "previous", at: Date(timeIntervalSince1970: 0))

        _ = await updater.update(force: false)

        let applied = try XCTUnwrap(applier.lastImage)
        let expected = try renderer.render(calendar: TestCalendars.calendar(), settings: framed, size: screenSize, today: TestCalendars.today, avatar: nil)
        let unframed = try renderer.render(calendar: TestCalendars.calendar(), settings: WallpaperSettings(), size: screenSize, today: TestCalendars.today, avatar: nil)
        XCTAssertEqual(expected.pngData(), applied.pngData())
        XCTAssertNotEqual(unframed.pngData(), applied.pngData())
    }

    func testTheAvatarIsLoadedOnlyWhenRedrawing() async throws {
        let avatarUrl = "https://avatars.githubusercontent.com/u/1?v=4"
        source.handler = { _ in TestCalendars.calendar(avatarUrl: avatarUrl) }
        let updater = try makeUpdater()

        _ = await updater.update(force: true)
        _ = await updater.update(force: false)

        XCTAssertEqual([avatarUrl], avatarSource.requestedUrls)
    }

    func testSignedOutUsersAreNotUpdated() async throws {
        let updater = try makeUpdater(signedIn: false)

        let result = await updater.update(force: false)

        XCTAssertEqual(.failure(.notSignedIn), result)
        XCTAssertTrue(applier.applied.isEmpty)
    }

    func testRevokedTokensEndTheSession() async throws {
        let updater = try makeUpdater()
        source.handler = { _ in throw GitHubError.unauthorized }

        let result = await updater.update(force: true)

        XCTAssertEqual(.failure(.sessionExpired), result)
        XCTAssertNil(sessionRepository.currentSession())
        XCTAssertTrue(applier.applied.isEmpty)
    }

    func testWallpaperFailuresAreReportedAndNotRecordedAsApplied() async throws {
        let updater = try makeUpdater()
        applier.failure = CocoaError(.fileWriteOutOfSpace)

        let result = await updater.update(force: true)

        XCTAssertEqual(.failure(.wallpaper), result)
        XCTAssertNil(updateStatusRepository.current().lastAppliedAt)
    }

    func testConcurrentUpdatesRunOneAtATime() async throws {
        let updater = try makeUpdater()

        async let first = updater.update(force: true)
        async let second = updater.update(force: false)
        let results = await [first, second]

        XCTAssertEqual(1, applier.applied.count)
        XCTAssertTrue(results.contains(.applied))
        XCTAssertTrue(results.contains(.unchanged))
    }

    private func makeUpdater(signedIn: Bool = true) throws -> DefaultWallpaperUpdater {
        sessionRepository = SessionRepository(defaults: stores.defaults, secretStore: FakeSecretStore(), notificationCenter: NotificationCenter())
        if signedIn { try sessionRepository.save(Session(login: "octocat", token: "gho_token")) }
        contributionRepository = ContributionRepository(
            fileURL: stores.file("contributions.json"),
            contributionsSource: source,
            sessionRepository: sessionRepository
        )
        settingsRepository = SettingsRepository(defaults: stores.defaults)
        updateStatusRepository = UpdateStatusRepository(defaults: stores.defaults)
        return DefaultWallpaperUpdater(
            contributionRepository: contributionRepository,
            settingsRepository: settingsRepository,
            updateStatusRepository: updateStatusRepository,
            avatarSource: avatarSource,
            renderer: renderer,
            applier: applier,
            screenSizeProvider: FixedScreenSizeProvider(size: screenSize),
            clock: clock.wallClock
        )
    }
}
