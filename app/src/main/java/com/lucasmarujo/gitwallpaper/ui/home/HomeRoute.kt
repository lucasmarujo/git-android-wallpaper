package com.lucasmarujo.gitwallpaper.ui.home

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lucasmarujo.gitwallpaper.AppContainer
import com.lucasmarujo.gitwallpaper.ui.isIgnoringBatteryOptimizations
import com.lucasmarujo.gitwallpaper.ui.requestIgnoreBatteryOptimizations

@Composable
fun HomeRoute(container: AppContainer, login: String) {
    val viewModel: HomeViewModel = viewModel(
        key = HOME_VIEW_MODEL_KEY_PREFIX + login,
        factory = HomeViewModel.factory(container, login),
    )
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val resources = LocalResources.current
    var isIgnoringBatteryOptimizations by remember { mutableStateOf(context.isIgnoringBatteryOptimizations()) }
    val needsBatteryExemption by rememberUpdatedState(uiState.settings.autoUpdate && !isIgnoringBatteryOptimizations)

    LifecycleResumeEffect(context) {
        isIgnoringBatteryOptimizations = context.isIgnoringBatteryOptimizations()
        onPauseOrDispose { }
    }
    LaunchedEffect(viewModel, resources) {
        viewModel.messages.collect { message ->
            if (message == HomeMessage.APPLIED && needsBatteryExemption) {
                context.requestIgnoreBatteryOptimizations()
            }
            snackbarHostState.showSnackbar(resources.getString(message.textRes()))
        }
    }

    HomeScreen(
        uiState = uiState,
        snackbarHostState = snackbarHostState,
        showBatteryHint = needsBatteryExemption,
        onSettingsChange = viewModel::updateSettings,
        onApply = viewModel::applyWallpaper,
        onRefresh = viewModel::refresh,
        onSignOut = viewModel::signOut,
        onOpenBatterySettings = context::requestIgnoreBatteryOptimizations,
    )
}

private const val HOME_VIEW_MODEL_KEY_PREFIX = "home-"
