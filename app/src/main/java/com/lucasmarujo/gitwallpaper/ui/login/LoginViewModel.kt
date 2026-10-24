package com.lucasmarujo.gitwallpaper.ui.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.lucasmarujo.gitwallpaper.data.auth.AuthRepository
import com.lucasmarujo.gitwallpaper.data.auth.DeviceFlowError
import com.lucasmarujo.gitwallpaper.data.auth.DeviceFlowException
import com.lucasmarujo.gitwallpaper.data.github.GitHubException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class LoginViewModel(private val authRepository: AuthRepository) : ViewModel() {

    private val _uiState = MutableStateFlow<LoginUiState>(LoginUiState.Idle)
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    val isDeviceFlowAvailable: Boolean = authRepository.isDeviceFlowAvailable

    private var signInJob: Job? = null

    fun startDeviceFlow() {
        launchSignIn {
            _uiState.value = LoginUiState.RequestingCode
            val deviceCode = authRepository.requestDeviceCode()
            _uiState.value = LoginUiState.AwaitingAuthorization(deviceCode.userCode, deviceCode.verificationUri)
            val token = authRepository.awaitDeviceAuthorization(deviceCode)
            _uiState.value = LoginUiState.SigningIn
            authRepository.signIn(token)
        }
    }

    fun signInWithToken(token: String) {
        if (token.isBlank()) return
        launchSignIn {
            _uiState.value = LoginUiState.SigningIn
            authRepository.signIn(token)
        }
    }

    fun cancel() {
        signInJob?.cancel()
        signInJob = null
        _uiState.value = LoginUiState.Idle
    }

    private fun launchSignIn(block: suspend () -> Unit) {
        signInJob?.cancel()
        signInJob = viewModelScope.launch {
            _uiState.value = try {
                block()
                LoginUiState.Idle
            } catch (exception: DeviceFlowException) {
                LoginUiState.Error(exception.error.toLoginError())
            } catch (exception: GitHubException) {
                LoginUiState.Error(exception.toLoginError())
            }
        }
    }

    private fun DeviceFlowError.toLoginError(): LoginError = when (this) {
        DeviceFlowError.EXPIRED -> LoginError.EXPIRED
        DeviceFlowError.ACCESS_DENIED -> LoginError.ACCESS_DENIED
        DeviceFlowError.DEVICE_FLOW_DISABLED, DeviceFlowError.INVALID_CLIENT -> LoginError.DEVICE_FLOW_UNAVAILABLE
        DeviceFlowError.UNKNOWN -> LoginError.UNKNOWN
    }

    private fun GitHubException.toLoginError(): LoginError = when (this) {
        is GitHubException.Network -> LoginError.NETWORK
        is GitHubException.Unauthorized -> LoginError.INVALID_TOKEN
        is GitHubException.Http, is GitHubException.Api -> LoginError.UNKNOWN
    }

    companion object {
        fun factory(authRepository: AuthRepository): ViewModelProvider.Factory = viewModelFactory {
            initializer { LoginViewModel(authRepository) }
        }
    }
}
