package com.lucasmarujo.gitwallpaper.ui

sealed interface SessionState {
    data object Loading : SessionState

    data object SignedOut : SessionState

    data class SignedIn(val login: String) : SessionState
}
