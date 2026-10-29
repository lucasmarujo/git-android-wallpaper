import GitWallpaperKit
import SwiftUI

enum WallpaperAdjustAccessibility {
    static let frame = "adjust_frame"
    static let applyButton = "adjust_apply_button"
    static let lockScreenClock = "adjust_lock_screen_clock"
}

struct WallpaperAdjustView: View {
    let uiState: HomeUiState
    let onApply: (WallpaperFraming) -> Void
    let onCancel: () -> Void

    @State private var framing: WallpaperFraming
    @State private var lastMagnification: CGFloat = 1
    @State private var lastTranslation: CGSize = .zero

    init(uiState: HomeUiState, onApply: @escaping (WallpaperFraming) -> Void, onCancel: @escaping () -> Void) {
        self.uiState = uiState
        self.onApply = onApply
        self.onCancel = onCancel
        _framing = State(initialValue: uiState.settings.framing)
    }

    private var palette: WallpaperPalette {
        WallpaperPalette.of(uiState.settings.theme)
    }

    var body: some View {
        ZStack {
            Color.black.ignoresSafeArea()
            VStack(spacing: 0) {
                editorFrame
                    .aspectRatio(uiState.previewAspectRatio, contentMode: .fit)
                    .frame(maxWidth: .infinity, maxHeight: .infinity)
                AdjustControls(
                    isApplying: uiState.isApplying,
                    canReset: framing != WallpaperFraming(),
                    onReset: { framing = WallpaperFraming() },
                    onCancel: onCancel,
                    onApply: { onApply(framing) }
                )
            }
        }
        .statusBarHidden()
        .preferredColorScheme(palette.background.luminance > Self.lightBackgroundLuminance ? .light : .dark)
    }

    private var editorFrame: some View {
        GeometryReader { proxy in
            let size = proxy.size
            ZStack(alignment: .top) {
                Color(palette.background)
                if let image = uiState.adjustmentPreview {
                    Image(decorative: image, scale: 1)
                        .resizable()
                        .scaledToFill()
                        .frame(width: size.width, height: size.height)
                        .scaleEffect(WallpaperLayout.systemWallpaperZoom * CGFloat(framing.zoom))
                        .offset(
                            x: WallpaperLayout.systemWallpaperZoom * CGFloat(framing.offsetX) * size.width,
                            y: WallpaperLayout.systemWallpaperZoom * CGFloat(framing.offsetY) * size.height
                        )
                        .accessibilityLabel(Text("home_preview_description"))
                } else {
                    ProgressView()
                        .tint(Color(palette.primaryText))
                        .frame(width: size.width, height: size.height)
                }
                if uiState.settings.target != .homeScreen {
                    LockScreenClock(color: Color(palette.primaryText), frameHeight: size.height)
                }
            }
            .frame(width: size.width, height: size.height)
            .clipped()
            .contentShape(Rectangle())
            .gesture(transformGesture(size: size))
            .accessibilityIdentifier(WallpaperAdjustAccessibility.frame)
        }
    }

    private func transformGesture(size: CGSize) -> some Gesture {
        MagnifyGesture()
            .onChanged { value in
                let change = value.magnification / lastMagnification
                lastMagnification = value.magnification
                framing = framing.transformed(zoomChange: Float(change), panX: 0, panY: 0)
            }
            .onEnded { _ in lastMagnification = 1 }
            .simultaneously(
                with: DragGesture(minimumDistance: 0)
                    .onChanged { value in
                        let deltaX = value.translation.width - lastTranslation.width
                        let deltaY = value.translation.height - lastTranslation.height
                        lastTranslation = value.translation
                        framing = framing.transformed(
                            zoomChange: 1,
                            panX: Float(deltaX / (size.width * WallpaperLayout.systemWallpaperZoom)),
                            panY: Float(deltaY / (size.height * WallpaperLayout.systemWallpaperZoom))
                        )
                    }
                    .onEnded { _ in lastTranslation = .zero }
            )
    }

    private static let lightBackgroundLuminance = 0.5
}

private struct LockScreenClock: View {
    let color: Color
    let frameHeight: CGFloat
    private let now = Date()

    var body: some View {
        VStack(spacing: 0) {
            Text(now, format: .dateTime.weekday(.wide).month(.wide).day())
                .font(.system(size: frameHeight * Self.dateRatio, weight: .semibold))
            Text(now, format: .dateTime.hour(.defaultDigits(amPM: .omitted)).minute())
                .font(.system(size: frameHeight * Self.timeRatio, weight: .bold, design: .rounded))
                .monospacedDigit()
        }
        .foregroundStyle(color)
        .padding(.top, frameHeight * Self.topRatio)
        .accessibilityIdentifier(WallpaperAdjustAccessibility.lockScreenClock)
    }

    private static let topRatio: CGFloat = 0.07
    private static let dateRatio: CGFloat = 0.024
    private static let timeRatio: CGFloat = 0.11
}

private struct AdjustControls: View {
    let isApplying: Bool
    let canReset: Bool
    let onReset: () -> Void
    let onCancel: () -> Void
    let onApply: () -> Void

    var body: some View {
        VStack(spacing: 12) {
            Text("adjust_hint")
                .font(.callout)
                .foregroundStyle(.secondary)
                .multilineTextAlignment(.center)
                .frame(maxWidth: .infinity)
            HStack(spacing: 8) {
                Button("login_cancel", action: onCancel)
                    .disabled(isApplying)
                Button("adjust_reset", action: onReset)
                    .buttonStyle(.bordered)
                    .disabled(!canReset || isApplying)
                Button(action: onApply) {
                    HStack(spacing: 12) {
                        if isApplying {
                            ProgressView()
                            Text("home_applying")
                        } else {
                            Text("adjust_apply")
                        }
                    }
                    .frame(maxWidth: .infinity, minHeight: 36)
                }
                .buttonStyle(.borderedProminent)
                .controlSize(.large)
                .disabled(isApplying)
                .accessibilityIdentifier(WallpaperAdjustAccessibility.applyButton)
            }
        }
        .padding(.horizontal, 20)
        .padding(.vertical, 12)
        .background(.regularMaterial)
    }
}
