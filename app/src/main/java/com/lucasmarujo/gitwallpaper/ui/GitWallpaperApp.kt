package com.lucasmarujo.gitwallpaper.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lucasmarujo.gitwallpaper.AppContainer
import com.lucasmarujo.gitwallpaper.ui.home.HomeRoute
import com.lucasmarujo.gitwallpaper.ui.login.LoginRoute
import kotlinx.coroutines.flow.map

@Composable
fun GitWallpaperApp(container: AppContainer) {
    val sessionFlow = remember(container) {
        container.sessionRepository.session.map { session ->
            if (session == null) SessionState.SignedOut else SessionState.SignedIn(session.login)
        }
    }
    val sessionState by sessionFlow.collectAsStateWithLifecycle(initialValue = SessionState.Loading)
    when (val state = sessionState) {
        SessionState.Loading -> Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background))
        SessionState.SignedOut -> LoginRoute(container)
        is SessionState.SignedIn -> HomeRoute(container, state.login)
    }
}
