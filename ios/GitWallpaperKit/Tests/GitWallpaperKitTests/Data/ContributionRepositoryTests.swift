import Foundation
import GitWallpaperKit
import XCTest

final class ContributionRepositoryTests: XCTestCase {
    private let stores = TestStores()
    private let source = FakeContributionsSource()
    private lazy var sessionRepository = SessionRepository(defaults: stores.defaults, secretStore: FakeSecretStore(), notificationCenter: NotificationCenter())
    private lazy var repository = ContributionRepository(
        fileURL: stores.file("contributions.json"),
        contributionsSource: source,
        sessionRepository: sessionRepository
    )

    override func tearDown() {
        stores.tearDown()
        super.tearDown()
    }

    func testRefreshWithoutASessionReportsNotSignedIn() async {
        let result = await repository.refresh()

        XCTAssertEqual(.failure(.notSignedIn), result)
        XCTAssertTrue(source.requestedTokens.isEmpty)
    }

    func testRefreshFetchesWithTheSessionTokenAndCachesTheCalendar() async throws {
        try sessionRepository.save(Session(login: "octocat", token: "gho_token"))

        let result = await repository.refresh()

        XCTAssertEqual(.success(TestCalendars.calendar()), result)
        XCTAssertEqual(["gho_token"], source.requestedTokens)
        let cached = await repository.cachedCalendar()
        XCTAssertEqual(TestCalendars.calendar(), cached)
    }

    func testUnauthorizedResponsesClearTheSession() async throws {
        try sessionRepository.save(Session(login: "octocat", token: "gho_token"))
        source.handler = { _ in throw GitHubError.unauthorized }

        let result = await repository.refresh()

        XCTAssertEqual(.failure(.sessionExpired), result)
        XCTAssertNil(sessionRepository.currentSession())
    }

    func testNetworkAndApiFailuresKeepTheCachedCalendar() async throws {
        try sessionRepository.save(Session(login: "octocat", token: "gho_token"))
        await repository.save(TestCalendars.calendar())

        source.handler = { _ in throw GitHubError.network(URLError(.timedOut)) }
        let network = await repository.refresh()
        source.handler = { _ in throw GitHubError.api("boom") }
        let api = await repository.refresh()

        XCTAssertEqual(.failure(.network), network)
        XCTAssertEqual(.failure(.github), api)
        let cached = await repository.cachedCalendar()
        XCTAssertEqual(TestCalendars.calendar(), cached)
    }

    func testCorruptedCacheEntriesAreIgnored() async throws {
        try FileManager.default.createDirectory(at: stores.directory, withIntermediateDirectories: true)
        try Data("{not json".utf8).write(to: stores.file("contributions.json"))

        let cached = await repository.cachedCalendar()

        XCTAssertNil(cached)
    }

    func testTheCacheIsExcludedFromBackups() async throws {
        await repository.save(TestCalendars.calendar())

        let values = try stores.file("contributions.json").resourceValues(forKeys: [.isExcludedFromBackupKey])
        XCTAssertEqual(true, values.isExcludedFromBackup)
    }

    func testClearRemovesTheCachedCalendar() async {
        await repository.save(TestCalendars.calendar())

        await repository.clear()

        let cached = await repository.cachedCalendar()
        XCTAssertNil(cached)
    }
}
