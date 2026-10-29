import GitWallpaperKit
import SwiftUI

extension Color {
    init(_ color: ARGBColor) {
        self.init(cgColor: color.cgColor)
    }
}

struct ContributionPattern: View {
    let palette: WallpaperPalette
    let pattern: [[ContributionLevel]]

    var body: some View {
        Canvas { context, size in
            let rows = pattern.count
            let columns = pattern.map(\.count).max() ?? 0
            guard rows > 0, columns > 0 else { return }
            let cell = min(
                size.width / (CGFloat(columns) + CGFloat(columns - 1) * Self.gapRatio),
                size.height / (CGFloat(rows) + CGFloat(rows - 1) * Self.gapRatio)
            )
            let gap = cell * Self.gapRatio
            let offsetX = (size.width - (CGFloat(columns) * cell + CGFloat(columns - 1) * gap)) / 2
            let offsetY = (size.height - (CGFloat(rows) * cell + CGFloat(rows - 1) * gap)) / 2
            for (row, levels) in pattern.enumerated() {
                for (column, level) in levels.enumerated() {
                    let rect = CGRect(
                        x: offsetX + CGFloat(column) * (cell + gap),
                        y: offsetY + CGFloat(row) * (cell + gap),
                        width: cell,
                        height: cell
                    )
                    context.fill(
                        Path(roundedRect: rect, cornerRadius: cell * Self.cornerRatio),
                        with: .color(Color(palette.color(for: level)))
                    )
                }
            }
        }
        .accessibilityHidden(true)
    }

    static func of(_ rows: String...) -> [[ContributionLevel]] {
        rows.map { row in
            row.compactMap { $0.wholeNumberValue }.map { ContributionLevel.allCases[$0] }
        }
    }

    private static let gapRatio: CGFloat = 0.2
    private static let cornerRatio: CGFloat = 0.22
}
