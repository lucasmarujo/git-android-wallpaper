import GitWallpaperKit
import SwiftUI

enum HomeAccessibility {
    static let applyButton = "home_apply_button"
    static let preview = "home_preview"
    static let lastApplied = "home_last_applied"
    static let customCommitColor = "home_custom_commit_color"
    static let message = "home_message"
}

struct HomeView: View {
    @StateObject private var viewModel: HomeViewModel
    @State private var isSignOutConfirmationPresented = false
    @State private var isCommitColorPresented = false
    @Environment(\.scenePhase) private var scenePhase
    @Environment(\.openURL) private var openURL

    init(viewModel: @autoclosure @escaping () -> HomeViewModel) {
        _viewModel = StateObject(wrappedValue: viewModel())
    }

    private var uiState: HomeUiState { viewModel.uiState }

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(alignment: .leading, spacing: 20) {
                    WallpaperPreview(uiState: uiState) { Task { await viewModel.refresh() } }
                    if let stats = uiState.stats {
                        StatsRow(stats: stats)
                    }
                    if uiState.settings.autoUpdate && !uiState.isShortcutReady {
                        AutomationHintCard { viewModel.presentShortcutSetup() }
                    }
                    SettingsSection(title: "home_section_target") {
                        ChoicePicker(options: WallpaperTarget.allCases, selection: setting(\.target), label: \.labelKey)
                    }
                    WidgetCard()
                    SettingsSection(title: "home_section_theme") {
                        ThemeSelector(selected: setting(\.theme), commitColor: uiState.settings.commitColor)
                    }
                    SettingsSection(title: "home_section_commit_color") {
                        CommitColorSelector(
                            settings: uiState.settings,
                            onUseThemeColors: { viewModel.updateSettings { $0.commitColor = nil } },
                            onPickCustomColor: { isCommitColorPresented = true }
                        )
                    }
                    SettingsSection(title: "home_section_period") {
                        ChoicePicker(options: ContributionPeriod.allCases, selection: setting(\.period), label: \.labelKey)
                    }
                    SettingsSection(title: "home_section_shape") {
                        ChoicePicker(options: CellShape.allCases, selection: setting(\.cellShape), label: \.labelKey)
                    }
                    Toggle("home_show_stats", isOn: setting(\.showStats))
                    SettingsSection(title: "home_section_auto_update") {
                        Toggle("home_auto_update", isOn: setting(\.autoUpdate))
                        Text("home_auto_update_every")
                            .font(.callout)
                            .foregroundStyle(.secondary)
                        ChoicePicker(options: UpdateInterval.allCases, selection: setting(\.updateInterval), label: \.labelKey)
                            .disabled(!uiState.settings.autoUpdate)
                        Text("home_auto_update_hint")
                            .font(.caption)
                            .foregroundStyle(.secondary)
                    }
                    LastAppliedText(lastAppliedAt: uiState.lastAppliedAt)
                    Button("privacy_policy") { openURL(AppLinks.privacyPolicy) }
                        .font(.footnote)
                        .frame(maxWidth: .infinity)
                }
                .padding(.horizontal, 20)
                .padding(.vertical, 12)
            }
            .safeAreaInset(edge: .bottom) {
                ApplyButton(isApplying: uiState.isApplying, enabled: uiState.hasCalendar) {
                    Task { await viewModel.startAdjusting() }
                }
            }
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .principal) {
                    VStack(spacing: 0) {
                        Text(verbatim: Self.appName).font(.headline)
                        Text(verbatim: "@\(uiState.login)")
                            .font(.caption)
                            .foregroundStyle(.secondary)
                    }
                }
                ToolbarItemGroup(placement: .topBarTrailing) {
                    Button {
                        Task { await viewModel.refresh() }
                    } label: {
                        Label("home_refresh", systemImage: "arrow.clockwise")
                    }
                    .disabled(uiState.isRefreshing)
                    Button {
                        isSignOutConfirmationPresented = true
                    } label: {
                        Label("home_sign_out", systemImage: "rectangle.portrait.and.arrow.right")
                    }
                }
            }
        }
        .overlay(alignment: .bottom) {
            if let event = viewModel.messageEvent {
                MessageBanner(text: String(localized: event.message.textKey))
                    .padding(.bottom, 96)
                    .transition(.move(edge: .bottom).combined(with: .opacity))
                    .accessibilityIdentifier(HomeAccessibility.message)
                    .task(id: event.id) {
                        try? await Task.sleep(for: .seconds(Self.messageSeconds))
                        withAnimation { viewModel.clearMessage(event) }
                    }
            }
        }
        .animation(.default, value: viewModel.messageEvent)
        .fullScreenCover(isPresented: adjustingBinding, onDismiss: viewModel.adjustmentDismissed) {
            WallpaperAdjustView(
                uiState: uiState,
                onApply: { framing in Task { await viewModel.applyWallpaper(framing) } },
                onCancel: viewModel.cancelAdjusting
            )
        }
        .sheet(isPresented: $viewModel.isShortcutSetupPresented) {
            ShortcutSetupSheet(
                target: uiState.settings.target,
                imageURL: viewModel.stagedWallpaperURL,
                onOpenShortcuts: { openURL(WallpaperShortcut.shortcutsAppURL) },
                onRunShortcut: viewModel.runShortcut
            )
        }
        .sheet(isPresented: $isCommitColorPresented) {
            CommitColorSheet(
                theme: uiState.settings.theme,
                initialColor: uiState.settings.commitColor ?? WallpaperPalette.of(uiState.settings.theme).level4,
                onConfirm: { color in
                    isCommitColorPresented = false
                    viewModel.updateSettings { $0.commitColor = color }
                },
                onCancel: { isCommitColorPresented = false }
            )
        }
        .alert("home_sign_out_title", isPresented: $isSignOutConfirmationPresented) {
            Button("home_sign_out", role: .destructive) { Task { await viewModel.signOut() } }
            Button("login_cancel", role: .cancel) {}
        } message: {
            Text("home_sign_out_body")
        }
        .task { await viewModel.start() }
        .onChange(of: scenePhase) { _, phase in
            if phase == .active { Task { await viewModel.reload() } }
        }
        .onOpenURL { url in
            if let callback = WallpaperShortcut.callback(from: url) {
                viewModel.handleShortcutCallback(callback)
            }
        }
    }

    private var adjustingBinding: Binding<Bool> {
        Binding(
            get: { viewModel.uiState.isAdjusting },
            set: { isPresented in if !isPresented { viewModel.cancelAdjusting() } }
        )
    }

    private func setting<Value>(_ keyPath: WritableKeyPath<WallpaperSettings, Value>) -> Binding<Value> {
        Binding(
            get: { viewModel.uiState.settings[keyPath: keyPath] },
            set: { value in viewModel.updateSettings { $0[keyPath: keyPath] = value } }
        )
    }

    private static let appName = "Git Wallpaper"
    private static let messageSeconds = 3.0
}

