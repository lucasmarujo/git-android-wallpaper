public enum SyncError: Error, Equatable, Sendable {
    case notSignedIn
    case sessionExpired
    case network
    case github
    case wallpaper
}
