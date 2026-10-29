import CoreGraphics
import Foundation

public struct StatsLayout: Equatable, Sendable {
    public var centerX: CGFloat
    public var titleBaseline: CGFloat
    public var titleTextSize: CGFloat
    public var bodyBaseline: CGFloat
    public var bodyTextSize: CGFloat
    public var userRowCenterY: CGFloat
    public var userTextSize: CGFloat
    public var avatarSize: CGFloat
    public var avatarGap: CGFloat
}

public enum WallpaperLayoutError: Error, Equatable {
    case invalidSize
    case emptyGrid
}

public struct WallpaperLayout: Equatable, Sendable {
    public static let systemWallpaperZoom: CGFloat = 1.1
    public static let visibleSafeTopRatio: CGFloat = 0.27
    public static let visibleSafeBottomRatio: CGFloat = 0.8
    public static let visibleMaxGridWidthRatio: CGFloat = 0.7

    public var columns: Int
    public var rows: Int
    public var cellSize: CGFloat
    public var cellGap: CGFloat
    public var gridLeft: CGFloat
    public var gridTop: CGFloat
    public var stats: StatsLayout?
    public var top: CGFloat
    public var bottom: CGFloat

    public var gridWidth: CGFloat {
        CGFloat(columns) * cellSize + CGFloat(columns - 1) * cellGap
    }

    public var gridHeight: CGFloat {
        CGFloat(rows) * cellSize + CGFloat(rows - 1) * cellGap
    }

    public func cellLeft(_ index: Int) -> CGFloat {
        gridLeft + CGFloat(index % columns) * (cellSize + cellGap)
    }

    public func cellTop(_ index: Int) -> CGFloat {
        gridTop + CGFloat(index / columns) * (cellSize + cellGap)
    }

    public static func calculate(width: Int, height: Int, dayCount: Int, showStats: Bool) throws -> WallpaperLayout {
        guard width > 0, height > 0 else { throw WallpaperLayoutError.invalidSize }
        guard dayCount > 0 else { throw WallpaperLayoutError.emptyGrid }
        let width = CGFloat(width)
        let height = CGFloat(height)
        let safeTop = height * toImageRatio(visibleSafeTopRatio)
        let safeHeight = height * toImageRatio(visibleSafeBottomRatio) - safeTop

        let titleTextSize = width * titleTextWidthRatio
        let bodyTextSize = width * bodyTextWidthRatio
        let userTextSize = width * userTextWidthRatio
        let avatarSize = userTextSize * avatarTextRatio
        let userRowHeight = max(avatarSize, userTextSize * lineHeightRatio)
        let statsSpacing = width * statsSpacingWidthRatio
        let userRowSpacing = width * userRowSpacingWidthRatio
        let statsHeight = showStats
            ? statsSpacing + titleTextSize + bodyTextSize * lineHeightRatio + userRowSpacing + userRowHeight
            : 0

        let maxGridWidth = width * visibleMaxGridWidthRatio / systemWallpaperZoom
        let maxGridHeight = max(safeHeight - statsHeight, height * minGridHeightRatio)
        let maxColumns = min(maxColumnCount, dayCount)
        let minColumns = min(minColumnCount, maxColumns)
        let best = (minColumns...maxColumns)
            .map { columns in
                (columns, cellSizeFor(columns: columns, rows: rowsFor(dayCount, columns), maxWidth: maxGridWidth, maxHeight: maxGridHeight))
            }
            .max { $0.1 < $1.1 }
        let columns = best?.0 ?? minColumns
        let cellSize = best?.1 ?? 0
        let rows = rowsFor(dayCount, columns)
        let cellGap = cellSize * cellGapRatio
        let gridWidth = CGFloat(columns) * cellSize + CGFloat(columns - 1) * cellGap
        let gridHeight = CGFloat(rows) * cellSize + CGFloat(rows - 1) * cellGap

        let blockHeight = gridHeight + statsHeight
        let top = safeTop + (safeHeight - blockHeight) / 2
        let gridLeft = (width - gridWidth) / 2
        var stats: StatsLayout?
        if showStats {
            let titleBaseline = top + gridHeight + statsSpacing + titleTextSize
            let bodyBaseline = titleBaseline + bodyTextSize * lineHeightRatio
            stats = StatsLayout(
                centerX: width / 2,
                titleBaseline: titleBaseline,
                titleTextSize: titleTextSize,
                bodyBaseline: bodyBaseline,
                bodyTextSize: bodyTextSize,
                userRowCenterY: bodyBaseline + userRowSpacing + userRowHeight / 2,
                userTextSize: userTextSize,
                avatarSize: avatarSize,
                avatarGap: userTextSize * avatarGapTextRatio
            )
        }
        return WallpaperLayout(
            columns: columns,
            rows: rows,
            cellSize: cellSize,
            cellGap: cellGap,
            gridLeft: gridLeft,
            gridTop: top,
            stats: stats,
            top: top,
            bottom: top + blockHeight
        )
    }

    public static func toImageRatio(_ visibleRatio: CGFloat) -> CGFloat {
        centerRatio + (visibleRatio - centerRatio) / systemWallpaperZoom
    }

    private static let centerRatio: CGFloat = 0.5
    private static let minColumnCount = 10
    private static let maxColumnCount = 30
    private static let cellGapRatio: CGFloat = 0.22
    private static let minGridHeightRatio: CGFloat = 0.2
    private static let statsSpacingWidthRatio: CGFloat = 0.045
    private static let titleTextWidthRatio: CGFloat = 0.044
    private static let bodyTextWidthRatio: CGFloat = 0.032
    private static let userTextWidthRatio: CGFloat = 0.034
    private static let userRowSpacingWidthRatio: CGFloat = 0.025
    private static let avatarTextRatio: CGFloat = 1.8
    private static let avatarGapTextRatio: CGFloat = 0.5
    private static let lineHeightRatio: CGFloat = 1.5

    private static func rowsFor(_ dayCount: Int, _ columns: Int) -> Int {
        Int(ceil(Double(dayCount) / Double(columns)))
    }

    private static func cellSizeFor(columns: Int, rows: Int, maxWidth: CGFloat, maxHeight: CGFloat) -> CGFloat {
        min(
            maxWidth / (CGFloat(columns) + CGFloat(columns - 1) * cellGapRatio),
            maxHeight / (CGFloat(rows) + CGFloat(rows - 1) * cellGapRatio)
        )
    }
}
