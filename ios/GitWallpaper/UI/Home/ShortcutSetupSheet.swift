import GitWallpaperKit
import SwiftUI

enum ShortcutSetupAccessibility {
    static let createButton = "shortcut_create_button"
    static let runButton = "shortcut_run_button"
    static let shareButton = "shortcut_share_button"
    static let failure = "shortcut_failure"
}

struct ShortcutSetupSheet: View {
    let target: WallpaperTarget
    let imageURL: URL?
    let failure: ShortcutFailure?
    let onCreateShortcut: () -> Void
    let onRunShortcut: () -> Void

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(alignment: .leading, spacing: 16) {
                    if let failure {
                        FailureCallout(failure: failure)
                    }
                    Text("shortcut_setup_body")
                        .font(.callout)
                    VStack(alignment: .leading, spacing: 12) {
                        SetupStep(number: 1, text: String(format: String(localized: "shortcut_setup_step_create"), WallpaperShortcut.name))
                        SetupStep(number: 2, text: String(localized: "shortcut_setup_step_get"))
                        SetupStep(number: 3, text: String(format: String(localized: "shortcut_setup_step_set"), String(localized: target.shortcutLocationKey)))
                        SetupStep(number: 4, text: String(localized: "shortcut_setup_step_return"))
                    }
                    Button(action: onCreateShortcut) {
                        Label("shortcut_create", systemImage: "plus.square.on.square")
                            .frame(maxWidth: .infinity, minHeight: 36)
                    }
                    .buttonStyle(.borderedProminent)
                    .controlSize(.large)
                    .accessibilityIdentifier(ShortcutSetupAccessibility.createButton)
                    Text(String(format: String(localized: "shortcut_create_hint"), WallpaperShortcut.name))
                        .font(.footnote)
                        .foregroundStyle(.secondary)
                    Button(action: onRunShortcut) {
                        Text("shortcut_run").frame(maxWidth: .infinity, minHeight: 36)
                    }
                    .buttonStyle(.bordered)
                    .controlSize(.large)
                    .accessibilityIdentifier(ShortcutSetupAccessibility.runButton)
                    Text("shortcut_setup_step_automation")
                        .font(.footnote)
                        .foregroundStyle(.secondary)
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

private struct FailureCallout: View {
    let failure: ShortcutFailure

    var body: some View {
        VStack(alignment: .leading, spacing: 6) {
            Label("message_shortcut_failed", systemImage: "exclamationmark.triangle.fill")
                .font(.callout.weight(.semibold))
                .foregroundStyle(.red)
            if let reason = failure.reason, !reason.isEmpty {
                Text(String(format: String(localized: "shortcut_failure_reason"), reason))
                    .font(.footnote)
                    .foregroundStyle(.secondary)
            }
        }
        .padding(14)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(RoundedRectangle(cornerRadius: 12).fill(Color.red.opacity(0.12)))
        .accessibilityElement(children: .combine)
        .accessibilityIdentifier(ShortcutSetupAccessibility.failure)
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
