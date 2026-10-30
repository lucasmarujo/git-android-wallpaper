#if DEBUG
import Foundation
import GitWallpaperKit

enum ScreenshotMode {
    static let launchArgument = "-GitWallpaperScreenshots"

    static var isEnabled: Bool {
        ProcessInfo.processInfo.arguments.contains(launchArgument)
    }

    static func makeEnvironment() -> (SharedContainer, WallpaperShortcut) {
        let defaults = UserDefaults(suiteName: suiteName) ?? .standard
        defaults.removePersistentDomain(forName: suiteName)
        let directory = FileManager.default.temporaryDirectory.appendingPathComponent(suiteName, isDirectory: true)
        try? FileManager.default.removeItem(at: directory)
        let clock = WallClock.system
        let container = SharedContainer(
            urlSession: URLSession(configuration: .ephemeral),
            contributionsSource: SampleContributionsSource(calendar: .sample(login: login, endingAt: clock.today())),
            secretStore: InMemorySecretStore(),
            defaults: defaults,
            directory: directory,
            clock: clock
        )
        try? container.sessionRepository.save(Session(login: login, token: token))
        container.updateStatusRepository.markApplied(fingerprint: token, at: clock.now.addingTimeInterval(-lastAppliedSecondsAgo))
        let shortcut = WallpaperShortcut(defaults: defaults)
        shortcut.isReady = true
        return (container, shortcut)
    }

    private static let suiteName = "dev.marujo.gitwallpaper.screenshots"
    private static let login = "lucasmarujo"
    private static let token = "screenshots"
    private static let lastAppliedSecondsAgo: TimeInterval = 3600
}

private struct SampleContributionsSource: ContributionsSource {
    let calendar: ContributionCalendar

    func fetchCalendar(token: String) async throws -> ContributionCalendar {
        calendar
    }
}

private final class InMemorySecretStore: SecretStore, @unchecked Sendable {
    private let lock = NSLock()
    private var secret: String?

    func read() throws -> String? {
        lock.lock()
        defer { lock.unlock() }
        return secret
    }

    func write(_ secret: String) throws {
        lock.lock()
        defer { lock.unlock() }
        self.secret = secret
    }

    func delete() throws {
        lock.lock()
        defer { lock.unlock() }
        secret = nil
    }
}
#endif
