import Foundation

public final class DeviceFlowClient: Sendable {
    public typealias Sleeper = @Sendable (_ seconds: Int) async throws -> Void

    public static let requiredScope = "read:user"
    public static let defaultBaseURL = URL(string: "https://github.com/")!

    private let session: URLSession
    private let clientId: String
    private let baseURL: URL
    private let sleeper: Sleeper

    public init(
        session: URLSession,
        clientId: String,
        baseURL: URL = DeviceFlowClient.defaultBaseURL,
        sleep: @escaping Sleeper = { seconds in try await Task.sleep(nanoseconds: UInt64(seconds) * 1_000_000_000) }
    ) {
        self.session = session
        self.clientId = clientId
        self.baseURL = baseURL
        self.sleeper = sleep
    }

    public var isConfigured: Bool {
        !clientId.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty
    }

    public func requestDeviceCode() async throws -> DeviceCode {
        let data = try await post(path: Self.deviceCodePath, form: [
            (Self.clientIdField, clientId),
            (Self.scopeField, Self.requiredScope),
        ])
        let response = try decode(DeviceCodeResponse.self, from: data)
        if let error = response.error { throw Self.deviceFlowError(error) }
        guard let deviceCode = response.deviceCode, let userCode = response.userCode else {
            throw DeviceFlowError.unknown
        }
        return DeviceCode(
            deviceCode: deviceCode,
            userCode: userCode,
            verificationUri: response.verificationUri ?? Self.defaultVerificationUri,
            expiresInSeconds: response.expiresIn ?? Self.defaultExpiresInSeconds,
            intervalSeconds: response.interval ?? Self.minIntervalSeconds
        )
    }

    public func awaitAccessToken(_ deviceCode: DeviceCode) async throws -> String {
        let form = [
            (Self.clientIdField, clientId),
            (Self.deviceCodeField, deviceCode.deviceCode),
            (Self.grantTypeField, Self.deviceCodeGrantType),
        ]
        var intervalSeconds = max(deviceCode.intervalSeconds, Self.minIntervalSeconds)
        var elapsedSeconds = 0
        while elapsedSeconds < deviceCode.expiresInSeconds {
            try await sleeper(intervalSeconds)
            elapsedSeconds += intervalSeconds
            let response: AccessTokenResponse
            do {
                response = try decode(AccessTokenResponse.self, from: try await post(path: Self.accessTokenPath, form: form))
            } catch GitHubError.network(_) {
                continue
            }
            if let accessToken = response.accessToken { return accessToken }
            switch response.error {
            case .some(Self.authorizationPending):
                continue
            case .some(Self.slowDown):
                intervalSeconds = response.interval ?? intervalSeconds + Self.slowDownIncrementSeconds
            default:
                throw Self.deviceFlowError(response.error)
            }
        }
        throw DeviceFlowError.expired
    }

    private func post(path: String, form: [(String, String)]) async throws -> Data {
        var request = URLRequest(url: baseURL.appendingPathComponent(path))
        request.httpMethod = GitHubHTTP.postMethod
        request.setValue(GitHubHTTP.jsonContentType, forHTTPHeaderField: GitHubHTTP.acceptHeader)
        request.setValue(GitHubHTTP.formContentType, forHTTPHeaderField: GitHubHTTP.contentTypeHeader)
        request.httpBody = Data(Self.formEncoded(form).utf8)
        let (data, response) = try await GitHubHTTP.send(request, using: session)
        guard GitHubHTTP.successStatuses.contains(response.statusCode) else { throw GitHubError.http(response.statusCode) }
        return data
    }

    private func decode<T: Decodable>(_ type: T.Type, from data: Data) throws -> T {
        do {
            return try JSONDecoder().decode(type, from: data)
        } catch {
            throw DeviceFlowError.unknown
        }
    }

    private static func formEncoded(_ form: [(String, String)]) -> String {
        form.map { "\(percentEncoded($0.0))=\(percentEncoded($0.1))" }.joined(separator: "&")
    }

    private static func percentEncoded(_ value: String) -> String {
        value.addingPercentEncoding(withAllowedCharacters: formAllowedCharacters) ?? value
    }

    private static func deviceFlowError(_ code: String?) -> DeviceFlowError {
        switch code {
        case .some(expiredToken): .expired
        case .some(accessDenied): .accessDenied
        case .some(deviceFlowDisabled): .deviceFlowDisabled
        case .some(incorrectClientCredentials): .invalidClient
        default: .unknown
        }
    }

    private static let formAllowedCharacters = CharacterSet(charactersIn: "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789-._~")
    private static let defaultVerificationUri = "https://github.com/login/device"
    private static let deviceCodePath = "login/device/code"
    private static let accessTokenPath = "login/oauth/access_token"
    private static let clientIdField = "client_id"
    private static let scopeField = "scope"
    private static let deviceCodeField = "device_code"
    private static let grantTypeField = "grant_type"
    private static let deviceCodeGrantType = "urn:ietf:params:oauth:grant-type:device_code"
    private static let authorizationPending = "authorization_pending"
    private static let slowDown = "slow_down"
    private static let expiredToken = "expired_token"
    private static let accessDenied = "access_denied"
    private static let deviceFlowDisabled = "device_flow_disabled"
    private static let incorrectClientCredentials = "incorrect_client_credentials"
    private static let minIntervalSeconds = 5
    private static let slowDownIncrementSeconds = 5
    private static let defaultExpiresInSeconds = 900
}
