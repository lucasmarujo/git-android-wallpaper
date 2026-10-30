import CoreGraphics
import CoreText
import Foundation

public enum RenderError: Error, Equatable {
    case contextUnavailable
}

public final class ContributionWallpaperRenderer: Sendable {
    private let strings: WallpaperStrings

    public init(strings: WallpaperStrings = .standard) {
        self.strings = strings
    }

    public var languageTag: String {
        strings.languageTag
    }

    public func render(
        calendar: ContributionCalendar,
        settings: WallpaperSettings,
        size: ScreenSize,
        today: LocalDate,
        avatar: CGImage?
    ) throws -> CGImage {
        let palette = WallpaperPalette.of(settings.theme).withCommitColor(settings.commitColor)
        let grid = DayGrid.from(calendar.days, period: settings.period, today: today)
        let layout = try WallpaperLayout.calculate(
            width: size.width,
            height: size.height,
            dayCount: grid.cells.count,
            showStats: settings.showStats
        )
        let context = try Self.makeContext(size: size)
        context.setFillColor(palette.background.cgColor)
        context.fill(CGRect(x: 0, y: 0, width: size.width, height: size.height))
        context.saveGState()
        context.concatenate(Self.framingTransform(settings.framing, size: size))
        let painter = CellPainter(palette: palette, cellShape: settings.cellShape, cellSize: layout.cellSize, cellGap: layout.cellGap)
        for (index, cell) in grid.cells.enumerated() {
            painter.draw(cell, left: layout.cellLeft(index), top: layout.cellTop(index), in: context)
        }
        if let statsLayout = layout.stats {
            drawStats(
                in: context,
                statsLayout: statsLayout,
                maxTextWidth: CGFloat(size.width) * Self.maxTextWidthRatio,
                calendar: calendar,
                grid: grid,
                period: settings.period,
                palette: palette,
                avatar: avatar
            )
        }
        context.restoreGState()
        return try Self.makeImage(context)
    }

    public func renderWidget(
        calendar: ContributionCalendar,
        settings: WallpaperSettings,
        palette: WallpaperPalette,
        size: ScreenSize,
        density: CGFloat,
        today: LocalDate
    ) throws -> CGImage {
        let grid = DayGrid.from(calendar.days, period: settings.period, today: today)
        let firstWeekStart = grid.startDate.previousOrSameSunday
        let lastDate = grid.cells.last?.date ?? grid.today
        let weekCount = Self.weekIndex(firstWeekStart, lastDate) + 1
        let layout = try WidgetLayout.calculate(
            width: size.width,
            height: size.height,
            density: density,
            weekCount: weekCount,
            dayCount: grid.cells.count,
            showHeader: settings.showStats
        )
        let context = try Self.makeContext(size: size)
        context.setFillColor(palette.background.cgColor)
        context.fill(CGRect(x: 0, y: 0, width: size.width, height: size.height))
        let painter = CellPainter(palette: palette, cellShape: settings.cellShape, cellSize: layout.cellSize, cellGap: layout.cellGap)
        switch layout.style {
        case .weekColumns:
            drawWeekColumns(in: context, grid: grid, layout: layout, painter: painter, firstWeekStart: firstWeekStart, weekCount: weekCount)
        case .dayRows:
            drawDayRows(in: context, grid: grid, layout: layout, painter: painter)
        }
        if let header = layout.header {
            drawTitle(
                in: context,
                centerX: header.centerX,
                baseline: header.baseline,
                textSize: header.textSize,
                maxTextWidth: header.maxTextWidth,
                stats: ContributionStats.from(calendar.days, periodStart: grid.startDate),
                period: settings.period,
                today: grid.today,
                palette: palette
            )
        }
        return try Self.makeImage(context)
    }

