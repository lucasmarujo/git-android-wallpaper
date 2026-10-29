import Foundation
import os

public enum RefreshResult: Equatable, Sendable {
    case success(ContributionCalendar)
    case failure(SyncError)
}

public actor ContributionRepository {
    private let fileURL: URL
    private let contributionsSource: ContributionsSource
    private let sessionRepository: SessionRepository

    public init(fileURL: URL, contributionsSource: ContributionsSource, sessionRepository: SessionRepository) {
        self.fileURL = fileURL
        self.contributionsSource = contributionsSource
        self.sessionRepository = sessionRepository
    }

    public func cachedCalendar() -> ContributionCalendar? {
        guard let data = try? Data(contentsOf: fileURL) else { return nil }
        do {
            return try JSONDecoder().decode(ContributionCalendar.self, from: data)
        } catch {
            Self.logger.warning("Discarding unreadable contribution cache: \(error.localizedDescription, privacy: .public)")
            return nil
        }
    }

    public func refresh() async -> RefreshResult {
        guard let session = sessionRepository.currentSession() else { return .failure(.notSignedIn) }
        do {
            let calendar = try await contributionsSource.fetchCalendar(token: session.token)
            save(calendar)
            return .success(calendar)
        } catch GitHubError.unauthorized {
            Self.logger.warning("GitHub rejected the stored session")
            sessionRepository.clear()
            return .failure(.sessionExpired)
        } catch GitHubError.network(let error) {
            Self.logger.warning("Could not reach GitHub: \(error.localizedDescription, privacy: .public)")
            return .failure(.network)
        } catch {
            Self.logger.warning("GitHub returned an error: \(String(describing: error), privacy: .public)")
            return .failure(.github)
        }
    }

    public func save(_ calendar: ContributionCalendar) {
        do {
            let data = try JSONEncoder().encode(calendar)
            try FileManager.default.createDirectory(at: fileURL.deletingLastPathComponent(), withIntermediateDirectories: true)
            try data.write(to: fileURL, options: [.atomic, .completeFileProtectionUntilFirstUserAuthentication])
            var resourceValues = URLResourceValues()
            resourceValues.isExcludedFromBackup = true
            var url = fileURL
            try url.setResourceValues(resourceValues)
        } catch {
            Self.logger.error("Could not cache the contribution calendar: \(error.localizedDescription, privacy: .public)")
        }
    }

    public func clear() {
        try? FileManager.default.removeItem(at: fileURL)
    }

    private static let logger = Logger(subsystem: "dev.marujo.gitwallpaper", category: "ContributionRepository")
}
