import Foundation
import GitWallpaperKit
import XCTest
@testable import GitWallpaper

@MainActor
final class RootViewModelTests: XCTestCase {
    private let stores = TestStores()
    private let notificationCenter = NotificationCenter()

    override func tearDown() async throws {
        stores.tearDown()
    }

    func testSignedOutWithoutASession() {
        let viewModel = RootViewModel(sessionRepository: repository(), notificationCenter: notificationCenter)

        viewModel.reload()

        XCTAssertEqual(.signedOut, viewModel.state)
    }

    func testSessionChangesSwitchBetweenLoginAndHome() async throws {
        let repository = repository()
        let viewModel = RootViewModel(sessionRepository: repository, notificationCenter: notificationCenter)
        let observation = Task { await viewModel.observe() }
        defer { observation.cancel() }
        await waitUntil { viewModel.state == .signedOut }

        try repository.save(Session(login: "octocat", token: "gho_token"))
        await waitUntil { viewModel.state == .signedIn(login: "octocat") }

        repository.clear()
        await waitUntil { viewModel.state == .signedOut }
    }

    private func repository() -> SessionRepository {
        SessionRepository(defaults: stores.defaults, secretStore: FakeSecretStore(), notificationCenter: notificationCenter)
    }
}
