import Foundation
import GitWallpaperKit

enum LoginError: Equatable {
    case network
    case expired
    case accessDenied
    case deviceFlowUnavailable
    case invalidToken
    case unknown
}

enum LoginUiState: Equatable {
    case idle
    case requestingCode
    case awaitingAuthorization(userCode: String, verificationUri: String)
    case signingIn
    case error(LoginError)

    var isBusy: Bool {
        switch self {
        case .requestingCode, .awaitingAuthorization, .signingIn: true
        case .idle, .error: false
        }
    }
}

@MainActor
final class LoginViewModel: ObservableObject {
    @Published private(set) var uiState: LoginUiState = .idle

    let isDeviceFlowAvailable: Bool

    private let authRepository: AuthRepository
    private var signInTask: Task<Void, Never>?

    init(authRepository: AuthRepository) {
        self.authRepository = authRepository
        isDeviceFlowAvailable = authRepository.isDeviceFlowAvailable
    }

    @discardableResult
    func startDeviceFlow() -> Task<Void, Never> {
        launchSignIn { [authRepository] in
            self.uiState = .requestingCode
            let deviceCode = try await authRepository.requestDeviceCode()
            self.uiState = .awaitingAuthorization(userCode: deviceCode.userCode, verificationUri: deviceCode.verificationUri)
            let token = try await authRepository.awaitDeviceAuthorization(deviceCode)
            self.uiState = .signingIn
            try await authRepository.signIn(token: token)
        }
    }

    @discardableResult
    func signInWithToken(_ token: String) -> Task<Void, Never>? {
        guard !token.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty else { return nil }
        return launchSignIn { [authRepository] in
            self.uiState = .signingIn
            try await authRepository.signIn(token: token)
        }
    }

    func cancel() {
        signInTask?.cancel()
        signInTask = nil
        uiState = .idle
    }

    private func launchSignIn(_ block: @escaping @MainActor () async throws -> Void) -> Task<Void, Never> {
        signInTask?.cancel()
        let task = Task { @MainActor in
            do {
                try await block()
                guard !Task.isCancelled else { return }
                uiState = .idle
            } catch {
                guard !Task.isCancelled, !(error is CancellationError) else { return }
                uiState = .error(Self.loginError(for: error))
            }
        }
        signInTask = task
        return task
    }

    private static func loginError(for error: Error) -> LoginError {
        if let deviceFlowError = error as? DeviceFlowError {
            return loginError(for: deviceFlowError)
        }
        if let gitHubError = error as? GitHubError {
            return loginError(for: gitHubError)
        }
        return .unknown
    }

    private static func loginError(for error: DeviceFlowError) -> LoginError {
        switch error {
        case .expired: .expired
        case .accessDenied: .accessDenied
        case .deviceFlowDisabled, .invalidClient: .deviceFlowUnavailable
        case .unknown: .unknown
        }
    }

    private static func loginError(for error: GitHubError) -> LoginError {
        switch error {
        case .network: .network
        case .unauthorized: .invalidToken
        case .http, .api: .unknown
        }
    }
}
