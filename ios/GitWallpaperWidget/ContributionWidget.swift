import GitWallpaperKit
import SwiftUI
import WidgetKit

struct ContributionWidget: Widget {
    static let kind = "ContributionWidget"

    var body: some WidgetConfiguration {
        StaticConfiguration(kind: Self.kind, provider: ContributionTimelineProvider()) { entry in
            ContributionWidgetView(entry: entry)
        }
        .configurationDisplayName("widget_name")
        .description("widget_description")
        .supportedFamilies([.systemSmall, .systemMedium, .systemLarge, .accessoryRectangular])
        .contentMarginsDisabled()
    }
}

struct ContributionWidgetView: View {
    let entry: ContributionEntry
    private let renderer = WidgetEnvironment.container.renderer
    @Environment(\.widgetRenderingMode) private var renderingMode
    @Environment(\.displayScale) private var displayScale

    private var palette: WallpaperPalette {
        renderingMode == .fullColor
            ? WallpaperPalette.of(entry.settings.theme).withCommitColor(entry.settings.commitColor)
            : .monochrome
    }

    var body: some View {
        GeometryReader { proxy in
            if let calendar = entry.calendar, let rendered = render(calendar, size: proxy.size) {
                Image(decorative: rendered.image, scale: rendered.scale)
                    .resizable()
                    .frame(width: proxy.size.width, height: proxy.size.height)
                    .accessibilityLabel(Text("widget_content_description"))
            } else {
                Text("widget_empty")
                    .font(.caption)
                    .foregroundStyle(Color(cgColor: palette.primaryText.cgColor))
                    .multilineTextAlignment(.center)
                    .padding(12)
                    .frame(width: proxy.size.width, height: proxy.size.height)
            }
        }
        .containerBackground(for: .widget) {
            renderingMode == .fullColor ? Color(cgColor: palette.background.cgColor) : Color.clear
        }
    }

    private func render(_ calendar: ContributionCalendar, size: CGSize) -> (image: CGImage, scale: CGFloat)? {
        guard size.width > 0, size.height > 0 else { return nil }
        let density = min(displayScale, (Self.maxBitmapPixels / (size.width * size.height)).squareRoot())
        let pixelSize = ScreenSize(
            width: max(Int((size.width * density).rounded()), 1),
            height: max(Int((size.height * density).rounded()), 1)
        )
        guard let image = try? renderer.renderWidget(
            calendar: calendar,
            settings: entry.settings,
            palette: palette,
            size: pixelSize,
            density: density,
            today: entry.today
        ) else { return nil }
        return (image, density)
    }

    private static let maxBitmapPixels: CGFloat = 1_000_000
}