private struct ApplyButton: View {
    let isApplying: Bool
    let enabled: Bool
    let onApply: () -> Void

    var body: some View {
        Button(action: onApply) {
            HStack(spacing: 12) {
                if isApplying {
                    ProgressView().tint(.white)
                    Text("home_applying")
                } else {
                    Text("home_apply")
                }
            }
            .frame(maxWidth: .infinity, minHeight: 36)
        }
        .buttonStyle(.borderedProminent)
        .controlSize(.large)
        .disabled(!enabled || isApplying)
        .padding(.horizontal, 20)
        .padding(.vertical, 12)
        .background(.bar)
        .accessibilityIdentifier(HomeAccessibility.applyButton)
    }
}

private struct WallpaperPreview: View {
    let uiState: HomeUiState
    let onRetry: () -> Void

    var body: some View {
        let shape = RoundedRectangle(cornerRadius: 24, style: .continuous)
        ZStack {
            shape.fill(Color(.secondarySystemBackground))
            if let preview = uiState.preview {
                Image(decorative: preview, scale: 1)
                    .resizable()
                    .scaledToFill()
                    .accessibilityLabel(Text("home_preview_description"))
            } else if uiState.isLoadingCalendar {
                ProgressView()
            } else {
                VStack(spacing: 8) {
                    Text("home_no_contributions")
                        .font(.callout)
                        .multilineTextAlignment(.center)
                    Button("home_retry", action: onRetry)
                }
                .padding(16)
            }
        }
        .aspectRatio(uiState.previewAspectRatio, contentMode: .fit)
        .clipShape(shape)
        .overlay(shape.stroke(Color(.separator), lineWidth: 1))
        .containerRelativeFrame(.horizontal) { width, _ in width * 0.55 }
        .frame(maxWidth: .infinity)
        .accessibilityIdentifier(HomeAccessibility.preview)
    }
}

private struct StatsRow: View {
    let stats: ContributionStats
    private let strings = WallpaperStrings.standard

    var body: some View {
        HStack {
            StatItem(value: strings.number(stats.totalContributions), label: "home_stat_contributions")
            StatItem(value: strings.days(stats.currentStreak), label: "home_stat_current_streak")
            StatItem(value: strings.days(stats.longestStreak), label: "home_stat_longest_streak")
        }
    }
}

private struct StatItem: View {
    let value: String
    let label: LocalizedStringKey

    var body: some View {
        VStack(spacing: 2) {
            Text(verbatim: value).font(.headline)
            Text(label)
                .font(.caption)
                .foregroundStyle(.secondary)
        }
        .frame(maxWidth: .infinity)
    }
}

