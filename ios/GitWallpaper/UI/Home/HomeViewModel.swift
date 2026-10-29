import CoreGraphics
import Foundation
import GitWallpaperKit

@MainActor
final class HomeViewModel: ObservableObject {
    @Published private(set) var uiState: HomeUiState
    @Published private(set) var messageEvent: HomeMessageEvent?
    @Published var isShortcutSetupPresented = false

    private let contributionRepository: ContributionRepository
    private let settingsRepository: SettingsRepository
    private let updateStatusRepository: UpdateStatusRepository
    private let wallpaperUpdater: WallpaperUpdater
    private let widgetUpdater: WidgetUpdater
    private let authRepository: AuthRepository
    private let avatarSource: AvatarSource
    private let renderer: ContributionWallpaperRenderer
    private let stagedWallpaper: StagedWallpaperStore
    private let wallpaperShortcut: WallpaperShortcut
    private let urlOpener: URLOpening
    private let previewSize: ScreenSize
    private let adjustmentSize: ScreenSize
    private let clock: WallClock

    private var calendar: ContributionCalendar?
    private var hasStarted = false
    private var initialRefreshFinished = false
    private var previewGeneration = 0
    private var adjustmentGeneration = 0

    init(
        login: String,
        contributionRepository: ContributionRepository,
        settingsRepository: SettingsRepository,
        updateStatusRepository: UpdateStatusRepository,
        wallpaperUpdater: WallpaperUpdater,
        widgetUpdater: WidgetUpdater,
        authRepository: AuthRepository,
        avatarSource: AvatarSource,
        renderer: ContributionWallpaperRenderer,
        stagedWallpaper: StagedWallpaperStore,
        wallpaperShortcut: WallpaperShortcut,
        urlOpener: URLOpening,
        previewSize: ScreenSize,
        adjustmentSize: ScreenSize,
        clock: WallClock
    ) {
        self.contributionRepository = contributionRepository
        self.settingsRepository = settingsRepository
        self.updateStatusRepository = updateStatusRepository
        self.wallpaperUpdater = wallpaperUpdater
        self.widgetUpdater = widgetUpdater
        self.authRepository = authRepository
        self.avatarSource = avatarSource
        self.renderer = renderer
        self.stagedWallpaper = stagedWallpaper
        self.wallpaperShortcut = wallpaperShortcut
        self.urlOpener = urlOpener
        self.previewSize = previewSize
        self.adjustmentSize = adjustmentSize
        self.clock = clock
        var initialState = HomeUiState(login: login)
        initialState.settings = settingsRepository.current()
        initialState.previewAspectRatio = previewSize.aspectRatio
        initialState.isShortcutReady = wallpaperShortcut.isReady
        uiState = initialState
    }

    var stagedWallpaperURL: URL? {
        stagedWallpaper.hasImage ? stagedWallpaper.fileURL : nil
    }

    func start() async {
        guard !hasStarted else { return }
        hasStarted = true
        await reload()
        await refreshContributions(notify: false)
        initialRefreshFinished = true
        updateLoadingState()
    }

    func reload() async {
        calendar = await contributionRepository.cachedCalendar()
        uiState.settings = settingsRepository.current()
        uiState.lastAppliedAt = updateStatusRepository.current().lastAppliedAt
        uiState.hasCalendar = calendar != nil
        uiState.isShortcutReady = wallpaperShortcut.isReady
        updateLoadingState()
        await renderPreview()
    }

    @discardableResult
    func updateSettings(_ change: (inout WallpaperSettings) -> Void) -> Task<Void, Never> {
        uiState.settings = settingsRepository.update(change)
        return Task {
            await widgetUpdater.updateAll()
            await renderPreview()
            if uiState.isAdjusting { await renderAdjustmentPreview() }
        }
    }

    func startAdjusting() async {
        uiState.isAdjusting = true
        await renderAdjustmentPreview()
    }

    func cancelAdjusting() {
        guard !uiState.isApplying else { return }
        uiState.isAdjusting = false
        uiState.adjustmentPreview = nil
    }