    private func drawWeekColumns(
        in context: CGContext,
        grid: DayGrid,
        layout: WidgetLayout,
        painter: CellPainter,
        firstWeekStart: LocalDate,
        weekCount: Int
    ) {
        let firstWeek = WidgetLayout.firstVisible(
            total: weekCount,
            todayIndex: Self.weekIndex(firstWeekStart, grid.today),
            visible: layout.visibleCells
        )
        for cell in grid.cells {
            let column = Self.weekIndex(firstWeekStart, cell.date) - firstWeek
            guard (0..<layout.columns).contains(column) else { continue }
            let row = cell.date.dayOfWeek % WidgetLayout.daysPerWeek
            painter.draw(cell, left: layout.cellLeft(column), top: layout.cellTop(row), in: context)
        }
    }

    private func drawDayRows(in context: CGContext, grid: DayGrid, layout: WidgetLayout, painter: CellPainter) {
        let todayIndex = grid.startDate.daysUntil(grid.today)
        let firstDay = WidgetLayout.firstVisible(total: grid.cells.count, todayIndex: todayIndex, visible: layout.visibleCells)
        for slot in 0..<layout.visibleCells {
            painter.draw(
                grid.cells[firstDay + slot],
                left: layout.cellLeft(slot % layout.columns),
                top: layout.cellTop(slot / layout.columns),
                in: context
            )
        }
    }

    private func drawStats(
        in context: CGContext,
        statsLayout: StatsLayout,
        maxTextWidth: CGFloat,
        calendar: ContributionCalendar,
        grid: DayGrid,
        period: ContributionPeriod,
        palette: WallpaperPalette,
        avatar: CGImage?
    ) {
        let stats = ContributionStats.from(calendar.days, periodStart: grid.startDate)
        drawTitle(
            in: context,
            centerX: statsLayout.centerX,
            baseline: statsLayout.titleBaseline,
            textSize: statsLayout.titleTextSize,
            maxTextWidth: maxTextWidth,
            stats: stats,
            period: period,
            today: grid.today,
            palette: palette
        )

        let streakLine = strings.streaks(current: stats.currentStreak, longest: stats.longestStreak)
        let bodyText = TextRun(streakLine, size: statsLayout.bodyTextSize, weight: .regular, color: palette.secondaryText)
            .shrunkToFit(maxTextWidth)
        bodyText.draw(in: context, x: statsLayout.centerX - bodyText.width / 2, baseline: statsLayout.bodyBaseline)

        drawUserRow(in: context, statsLayout: statsLayout, maxTextWidth: maxTextWidth, login: calendar.login, palette: palette, avatar: avatar)
    }

    private func drawTitle(
        in context: CGContext,
        centerX: CGFloat,
        baseline: CGFloat,
        textSize: CGFloat,
        maxTextWidth: CGFloat,
        stats: ContributionStats,
        period: ContributionPeriod,
        today: LocalDate,
        palette: WallpaperPalette
    ) {
        let highlightText = strings.totalHighlight(stats.totalContributions)
        let suffixText = strings.totalSuffix(period: period, year: today.year)
        let natural = TextRun(highlightText, size: textSize, weight: .bold, color: palette.accent).width
            + TextRun(Self.wordSeparator, size: textSize, weight: .regular, color: palette.secondaryText).width
            + TextRun(suffixText, size: textSize, weight: .regular, color: palette.secondaryText).width
        let fittedSize = Self.shrunkSize(textSize, width: natural, maxWidth: maxTextWidth)
        let highlight = TextRun(highlightText, size: fittedSize, weight: .bold, color: palette.accent)
        let separator = TextRun(Self.wordSeparator, size: fittedSize, weight: .regular, color: palette.secondaryText)
        let suffix = TextRun(suffixText, size: fittedSize, weight: .regular, color: palette.secondaryText)
        let startX = centerX - (highlight.width + separator.width + suffix.width) / 2
        highlight.draw(in: context, x: startX, baseline: baseline)
        suffix.draw(in: context, x: startX + highlight.width + separator.width, baseline: baseline)
    }

