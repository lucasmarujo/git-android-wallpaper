import Foundation
import GitWallpaperKit
import XCTest

final class SharedContainerTests: XCTestCase {
    private let stores = TestStores()
    private let source = FakeContributionsSource()
    private let secretStore = FakeSecretStore()

    override func tearDown() {
        stores.tearDown()
        super.tearDown()
    }

    func testInjectedDependenciesBackEveryRepository() async throws {
        let container = makeContainer()

        try container.sessionRepository.save(Session(login: "octocat", token: "gho_token"))
        container.settingsRepository.update { $0.theme = .ocean }
        let refresh = await container.contributionRepository.refresh()

        XCTAssertEqual(.success(TestCalendars.calendar()), refresh)
        XCTAssertEqual(["gho_token"], source.requestedTokens)
        XCTAssertEqual("gho_token", secretStore.storedSecret)
        XCTAssertEqual("OCEAN", stores.defaults.string(forKey: "theme"))
        XCTAssertTrue(FileManager.default.fileExists(atPath: stores.file("contributions.json").path))
    }

    func testTheStagedWallpaperLivesInTheGivenDirectory() {
        let container = makeContainer()

        XCTAssertEqual(stores.file("wallpaper.png"), container.stagedWallpaper.fileURL)
    }

    private func makeContainer() -> SharedContainer {
        SharedContainer(
            urlSession: URLSession(configuration: .ephemeral),
            contributionsSource: source,
            secretStore: secretStore,
            defaults: stores.defaults,
            directory: stores.directory,
            clock: .system
        )
    }
}
