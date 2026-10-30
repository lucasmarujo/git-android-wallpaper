import Foundation
import GitWallpaperKit
import XCTest

final class SettingsRepositoryTests: XCTestCase {
    private let stores = TestStores()
    private lazy var repository = SettingsRepository(defaults: stores.defaults)

    override func tearDown() {
        stores.tearDown()
        super.tearDown()
    }

    func testDefaultsAreReturnedWhenNothingIsStored() {
        XCTAssertEqual(WallpaperSettings(), repository.current())
    }

    func testUpdatesArePersistedAndReturned() {
        let expected = WallpaperSettings(
            target: .both,
            theme: .sunset,
            commitColor: ARGBColor(argb: 0xFF8A_2BE2),
            period: .last12Months,
            cellShape: .circle,
            showStats: false,
            framing: WallpaperFraming(zoom: 1.3, offsetX: 0.1, offsetY: -0.2),
            autoUpdate: false,
            updateInterval: .sixHours
        )

        let returned = repository.update { $0 = expected }

        XCTAssertEqual(expected, returned)
        XCTAssertEqual(expected, SettingsRepository(defaults: stores.defaults).current())
    }

    func testUpdatesAreAppliedOnTopOfTheStoredSettings() {
        repository.update { $0.theme = .ocean }

        repository.update { $0.showStats = false }

        let current = repository.current()
        XCTAssertEqual(.ocean, current.theme)
        XCTAssertFalse(current.showStats)
    }

    func testClearingTheCommitColorGoesBackToTheThemeColors() {
        repository.update { $0.commitColor = ARGBColor(argb: 0xFFFF_0000) }

        repository.update { $0.commitColor = nil }

        XCTAssertNil(repository.current().commitColor)
    }

    func testOutOfRangeFramingIsLimitedWhenSaved() {
        repository.update { $0.framing = WallpaperFraming(zoom: 9, offsetX: -4, offsetY: 4) }

        XCTAssertEqual(
            WallpaperFraming(zoom: WallpaperFraming.maxZoom, offsetX: -WallpaperFraming.maxOffset, offsetY: WallpaperFraming.maxOffset),
            repository.current().framing
        )
    }

    func testInvalidStoredFramingFallsBackToSafeValues() {
        stores.defaults.set(Float.nan, forKey: "framing_zoom")
        stores.defaults.set(Float.infinity, forKey: "framing_offset_x")

        XCTAssertEqual(WallpaperFraming(), repository.current().framing)
    }

    func testUnknownStoredValuesFallBackToDefaults() {
        stores.defaults.set("RAINBOW", forKey: "theme")
        stores.defaults.set("HEXAGON", forKey: "cell_shape")
        stores.defaults.set("ALL_TIME", forKey: "period")
        stores.defaults.set("EVERY_SECOND", forKey: "update_interval")

        let current = repository.current()

        XCTAssertEqual(.githubDark, current.theme)
        XCTAssertEqual(.roundedSquare, current.cellShape)
        XCTAssertEqual(.currentYear, current.period)
        XCTAssertEqual(.oneHour, current.updateInterval)
    }

    func testStoredKeysMatchTheAndroidNames() {
        repository.update { $0.target = .homeScreen }

        XCTAssertEqual("HOME_SCREEN", stores.defaults.string(forKey: "target"))
    }
}
