import GitWallpaperKit
import XCTest

final class ARGBColorTests: XCTestCase {
    func testChannelsAreReadFromThePackedValue() {
        let color = ARGBColor(argb: 0x808A_2BE2)

        XCTAssertEqual(0x80, color.alpha)
        XCTAssertEqual(0x8A, color.red)
        XCTAssertEqual(0x2B, color.green)
        XCTAssertEqual(0xE2, color.blue)
    }

    func testChannelsAreClampedWhenBuilding() {
        XCTAssertEqual(ARGBColor(argb: 0xFFFF_0000), ARGBColor(red: 300, green: -4, blue: 0))
    }

    func testHexUsesOnlyTheRgbChannels() {
        XCTAssertEqual("#8A2BE2", ARGBColor(argb: 0x008A_2BE2).hexRGB)
    }

    func testBlendingMatchesAndroidColorUtils() {
        let empty = ARGBColor(argb: 0xFF1A_2029)
        let red = ARGBColor(argb: 0xFFFF_0000)

        XCTAssertEqual(ARGBColor(red: 94, green: 22, blue: 28), empty.blended(with: red, ratio: 0.3))
        XCTAssertEqual(empty, empty.blended(with: red, ratio: 0))
        XCTAssertEqual(red, empty.blended(with: red, ratio: 1))
    }

    func testChangingOneChannelKeepsTheOthers() {
        let color = ARGBColor(argb: 0xFF11_2233)

        XCTAssertEqual(ARGBColor(argb: 0xFFAA_2233), color.withRed(0xAA))
        XCTAssertEqual(ARGBColor(argb: 0xFF11_AA33), color.withGreen(0xAA))
        XCTAssertEqual(ARGBColor(argb: 0xFF11_22AA), color.withBlue(0xAA))
    }

    func testLuminanceSeparatesLightAndDarkColors() {
        XCTAssertGreaterThan(ARGBColor(argb: 0xFFFF_FFFF).luminance, 0.5)
        XCTAssertLessThan(ARGBColor(argb: 0xFF0D_1117).luminance, 0.5)
    }
}
