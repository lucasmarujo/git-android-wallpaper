package com.lucasmarujo.gitwallpaper.ui.login

import androidx.annotation.StringRes
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lucasmarujo.gitwallpaper.R
import com.lucasmarujo.gitwallpaper.domain.WallpaperTheme
import com.lucasmarujo.gitwallpaper.ui.components.ContributionPattern
import com.lucasmarujo.gitwallpaper.ui.components.contributionPatternOf
import com.lucasmarujo.gitwallpaper.wallpaper.WallpaperPalette

object LoginTestTags {
    const val USER_CODE = "login_user_code"
    const val TOKEN_FIELD = "login_token_field"
}

private val LogoPattern = contributionPatternOf("1024310", "2413042", "0342134", "3104243")

@Composable
fun LoginScreen(
    uiState: LoginUiState,
    isDeviceFlowAvailable: Boolean,
    onStartDeviceFlow: () -> Unit,
    onOpenVerification: (userCode: String, verificationUri: String) -> Unit,
    onCancel: () -> Unit,
    onSignInWithToken: (String) -> Unit,
    onOpenPrivacyPolicy: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isBusy = uiState is LoginUiState.RequestingCode ||
        uiState is LoginUiState.SigningIn ||
        uiState is LoginUiState.AwaitingAuthorization
    Scaffold(modifier = modifier) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            ContributionPattern(
                palette = WallpaperPalette.of(
                    if (isSystemInDarkTheme()) WallpaperTheme.GITHUB_DARK else WallpaperTheme.GITHUB_LIGHT,
                ),
                pattern = LogoPattern,
                modifier = Modifier.size(width = 168.dp, height = 96.dp),
            )
            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = stringResource(R.string.login_tagline),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(8.dp))
            when (uiState) {
                is LoginUiState.AwaitingAuthorization -> DeviceCodeCard(uiState, onOpenVerification, onCancel)
                LoginUiState.RequestingCode -> ProgressMessage(R.string.login_requesting_code)
                LoginUiState.SigningIn -> ProgressMessage(R.string.login_signing_in)
                LoginUiState.Idle, is LoginUiState.Error -> {
                    if (uiState is LoginUiState.Error) {
                        Text(
                            text = stringResource(uiState.error.messageRes()),
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                        )
                    }
                    if (isDeviceFlowAvailable) {
                        Button(
                            onClick = onStartDeviceFlow,
                            modifier = Modifier.fillMaxWidth().height(52.dp),
                        ) {
                            Text(stringResource(R.string.login_sign_in_github))
                        }
                    }
                }
            }
            if (!isBusy) {
                TokenSection(
                    initiallyExpanded = !isDeviceFlowAvailable,
                    onSignInWithToken = onSignInWithToken,
                )
            }
            Text(
                text = stringResource(R.string.login_privacy),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            TextButton(onClick = onOpenPrivacyPolicy) {
                Text(stringResource(R.string.privacy_policy))
            }
        }
    }
}

@Composable
private fun DeviceCodeCard(
    state: LoginUiState.AwaitingAuthorization,
    onOpenVerification: (userCode: String, verificationUri: String) -> Unit,
    onCancel: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = stringResource(R.string.login_enter_code),
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
            )
            Text(
                text = state.userCode,
                style = MaterialTheme.typography.headlineLarge,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                letterSpacing = 4.sp,
                modifier = Modifier.testTag(LoginTestTags.USER_CODE),
            )
            Button(
                onClick = { onOpenVerification(state.userCode, state.verificationUri) },
                modifier = Modifier.fillMaxWidth().height(52.dp),
            ) {
                Text(stringResource(R.string.login_copy_and_open))
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                Text(
                    text = stringResource(R.string.login_waiting),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            TextButton(onClick = onCancel) {
                Text(stringResource(R.string.login_cancel))
            }
        }
    }
}

@Composable
private fun ProgressMessage(@StringRes messageRes: Int) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
        Text(stringResource(messageRes), style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun TokenSection(
    initiallyExpanded: Boolean,
    onSignInWithToken: (String) -> Unit,
) {
    var expanded by rememberSaveable { mutableStateOf(initiallyExpanded) }
    var token by remember { mutableStateOf("") }
    if (!expanded) {
        TextButton(onClick = { expanded = true }) {
            Text(stringResource(R.string.login_use_token))
        }
        return
    }
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        OutlinedTextField(
            value = token,
            onValueChange = { token = it },
            label = { Text(stringResource(R.string.login_token_label)) },
            supportingText = { Text(stringResource(R.string.login_token_hint)) },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { onSignInWithToken(token) }),
            modifier = Modifier.fillMaxWidth().testTag(LoginTestTags.TOKEN_FIELD),
        )
        OutlinedButton(
            onClick = { onSignInWithToken(token) },
            enabled = token.isNotBlank(),
            modifier = Modifier.fillMaxWidth().height(52.dp),
        ) {
            Text(stringResource(R.string.login_sign_in_token))
        }
    }
}

@StringRes
private fun LoginError.messageRes(): Int = when (this) {
    LoginError.NETWORK -> R.string.login_error_network
    LoginError.EXPIRED -> R.string.login_error_expired
    LoginError.ACCESS_DENIED -> R.string.login_error_access_denied
    LoginError.DEVICE_FLOW_UNAVAILABLE -> R.string.login_error_device_flow_unavailable
    LoginError.INVALID_TOKEN -> R.string.login_error_invalid_token
    LoginError.UNKNOWN -> R.string.login_error_unknown
}
