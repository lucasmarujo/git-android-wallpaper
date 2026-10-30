import GitWallpaperKit
import SwiftUI

enum ShortcutSetupAccessibility {
    static let runButton = "shortcut_run_button"
    static let shareButton = "shortcut_share_button"
}

struct ShortcutSetupSheet: View {
    let target: WallpaperTarget
    let imageURL: URL?
    let onOpenShortcuts: () -> Void
    let onRunShortcut: () -> Void

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(alignment: .leading, spacing: 16) {
                    Text("shortcut_setup_body")
                        .font(.callout)
                    VStack(alignment: .leading, spacing: 12) {
                        SetupStep(number: 1, text: String(format: String(localized: "shortcut_setup_step_create"), WallpaperShortcut.name))
                        SetupStep(number: 2, text: String(localized: "shortcut_setup_step_get"))
                        SetupStep(number: 3, text: String(format: String(localized: "shortcut_setup_step_set"), String(localized: target.shortcutLocationKey)))
                        SetupStep(number: 4, text: String(localized: "shortcut_setup_step_automation"))
                    }
                    Button(action: onOpenShortcuts) {
                        Label("shortcut_open_shortcuts", systemImage: "square.stack.3d.up")
                            .frame(maxWidth: .infinity, minHeight: 36)
                    }
                    .buttonStyle(.bordered)
                    .controlSize(.large)
                    Button(action: onRunShortcut) {
                        Text("shortcut_run").frame(maxWidth: .infinity, minHeight: 36)
                    }
                    .buttonStyle(.borderedProminent)
                    .controlSize(.large)
                    .accessibilityIdentifier(ShortcutSetupAccessibility.runButton)
                    if let imageURL {
                        Text("shortcut_manual_hint")
                            .font(.footnote)
                            .foregroundStyle(.secondary)
                        ShareLink(item: imageURL) {
                            Label("shortcut_share_image", systemImage: "square.and.arrow.up")
                                .frame(maxWidth: .infinity, minHeight: 36)
                        }
                        .buttonStyle(.bordered)
                        .controlSize(.large)
                        .accessibilityIdentifier(ShortcutSetupAccessibility.shareButton)
                    }
                }
                .padding(20)
            }
            .navigationTitle("shortcut_setup_title")
            .navigationBarTitleDisplayMode(.inline)
        }
        .presentationDetents([.large])
    }
}

private struct SetupStep: View {
    let number: Int
    let text: String

    var body: some View {
        HStack(alignment: .firstTextBaseline, spacing: 12) {
            Text(verbatim: String(number))
                .font(.footnote.bold())
                .foregroundStyle(.white)
                .frame(width: 24, height: 24)
                .background(Circle().fill(Color.accentColor))
            Text(verbatim: text)
                .font(.callout)
                .fixedSize(horizontal: false, vertical: true)
        }
    }
}
