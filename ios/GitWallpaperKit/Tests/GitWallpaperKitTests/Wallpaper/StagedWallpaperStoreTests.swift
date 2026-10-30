import CoreGraphics
import Foundation
import GitWallpaperKit
import ImageIO
import XCTest

final class StagedWallpaperStoreTests: XCTestCase {
    private let stores = TestStores()

    override func tearDown() {
        stores.tearDown()
        super.tearDown()
    }

    func testApplyingWritesAPngWithTheSameSize() throws {
        let store = StagedWallpaperStore(fileURL: stores.file("nested/wallpaper.png"))
        XCTAssertFalse(store.hasImage)

        try store.apply(solidImage(color: ARGBColor(argb: 0xFF0D_1117), size: 32), target: .lockScreen)

        let data = try XCTUnwrap(store.imageData())
        let source = try XCTUnwrap(CGImageSourceCreateWithData(data as CFData, nil))
        let decoded = try XCTUnwrap(CGImageSourceCreateImageAtIndex(source, 0, nil))
        XCTAssertTrue(store.hasImage)
        XCTAssertEqual(32, decoded.width)
        XCTAssertEqual(32, decoded.height)
    }

    func testTheStagedFileIsExcludedFromBackups() throws {
        let store = StagedWallpaperStore(fileURL: stores.file("wallpaper.png"))

        try store.apply(solidImage(color: ARGBColor(argb: 0xFFFF_FFFF), size: 8), target: .both)

        let values = try store.fileURL.resourceValues(forKeys: [.isExcludedFromBackupKey])
        XCTAssertEqual(true, values.isExcludedFromBackup)
    }

    func testApplyingAgainReplacesTheImage() throws {
        let store = StagedWallpaperStore(fileURL: stores.file("wallpaper.png"))
        try store.apply(solidImage(color: ARGBColor(argb: 0xFFFF_0000), size: 8), target: .lockScreen)
        let first = store.imageData()

        try store.apply(solidImage(color: ARGBColor(argb: 0xFF00_FF00), size: 8), target: .lockScreen)

        XCTAssertNotEqual(first, store.imageData())
    }

    func testClearingRemovesTheImage() throws {
        let store = StagedWallpaperStore(fileURL: stores.file("wallpaper.png"))
        try store.apply(solidImage(color: ARGBColor(argb: 0xFFFF_0000), size: 8), target: .lockScreen)

        store.clear()

        XCTAssertFalse(store.hasImage)
        XCTAssertNil(store.imageData())
    }
}
