public protocol WidgetUpdater: Sendable {
    func updateAll() async
}
