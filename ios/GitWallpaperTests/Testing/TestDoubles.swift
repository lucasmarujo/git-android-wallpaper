import CoreGraphics
import Foundation
import GitWallpaperKit
import XCTest
@testable import GitWallpaper

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

    func read() throws -> String? { secret.value }

    func write(_ value: String) throws { secret.value = value }

    func delete() throws { secret.value = nil }
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

final class FakeWidgetUpdater: WidgetUpdater, @unchecked Sendable {
    private let calls = Locked(0)

    var updateAllCount: Int { calls.value }

    func updateAll() async {
        calls.mutate { $0 += 1 }
    }
}

final class FakeWallpaperUpdater: WallpaperUpdater, @unchecked Sendable {
    private let resultBox = Locked<UpdateResult>(.applied)
    private let calls = Locked<[Bool]>([])

    var result: UpdateResult {
        get { resultBox.value }
        set { resultBox.value = newValue }
    }

    var forcedCalls: [Bool] { calls.value }

    func update(force: Bool) async -> UpdateResult {
        calls.mutate { $0.append(force) }
        return result
    }
}

final class FakeAvatarSource: AvatarSource, @unchecked Sendable {
    func load(url: String?) async -> CGImage? { nil }
}

@MainActor
final class RecordingURLOpener: URLOpening {
    private(set) var openedURLs: [URL] = []

    nonisolated init() {}

    func open(_ url: URL) {
        openedURLs.append(url)
    }
}

enum TestCalendars {
    static let today = LocalDate(year: 2026, month: 10, day: 24)

    static func calendar(login: String = "octocat", counts: [Int] = (0..<371).map { $0 % 5 }) -> ContributionCalendar {
        let days = counts.enumerated().map { index, count in
            ContributionDay(
                date: today.minusDays(counts.count - 1 - index),
                count: count,
                level: ContributionLevel.allCases[min(count, ContributionLevel.allCases.count - 1)]
            )
        }
        return ContributionCalendar(login: login, totalContributions: days.reduce(0) { $0 + $1.count }, days: days)
    }
}

final class TestStores {
    let suiteName = "GitWallpaperTests.\(UUID().uuidString)"
    let directory = FileManager.default.temporaryDirectory.appendingPathComponent(UUID().uuidString, isDirectory: true)
    let defaults: UserDefaults

    init() {
        defaults = UserDefaults(suiteName: suiteName)!
    }

    func tearDown() {
        defaults.removePersistentDomain(forName: suiteName)
        try? FileManager.default.removeItem(at: directory)
    }

    func file(_ name: String) -> URL {
        directory.appendingPathComponent(name)
    }
}

struct StubResponse {
    var statusCode: Int = 200
    var body: Data = Data()
    var failure: URLError?

    static func json(_ body: String, statusCode: Int = 200) -> StubResponse {
        StubResponse(statusCode: statusCode, body: Data(body.utf8))
    }

    static func networkFailure() -> StubResponse {
        StubResponse(failure: URLError(.notConnectedToInternet))
    }
}

final class StubServer: @unchecked Sendable {
    let host = "stub-\(UUID().uuidString.lowercased()).test"
    private let queue = Locked<[StubResponse]>([])

    init() {
        StubURLProtocol.register(self)
    }

    func url(_ path: String) -> URL {
        URL(string: "https://\(host)\(path)")!
    }

    func enqueue(_ response: StubResponse) {
        queue.mutate { $0.append(response) }
    }

    func makeSession() -> URLSession {
        let configuration = URLSessionConfiguration.ephemeral
        configuration.protocolClasses = [StubURLProtocol.self]
        return URLSession(configuration: configuration)
    }

    func next() -> StubResponse {
        var response = StubResponse(statusCode: 500)
        queue.mutate { pending in
            if !pending.isEmpty { response = pending.removeFirst() }
        }
        return response
    }
}

final class StubURLProtocol: URLProtocol {
    private static let servers = Locked<[String: StubServer]>([:])

    static func register(_ server: StubServer) {
        servers.mutate { $0[server.host] = server }
    }

    override class func canInit(with request: URLRequest) -> Bool { true }

    override class func canonicalRequest(for request: URLRequest) -> URLRequest { request }

    override func startLoading() {
        guard let host = request.url?.host, let server = Self.servers.value[host] else {
            client?.urlProtocol(self, didFailWithError: URLError(.unsupportedURL))
            return
        }
        let stub = server.next()
        if let failure = stub.failure {
            client?.urlProtocol(self, didFailWithError: failure)
            return
        }
        let response = HTTPURLResponse(url: request.url!, statusCode: stub.statusCode, httpVersion: "HTTP/1.1", headerFields: nil)!
        client?.urlProtocol(self, didReceive: response, cacheStoragePolicy: .notAllowed)
        client?.urlProtocol(self, didLoad: stub.body)
        client?.urlProtocolDidFinishLoading(self)
    }

    override func stopLoading() {}
}

@MainActor
func waitUntil(timeout: TimeInterval = 5, file: StaticString = #filePath, line: UInt = #line, _ condition: () -> Bool) async {
    let deadline = Date().addingTimeInterval(timeout)
    while !condition() {
        if Date() > deadline {
            XCTFail("Timed out waiting for the condition", file: file, line: line)
            return
        }
        try? await Task.sleep(nanoseconds: 10_000_000)
    }
}
