import GitWallpaperKit
import UIKit

final class DisplayScreenSizeProvider: ScreenSizeProvider, @unchecked Sendable {
    private let lock = NSLock()
    private var cachedSize: ScreenSize?

    func screenSize() -> ScreenSize {
        if let cached = cached() { return cached }
        let size = Thread.isMainThread
            ? MainActor.assumeIsolated { Self.nativeScreenSize() }
            : DispatchQueue.main.sync { MainActor.assumeIsolated { Self.nativeScreenSize() } }
        lock.lock()
        cachedSize = size
        lock.unlock()
        return size
    }

    private func cached() -> ScreenSize? {
        lock.lock()
        defer { lock.unlock() }
        return cachedSize
    }

    @MainActor
    private static func nativeScreenSize() -> ScreenSize {
        let bounds = UIScreen.main.nativeBounds
        let width = Int(bounds.width.rounded())
        let height = Int(bounds.height.rounded())
        return ScreenSize(width: min(width, height), height: max(width, height))
    }
}
