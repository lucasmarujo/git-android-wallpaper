package com.lucasmarujo.gitwallpaper.ui.login

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.lucasmarujo.gitwallpaper.ui.theme.GitWallpaperTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LoginScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private var deviceFlowStarts = 0
    private var openedVerification: Pair<String, String>? = null
    private var cancels = 0
    private val openedLinks = mutableListOf<String>()
    private val copiedLinks = mutableListOf<String>()
    private val submittedTokens = mutableListOf<String>()
    private var privacyPolicyOpens = 0

    @Test
    fun `idle state offers github sign in`() {
        setContent(LoginUiState.Idle)

        composeRule.onNodeWithText("Sign in with GitHub").performClick()

        assertEquals(1, deviceFlowStarts)
        composeRule.onNodeWithText("Use a personal access token").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun `awaiting authorization shows the code and opens github with it`() {
        setContent(LoginUiState.AwaitingAuthorization("ABCD-1234", "https://github.com/login/device"))

        composeRule.onNodeWithTag(LoginTestTags.USER_CODE).performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("ABCD-1234").assertIsDisplayed()
        composeRule.onNodeWithText("Copy code and open GitHub").performScrollTo().performClick()
        composeRule.onNodeWithText("Cancel").performScrollTo().performClick()

        assertEquals("ABCD-1234" to "https://github.com/login/device", openedVerification)
        assertEquals(1, cancels)
    }

    @Test
    fun `awaiting authorization shows the link with open and copy buttons`() {
        setContent(LoginUiState.AwaitingAuthorization("ABCD-1234", VERIFICATION_URI))

        composeRule.onNodeWithTag(LoginTestTags.VERIFICATION_URI).assertTextEquals("github.com/login/device")
        composeRule.onNodeWithText("Open link").performScrollTo().performClick()
        composeRule.onNodeWithText("Copy link").performScrollTo().performClick()

        assertEquals(listOf(VERIFICATION_URI), openedLinks)
        assertEquals(listOf(VERIFICATION_URI), copiedLinks)
        assertNull(openedVerification)
    }

    @Test
    fun `copying the link confirms it on the button`() {
        setContent(LoginUiState.AwaitingAuthorization("ABCD-1234", VERIFICATION_URI))

        composeRule.onNodeWithText("Copy link").performScrollTo().performClick()

        composeRule.onNodeWithText("Link copied").assertIsDisplayed()
        composeRule.onNodeWithText("Copy link").assertDoesNotExist()
    }

    @Test
    fun `errors are shown with a retry button`() {
        setContent(LoginUiState.Error(LoginError.EXPIRED))

        composeRule.onNodeWithText("The code expired. Start again.").assertIsDisplayed()
        composeRule.onNodeWithText("Sign in with GitHub").assertIsDisplayed()
    }

    @Test
    fun `personal access tokens can be submitted`() {
        setContent(LoginUiState.Idle)

        composeRule.onNodeWithText("Use a personal access token").performScrollTo().performClick()
        composeRule.onNodeWithTag(LoginTestTags.TOKEN_FIELD).performTextInput("ghp_token")
        composeRule.onNodeWithText("Sign in with token").performScrollTo().performClick()

        assertEquals(listOf("ghp_token"), submittedTokens)
    }

    @Test
    fun `without device flow the token form is shown directly`() {
        setContent(LoginUiState.Idle, isDeviceFlowAvailable = false)

        composeRule.onNodeWithTag(LoginTestTags.TOKEN_FIELD).performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Sign in with GitHub").assertDoesNotExist()
    }

    @Test
    fun `privacy policy link is available before signing in`() {
        setContent(LoginUiState.Idle)

        composeRule.onNodeWithText("Privacy policy").performScrollTo().performClick()

        assertEquals(1, privacyPolicyOpens)
    }

    private fun setContent(state: LoginUiState, isDeviceFlowAvailable: Boolean = true) {
        composeRule.setContent {
            GitWallpaperTheme {
                LoginScreen(
                    uiState = state,
                    isDeviceFlowAvailable = isDeviceFlowAvailable,
                    onStartDeviceFlow = { deviceFlowStarts++ },
                    onOpenVerification = { code, uri -> openedVerification = code to uri },
                    onOpenLink = { openedLinks += it },
                    onCopyLink = { copiedLinks += it },
                    onCancel = { cancels++ },
                    onSignInWithToken = { submittedTokens += it },
                    onOpenPrivacyPolicy = { privacyPolicyOpens++ },
                )
            }
        }
    }

    private companion object {
        const val VERIFICATION_URI = "https://github.com/login/device"
    }
}
