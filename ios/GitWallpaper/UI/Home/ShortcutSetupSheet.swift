import GitWallpaperKit
import SwiftUI

enum ShortcutSetupAccessibility {
    static let addButton = "shortcut_add_button"
    static let runButton = "shortcut_run_button"
    static let shareButton = "shortcut_share_button"
    static let failure = "shortcut_failure"
}

struct ShortcutSetupSheet: View {
    let imageURL: URL?
    let failure: ShortcutFailure?
    let onAddShortcut: () -> Void
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
                    SetupStep(number: 1, text: "shortcut_setup_step_add")
                    Button(action: onAddShortcut) {
                        Label("shortcut_add", systemImage: "plus.square.on.square")
                            .frame(maxWidth: .infinity, minHeight: 36)
                    }
                    .buttonStyle(.borderedProminent)
                    .controlSize(.large)
                    .accessibilityIdentifier(ShortcutSetupAccessibility.addButton)
                    SetupStep(number: 2, text: "shortcut_setup_step_apply")
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
    let text: LocalizedStringKey

    var body: some View {
        HStack(alignment: .firstTextBaseline, spacing: 12) {
            Text(verbatim: String(number))
                .font(.footnote.bold())
                .foregroundStyle(.white)
                .frame(width: 24, height: 24)
                .background(Circle().fill(Color.accentColor))
            Text(text)
                .font(.callout)
                .fixedSize(horizontal: false, vertical: true)
        }
    }
}
