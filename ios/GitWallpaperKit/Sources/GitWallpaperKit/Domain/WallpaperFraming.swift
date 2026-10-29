public struct WallpaperFraming: Equatable, Sendable {
    public static let defaultZoom: Float = 1
    public static let defaultOffset: Float = 0
    public static let minZoom: Float = 0.5
    public static let maxZoom: Float = 2.5
    public static let maxOffset: Float = 0.5

    public var zoom: Float
    public var offsetX: Float
    public var offsetY: Float

    public init(
        zoom: Float = WallpaperFraming.defaultZoom,
        offsetX: Float = WallpaperFraming.defaultOffset,
        offsetY: Float = WallpaperFraming.defaultOffset
    ) {
        self.zoom = zoom
        self.offsetX = offsetX
        self.offsetY = offsetY
    }

    public func transformed(zoomChange: Float, panX: Float, panY: Float) -> WallpaperFraming {
        WallpaperFraming(zoom: zoom * zoomChange, offsetX: offsetX + panX, offsetY: offsetY + panY).coerced()
    }

    public func coerced() -> WallpaperFraming {
        WallpaperFraming(
            zoom: Self.clamp(Self.finite(zoom, or: Self.defaultZoom), Self.minZoom, Self.maxZoom),
            offsetX: Self.clamp(Self.finite(offsetX, or: Self.defaultOffset), -Self.maxOffset, Self.maxOffset),
            offsetY: Self.clamp(Self.finite(offsetY, or: Self.defaultOffset), -Self.maxOffset, Self.maxOffset)
        )
    }

    private static func finite(_ value: Float, or fallback: Float) -> Float {
        value.isFinite ? value : fallback
    }

    private static func clamp(_ value: Float, _ lower: Float, _ upper: Float) -> Float {
        min(max(value, lower), upper)
    }
}
