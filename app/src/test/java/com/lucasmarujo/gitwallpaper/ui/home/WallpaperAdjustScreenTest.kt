package com.lucasmarujo.gitwallpaper.ui.home

import androidx.activity.ComponentActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.pinch
import androidx.compose.ui.test.swipeUp
import androidx.core.view.WindowCompat
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.lucasmarujo.gitwallpaper.domain.WallpaperFraming
import com.lucasmarujo.gitwallpaper.domain.WallpaperSettings
import com.lucasmarujo.gitwallpaper.domain.WallpaperTarget
import com.lucasmarujo.gitwallpaper.domain.WallpaperTheme
import com.lucasmarujo.gitwallpaper.ui.theme.GitWallpaperTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class WallpaperAdjustScreenTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val applied = mutableListOf<WallpaperFraming>()
    private var cancels = 0

    @Test
    fun `apply keeps the saved framing when nothing is changed`() {
        val saved = WallpaperFraming(zoom = 1.3f, offsetY = 0.1f)
        setContent(state(framing = saved))

        composeRule.onNodeWithTag(WallpaperAdjustTestTags.APPLY_BUTTON).performClick()

        assertEquals(listOf(saved), applied)
    }

    @Test
    fun `dragging up moves the graph up`() {
        setContent(state())

        composeRule.onNodeWithTag(WallpaperAdjustTestTags.FRAME).performTouchInput { swipeUp(startY = centerY, endY = centerY - height / 4f) }
        composeRule.onNodeWithTag(WallpaperAdjustTestTags.APPLY_BUTTON).performClick()

        val framing = applied.single()
        assertTrue(framing.offsetY < 0f)
        assertEquals(WallpaperFraming.DEFAULT_ZOOM, framing.zoom, ZOOM_DELTA)
    }

    @Test
    fun `pinching out zooms the graph in`() {
        setContent(state())

        composeRule.onNodeWithTag(WallpaperAdjustTestTags.FRAME).performTouchInput {
            pinch(
                start0 = center - Offset(PINCH_START, 0f),
                end0 = center - Offset(PINCH_END, 0f),
                start1 = center + Offset(PINCH_START, 0f),
                end1 = center + Offset(PINCH_END, 0f),
            )
        }
        composeRule.onNodeWithTag(WallpaperAdjustTestTags.APPLY_BUTTON).performClick()

        assertTrue(applied.single().zoom > WallpaperFraming.DEFAULT_ZOOM)
    }

    @Test
    fun `reset restores the default framing`() {
        setContent(state(framing = WallpaperFraming(zoom = 2f, offsetX = 0.2f)))

        composeRule.onNodeWithText("Reset").assertIsEnabled().performClick()
        composeRule.onNodeWithText("Reset").assertIsNotEnabled()
        composeRule.onNodeWithTag(WallpaperAdjustTestTags.APPLY_BUTTON).performClick()

        assertEquals(listOf(WallpaperFraming()), applied)
    }

    @Test
    fun `cancel leaves without applying`() {
        setContent(state())

        composeRule.onNodeWithText("Cancel").performClick()

        assertEquals(1, cancels)
        assertTrue(applied.isEmpty())
    }

    @Test
    fun `controls are disabled while applying`() {
        setContent(state(isApplying = true))

        composeRule.onNodeWithTag(WallpaperAdjustTestTags.APPLY_BUTTON).assertIsNotEnabled()
        composeRule.onNodeWithText("Cancel").assertIsNotEnabled()
        composeRule.onNodeWithText("Applying…").assertExists()
    }

    @Test
    fun `lock screen clock is shown only when the lock screen is a target`() {
        setContent(state(target = WallpaperTarget.HOME_SCREEN))

        composeRule.onNodeWithTag(WallpaperAdjustTestTags.LOCK_SCREEN_CLOCK).assertDoesNotExist()
    }

    @Test
    fun `lock screen clock is shown when applying to both screens`() {
        setContent(state(target = WallpaperTarget.BOTH))

        composeRule.onNodeWithTag(WallpaperAdjustTestTags.LOCK_SCREEN_CLOCK).assertExists()
    }

    @Test
    fun `status bar icons follow the wallpaper brightness and are restored on exit`() {
        val window = composeRule.activity.window
        val controller = WindowCompat.getInsetsController(window, window.decorView)
        composeRule.runOnUiThread { controller.isAppearanceLightStatusBars = true }
        var isAdjusting by mutableStateOf(true)
        composeRule.setContent {
            GitWallpaperTheme {
                if (isAdjusting) WallpaperAdjustScreen(uiState = state(), onApply = {}, onCancel = {})
            }
        }

        composeRule.waitForIdle()
        assertFalse(controller.isAppearanceLightStatusBars)
        isAdjusting = false
        composeRule.waitForIdle()
        assertTrue(controller.isAppearanceLightStatusBars)
    }

    @Test
    fun `light wallpapers keep dark status bar icons`() {
        val window = composeRule.activity.window
        val controller = WindowCompat.getInsetsController(window, window.decorView)

        setContent(state(theme = WallpaperTheme.GITHUB_LIGHT))

        composeRule.waitForIdle()
        assertTrue(controller.isAppearanceLightStatusBars)
    }

    private fun state(
        framing: WallpaperFraming = WallpaperFraming(),
        target: WallpaperTarget = WallpaperTarget.LOCK_SCREEN,
        theme: WallpaperTheme = WallpaperTheme.GITHUB_DARK,
        isApplying: Boolean = false,
    ) = HomeUiState(
        login = "octocat",
        settings = WallpaperSettings(target = target, theme = theme, framing = framing),
        hasCalendar = true,
        isLoadingCalendar = false,
        isAdjusting = true,
        isApplying = isApplying,
    )

    private fun setContent(state: HomeUiState) {
        composeRule.setContent {
            GitWallpaperTheme {
                WallpaperAdjustScreen(
                    uiState = state,
                    onApply = { applied += it },
                    onCancel = { cancels++ },
                )
            }
        }
    }

    private companion object {
        const val PINCH_START = 20f
        const val PINCH_END = 150f
        const val ZOOM_DELTA = 0.0001f
    }
}
