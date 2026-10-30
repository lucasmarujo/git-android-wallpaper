import CoreGraphics
import GitWallpaperKit
import XCTest

final class WidgetLayoutTests: XCTestCase {
    private let yearWeeks = 53
    private let yearDays = 365
    private let minCellPoints: CGFloat = 6
    private let tolerance: CGFloat = 0.01

    func testShortWidgetsShowTheGitHubWeekColumnsInASingleRow() throws {
        let layout = try layout(width: 250, height: 80)

        XCTAssertEqual(.weekColumns, layout.style)
        XCTAssertEqual(WidgetLayout.daysPerWeek, layout.rows)
        XCTAssertEqual(26, layout.visibleCells)
        XCTAssertEqual(layout.visibleCells, layout.columns)
    }

    func testStretchingAShortWidgetSidewaysShowsMoreWeeksUntilTheWholePeriodFits() throws {
        let widths = Array(stride(from: 60, through: 1200, by: 20))
        let visibleWeeks = try widths.map { try layout(width: $0, height: 80).visibleCells }

        XCTAssertEqual(visibleWeeks.sorted(), visibleWeeks)
        XCTAssertEqual(yearWeeks, visibleWeeks.last)
        for width in widths {
            XCTAssertEqual(WidgetLayout.daysPerWeek, try layout(width: width, height: 80).rows)
        }
    }

    func testWideLandscapeWidgetsKeepTheWeekColumnsWithTheHeader() throws {
        let layout = try layout(width: 600, height: 130)

        XCTAssertEqual(.weekColumns, layout.style)
        XCTAssertEqual(48, layout.visibleCells)
        XCTAssertNotNil(layout.header)
    }

    func testStretchingTheWidgetVerticallySwitchesToTheWallpaperDayGrid() throws {
        let layout = try layout(width: 346, height: 205)

        XCTAssertEqual(.dayRows, layout.style)
        XCTAssertEqual(yearDays, layout.visibleCells)
        XCTAssertGreaterThan(layout.rows, WidgetLayout.daysPerWeek)
        XCTAssertGreaterThanOrEqual(layout.columns * layout.rows, yearDays)
        XCTAssertLessThan(layout.columns * (layout.rows - 1), yearDays)
    }

    func testTallerWidgetsGetBiggerDayCells() throws {
        let cellSizes = try [205, 300, 420, 560].map { try layout(width: 346, height: $0).cellSize }

        XCTAssertEqual(cellSizes.sorted(), cellSizes)
        XCTAssertLessThan(cellSizes[0], cellSizes[cellSizes.count - 1])
    }

    func testNarrowTallWidgetsShowTheLatestDaysThatKeepTheCellsReadable() throws {
        let layout = try layout(width: 70, height: 300)

        XCTAssertEqual(.dayRows, layout.style)
        XCTAssertEqual(266, layout.visibleCells)
        XCTAssertGreaterThanOrEqual(layout.cellSize, minCellPoints)
        XCTAssertNil(layout.header)
    }

    func testIPhoneWidgetFamiliesPickTheExpectedGrid() throws {
        let lockScreen = try WidgetLayout.calculate(width: 516, height: 228, density: 3, weekCount: yearWeeks, dayCount: yearDays, showHeader: true)
        let medium = try WidgetLayout.calculate(width: 1014, height: 474, density: 3, weekCount: yearWeeks, dayCount: yearDays, showHeader: true)

        XCTAssertEqual(.weekColumns, lockScreen.style)
        XCTAssertNil(lockScreen.header)
        XCTAssertEqual(.dayRows, medium.style)
        XCTAssertEqual(yearDays, medium.visibleCells)
        XCTAssertNotNil(medium.header)
    }

    func testTheGridFitsInsideTheWidgetAtAnySizeAndDensity() throws {
        let sizes = [(40, 40), (110, 40), (40, 250), (250, 110), (346, 205), (400, 200), (320, 520), (600, 90)]
        for density: CGFloat in [1, 2.75, 3] {
            for (widthPoints, heightPoints) in sizes {
                let width = Int(CGFloat(widthPoints) * density)
                let height = Int(CGFloat(heightPoints) * density)
                let layout = try WidgetLayout.calculate(
                    width: width,
                    height: height,
                    density: density,
                    weekCount: yearWeeks,
                    dayCount: yearDays,
                    showHeader: true
                )
                let label = "\(widthPoints)x\(heightPoints)@\(density)"
                let lastSlot = layout.visibleCells - 1
                let (lastColumn, lastRow) = switch layout.style {
                case .weekColumns: (lastSlot, WidgetLayout.daysPerWeek - 1)
                case .dayRows: (lastSlot % layout.columns, lastSlot / layout.columns)
                }

                XCTAssertGreaterThan(layout.cellSize, 0, label)
                XCTAssertTrue(layout.gridLeft >= 0 && layout.gridTop >= 0, label)
                XCTAssertLessThanOrEqual(layout.gridLeft + layout.gridWidth, CGFloat(width) + tolerance, label)
                XCTAssertLessThanOrEqual(layout.gridTop + layout.gridHeight, CGFloat(height) + tolerance, label)
                XCTAssertLessThanOrEqual(layout.cellLeft(lastColumn) + layout.cellSize, CGFloat(width) + tolerance, label)
                XCTAssertLessThanOrEqual(layout.cellTop(lastRow) + layout.cellSize, CGFloat(height) + tolerance, label)
            }
        }
    }

    func testTheHeaderNeedsStatsEnabledAndEnoughRoom() throws {
        let layout = try layout(width: 346, height: 205)

        XCTAssertLessThan(try XCTUnwrap(layout.header).baseline, layout.gridTop)
        XCTAssertNil(try self.layout(width: 346, height: 205, showHeader: false).header)
        XCTAssertNil(try self.layout(width: 250, height: 80).header)
        XCTAssertNil(try self.layout(width: 100, height: 300).header)
    }

    func testVisibleCellsEndAtToday() {
        XCTAssertEqual(23, WidgetLayout.firstVisible(total: yearWeeks, todayIndex: 42, visible: 20))
        XCTAssertEqual(0, WidgetLayout.firstVisible(total: yearWeeks, todayIndex: 42, visible: yearWeeks))
        XCTAssertEqual(0, WidgetLayout.firstVisible(total: yearWeeks, todayIndex: 2, visible: 20))
        XCTAssertEqual(33, WidgetLayout.firstVisible(total: yearWeeks, todayIndex: 52, visible: 20))
        XCTAssertEqual(31, WidgetLayout.firstVisible(total: yearDays, todayIndex: 296, visible: 266))
    }

    func testInvalidSizesAreRejected() {
        XCTAssertThrowsError(try WidgetLayout.calculate(width: 0, height: 110, density: 1, weekCount: yearWeeks, dayCount: yearDays, showHeader: true))
        XCTAssertThrowsError(try WidgetLayout.calculate(width: 250, height: 110, density: 0, weekCount: yearWeeks, dayCount: yearDays, showHeader: true))
        XCTAssertThrowsError(try WidgetLayout.calculate(width: 250, height: 110, density: 1, weekCount: 0, dayCount: yearDays, showHeader: true))
        XCTAssertThrowsError(try WidgetLayout.calculate(width: 250, height: 110, density: 1, weekCount: yearWeeks, dayCount: 0, showHeader: true))
    }

    private func layout(width: Int, height: Int, showHeader: Bool = true) throws -> WidgetLayout {
        try WidgetLayout.calculate(width: width, height: height, density: 1, weekCount: yearWeeks, dayCount: yearDays, showHeader: showHeader)
    }
}