private struct AutomationHintCard: View {
    let onSetUp: () -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text("home_automation_title").font(.subheadline.weight(.semibold))
            Text("home_automation_body").font(.footnote)
            Button("home_automation_action", action: onSetUp)
                .font(.footnote.weight(.semibold))
        }
        .padding(16)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(RoundedRectangle(cornerRadius: 16).fill(Color.accentColor.opacity(0.15)))
    }
}

private struct WidgetCard: View {
    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text("home_section_widget")
                .font(.subheadline.weight(.semibold))
                .foregroundStyle(.tint)
            Text("home_widget_body").font(.callout)
            Text("home_widget_system_hint")
                .font(.footnote)
                .foregroundStyle(.secondary)
        }
        .padding(16)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(RoundedRectangle(cornerRadius: 16).fill(Color(.secondarySystemBackground)))
    }
}

private struct SettingsSection<Content: View>: View {
    let title: LocalizedStringKey
    @ViewBuilder let content: Content

    var body: some View {
        VStack(alignment: .leading, spacing: 10) {
            Text(title)
                .font(.subheadline.weight(.semibold))
                .foregroundStyle(.tint)
            content
        }
    }
}

private struct ChoicePicker<Option: Hashable>: View {
    let options: [Option]
    @Binding var selection: Option
    let label: KeyPath<Option, LocalizedStringKey>

    var body: some View {
        Picker(selection: $selection) {
            ForEach(options, id: \.self) { option in
                Text(option[keyPath: label]).tag(option)
            }
        } label: {
            EmptyView()
        }
        .pickerStyle(.segmented)
    }
}

private struct ThemeSelector: View {
    @Binding var selected: WallpaperTheme
    let commitColor: ARGBColor?

    var body: some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: 12) {
                ForEach(WallpaperTheme.allCases, id: \.self) { theme in
                    swatch(theme)
                }
            }
            .padding(4)
        }
    }

    private func swatch(_ theme: WallpaperTheme) -> some View {
        let palette = WallpaperPalette.of(theme).withCommitColor(commitColor)
        let isSelected = theme == selected
        let shape = RoundedRectangle(cornerRadius: 14, style: .continuous)
        return Button {
            selected = theme
        } label: {
            VStack(spacing: 6) {
                ZStack {
                    shape.fill(Color(palette.background))
                    ContributionPattern(palette: palette, pattern: Self.swatchPattern)
                        .frame(width: 32, height: 32)
                }
                .frame(width: 60, height: 60)
                .overlay(shape.strokeBorder(isSelected ? Color.accentColor : Color(.separator), lineWidth: isSelected ? 3 : 1))
                Text(theme.labelKey)
                    .font(.caption)
                    .fontWeight(isSelected ? .bold : .regular)
                    .foregroundStyle(Color.primary)
            }
        }
        .buttonStyle(.plain)
        .accessibilityAddTraits(isSelected ? .isSelected : [])
    }

    private static let swatchPattern = ContributionPattern.of("142", "031", "204")
}

private struct CommitColorSelector: View {
    let settings: WallpaperSettings
    let onUseThemeColors: () -> Void
    let onPickCustomColor: () -> Void

    var body: some View {
        HStack(spacing: 8) {
            Chip(isSelected: settings.commitColor == nil, action: onUseThemeColors) {
                Text("commit_color_theme")
            }
            Chip(isSelected: settings.commitColor != nil, action: onPickCustomColor) {
                HStack(spacing: 6) {
                    Circle()
                        .fill(Color(settings.commitColor ?? WallpaperPalette.of(settings.theme).level4))
                        .overlay(Circle().stroke(Color(.separator)))
                        .frame(width: 18, height: 18)
                    Text("commit_color_custom")
                }
            }
            .accessibilityIdentifier(HomeAccessibility.customCommitColor)
        }
    }
}

private struct Chip<Label: View>: View {
    let isSelected: Bool
    let action: () -> Void
    @ViewBuilder let label: Label

    var body: some View {
        Button(action: action) {
            label
                .font(.callout)
                .padding(.horizontal, 14)
                .padding(.vertical, 8)
                .background(Capsule().fill(isSelected ? Color.accentColor.opacity(0.2) : Color(.secondarySystemBackground)))
                .overlay(Capsule().stroke(isSelected ? Color.accentColor : Color(.separator)))
        }
        .buttonStyle(.plain)
        .accessibilityAddTraits(isSelected ? .isSelected : [])
    }
}

private struct LastAppliedText: View {
    let lastAppliedAt: Date?

    var body: some View {
        Group {
            if let lastAppliedAt {
                Text(String(format: String(localized: "home_last_applied"), lastAppliedAt.formatted(date: .numeric, time: .shortened)))
            } else {
                Text("home_never_applied")
            }
        }
        .font(.footnote)
        .foregroundStyle(.secondary)
        .frame(maxWidth: .infinity)
        .multilineTextAlignment(.center)
        .accessibilityIdentifier(HomeAccessibility.lastApplied)
    }
}
