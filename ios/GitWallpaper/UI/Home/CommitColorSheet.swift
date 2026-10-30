import GitWallpaperKit
import SwiftUI

enum CommitColorAccessibility {
    static let redSlider = "commit_color_red"
    static let greenSlider = "commit_color_green"
    static let blueSlider = "commit_color_blue"
    static let hex = "commit_color_hex"
    static let cancel = "commit_color_cancel"
}

struct CommitColorSheet: View {
    let theme: WallpaperTheme
    let onConfirm: (ARGBColor) -> Void
    let onCancel: () -> Void
    @State private var color: ARGBColor

    init(theme: WallpaperTheme, initialColor: ARGBColor, onConfirm: @escaping (ARGBColor) -> Void, onCancel: @escaping () -> Void) {
        self.theme = theme
        self.onConfirm = onConfirm
        self.onCancel = onCancel
        _color = State(initialValue: initialColor)
    }

    var body: some View {
        let palette = WallpaperPalette.of(theme).withCommitColor(color)
        NavigationStack {
            VStack(alignment: .leading, spacing: 16) {
                ZStack {
                    RoundedRectangle(cornerRadius: 16, style: .continuous).fill(Color(palette.background))
                    ContributionPattern(palette: palette, pattern: Self.previewPattern)
                        .frame(width: 168, height: 68)
                }
                .frame(height: 88)
                HStack(spacing: 12) {
                    RoundedRectangle(cornerRadius: 6).fill(Color(palette.level4)).frame(width: 24, height: 24)
                    Text(verbatim: color.hexRGB)
                        .font(.title3.monospaced())
                        .accessibilityIdentifier(CommitColorAccessibility.hex)
                }
                ChannelSlider(label: "commit_color_red", value: channel(\.red, update: { color.withRed($0) }), tint: .red)
                    .accessibilityIdentifier(CommitColorAccessibility.redSlider)
                ChannelSlider(label: "commit_color_green", value: channel(\.green, update: { color.withGreen($0) }), tint: .green)
                    .accessibilityIdentifier(CommitColorAccessibility.greenSlider)
                ChannelSlider(label: "commit_color_blue", value: channel(\.blue, update: { color.withBlue($0) }), tint: .blue)
                    .accessibilityIdentifier(CommitColorAccessibility.blueSlider)
                Spacer(minLength: 0)
            }
            .padding(20)
            .navigationTitle("home_section_commit_color")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("login_cancel", action: onCancel)
                        .accessibilityIdentifier(CommitColorAccessibility.cancel)
                }
                ToolbarItem(placement: .confirmationAction) {
                    Button("commit_color_confirm") { onConfirm(color) }
                }
            }
        }
        .presentationDetents([.medium, .large])
    }

    private func channel(_ keyPath: KeyPath<ARGBColor, Int>, update: @escaping (Int) -> ARGBColor) -> Binding<Double> {
        Binding(
            get: { Double(color[keyPath: keyPath]) },
            set: { color = update(Int($0.rounded())) }
        )
    }

    private static let previewPattern = ContributionPattern.of("0123401", "1234012", "2340123")
}

private struct ChannelSlider: View {
    let label: LocalizedStringKey
    @Binding var value: Double
    let tint: Color

    var body: some View {
        HStack(spacing: 12) {
            Text(label)
                .font(.callout)
                .frame(width: 80, alignment: .leading)
            Slider(value: $value, in: 0...Double(ARGBColor.maxChannel), step: 1)
                .tint(tint)
            Text(verbatim: String(Int(value)))
                .font(.callout.monospacedDigit())
                .frame(width: 36, alignment: .trailing)
        }
    }
}
