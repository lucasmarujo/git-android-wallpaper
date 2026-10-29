import CoreGraphics
import Foundation
import GitWallpaperKit
import XCTest

final class CachedAvatarRepositoryTests: XCTestCase {
    private let stores = TestStores()
    private let server = StubServer()

    override func tearDown() {
        stores.tearDown()
        super.tearDown()
    }

    func testMissingUrlsReturnNoAvatar() async {
        let avatar = await repository().load(url: nil)

        XCTAssertNil(avatar)
        XCTAssertEqual(0, server.requestCount)
    }

    func testUrlsOutsideTheGitHubAvatarCdnAreNeverRequested() async {
        let repository = CachedAvatarRepository(session: server.makeSession(), cacheDirectory: stores.directory)

        let avatar = await repository.load(url: server.url("/u/1").absoluteString)

        XCTAssertNil(avatar)
        XCTAssertEqual(0, server.requestCount)
    }

    func testDownloadsAndDecodesTheAvatar() async throws {
        server.enqueue(.data(try pngData(size: 16)))

        let avatar = await repository().load(url: server.url("/u/1").absoluteString)

        XCTAssertEqual(16, avatar?.width)
    }

    func testCachedAvatarsAreReusedAcrossInstancesWithoutDownloadingAgain() async throws {
        server.enqueue(.data(try pngData(size: 16)))
        let url = server.url("/u/1").absoluteString
        _ = await repository().load(url: url)

        let avatar = await repository().load(url: url)

        XCTAssertEqual(16, avatar?.width)
        XCTAssertEqual(1, server.requestCount)
    }

    func testANewAvatarUrlDownloadsTheNewImage() async throws {
        server.enqueue(.data(try pngData(size: 16)))
        server.enqueue(.data(try pngData(size: 24)))
        let repository = repository()
        _ = await repository.load(url: server.url("/u/1").absoluteString)

        let avatar = await repository.load(url: server.url("/u/2").absoluteString)

        XCTAssertEqual(24, avatar?.width)
        XCTAssertEqual(2, server.requestCount)
    }

    func testServerErrorsReturnNoAvatar() async {
        server.enqueue(.json("", statusCode: 404))

        let avatar = await repository().load(url: server.url("/u/1").absoluteString)

        XCTAssertNil(avatar)
    }

    func testInvalidImagesAreNotCached() async {
        server.enqueue(.json("not an image"))
        server.enqueue(.json("not an image"))
        let url = server.url("/u/1").absoluteString

        _ = await repository().load(url: url)
        let avatar = await repository().load(url: url)

        XCTAssertNil(avatar)
        XCTAssertEqual(2, server.requestCount)
    }

    func testOversizedDownloadsAreRejected() async {
        server.enqueue(.data(Data(count: 1_048_577)))

        let avatar = await repository().load(url: server.url("/u/1").absoluteString)

        XCTAssertNil(avatar)
    }

    func testOnlyHttpsGitHubAvatarHostsAreAllowed() {
        XCTAssertTrue(isGitHubAvatarURL(URL(string: "https://avatars.githubusercontent.com/u/1?v=4")!))
        XCTAssertTrue(isGitHubAvatarURL(URL(string: "https://githubusercontent.com/u/1")!))
        XCTAssertFalse(isGitHubAvatarURL(URL(string: "http://avatars.githubusercontent.com/u/1")!))
        XCTAssertFalse(isGitHubAvatarURL(URL(string: "https://githubusercontent.com.evil.example/u/1")!))
        XCTAssertFalse(isGitHubAvatarURL(URL(string: "https://evilgithubusercontent.com/u/1")!))
    }

    private func repository() -> CachedAvatarRepository {
        CachedAvatarRepository(session: server.makeSession(), cacheDirectory: stores.directory, isAllowedURL: { _ in true })
    }

    private func pngData(size: Int) throws -> Data {
        try XCTUnwrap(solidImage(color: ARGBColor(argb: 0xFFFF_0000), size: size).pngData())
    }
}
