import CoreGraphics
import Foundation
import ImageIO
import UniformTypeIdentifiers

public protocol WallpaperApplier: Sendable {
    func apply(_ image: CGImage, target: WallpaperTarget) throws
}

public enum StagedWallpaperError: Error, Equatable {
    case encodingFailed
}

public final class StagedWallpaperStore: WallpaperApplier, @unchecked Sendable {
    public let fileURL: URL
    private let fileManager: FileManager

    public init(fileURL: URL, fileManager: FileManager = .default) {
        self.fileURL = fileURL
        self.fileManager = fileManager
    }

    public func apply(_ image: CGImage, target: WallpaperTarget) throws {
        guard let data = image.pngData() else { throw StagedWallpaperError.encodingFailed }
        try fileManager.createDirectory(at: fileURL.deletingLastPathComponent(), withIntermediateDirectories: true)
        try data.write(to: fileURL, options: [.atomic, .completeFileProtectionUntilFirstUserAuthentication])
        var resourceValues = URLResourceValues()
        resourceValues.isExcludedFromBackup = true
        var url = fileURL
        try url.setResourceValues(resourceValues)
    }

    public var hasImage: Bool {
        fileManager.fileExists(atPath: fileURL.path)
    }

    public func imageData() -> Data? {
        try? Data(contentsOf: fileURL)
    }

    public func clear() {
        try? fileManager.removeItem(at: fileURL)
    }
}

public extension CGImage {
    func pngData() -> Data? {
        let data = NSMutableData()
        guard let destination = CGImageDestinationCreateWithData(data, UTType.png.identifier as CFString, 1, nil) else {
            return nil
        }
        CGImageDestinationAddImage(destination, self, nil)
        guard CGImageDestinationFinalize(destination) else { return nil }
        return data as Data
    }
}
