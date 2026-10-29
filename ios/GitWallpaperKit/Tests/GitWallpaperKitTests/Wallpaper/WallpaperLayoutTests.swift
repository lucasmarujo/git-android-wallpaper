import CoreGraphics
import GitWallpaperKit
import XCTest

final class WallpaperLayoutTests: XCTestCase {
    private let width = 1080
    private let height = 2340
    private let dayCount = 365

    func testGridIsVerticalWithMoreRowsThanColumnsOnPhones() throws {
        let layout = try layout()

        XCTAssertGreaterThan(layout.rows, layout.columns)
        XCTAssertEqual(Int((Double(dayCount) / Double(layout.columns)).rounded(.up)), layout.rows)
        XCTAssertGreaterThan(layout.gridHeight, layout.gridWidth)
    }

    func testEveryDayHasACellInsideTheGrid() throws {
        let layout = try layout()

        XCTAssertGreaterThanOrEqual(layout.rows * layout.columns, dayCount)
        XCTAssertLessThan((layout.rows - 1) * layout.columns, dayCount)
        let lastIndex = dayCount - 1
        XCTAssertLessThanOrEqual(layout.cellLeft(lastIndex) + layout.cellSize, layout.gridLeft + layout.gridWidth + 0.5)
        XCTAssertLessThanOrEqual(layout.cellTop(lastIndex) + layout.cellSize, layout.gridTop + layout.gridHeight + 0.5)
    }

    func testCellsFlowLeftToRightThenTopToBottom() throws {
        let layout = try layout()

        XCTAssertEqual(layout.gridLeft, layout.cellLeft(0), accuracy: 0.01)
        XCTAssertEqual(layout.gridTop, layout.cellTop(0), accuracy: 0.01)
        XCTAssertEqual(layout.cellTop(0), layout.cellTop(layout.columns - 1), accuracy: 0.01)
        XCTAssertEqual(layout.gridLeft, layout.cellLeft(layout.columns), accuracy: 0.01)
        XCTAssertEqual(layout.cellTop(0) + layout.cellSize + layout.cellGap, layout.cellTop(layout.columns), accuracy: 0.01)
    }

    func testVisibleContentStaysBetweenTheLockScreenClockAndTheBottomButtons() throws {
        for showStats in [true, false] {
            let layout = try layout(showStats: showStats)

            XCTAssertGreaterThanOrEqual(visibleRatio(layout.top), WallpaperLayout.visibleSafeTopRatio - 0.001, "stats=\(showStats)")
            XCTAssertLessThanOrEqual(visibleRatio(layout.bottom), WallpaperLayout.visibleSafeBottomRatio + 0.001, "stats=\(showStats)")
        }
    }

    func testImageRatiosUndoTheSystemZoom() {
        XCTAssertEqual(0.5, WallpaperLayout.toImageRatio(0.5), accuracy: 0.0001)
        XCTAssertEqual(0.27, visibleRatio(CGFloat(height) * WallpaperLayout.toImageRatio(0.27)), accuracy: 0.0001)
    }

    func testGridIsHorizontallyCenteredAndNarrowerThanTheLimit() throws {
        let layout = try layout()

        XCTAssertEqual(layout.gridLeft, CGFloat(width) - (layout.gridLeft + layout.gridWidth), accuracy: 0.5)
        let visibleGridWidth = layout.gridWidth * WallpaperLayout.systemWallpaperZoom
        XCTAssertLessThanOrEqual(visibleGridWidth, CGFloat(width) * WallpaperLayout.visibleMaxGridWidthRatio + 0.5)
    }

    func testGridStaysClearOfTheEdgesWhenTheSystemZoomsTheWallpaper() throws {
        let systemZoom: CGFloat = 1.1
        let croppedPerSide = (CGFloat(width) - CGFloat(width) / systemZoom) / 2

        let visibleMargin = (try layout().gridLeft - croppedPerSide) * systemZoom

        XCTAssertGreaterThanOrEqual(visibleMargin, CGFloat(width) * 0.05)
    }

    func testStatsSitBelowTheGridInReadingOrder() throws {
        let layout = try layout()
        let stats = try XCTUnwrap(layout.stats)

        XCTAssertGreaterThan(stats.titleBaseline, layout.gridTop + layout.gridHeight)
        XCTAssertGreaterThan(stats.bodyBaseline, stats.titleBaseline)
        XCTAssertGreaterThan(stats.userRowCenterY, stats.bodyBaseline)
        XCTAssertEqual(CGFloat(width) / 2, stats.centerX, accuracy: 0.01)
    }

    func testHidingStatsLeavesRoomForBiggerCells() throws {
        let withStats = try layout(showStats: true)
        let withoutStats = try layout(showStats: false)

        XCTAssertNil(withoutStats.stats)
        XCTAssertGreaterThanOrEqual(withoutStats.cellSize, withStats.cellSize)
    }

    func testWiderScreensUseMoreColumns() throws {
        let phone = try layout()
        let tablet = try WallpaperLayout.calculate(width: 1600, height: 2560, dayCount: dayCount, showStats: true)

        XCTAssertGreaterThan(tablet.columns, phone.columns)
    }

    func testScaledPreviewsKeepTheSameProportions() throws {
        let full = try layout()
        let preview = try WallpaperLayout.calculate(width: 432, height: 936, dayCount: dayCount, showStats: true)

        XCTAssertEqual(full.columns, preview.columns)
        XCTAssertEqual(full.cellSize * 0.4, preview.cellSize, accuracy: 0.5)
    }

    func testIPhoneScreensKeepTheVerticalGrid() throws {
        for (screenWidth, screenHeight) in [(1170, 2532), (1206, 2622), (1320, 2868)] {
            let layout = try WallpaperLayout.calculate(width: screenWidth, height: screenHeight, dayCount: dayCount, showStats: true)

            XCTAssertGreaterThan(layout.rows, layout.columns, "\(screenWidth)x\(screenHeight)")
        }
    }

    func testInvalidSizesAreRejected() {
        XCTAssertThrowsError(try WallpaperLayout.calculate(width: 0, height: height, dayCount: dayCount, showStats: true)) { error in
            XCTAssertEqual(WallpaperLayoutError.invalidSize, error as? WallpaperLayoutError)
        }
    }

    func testEmptyGridsAreRejected() {
        XCTAssertThrowsError(try WallpaperLayout.calculate(width: width, height: height, dayCount: 0, showStats: true)) { error in
            XCTAssertEqual(WallpaperLayoutError.emptyGrid, error as? WallpaperLayoutError)
        }
    }

    private func visibleRatio(_ imageY: CGFloat) -> CGFloat {
        0.5 + (imageY / CGFloat(height) - 0.5) * WallpaperLayout.systemWallpaperZoom
    }

    private func layout(showStats: Bool = true) throws -> WallpaperLayout {
        try WallpaperLayout.calculate(width: width, height: height, dayCount: dayCount, showStats: showStats)
    }
}
