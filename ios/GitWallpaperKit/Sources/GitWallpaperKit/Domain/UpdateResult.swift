public enum UpdateResult: Equatable, Sendable {
    case applied
    case unchanged
    case failure(SyncError)
}
