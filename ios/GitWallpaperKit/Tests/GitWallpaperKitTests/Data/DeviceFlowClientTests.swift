import GitWallpaperKit
import XCTest

final class DeviceFlowClientTests: XCTestCase {
    private let server = StubServer()
    private let elapsed = Locked(0)
    private let clientId = "client-123"
    private let pending = #"{"error":"authorization_pending"}"#
    private let deviceCode = DeviceCode(
        deviceCode: "device-123",
        userCode: "ABCD-1234",
        verificationUri: "https://github.com/login/device",
        expiresInSeconds: 900,
        intervalSeconds: 5
    )

    private lazy var client = DeviceFlowClient(
        session: server.makeSession(),
        clientId: clientId,
        baseURL: server.url("/"),
        sleep: { [elapsed] seconds in elapsed.mutate { $0 += seconds } }
    )

    func testRequestsADeviceCodeWithTheClientIdAndReadUserScope() async throws {
        server.enqueue(.json(#"{"device_code":"device-123","user_code":"ABCD-1234","verification_uri":"https://github.com/login/device","expires_in":900,"interval":5}"#))

        let result = try await client.requestDeviceCode()

        let request = try XCTUnwrap(server.requests.first)
        XCTAssertEqual("/login/device/code", request.url.path)
        XCTAssertEqual("application/json", request.headers["Accept"])
        XCTAssertTrue(request.body.contains("client_id=\(clientId)"))
        XCTAssertTrue(request.body.contains("scope=read%3Auser"))
        XCTAssertEqual(deviceCode, result)
    }

    func testDeviceCodeErrorsAreMappedToDeviceFlowErrors() async {
        server.enqueue(.json(#"{"error":"device_flow_disabled"}"#))

        await XCTAssertThrowsErrorAsync(try await client.requestDeviceCode()) { error in
            XCTAssertEqual(.deviceFlowDisabled, error as? DeviceFlowError)
        }
    }

    func testPollsUntilTheUserAuthorizesTheDevice() async throws {
        server.enqueue(.json(pending))
        server.enqueue(.json(pending))
        server.enqueue(.json(#"{"access_token":"gho_token","token_type":"bearer","scope":"read:user"}"#))

        let token = try await client.awaitAccessToken(deviceCode)

        XCTAssertEqual("gho_token", token)
        XCTAssertEqual(3, server.requestCount)
        XCTAssertEqual(15, elapsed.value)
        let request = try XCTUnwrap(server.requests.first)
        XCTAssertEqual("/login/oauth/access_token", request.url.path)
        XCTAssertTrue(request.body.contains("device_code=device-123"))
    }

    func testSlowDownResponsesIncreaseThePollingInterval() async throws {
        server.enqueue(.json(#"{"error":"slow_down","interval":10}"#))
        server.enqueue(.json(#"{"access_token":"gho_token"}"#))

        _ = try await client.awaitAccessToken(deviceCode)

        XCTAssertEqual(15, elapsed.value)
    }

    func testDeniedAuthorizationsStopPolling() async {
        server.enqueue(.json(#"{"error":"access_denied"}"#))

        await XCTAssertThrowsErrorAsync(try await client.awaitAccessToken(deviceCode)) { error in
            XCTAssertEqual(.accessDenied, error as? DeviceFlowError)
        }
        XCTAssertEqual(1, server.requestCount)
    }

    func testExpiredTokensReportedByGitHubStopPolling() async {
        server.enqueue(.json(#"{"error":"expired_token"}"#))

        await XCTAssertThrowsErrorAsync(try await client.awaitAccessToken(deviceCode)) { error in
            XCTAssertEqual(.expired, error as? DeviceFlowError)
        }
    }

    func testPollingStopsWhenTheDeviceCodeLifetimeIsOver() async {
        server.enqueue(.json(pending))
        server.enqueue(.json(pending))
        var shortLived = deviceCode
        shortLived.expiresInSeconds = 10

        await XCTAssertThrowsErrorAsync(try await client.awaitAccessToken(shortLived)) { error in
            XCTAssertEqual(.expired, error as? DeviceFlowError)
        }
        XCTAssertEqual(2, server.requestCount)
    }

    func testTransientNetworkFailuresWhilePollingAreRetried() async throws {
        server.enqueue(.networkFailure())
        server.enqueue(.json(#"{"access_token":"gho_token"}"#))

        let token = try await client.awaitAccessToken(deviceCode)

        XCTAssertEqual("gho_token", token)
    }

    func testCancellingStopsPolling() async {
        let cancelling = DeviceFlowClient(
            session: server.makeSession(),
            clientId: clientId,
            baseURL: server.url("/"),
            sleep: { _ in throw CancellationError() }
        )

        await XCTAssertThrowsErrorAsync(try await cancelling.awaitAccessToken(deviceCode)) { error in
            XCTAssertTrue(error is CancellationError)
        }
        XCTAssertEqual(0, server.requestCount)
    }

    func testClientWithoutIdIsNotConfigured() {
        let unconfigured = DeviceFlowClient(session: server.makeSession(), clientId: " ")

        XCTAssertFalse(unconfigured.isConfigured)
        XCTAssertTrue(client.isConfigured)
    }
}
