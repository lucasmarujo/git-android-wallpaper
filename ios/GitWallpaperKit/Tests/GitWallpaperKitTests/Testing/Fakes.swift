import CoreGraphics
import Foundation
import GitWallpaperKit

final class Locked<Value>: @unchecked Sendable {
    private let lock = NSLock()
    private var storage: Value

    init(_ value: Value) {
        storage = value
    }

    var value: Value {
        get {
            lock.lock()
            defer { lock.unlock() }
            return storage
        }
        set {
            lock.lock()
            defer { lock.unlock() }
            storage = newValue
        }
    }

    func mutate(_ change: (inout Value) -> Void) {
        lock.lock()
        defer { lock.unlock() }
        change(&storage)
    }
}

final class FakeSecretStore: SecretStore, @unchecked Sendable {
    private let secret = Locked<String?>(nil)
    var failOnRead = false

    func read() throws -> String? {
        if failOnRead { throw KeychainError(status: -25300) }
        return secret.value
    }

    func write(_ value: String) throws {
        secret.value = value
    }

    func delete() throws {
        secret.value = nil
    }

    var storedSecret: String? { secret.value }
}

final class FakeContributionsSource: ContributionsSource, @unchecked Sendable {
    private let handlerBox: Locked<@Sendable (String) throws -> ContributionCalendar>
    private let tokens = Locked<[String]>([])

    init(handler: @escaping @Sendable (String) throws -> ContributionCalendar = { _ in TestCalendars.calendar() }) {
        handlerBox = Locked(handler)
    }

    var handler: @Sendable (String) throws -> ContributionCalendar {
        get { handlerBox.value }
        set { handlerBox.value = newValue }
    }

    var requestedTokens: [String] { tokens.value }

    func fetchCalendar(token: String) async throws -> ContributionCalendar {
        tokens.mutate { $0.append(token) }
        return try handler(token)
    }
}

final class RecordingWallpaperApplier: WallpaperApplier, @unchecked Sendable {
    struct Applied: Equatable {
        let width: Int
        let height: Int
        let target: WallpaperTarget
    }

    private let records = Locked<[Applied]>([])
    private let images = Locked<[CGImage]>([])
    var failure: Error?

    var applied: [Applied] { records.value }

    var lastImage: CGImage? { images.value.last }

    func apply(_ image: CGImage, target: WallpaperTarget) throws {
        if let failure { throw failure }
        records.mutate { $0.append(Applied(width: image.width, height: image.height, target: target)) }
        images.mutate { $0.append(image) }
    }
}

final class FakeAvatarSource: AvatarSource, @unchecked Sendable {
    private let urls = Locked<[String?]>([])
    var avatar: CGImage?

    var requestedUrls: [String?] { urls.value }

    func load(url: String?) async -> CGImage? {
        urls.mutate { $0.append(url) }
        return avatar
    }
}

final class FakeWidgetUpdater: WidgetUpdater, @unchecked Sendable {
    private let calls = Locked(0)

    var updateAllCount: Int { calls.value }

    func updateAll() async {
        calls.mutate { $0 += 1 }
    }
}

struct FixedScreenSizeProvider: ScreenSizeProvider {
    let size: ScreenSize

    func screenSize() -> ScreenSize { size }
}

final class MutableClock: @unchecked Sendable {
    private let instant: Locked<Date>
    let timeZone: TimeZone

    init(_ date: Date, timeZone: TimeZone = TimeZone(identifier: "UTC")!) {
        instant = Locked(date)
        self.timeZone = timeZone
    }

    var now: Date {
        get { instant.value }
        set { instant.value = newValue }
    }

    var wallClock: WallClock {
        WallClock(now: { self.now }, timeZone: { self.timeZone })
    }
}

struct TestError: Error {}

func solidImage(color: ARGBColor, size: Int) -> CGImage {
    let context = CGContext(
        data: nil,
        width: size,
        height: size,
        bitsPerComponent: 8,
        bytesPerRow: 0,
        space: CGColorSpace(name: CGColorSpace.sRGB)!,
        bitmapInfo: CGImageAlphaInfo.premultipliedLast.rawValue
    )!
    context.setFillColor(color.cgColor)
    context.fill(CGRect(x: 0, y: 0, width: size, height: size))
    return context.makeImage()!
}
