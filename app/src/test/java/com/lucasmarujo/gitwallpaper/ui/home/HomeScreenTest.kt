package com.lucasmarujo.gitwallpaper.ui.home

import androidx.compose.material3.SnackbarHostState
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.lucasmarujo.gitwallpaper.domain.ContributionStats
import com.lucasmarujo.gitwallpaper.domain.WallpaperSettings
import com.lucasmarujo.gitwallpaper.domain.WallpaperTarget
import com.lucasmarujo.gitwallpaper.domain.WallpaperTheme
import com.lucasmarujo.gitwallpaper.ui.theme.GitWallpaperTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HomeScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private var settings = WallpaperSettings()
    private var applyClicks = 0
    private var signOuts = 0
    private var batterySettingsOpens = 0
    private var privacyPolicyOpens = 0

    @Test
    fun `apply is disabled until contributions are loaded`() {
        setContent(HomeUiState(login = "octocat", hasCalendar = false))

        composeRule.onNodeWithTag(HomeTestTags.APPLY_BUTTON).assertIsNotEnabled()
    }

    @Test
    fun `apply button triggers the apply action`() {
        setContent(loadedState())

        composeRule.onNodeWithTag(HomeTestTags.APPLY_BUTTON).assertIsEnabled().performClick()

        assertEquals(1, applyClicks)
    }

    @Test
    fun `stats and never applied status are shown`() {
        setContent(loadedState())

        composeRule.onNodeWithText("1,234").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("5 days").assertIsDisplayed()
        composeRule.onNodeWithText("1 day").assertIsDisplayed()
        composeRule.onNodeWithTag(HomeTestTags.LAST_APPLIED).performScrollTo()
        composeRule.onNodeWithText("Not applied yet").assertIsDisplayed()
    }

    @Test
    fun `choosing options updates the settings`() {
        setContent(loadedState())

        composeRule.onNodeWithText("Both").performScrollTo().performClick()
        composeRule.onNodeWithText("Period").performScrollTo()
        composeRule.onNodeWithText("Ocean").performScrollTo().performClick()

        assertEquals(WallpaperTarget.BOTH, settings.target)
        assertEquals(WallpaperTheme.OCEAN, settings.theme)
    }

    @Test
    fun `sign out asks for confirmation`() {
        setContent(loadedState())

        composeRule.onNodeWithContentDescription("Sign out").performClick()
        composeRule.onNodeWithText("Sign out?").assertIsDisplayed()
        composeRule.onNodeWithText("Sign out").performClick()

        assertEquals(1, signOuts)
    }

    @Test
    fun `battery hint opens the battery settings`() {
        setContent(loadedState(), showBatteryHint = true)

        composeRule.onNodeWithText("Open battery settings").performScrollTo().performClick()

        assertEquals(1, batterySettingsOpens)
    }

    @Test
    fun `battery hint is hidden when not needed`() {
        setContent(loadedState())

        composeRule.onNodeWithText("Open battery settings").assertDoesNotExist()
    }

    @Test
    fun `privacy policy link opens the policy`() {
        setContent(loadedState())

        composeRule.onNodeWithText("Privacy policy").performScrollTo().performClick()

        assertEquals(1, privacyPolicyOpens)
    }

    private fun loadedState() = HomeUiState(
        login = "octocat",
        hasCalendar = true,
        isLoadingCalendar = false,
        stats = ContributionStats(totalContributions = 1234, currentStreak = 5, longestStreak = 1),
    )

    private fun setContent(state: HomeUiState, showBatteryHint: Boolean = false) {
        composeRule.setContent {
            GitWallpaperTheme {
                HomeScreen(
                    uiState = state,
                    snackbarHostState = SnackbarHostState(),
                    showBatteryHint = showBatteryHint,
                    onSettingsChange = { transform -> settings = transform(settings) },
                    onApply = { applyClicks++ },
                    onRefresh = {},
                    onSignOut = { signOuts++ },
                    onOpenBatterySettings = { batterySettingsOpens++ },
                    onOpenPrivacyPolicy = { privacyPolicyOpens++ },
                )
            }
        }
    }
}
