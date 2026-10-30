import Foundation

final class WallpaperShortcut: @unchecked Sendable {
    enum Callback: Equatable {
        case success
        case cancel
        case error(message: String?)
    }

    static let name = "Git Wallpaper"
    static let callbackScheme = "gitwallpaper"
    static let installURL = URL(string: "https://www.icloud.com/shortcuts/eadb2565efde4dc69dd126bb60bbcc68")!
    static let automationInstallURL = URL(string: "https://www.icloud.com/shortcuts/67ecf3a0f21a4ce49243079a1bbefc6f")!

    private let defaults: UserDefaults
    private let operatingSystemVersion: OperatingSystemVersion

    init(defaults: UserDefaults, operatingSystemVersion: OperatingSystemVersion = ProcessInfo.processInfo.operatingSystemVersion) {
        self.defaults = defaults
        self.operatingSystemVersion = operatingSystemVersion
    }

    var isReady: Bool {
        get { defaults.bool(forKey: Self.readyKey) }
        set { defaults.set(newValue, forKey: Self.readyKey) }
    }

    var isAutomationAdded: Bool {
        get { defaults.bool(forKey: Self.automationAddedKey) }
        set { defaults.set(newValue, forKey: Self.automationAddedKey) }
    }

    var canInstallAutomation: Bool {
        operatingSystemVersion.majorVersion >= Self.automationLinksMajorVersion
    }

    var runURL: URL {
        let parameters = [
            (Self.nameParameter, Self.name),
            (Self.successParameter, Self.callbackURL(Self.successPath)),
            (Self.cancelParameter, Self.callbackURL(Self.cancelPath)),
            (Self.errorParameter, Self.callbackURL(Self.errorPath)),
        ]
        let query = parameters
            .map { "\($0.0)=\(Self.percentEncoded($0.1))" }
            .joined(separator: "&")
        return URL(string: "\(Self.runShortcutBase)?\(query)")!
    }

    static func callback(from url: URL) -> Callback? {
        guard url.scheme == callbackScheme, url.host == callbackHost else { return nil }
        switch url.path {
        case "/\(successPath)": return .success
        case "/\(cancelPath)": return .cancel
        case "/\(errorPath)": return .error(message: errorMessage(in: url))
        default: return nil
        }
    }

    private static func errorMessage(in url: URL) -> String? {
        URLComponents(url: url, resolvingAgainstBaseURL: false)?
            .queryItems?
            .first { $0.name == errorMessageParameter }?
            .value
    }

    private static func callbackURL(_ path: String) -> String {
        "\(callbackScheme)://\(callbackHost)/\(path)"
    }

    private static func percentEncoded(_ value: String) -> String {
        value.addingPercentEncoding(withAllowedCharacters: unreservedCharacters) ?? value
    }

    private static let readyKey = "shortcut_ready"
    private static let automationAddedKey = "shortcut_automation_added"
    private static let automationLinksMajorVersion = 27
    private static let runShortcutBase = "shortcuts://x-callback-url/run-shortcut"
    private static let nameParameter = "name"
    private static let successParameter = "x-success"
    private static let cancelParameter = "x-cancel"
    private static let errorParameter = "x-error"
    private static let errorMessageParameter = "errorMessage"
    private static let callbackHost = "shortcut"
    private static let successPath = "success"
    private static let cancelPath = "cancel"
    private static let errorPath = "error"
    private static let unreservedCharacters = CharacterSet(charactersIn: "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789-._~")
}
