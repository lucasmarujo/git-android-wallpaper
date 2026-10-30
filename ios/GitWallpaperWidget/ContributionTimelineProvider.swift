import Foundation
import GitWallpaperKit
import WidgetKit

struct ContributionEntry: TimelineEntry {
    let date: Date
    let today: LocalDate
    let calendar: ContributionCalendar?
    let settings: WallpaperSettings
}

struct ContributionTimelineProvider: TimelineProvider {
    private let container: SharedContainer

    init(container: SharedContainer = WidgetEnvironment.container) {
        self.container = container
    }

    func placeholder(in context: Context) -> ContributionEntry {
        let today = container.clock.today()
        return ContributionEntry(date: Date(), today: today, calendar: .sample(login: Self.sampleLogin, endingAt: today), settings: WallpaperSettings())
    }

    func getSnapshot(in context: Context, completion: @escaping (ContributionEntry) -> Void) {
        Task {
            let today = container.clock.today()
            let calendar = await container.contributionRepository.cachedCalendar()
            completion(
                ContributionEntry(
                    date: Date(),
                    today: today,
                    calendar: calendar ?? (context.isPreview ? .sample(login: Self.sampleLogin, endingAt: today) : nil),
                    settings: container.settingsRepository.current()
                )
            )
        }
    }

    func getTimeline(in context: Context, completion: @escaping (Timeline<ContributionEntry>) -> Void) {
        Task {
            let settings = container.settingsRepository.current()
            if settings.autoUpdate {
                _ = await container.contributionRepository.refresh()
            }
            let calendar = await container.contributionRepository.cachedCalendar()
            let now = container.clock.now
            let timeZone = container.clock.timeZone
            let today = LocalDate.today(now: now, timeZone: timeZone)
            let tomorrow = today.plusDays(1)
            let entries = [
                ContributionEntry(date: now, today: today, calendar: calendar, settings: settings),
                ContributionEntry(date: tomorrow.startOfDay(in: timeZone), today: tomorrow, calendar: calendar, settings: settings),
            ]
            let policy: TimelineReloadPolicy = settings.autoUpdate
                ? .after(now.addingTimeInterval(TimeInterval(settings.updateInterval.minutes * Self.secondsPerMinute)))
                : .never
            completion(Timeline(entries: entries, policy: policy))
        }
    }

    private static let secondsPerMinute = 60
    private static let sampleLogin = "octocat"
}

enum WidgetEnvironment {
    static let container = SharedContainer(userAgent: "GitWallpaper-iOS-Widget/\(appVersion)")

    private static var appVersion: String {
        Bundle.main.object(forInfoDictionaryKey: "CFBundleShortVersionString") as? String ?? "0"
    }
}
