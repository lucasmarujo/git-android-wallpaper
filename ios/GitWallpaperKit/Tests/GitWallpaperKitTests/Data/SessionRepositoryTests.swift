import Foundation
import GitWallpaperKit
import XCTest

final class SessionRepositoryTests: XCTestCase {
    private let stores = TestStores()
    private let secretStore = FakeSecretStore()
    private let notificationCenter = NotificationCenter()
    private lazy var repository = SessionRepository(defaults: stores.defaults, secretStore: secretStore, notificationCenter: notificationCenter)

    override func tearDown() {
        stores.tearDown()
        super.tearDown()
    }

    func testNoSessionIsStoredInitially() {
        XCTAssertNil(repository.currentSession())
    }

    func testSavedSessionsAreReadBack() throws {
        try repository.save(Session(login: "octocat", token: "gho_secret"))

        XCTAssertEqual(Session(login: "octocat", token: "gho_secret"), repository.currentSession())
    }

    func testTokensAreKeptInTheSecretStoreAndNeverInUserDefaults() throws {
        try repository.save(Session(login: "octocat", token: "gho_secret"))

        XCTAssertEqual("gho_secret", secretStore.storedSecret)
        let plist = stores.defaults.dictionaryRepresentation().values.compactMap { $0 as? String }
        XCTAssertFalse(plist.contains("gho_secret"))
    }

    func testUnreadableTokensAreTreatedAsSignedOut() throws {
        try repository.save(Session(login: "octocat", token: "gho_secret"))
        secretStore.failOnRead = true

        XCTAssertNil(repository.currentSession())
    }

    func testClearRemovesTheSession() throws {
        try repository.save(Session(login: "octocat", token: "gho_secret"))

        repository.clear()

        XCTAssertNil(repository.currentSession())
        XCTAssertNil(secretStore.storedSecret)
    }

    func testChangesAreBroadcast() throws {
        let notifications = Locked(0)
        let observer = notificationCenter.addObserver(forName: SessionRepository.didChangeNotification, object: nil, queue: nil) { _ in
            notifications.mutate { $0 += 1 }
        }
        defer { notificationCenter.removeObserver(observer) }

        try repository.save(Session(login: "octocat", token: "gho_secret"))
        repository.clear()

        XCTAssertEqual(2, notifications.value)
    }

    func testSessionDescriptionHidesTheToken() {
        let description = String(describing: Session(login: "octocat", token: "gho_secret"))

        XCTAssertFalse(description.contains("gho_secret"))
        XCTAssertTrue(description.contains("octocat"))
    }
}
