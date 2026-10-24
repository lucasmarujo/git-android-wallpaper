package com.lucasmarujo.gitwallpaper.ui.login

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lucasmarujo.gitwallpaper.AppContainer
import com.lucasmarujo.gitwallpaper.R
import com.lucasmarujo.gitwallpaper.ui.copyToClipboard
import com.lucasmarujo.gitwallpaper.ui.openUrl

@Composable
fun LoginRoute(container: AppContainer) {
    val viewModel: LoginViewModel = viewModel(factory = LoginViewModel.factory(container.authRepository))
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val clipLabel = stringResource(R.string.login_code_clip_label)
    LoginScreen(
        uiState = uiState,
        isDeviceFlowAvailable = viewModel.isDeviceFlowAvailable,
        onStartDeviceFlow = viewModel::startDeviceFlow,
        onOpenVerification = { userCode, verificationUri ->
            context.copyToClipboard(clipLabel, userCode)
            context.openUrl(verificationUri)
        },
        onCancel = viewModel::cancel,
        onSignInWithToken = viewModel::signInWithToken,
    )
}
