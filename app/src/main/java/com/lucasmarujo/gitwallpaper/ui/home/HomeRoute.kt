package com.lucasmarujo.gitwallpaper.ui.home

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lucasmarujo.gitwallpaper.AppContainer
import com.lucasmarujo.gitwallpaper.R
import com.lucasmarujo.gitwallpaper.ui.canPinContributionWidget
import com.lucasmarujo.gitwallpaper.ui.isIgnoringBatteryOptimizations
import com.lucasmarujo.gitwallpaper.ui.openBatteryOptimizationSettings
import com.lucasmarujo.gitwallpaper.ui.openPrivacyPolicy
import com.lucasmarujo.gitwallpaper.ui.requestPinContributionWidget
import kotlinx.coroutines.launch

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
    val scope = rememberCoroutineScope()
    val canPinWidget = remember(context) { context.canPinContributionWidget() }
    var isIgnoringBatteryOptimizations by remember { mutableStateOf(context.isIgnoringBatteryOptimizations()) }

    LifecycleResumeEffect(context) {
        isIgnoringBatteryOptimizations = context.isIgnoringBatteryOptimizations()
        onPauseOrDispose { }
    }
    LaunchedEffect(viewModel, resources) {
        viewModel.messages.collect { message ->
            snackbarHostState.showSnackbar(resources.getString(message.textRes()))
        }
    }

    if (uiState.isAdjusting) {
        WallpaperAdjustScreen(
            uiState = uiState,
            onApply = viewModel::applyWallpaper,
            onCancel = viewModel::cancelAdjusting,
        )
    } else {
        HomeScreen(
            uiState = uiState,
            snackbarHostState = snackbarHostState,
            showBatteryHint = uiState.settings.autoUpdate && !isIgnoringBatteryOptimizations,
            canPinWidget = canPinWidget,
            onSettingsChange = viewModel::updateSettings,
            onApply = viewModel::startAdjusting,
            onRefresh = viewModel::refresh,
            onSignOut = viewModel::signOut,
            onAddWidget = {
                if (!context.requestPinContributionWidget()) {
                    scope.launch { snackbarHostState.showSnackbar(resources.getString(R.string.message_widget_unavailable)) }
                }
            },
            onOpenBatterySettings = context::openBatteryOptimizationSettings,
            onOpenPrivacyPolicy = context::openPrivacyPolicy,
        )
    }
}

private const val HOME_VIEW_MODEL_KEY_PREFIX = "home-"
