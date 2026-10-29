import GitWallpaperKit
import XCTest

final class WallpaperFramingTests: XCTestCase {
    func testDefaultFramingKeepsTheOriginalSizeAndPosition() {
        let framing = WallpaperFraming()

        XCTAssertEqual(1, framing.zoom)
        XCTAssertEqual(0, framing.offsetX)
        XCTAssertEqual(0, framing.offsetY)
    }

    func testGesturesMultiplyTheZoomAndAddThePan() {
        let framing = WallpaperFraming(zoom: 1.2, offsetX: 0.1, offsetY: -0.1)

        let transformed = framing.transformed(zoomChange: 1.5, panX: 0.05, panY: -0.05)

        XCTAssertEqual(1.8, transformed.zoom, accuracy: 0.0001)
        XCTAssertEqual(0.15, transformed.offsetX, accuracy: 0.0001)
        XCTAssertEqual(-0.15, transformed.offsetY, accuracy: 0.0001)
    }

    func testZoomIsLimitedToTheSupportedRange() {
        XCTAssertEqual(WallpaperFraming.maxZoom, WallpaperFraming().transformed(zoomChange: 10, panX: 0, panY: 0).zoom)
        XCTAssertEqual(WallpaperFraming.minZoom, WallpaperFraming().transformed(zoomChange: 0.01, panX: 0, panY: 0).zoom)
    }

    func testTheGraphCenterCannotLeaveTheScreen() {
        let framing = WallpaperFraming().transformed(zoomChange: 1, panX: 3, panY: -3)

        XCTAssertEqual(WallpaperFraming.maxOffset, framing.offsetX)
        XCTAssertEqual(-WallpaperFraming.maxOffset, framing.offsetY)
    }

    func testInvalidValuesFallBackToTheDefaults() {
        let framing = WallpaperFraming(zoom: .nan, offsetX: .infinity, offsetY: .nan).coerced()

        XCTAssertEqual(WallpaperFraming(), framing)
    }
}
