package com.lucasmarujo.gitwallpaper.ui.login

enum class LoginError {
    NETWORK,
    EXPIRED,
    ACCESS_DENIED,
    DEVICE_FLOW_UNAVAILABLE,
    INVALID_TOKEN,
    UNKNOWN,
}

sealed interface LoginUiState {
    data object Idle : LoginUiState

    data object RequestingCode : LoginUiState

    data class AwaitingAuthorization(val userCode: String, val verificationUri: String) : LoginUiState

    data object SigningIn : LoginUiState

    data class Error(val error: LoginError) : LoginUiState
}