    private func drawUserRow(
        in context: CGContext,
        statsLayout: StatsLayout,
        maxTextWidth: CGFloat,
        login: String,
        palette: WallpaperPalette,
        avatar: CGImage?
    ) {
        let avatarWidth = avatar != nil ? statsLayout.avatarSize + statsLayout.avatarGap : 0
        let user = TextRun(strings.login(login), size: statsLayout.userTextSize, weight: .regular, color: palette.secondaryText)
            .shrunkToFit(maxTextWidth - avatarWidth)
        let startX = statsLayout.centerX - (avatarWidth + user.width) / 2
        if let avatar {
            let radius = statsLayout.avatarSize / 2
            Self.drawAvatar(avatar, centerX: startX + radius, centerY: statsLayout.userRowCenterY, radius: radius, in: context)
        }
        let baseline = statsLayout.userRowCenterY + (user.ascent - user.descent) / 2
        user.draw(in: context, x: startX + avatarWidth, baseline: baseline)
    }

    private static func drawAvatar(_ avatar: CGImage, centerX: CGFloat, centerY: CGFloat, radius: CGFloat, in context: CGContext) {
        let scale = radius * 2 / CGFloat(min(avatar.width, avatar.height))
        let drawWidth = CGFloat(avatar.width) * scale
        let drawHeight = CGFloat(avatar.height) * scale
        let rect = CGRect(x: centerX - drawWidth / 2, y: centerY - drawHeight / 2, width: drawWidth, height: drawHeight)
        context.saveGState()
        context.addEllipse(in: CGRect(x: centerX - radius, y: centerY - radius, width: radius * 2, height: radius * 2))
        context.clip()
        context.interpolationQuality = .high
        context.translateBy(x: 0, y: rect.minY + rect.maxY)
        context.scaleBy(x: 1, y: -1)
        context.draw(avatar, in: rect)
        context.restoreGState()
    }

    private static func framingTransform(_ framing: WallpaperFraming, size: ScreenSize) -> CGAffineTransform {
        let coerced = framing.coerced()
        let zoom = CGFloat(coerced.zoom)
        let centerX = CGFloat(size.width) / 2
        let centerY = CGFloat(size.height) / 2
        return CGAffineTransform(translationX: -centerX, y: -centerY)
            .concatenating(CGAffineTransform(scaleX: zoom, y: zoom))
            .concatenating(CGAffineTransform(
                translationX: centerX + CGFloat(coerced.offsetX) * CGFloat(size.width),
                y: centerY + CGFloat(coerced.offsetY) * CGFloat(size.height)
            ))
    }

    private static func weekIndex(_ firstWeekStart: LocalDate, _ date: LocalDate) -> Int {
        firstWeekStart.daysUntil(date) / WidgetLayout.daysPerWeek
    }

    static func shrunkSize(_ textSize: CGFloat, width: CGFloat, maxWidth: CGFloat) -> CGFloat {
        width > maxWidth && width > 0 ? textSize * maxWidth / width : textSize
    }

    private static func makeContext(size: ScreenSize) throws -> CGContext {
        guard let colorSpace = CGColorSpace(name: CGColorSpace.sRGB),
              let context = CGContext(
                  data: nil,
                  width: size.width,
                  height: size.height,
                  bitsPerComponent: bitsPerComponent,
                  bytesPerRow: 0,
                  space: colorSpace,
                  bitmapInfo: CGImageAlphaInfo.premultipliedLast.rawValue
              )
        else { throw RenderError.contextUnavailable }
        context.translateBy(x: 0, y: CGFloat(size.height))
        context.scaleBy(x: 1, y: -1)
        context.setAllowsAntialiasing(true)
        context.setShouldAntialias(true)
        return context
    }

    private static func makeImage(_ context: CGContext) throws -> CGImage {
        guard let image = context.makeImage() else { throw RenderError.contextUnavailable }
        return image
    }

    private static let maxTextWidthRatio: CGFloat = 0.8
    private static let wordSeparator = " "
    private static let bitsPerComponent = 8
}

private struct CellPainter {
    let palette: WallpaperPalette
    let cellShape: CellShape
    let cellSize: CGFloat
    let ringWidth: CGFloat
    let ringOffset: CGFloat
    let cornerRadius: CGFloat

