import GitWallpaperKit
import XCTest
@testable import GitWallpaper

final class WallpaperIntentErrorTests: XCTestCase {
    func testSuccessfulUpdatesReturnTheImage() {
        XCTAssertNil(WallpaperIntentError(result: .applied, hasImage: true))
        XCTAssertNil(WallpaperIntentError(result: .unchanged, hasImage: true))
    }

    func testSignedOutUsersAreAskedToSignIn() {
        XCTAssertEqual(.notSignedIn, WallpaperIntentError(result: .failure(.notSignedIn), hasImage: true))
        XCTAssertEqual(.notSignedIn, WallpaperIntentError(result: .failure(.sessionExpired), hasImage: true))
    }

    func testOfflineRunsReuseTheLastImage() {
        XCTAssertNil(WallpaperIntentError(result: .failure(.network), hasImage: true))
        XCTAssertNil(WallpaperIntentError(result: .failure(.github), hasImage: true))
        XCTAssertEqual(.offline, WallpaperIntentError(result: .failure(.network), hasImage: false))
    }

    func testRenderFailuresWithoutAnImageAskToApplyOnce() {
        XCTAssertEqual(.unavailable, WallpaperIntentError(result: .failure(.wallpaper), hasImage: false))
        XCTAssertNil(WallpaperIntentError(result: .failure(.wallpaper), hasImage: true))
    }
}
