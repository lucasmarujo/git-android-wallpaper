import CoreGraphics
import Foundation
import os

public protocol WallpaperUpdater: Sendable {
    func update(force: Bool) async -> UpdateResult
}

public actor DefaultWallpaperUpdater: WallpaperUpdater {
    private let contributionRepository: ContributionRepository
    private let settingsRepository: SettingsRepository
    private let updateStatusRepository: UpdateStatusRepository
    private let avatarSource: AvatarSource
    private let renderer: ContributionWallpaperRenderer
    private let applier: WallpaperApplier
    private let screenSizeProvider: ScreenSizeProvider
    private let clock: WallClock
    private var pendingUpdate: Task<UpdateResult, Never>?

    public init(
        contributionRepository: ContributionRepository,
        settingsRepository: SettingsRepository,
        updateStatusRepository: UpdateStatusRepository,
        avatarSource: AvatarSource,
        renderer: ContributionWallpaperRenderer,
        applier: WallpaperApplier,
        screenSizeProvider: ScreenSizeProvider,
        clock: WallClock
    ) {
        self.contributionRepository = contributionRepository
        self.settingsRepository = settingsRepository
        self.updateStatusRepository = updateStatusRepository
        self.avatarSource = avatarSource
        self.renderer = renderer
        self.applier = applier
        self.screenSizeProvider = screenSizeProvider
        self.clock = clock
    }

    public func update(force: Bool) async -> UpdateResult {
        let previous = pendingUpdate
        let next = Task {
            _ = await previous?.value
            return await self.performUpdate(force: force)
        }
        pendingUpdate = next
        return await next.value
    }

    private func performUpdate(force: Bool) async -> UpdateResult {
        let refresh = await contributionRepository.refresh()
        let calendar: ContributionCalendar
        var staleError: SyncError?
        switch refresh {
        case .success(let fresh):
            calendar = fresh
        case .failure(let error):
            guard let cached = await cachedCalendar(for: error) else { return .failure(error) }
            calendar = cached
            staleError = error
        }
        let settings = settingsRepository.current()
        let screenSize = screenSizeProvider.screenSize()
        let today = clock.today()
        let fingerprint = WallpaperFingerprint.of(
            calendar: calendar,
            settings: settings,
            size: screenSize,
            languageTag: renderer.languageTag,
            today: today
        )
        let now = clock.now
        let status = updateStatusRepository.current()
        let changed = force || (status.lastAppliedAt != nil && fingerprint != status.fingerprint)
        if changed {
            let avatar = await avatarSource.load(url: calendar.avatarUrl)
            guard renderAndApply(calendar: calendar, settings: settings, screenSize: screenSize, today: today, avatar: avatar) else {
                return .failure(.wallpaper)
            }
            updateStatusRepository.markApplied(fingerprint: fingerprint, at: now)
        } else {
            updateStatusRepository.markChecked(at: now)
        }
        if let staleError, !force { return .failure(staleError) }
        return changed ? .applied : .unchanged
    }

    private func cachedCalendar(for error: SyncError) async -> ContributionCalendar? {
        let isTransient = error == .network || error == .github
        return isTransient ? await contributionRepository.cachedCalendar() : nil
    }

    private func renderAndApply(
        calendar: ContributionCalendar,
        settings: WallpaperSettings,
        screenSize: ScreenSize,
        today: LocalDate,
        avatar: CGImage?
    ) -> Bool {
        do {
            let image = try renderer.render(calendar: calendar, settings: settings, size: screenSize, today: today, avatar: avatar)
            try applier.apply(image, target: settings.target)
            return true
        } catch {
            Self.logger.error("Failed to stage the wallpaper: \(error.localizedDescription, privacy: .public)")
            return false
        }
    }

    private static let logger = Logger(subsystem: "dev.marujo.gitwallpaper", category: "WallpaperUpdater")
}