    init(palette: WallpaperPalette, cellShape: CellShape, cellSize: CGFloat, cellGap: CGFloat) {
        self.palette = palette
        self.cellShape = cellShape
        self.cellSize = cellSize
        ringWidth = cellGap * Self.todayRingWidthRatio
        ringOffset = cellGap * Self.todayRingOffsetRatio
        cornerRadius = cellSize * Self.cornerRadiusRatio
    }

    func draw(_ cell: DayCell, left: CGFloat, top: CGFloat, in context: CGContext) {
        let bounds = CGRect(x: left, y: top, width: cellSize, height: cellSize)
        let fill = cell.state == .future ? palette.future : palette.color(for: cell.level)
        context.setFillColor(fill.cgColor)
        context.addPath(path(in: bounds, cornerRadius: cornerRadius))
        context.fillPath()
        if cell.state == .today {
            let ringBounds = bounds.insetBy(dx: -ringOffset, dy: -ringOffset)
            context.setStrokeColor(palette.accent.cgColor)
            context.setLineWidth(ringWidth)
            context.addPath(path(in: ringBounds, cornerRadius: cornerRadius + ringOffset))
            context.strokePath()
        }
    }

    private func path(in rect: CGRect, cornerRadius: CGFloat) -> CGPath {
        switch cellShape {
        case .roundedSquare:
            let radius = min(cornerRadius, rect.width / 2, rect.height / 2)
            return CGPath(roundedRect: rect, cornerWidth: radius, cornerHeight: radius, transform: nil)
        case .circle:
            let diameter = rect.width
            return CGPath(ellipseIn: CGRect(x: rect.midX - diameter / 2, y: rect.midY - diameter / 2, width: diameter, height: diameter), transform: nil)
        }
    }

    private static let cornerRadiusRatio: CGFloat = 0.22
    private static let todayRingOffsetRatio: CGFloat = 0.5
    private static let todayRingWidthRatio: CGFloat = 0.5
}

private struct TextRun {
    enum Weight {
        case regular
        case bold
    }

    let line: CTLine
    let width: CGFloat
    let ascent: CGFloat
    let descent: CGFloat
    private let text: String
    private let size: CGFloat
    private let weight: Weight
    private let color: ARGBColor

    init(_ text: String, size: CGFloat, weight: Weight, color: ARGBColor) {
        self.text = text
        self.size = size
        self.weight = weight
        self.color = color
        let font = CTFontCreateUIFontForLanguage(weight == .bold ? .emphasizedSystem : .system, size, nil)
            ?? CTFontCreateWithName(Self.fallbackFontName as CFString, size, nil)
        let attributes: [NSAttributedString.Key: Any] = [
            NSAttributedString.Key(kCTFontAttributeName as String): font,
            NSAttributedString.Key(kCTForegroundColorAttributeName as String): color.cgColor,
        ]
        line = CTLineCreateWithAttributedString(NSAttributedString(string: text, attributes: attributes))
        var lineAscent: CGFloat = 0
        var lineDescent: CGFloat = 0
        var leading: CGFloat = 0
        width = CGFloat(CTLineGetTypographicBounds(line, &lineAscent, &lineDescent, &leading))
        ascent = CTFontGetAscent(font)
        descent = CTFontGetDescent(font)
    }

    func shrunkToFit(_ maxWidth: CGFloat) -> TextRun {
        let fitted = ContributionWallpaperRenderer.shrunkSize(size, width: width, maxWidth: maxWidth)
        return fitted == size ? self : TextRun(text, size: fitted, weight: weight, color: color)
    }

    func draw(in context: CGContext, x: CGFloat, baseline: CGFloat) {
        context.saveGState()
        context.textMatrix = CGAffineTransform(scaleX: 1, y: -1)
        context.textPosition = CGPoint(x: x, y: baseline)
        CTLineDraw(line, context)
        context.restoreGState()
    }

    private static let fallbackFontName = "Helvetica"
}
