import Foundation

struct StubResponse {
    var statusCode: Int = 200
    var body: Data = Data()
    var failure: URLError?

    static func json(_ body: String, statusCode: Int = 200) -> StubResponse {
        StubResponse(statusCode: statusCode, body: Data(body.utf8))
    }

    static func data(_ body: Data, statusCode: Int = 200) -> StubResponse {
        StubResponse(statusCode: statusCode, body: body)
    }

    static func networkFailure() -> StubResponse {
        StubResponse(failure: URLError(.networkConnectionLost))
    }
}

struct RecordedRequest {
    let url: URL
    let method: String
    let headers: [String: String]
    let body: String
}

final class StubServer: @unchecked Sendable {
    let host = "stub-\(UUID().uuidString.lowercased()).test"
    private let lock = NSLock()
    private var queue: [StubResponse] = []
    private var recorded: [RecordedRequest] = []

    init() {
        StubURLProtocol.register(self)
    }

    func url(_ path: String) -> URL {
        URL(string: "https://\(host)\(path)")!
    }

    func enqueue(_ response: StubResponse) {
        lock.lock()
        defer { lock.unlock() }
        queue.append(response)
    }

    var requests: [RecordedRequest] {
        lock.lock()
        defer { lock.unlock() }
        return recorded
    }

    var requestCount: Int { requests.count }

    func makeSession() -> URLSession {
        let configuration = URLSessionConfiguration.ephemeral
        configuration.protocolClasses = [StubURLProtocol.self]
        return URLSession(configuration: configuration)
    }

    func next(for request: URLRequest) -> StubResponse {
        lock.lock()
        defer { lock.unlock() }
        recorded.append(
            RecordedRequest(
                url: request.url!,
                method: request.httpMethod ?? "GET",
                headers: request.allHTTPHeaderFields ?? [:],
                body: String(data: Self.bodyData(of: request), encoding: .utf8) ?? ""
            )
        )
        return queue.isEmpty ? StubResponse(statusCode: 500) : queue.removeFirst()
    }

    private static func bodyData(of request: URLRequest) -> Data {
        if let body = request.httpBody { return body }
        guard let stream = request.httpBodyStream else { return Data() }
        stream.open()
        defer { stream.close() }
        var data = Data()
        var buffer = [UInt8](repeating: 0, count: 4096)
        while stream.hasBytesAvailable {
            let read = stream.read(&buffer, maxLength: buffer.count)
            if read <= 0 { break }
            data.append(buffer, count: read)
        }
        return data
    }
}

final class StubURLProtocol: URLProtocol {
    private static let lock = NSLock()
    nonisolated(unsafe) private static var servers: [String: StubServer] = [:]

    static func register(_ server: StubServer) {
        lock.lock()
        defer { lock.unlock() }
        servers[server.host] = server
    }

    private static func server(for request: URLRequest) -> StubServer? {
        guard let host = request.url?.host else { return nil }
        lock.lock()
        defer { lock.unlock() }
        return servers[host]
    }

    override class func canInit(with request: URLRequest) -> Bool { true }

    override class func canonicalRequest(for request: URLRequest) -> URLRequest { request }

    override func startLoading() {
        guard let server = Self.server(for: request) else {
            client?.urlProtocol(self, didFailWithError: URLError(.unsupportedURL))
            return
        }
        let stub = server.next(for: request)
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
