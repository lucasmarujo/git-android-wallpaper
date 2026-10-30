public struct ScreenSize: Hashable, Sendable {
    public var width: Int
    public var height: Int

    public init(width: Int, height: Int) {
        self.width = width
        self.height = height
    }

    public func scaled(_ factor: Double) -> ScreenSize {
        ScreenSize(
            width: max(Int((Double(width) * factor).rounded()), 1),
            height: max(Int((Double(height) * factor).rounded()), 1)
        )
    }

    public var aspectRatio: Double {
        Double(width) / Double(height)
    }
}

public protocol ScreenSizeProvider: Sendable {
    func screenSize() -> ScreenSize
}
