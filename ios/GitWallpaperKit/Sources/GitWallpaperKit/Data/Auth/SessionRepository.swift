import Foundation
import os

public struct Session: Equatable, Sendable, CustomStringConvertible {
    public var login: String
    public var token: String

    public init(login: String, token: String) {
        self.login = login
        self.token = token
    }

    public var description: String {
        "Session(login=\(login))"
    }
}

public final class SessionRepository: @unchecked Sendable {
    public static let didChangeNotification = Notification.Name("dev.marujo.gitwallpaper.session-did-change")

    private let defaults: UserDefaults
    private let secretStore: SecretStore
    private let notificationCenter: NotificationCenter

    public init(defaults: UserDefaults, secretStore: SecretStore, notificationCenter: NotificationCenter = .default) {
        self.defaults = defaults
        self.secretStore = secretStore
        self.notificationCenter = notificationCenter
    }

    public func currentSession() -> Session? {
        guard let login = defaults.string(forKey: Self.loginKey) else { return nil }
        do {
            guard let token = try secretStore.read() else { return nil }
            return Session(login: login, token: token)
        } catch {
            Self.logger.warning("Could not read the stored token: \(error.localizedDescription, privacy: .public)")
            return nil
        }
    }

    public func save(_ session: Session) throws {
        try secretStore.write(session.token)
        defaults.set(session.login, forKey: Self.loginKey)
        notificationCenter.post(name: Self.didChangeNotification, object: self)
    }

    public func clear() {
        do {
            try secretStore.delete()
        } catch {
            Self.logger.error("Could not delete the stored token: \(error.localizedDescription, privacy: .public)")
        }
        defaults.removeObject(forKey: Self.loginKey)
        notificationCenter.post(name: Self.didChangeNotification, object: self)
    }

    private static let loginKey = "login"
    private static let logger = Logger(subsystem: "dev.marujo.gitwallpaper", category: "SessionRepository")
}
