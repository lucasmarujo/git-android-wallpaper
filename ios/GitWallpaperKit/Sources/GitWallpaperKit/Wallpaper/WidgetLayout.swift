import CoreGraphics
import Foundation

public enum WidgetGridStyle: Sendable {
    case weekColumns
    case dayRows
}

public struct WidgetHeaderLayout: Equatable, Sendable {
    public var centerX: CGFloat
    public var baseline: CGFloat
    public var textSize: CGFloat
    public var maxTextWidth: CGFloat
}

public enum WidgetLayoutError: Error, Equatable {
    case invalidSize
    case invalidDensity
    case emptyGrid
}

public struct WidgetLayout: Equatable, Sendable {
    public static let daysPerWeek = 7

    public var style: WidgetGridStyle
    public var visibleCells: Int
    public var columns: Int
    public var rows: Int
    public var cellSize: CGFloat
    public var cellGap: CGFloat
    public var gridLeft: CGFloat
    public var gridTop: CGFloat
    public var header: WidgetHeaderLayout?

    public var gridWidth: CGFloat {
        CGFloat(columns) * cellSize + CGFloat(columns - 1) * cellGap
    }

    public var gridHeight: CGFloat {
        CGFloat(rows) * cellSize + CGFloat(rows - 1) * cellGap
    }

    public func cellLeft(_ column: Int) -> CGFloat {
        gridLeft + CGFloat(column) * (cellSize + cellGap)
    }

    public func cellTop(_ row: Int) -> CGFloat {
        gridTop + CGFloat(row) * (cellSize + cellGap)
    }

    private struct Grid {
        var visibleCells: Int
        var columns: Int
        var rows: Int
        var cellSize: CGFloat
    }

    public static func calculate(
        width: Int,
        height: Int,
        density: CGFloat,
        weekCount: Int,
        dayCount: Int,
        showHeader: Bool
    ) throws -> WidgetLayout {
        guard width > 0, height > 0 else { throw WidgetLayoutError.invalidSize }
        guard density > 0 else { throw WidgetLayoutError.invalidDensity }
        guard weekCount > 0, dayCount > 0 else { throw WidgetLayoutError.emptyGrid }
        let width = CGFloat(width)
        let height = CGFloat(height)
        let padding = min(paddingPoints * density, min(width, height) * maxPaddingRatio)
        let contentWidth = width - 2 * padding
        let contentHeight = height - 2 * padding
        let headerTextSize = headerTextPoints * density
        let headerHeight = headerTextSize * headerHeightTextRatio
        let hasHeader = showHeader
            && contentWidth >= headerMinWidthPoints * density
            && contentHeight - headerHeight >= headerMinGridHeightPoints * density
        let gridAreaHeight = hasHeader ? contentHeight - headerHeight : contentHeight

        let style: WidgetGridStyle = gridAreaHeight <= weekColumnsMaxGridHeightPoints * density ? .weekColumns : .dayRows
        let grid = switch style {
        case .weekColumns: weekColumns(width: contentWidth, height: gridAreaHeight, weekCount: weekCount)
        case .dayRows: dayRows(width: contentWidth, height: gridAreaHeight, dayCount: dayCount, minCellSize: minCellPoints * density)
        }

        let cellGap = grid.cellSize * cellGapRatio
        let gridWidth = CGFloat(grid.columns) * grid.cellSize + CGFloat(grid.columns - 1) * cellGap
        let gridHeight = CGFloat(grid.rows) * grid.cellSize + CGFloat(grid.rows - 1) * cellGap
        let blockHeight = gridHeight + (hasHeader ? headerHeight : 0)
        let top = padding + (contentHeight - blockHeight) / 2
        let header = hasHeader
            ? WidgetHeaderLayout(
                centerX: width / 2,
                baseline: top + headerTextSize * headerBaselineTextRatio,
                textSize: headerTextSize,
                maxTextWidth: contentWidth
            )
            : nil
        return WidgetLayout(
            style: style,
            visibleCells: grid.visibleCells,
            columns: grid.columns,
            rows: grid.rows,
            cellSize: grid.cellSize,
            cellGap: cellGap,
            gridLeft: (width - gridWidth) / 2,
            gridTop: hasHeader ? top + headerHeight : top,
            header: header
        )
    }

    public static func firstVisible(total: Int, todayIndex: Int, visible: Int) -> Int {
        min(max(todayIndex + 1, visible), total) - visible
    }

    private static let cellGapRatio: CGFloat = 0.22
    private static let paddingPoints: CGFloat = 12
    private static let maxPaddingRatio: CGFloat = 0.12
    private static let minCellPoints: CGFloat = 6
    private static let weekColumnsMaxGridHeightPoints: CGFloat = 100
    private static let headerTextPoints: CGFloat = 13
    private static let headerHeightTextRatio: CGFloat = 1.9
    private static let headerBaselineTextRatio: CGFloat = 0.95
    private static let headerMinGridHeightPoints: CGFloat = 56
    private static let headerMinWidthPoints: CGFloat = 120

    private static func weekColumns(width: CGFloat, height: CGFloat, weekCount: Int) -> Grid {
        let cellByHeight = height / units(daysPerWeek)
        let weeksThatFit = fittingCount(width, cellByHeight)
        let weeks = min(weekCount, weeksThatFit)
        return Grid(
            visibleCells: weeks,
            columns: weeks,
            rows: daysPerWeek,
            cellSize: min(cellByHeight, width / units(weeks))
        )
    }

    private static func dayRows(width: CGFloat, height: CGFloat, dayCount: Int, minCellSize: CGFloat) -> Grid {
        let capacity = fittingCount(width, minCellSize) * fittingCount(height, minCellSize)
        let days = min(dayCount, capacity)
        let candidates = (1...days).map { columns in
            let rows = Int(ceil(Double(days) / Double(columns)))
            return Grid(
                visibleCells: days,
                columns: columns,
                rows: rows,
                cellSize: min(width / units(columns), height / units(rows))
            )
        }
        return candidates.max { $0.cellSize < $1.cellSize } ?? candidates[0]
    }

    private static func units(_ cells: Int) -> CGFloat {
        CGFloat(cells) + CGFloat(cells - 1) * cellGapRatio
    }

    private static func fittingCount(_ length: CGFloat, _ cellSize: CGFloat) -> Int {
        max(1, Int(floor((length / cellSize + cellGapRatio) / (1 + cellGapRatio))))
    }
}
