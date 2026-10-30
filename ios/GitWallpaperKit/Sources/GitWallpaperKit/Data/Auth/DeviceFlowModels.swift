public struct DeviceCode: Equatable, Sendable {
    public var deviceCode: String
    public var userCode: String
    public var verificationUri: String
    public var expiresInSeconds: Int
    public var intervalSeconds: Int

    public init(deviceCode: String, userCode: String, verificationUri: String, expiresInSeconds: Int, intervalSeconds: Int) {
        self.deviceCode = deviceCode
        self.userCode = userCode
        self.verificationUri = verificationUri
        self.expiresInSeconds = expiresInSeconds
        self.intervalSeconds = intervalSeconds
    }
}

public enum DeviceFlowError: Error, Equatable, Sendable {
    case expired
    case accessDenied
    case deviceFlowDisabled
    case invalidClient
    case unknown
}

struct DeviceCodeResponse: Decodable {
    let deviceCode: String?
    let userCode: String?
    let verificationUri: String?
    let expiresIn: Int?
    let interval: Int?
    let error: String?

    private enum CodingKeys: String, CodingKey {
        case deviceCode = "device_code"
        case userCode = "user_code"
        case verificationUri = "verification_uri"
        case expiresIn = "expires_in"
        case interval
        case error
    }
}

struct AccessTokenResponse: Decodable {
    let accessToken: String?
    let interval: Int?
    let error: String?

    private enum CodingKeys: String, CodingKey {
        case accessToken = "access_token"
        case interval
        case error
    }
}
