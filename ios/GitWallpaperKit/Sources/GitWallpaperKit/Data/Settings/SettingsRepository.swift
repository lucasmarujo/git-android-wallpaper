import Foundation

public final class SettingsRepository: @unchecked Sendable {
    private let defaults: UserDefaults
    private let lock = NSLock()

    public init(defaults: UserDefaults) {
        self.defaults = defaults
    }

    public func current() -> WallpaperSettings {
        lock.lock()
        defer { lock.unlock() }
        return read()
    }

    @discardableResult
    public func update(_ change: (inout WallpaperSettings) -> Void) -> WallpaperSettings {
        lock.lock()
        defer { lock.unlock() }
        var settings = read()
        change(&settings)
        write(settings)
        return read()
    }

    private func read() -> WallpaperSettings {
        let fallback = WallpaperSettings()
        return WallpaperSettings(
            target: enumValue(Key.target, default: fallback.target),
            theme: enumValue(Key.theme, default: fallback.theme),
            commitColor: (defaults.object(forKey: Key.commitColor) as? NSNumber).map { ARGBColor(argb: $0.uint32Value) },
            period: enumValue(Key.period, default: fallback.period),
            cellShape: enumValue(Key.cellShape, default: fallback.cellShape),
            showStats: boolValue(Key.showStats, default: fallback.showStats),
            framing: WallpaperFraming(
                zoom: floatValue(Key.framingZoom, default: fallback.framing.zoom),
                offsetX: floatValue(Key.framingOffsetX, default: fallback.framing.offsetX),
                offsetY: floatValue(Key.framingOffsetY, default: fallback.framing.offsetY)
            ).coerced(),
            autoUpdate: boolValue(Key.autoUpdate, default: fallback.autoUpdate),
            updateInterval: enumValue(Key.updateInterval, default: fallback.updateInterval)
        )
    }

    private func write(_ settings: WallpaperSettings) {
        defaults.set(settings.target.rawValue, forKey: Key.target)
        defaults.set(settings.theme.rawValue, forKey: Key.theme)
        if let commitColor = settings.commitColor {
            defaults.set(NSNumber(value: commitColor.argb), forKey: Key.commitColor)
        } else {
            defaults.removeObject(forKey: Key.commitColor)
        }
        defaults.set(settings.period.rawValue, forKey: Key.period)
        defaults.set(settings.cellShape.rawValue, forKey: Key.cellShape)
        defaults.set(settings.showStats, forKey: Key.showStats)
        let framing = settings.framing.coerced()
        defaults.set(framing.zoom, forKey: Key.framingZoom)
        defaults.set(framing.offsetX, forKey: Key.framingOffsetX)
        defaults.set(framing.offsetY, forKey: Key.framingOffsetY)
        defaults.set(settings.autoUpdate, forKey: Key.autoUpdate)
        defaults.set(settings.updateInterval.rawValue, forKey: Key.updateInterval)
    }

    private func enumValue<T: RawRepresentable>(_ key: String, default fallback: T) -> T where T.RawValue == String {
        defaults.string(forKey: key).flatMap(T.init(rawValue:)) ?? fallback
    }

    private func boolValue(_ key: String, default fallback: Bool) -> Bool {
        (defaults.object(forKey: key) as? NSNumber)?.boolValue ?? fallback
    }

    private func floatValue(_ key: String, default fallback: Float) -> Float {
        (defaults.object(forKey: key) as? NSNumber)?.floatValue ?? fallback
    }

    private enum Key {
        static let target = "target"
        static let theme = "theme"
        static let commitColor = "commit_color"
        static let period = "period"
        static let cellShape = "cell_shape"
        static let showStats = "show_stats"
        static let framingZoom = "framing_zoom"
        static let framingOffsetX = "framing_offset_x"
        static let framingOffsetY = "framing_offset_y"
        static let autoUpdate = "auto_update"
        static let updateInterval = "update_interval"
    }
}