    func applyWallpaper(_ framing: WallpaperFraming) async {
        guard !uiState.isApplying else { return }
        uiState.isApplying = true
        uiState.settings = settingsRepository.update { $0.framing = framing.coerced() }
        let result = await wallpaperUpdater.update(force: true)
        uiState.isApplying = false
        uiState.isAdjusting = false
        uiState.adjustmentPreview = nil
        await widgetUpdater.updateAll()
        await reload()
        switch result {
        case .applied, .unchanged:
            if wallpaperShortcut.isReady {
                urlOpener.open(wallpaperShortcut.runURL)
            } else {
                isShortcutSetupPresented = true
            }
        case .failure(let error):
            if let message = Self.message(for: error) { show(message) }
        }
    }

    func runShortcut() {
        isShortcutSetupPresented = false
        urlOpener.open(wallpaperShortcut.runURL)
    }

    func presentShortcutSetup() {
        isShortcutSetupPresented = true
    }

    func handleShortcutCallback(_ callback: WallpaperShortcut.Callback) {
        switch callback {
        case .success:
            wallpaperShortcut.isReady = true
            uiState.isShortcutReady = true
            show(.applied)
        case .error:
            wallpaperShortcut.isReady = false
            uiState.isShortcutReady = false
            isShortcutSetupPresented = true
            show(.shortcutFailed)
        case .cancel:
            break
        }
    }

    func refresh() async {
        guard !uiState.isRefreshing else { return }
        await refreshContributions(notify: true)
    }

    func signOut() async {
        await authRepository.signOut()
    }

    func clearMessage(_ event: HomeMessageEvent) {
        if messageEvent == event { messageEvent = nil }
    }

    private func refreshContributions(notify: Bool) async {
        uiState.isRefreshing = true
        updateLoadingState()
        let result = await contributionRepository.refresh()
        uiState.isRefreshing = false
        await widgetUpdater.updateAll()
        await reload()
        guard notify else { return }
        switch result {
        case .success:
            show(.refreshed)
        case .failure(let error):
            if let message = Self.message(for: error) { show(message) }
        }
    }

    private func updateLoadingState() {
        uiState.isLoadingCalendar = calendar == nil && (uiState.isRefreshing || !initialRefreshFinished)
    }

    private func renderPreview() async {
        previewGeneration += 1
        let generation = previewGeneration
        guard let calendar else {
            uiState.preview = nil
            uiState.stats = nil
            return
        }
        let settings = uiState.settings
        let today = clock.today()
        let avatar = await avatarSource.load(url: calendar.avatarUrl)
        let image = await render(calendar: calendar, settings: settings, size: previewSize, today: today, avatar: avatar)
        guard generation == previewGeneration else { return }
        uiState.preview = image
        let periodStart = DayGrid.from(calendar.days, period: settings.period, today: today).startDate
        uiState.stats = ContributionStats.from(calendar.days, periodStart: periodStart)
    }

    private func renderAdjustmentPreview() async {
        adjustmentGeneration += 1
        let generation = adjustmentGeneration
        guard let calendar, uiState.isAdjusting else { return }
        var settings = uiState.settings
        settings.framing = WallpaperFraming()
        let avatar = await avatarSource.load(url: calendar.avatarUrl)
        let image = await render(calendar: calendar, settings: settings, size: adjustmentSize, today: clock.today(), avatar: avatar)
        guard generation == adjustmentGeneration, uiState.isAdjusting else { return }
        uiState.adjustmentPreview = image
    }

    private func render(
        calendar: ContributionCalendar,
        settings: WallpaperSettings,
        size: ScreenSize,
        today: LocalDate,
        avatar: CGImage?
    ) async -> CGImage? {
        let renderer = renderer
        return await Task.detached(priority: .userInitiated) {
            try? renderer.render(calendar: calendar, settings: settings, size: size, today: today, avatar: avatar)
        }.value
    }

    private func show(_ message: HomeMessage) {
        messageEvent = HomeMessageEvent(message: message)
    }

    private static func message(for error: SyncError) -> HomeMessage? {
        switch error {
        case .network: .networkError
        case .github: .gitHubError
        case .wallpaper: .wallpaperError
        case .sessionExpired: .sessionExpired
        case .notSignedIn: nil
        }
    }
}
