import CoreGraphics
import Foundation

public struct ARGBColor: Hashable, Sendable {
    public static let maxChannel = 255

    public let argb: UInt32

    public init(argb: UInt32) {
        self.argb = argb
    }

    public init(red: Int, green: Int, blue: Int, alpha: Int = ARGBColor.maxChannel) {
        argb = UInt32(Self.channel(alpha)) << 24
            | UInt32(Self.channel(red)) << 16
            | UInt32(Self.channel(green)) << 8
            | UInt32(Self.channel(blue))
    }

    public var alpha: Int { Int((argb >> 24) & 0xFF) }

    public var red: Int { Int((argb >> 16) & 0xFF) }

    public var green: Int { Int((argb >> 8) & 0xFF) }

    public var blue: Int { Int(argb & 0xFF) }

    public var opaque: ARGBColor {
        ARGBColor(argb: argb | Self.opaqueAlpha)
    }

    public var hexRGB: String {
        String(format: "#%06X", argb & Self.rgbMask)
    }

    public var cgColor: CGColor {
        CGColor(
            srgbRed: CGFloat(red) / CGFloat(Self.maxChannel),
            green: CGFloat(green) / CGFloat(Self.maxChannel),
            blue: CGFloat(blue) / CGFloat(Self.maxChannel),
            alpha: CGFloat(alpha) / CGFloat(Self.maxChannel)
        )
    }

    public func blended(with other: ARGBColor, ratio: Float) -> ARGBColor {
        let inverse = 1 - ratio
        func mix(_ first: Int, _ second: Int) -> Int {
            Int(Float(first) * inverse + Float(second) * ratio)
        }
        return ARGBColor(
            red: mix(red, other.red),
            green: mix(green, other.green),
            blue: mix(blue, other.blue),
            alpha: mix(alpha, other.alpha)
        )
    }

    public func withRed(_ value: Int) -> ARGBColor {
        ARGBColor(red: value, green: green, blue: blue, alpha: alpha)
    }

    public func withGreen(_ value: Int) -> ARGBColor {
        ARGBColor(red: red, green: value, blue: blue, alpha: alpha)
    }

    public func withBlue(_ value: Int) -> ARGBColor {
        ARGBColor(red: red, green: green, blue: value, alpha: alpha)
    }

    private static let opaqueAlpha: UInt32 = 0xFF00_0000
    private static let rgbMask: UInt32 = 0x00FF_FFFF

    private static func channel(_ value: Int) -> Int {
        min(max(value, 0), maxChannel)
    }
}
