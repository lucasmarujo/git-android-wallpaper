import GitWallpaperKit
import SwiftUI
import UIKit

struct LoginView: View {
    @StateObject private var viewModel: LoginViewModel
    @Environment(\.colorScheme) private var colorScheme
    @Environment(\.openURL) private var openURL

    init(viewModel: @autoclosure @escaping () -> LoginViewModel) {
        _viewModel = StateObject(wrappedValue: viewModel())
    }

    var body: some View {
        ScrollView {
            VStack(spacing: 16) {
                ContributionPattern(
                    palette: WallpaperPalette.of(colorScheme == .dark ? .githubDark : .githubLight),
                    pattern: Self.logoPattern
                )
                .frame(width: 168, height: 96)
                Text(verbatim: Self.appName)
                    .font(.largeTitle.bold())
                Text("login_tagline")
                    .font(.body)
                    .foregroundStyle(.secondary)
                    .multilineTextAlignment(.center)
                Spacer().frame(height: 8)
                content
                if !viewModel.uiState.isBusy {
                    TokenSection(initiallyExpanded: !viewModel.isDeviceFlowAvailable) { token in
                        viewModel.signInWithToken(token)
                    }
                }
                Text("login_privacy")
                    .font(.footnote)
                    .foregroundStyle(.secondary)
                    .multilineTextAlignment(.center)
                Button("privacy_policy") { openURL(AppLinks.privacyPolicy) }
                    .font(.footnote)
            }
            .padding(.horizontal, 24)
            .padding(.vertical, 40)
        }
    }

    @ViewBuilder
    private var content: some View {
        switch viewModel.uiState {
        case .awaitingAuthorization(let userCode, let verificationUri):
            DeviceCodeCard(
                userCode: userCode,
                verificationUri: verificationUri,
                onOpenVerification: {
                    UIPasteboard.general.string = userCode
                    openVerification(verificationUri)
                },
                onOpenLink: { openVerification(verificationUri) },
                onCopyLink: { UIPasteboard.general.string = verificationUri },
                onCancel: viewModel.cancel
            )
        case .requestingCode:
            ProgressMessage(text: "login_requesting_code")
        case .signingIn:
            ProgressMessage(text: "login_signing_in")
        case .idle, .error:
            if case .error(let error) = viewModel.uiState {
                Text(error.messageKey)
                    .font(.callout)
                    .foregroundStyle(.red)
                    .multilineTextAlignment(.center)
                    .accessibilityIdentifier(LoginAccessibility.error)
            }
            if viewModel.isDeviceFlowAvailable {
                Button {
                    viewModel.startDeviceFlow()
                } label: {
                    Text("login_sign_in_github").frame(maxWidth: .infinity, minHeight: 36)
                }
                .buttonStyle(.borderedProminent)
                .controlSize(.large)
            }
        }
    }

    private func openVerification(_ verificationUri: String) {
        guard let url = URL(string: verificationUri) else { return }
        openURL(url)
    }

    private static let appName = "Git Wallpaper"
    private static let logoPattern = ContributionPattern.of("1024310", "2413042", "0342134", "3104243")
}

enum LoginAccessibility {
    static let userCode = "login_user_code"
    static let verificationUri = "login_verification_uri"
    static let tokenField = "login_token_field"
    static let error = "login_error"
}

private struct DeviceCodeCard: View {
    let userCode: String
    let verificationUri: String
    let onOpenVerification: () -> Void
    let onOpenLink: () -> Void
    let onCopyLink: () -> Void
    let onCancel: () -> Void
    @State private var linkCopied = false

    var body: some View {
        VStack(spacing: 16) {
            Text("login_enter_code")
                .font(.callout)
                .multilineTextAlignment(.center)
            Text(verbatim: verificationUri.replacingOccurrences(of: Self.httpsPrefix, with: ""))
                .font(.body.monospaced())
                .foregroundStyle(.tint)
                .multilineTextAlignment(.center)
                .accessibilityIdentifier(LoginAccessibility.verificationUri)
            HStack(spacing: 12) {
                Button(action: onOpenLink) {
                    Text("login_open_link").frame(maxWidth: .infinity)
                }
                Button {
                    onCopyLink()
                    linkCopied = true
                } label: {
                    Text(linkCopied ? LocalizedStringKey("login_link_copied") : LocalizedStringKey("login_copy_link"))
                        .frame(maxWidth: .infinity)
                }
            }
            .buttonStyle(.bordered)
            Text(verbatim: userCode)
                .font(.system(size: 34, weight: .bold, design: .monospaced))
                .tracking(4)
                .textSelection(.enabled)
                .accessibilityIdentifier(LoginAccessibility.userCode)
            Button(action: onOpenVerification) {
                Text("login_copy_and_open").frame(maxWidth: .infinity, minHeight: 36)
            }
            .buttonStyle(.borderedProminent)
            .controlSize(.large)
            HStack(spacing: 8) {
                ProgressView().controlSize(.small)
                Text("login_waiting")
                    .font(.footnote)
                    .foregroundStyle(.secondary)
            }
            Button("login_cancel", action: onCancel)
        }
        .padding(20)
        .frame(maxWidth: .infinity)
        .background(RoundedRectangle(cornerRadius: 16).fill(Color(.secondarySystemBackground)))
        .onChange(of: verificationUri) { _, _ in linkCopied = false }
    }

    private static let httpsPrefix = "https://"
}

private struct ProgressMessage: View {
    let text: LocalizedStringKey

    var body: some View {
        HStack(spacing: 12) {
            ProgressView()
            Text(text).font(.callout)
        }
    }
}

private struct TokenSection: View {
    let initiallyExpanded: Bool
    let onSignIn: (String) -> Void
    @State private var expanded: Bool?
    @State private var token = ""

    var body: some View {
        if expanded ?? initiallyExpanded {
            VStack(alignment: .leading, spacing: 12) {
                SecureField("login_token_label", text: $token)
                    .textContentType(.password)
                    .textInputAutocapitalization(.never)
                    .autocorrectionDisabled()
                    .submitLabel(.done)
                    .onSubmit { onSignIn(token) }
                    .padding(12)
                    .background(RoundedRectangle(cornerRadius: 10).stroke(Color(.separator)))
                    .accessibilityIdentifier(LoginAccessibility.tokenField)
                Text("login_token_hint")
                    .font(.caption)
                    .foregroundStyle(.secondary)
                Button {
                    onSignIn(token)
                } label: {
                    Text("login_sign_in_token").frame(maxWidth: .infinity, minHeight: 36)
                }
                .buttonStyle(.bordered)
                .controlSize(.large)
                .disabled(token.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty)
            }
        } else {
            Button("login_use_token") { expanded = true }
        }
    }
}

private extension LoginError {
    var messageKey: LocalizedStringKey {
        switch self {
        case .network: "login_error_network"
        case .expired: "login_error_expired"
        case .accessDenied: "login_error_access_denied"
        case .deviceFlowUnavailable: "login_error_device_flow_unavailable"
        case .invalidToken: "login_error_invalid_token"
        case .unknown: "login_error_unknown"
        }
    }
}
