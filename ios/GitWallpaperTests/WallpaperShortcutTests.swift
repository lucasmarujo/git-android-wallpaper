import Foundation
import GitWallpaperKit
import XCTest
@testable import GitWallpaper

final class WallpaperShortcutTests: XCTestCase {
    private let stores = TestStores()

    override func tearDown() {
        stores.tearDown()
        super.tearDown()
    }

    func testRunUrlCallsTheNamedShortcutWithCallbacks() throws {
        let url = WallpaperShortcut(defaults: stores.defaults).runURL
        let components = try XCTUnwrap(URLComponents(url: url, resolvingAgainstBaseURL: false))
        let items = Dictionary(uniqueKeysWithValues: (components.queryItems ?? []).map { ($0.name, $0.value ?? "") })

        XCTAssertEqual("shortcuts", components.scheme)
        XCTAssertEqual("x-callback-url", components.host)
        XCTAssertEqual("/run-shortcut", components.path)
        XCTAssertEqual("Git Wallpaper", items["name"])
        XCTAssertEqual("gitwallpaper://shortcut/success", items["x-success"])
        XCTAssertEqual("gitwallpaper://shortcut/cancel", items["x-cancel"])
        XCTAssertEqual("gitwallpaper://shortcut/error", items["x-error"])
    }

    func testCallbacksAreParsedFromTheAppScheme() {
        XCTAssertEqual(.success, WallpaperShortcut.callback(from: URL(string: "gitwallpaper://shortcut/success")!))
        XCTAssertEqual(.cancel, WallpaperShortcut.callback(from: URL(string: "gitwallpaper://shortcut/cancel")!))
        XCTAssertEqual(
            .error(message: "Not found"),
            WallpaperShortcut.callback(from: URL(string: "gitwallpaper://shortcut/error?errorMessage=Not%20found")!)
        )
        XCTAssertEqual(.error(message: nil), WallpaperShortcut.callback(from: URL(string: "gitwallpaper://shortcut/error")!))
    }

    func testCreateShortcutUrlOpensANewShortcutInShortcuts() {
        XCTAssertEqual("shortcuts://create-shortcut", WallpaperShortcut.createShortcutURL.absoluteString)
    }

    func testUnrelatedUrlsAreIgnored() {
        XCTAssertNil(WallpaperShortcut.callback(from: URL(string: "gitwallpaper://shortcut/other")!))
        XCTAssertNil(WallpaperShortcut.callback(from: URL(string: "gitwallpaper://elsewhere/success")!))
        XCTAssertNil(WallpaperShortcut.callback(from: URL(string: "https://shortcut/success")!))
    }

    func testReadinessIsPersisted() {
        let shortcut = WallpaperShortcut(defaults: stores.defaults)
        XCTAssertFalse(shortcut.isReady)

        shortcut.isReady = true

        XCTAssertTrue(WallpaperShortcut(defaults: stores.defaults).isReady)
    }
}
