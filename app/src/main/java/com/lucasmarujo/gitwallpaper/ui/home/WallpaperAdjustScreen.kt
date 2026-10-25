package com.lucasmarujo.gitwallpaper.ui.home

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import com.lucasmarujo.gitwallpaper.R
import com.lucasmarujo.gitwallpaper.domain.WallpaperFraming
import com.lucasmarujo.gitwallpaper.domain.WallpaperTarget
import com.lucasmarujo.gitwallpaper.util.primaryLocale
import com.lucasmarujo.gitwallpaper.wallpaper.WallpaperLayout
import com.lucasmarujo.gitwallpaper.wallpaper.WallpaperPalette
import java.text.SimpleDateFormat
import java.util.Date
import android.text.format.DateFormat as AndroidDateFormat

object WallpaperAdjustTestTags {
    const val FRAME = "adjust_frame"
    const val APPLY_BUTTON = "adjust_apply_button"
    const val LOCK_SCREEN_CLOCK = "adjust_lock_screen_clock"
}

private const val DATE_SKELETON = "EEEMMMd"
private const val LIGHT_BACKGROUND_LUMINANCE = 0.5f

private val WallpaperFramingSaver = listSaver<WallpaperFraming, Float>(
    save = { framing -> listOf(framing.zoom, framing.offsetX, framing.offsetY) },
    restore = { (zoom, offsetX, offsetY) -> WallpaperFraming(zoom = zoom, offsetX = offsetX, offsetY = offsetY) },
)

@Composable
fun WallpaperAdjustScreen(
    uiState: HomeUiState,
    onApply: (WallpaperFraming) -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var framing by rememberSaveable(stateSaver = WallpaperFramingSaver) { mutableStateOf(uiState.settings.framing) }
    val palette = WallpaperPalette.of(uiState.settings.theme)
    BackHandler(onBack = onCancel)
    StatusBarAppearance(lightBackground = Color(palette.background).luminance() > LIGHT_BACKGROUND_LUMINANCE)
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .aspectRatio(uiState.previewAspectRatio)
                .clipToBounds()
                .background(Color(palette.background))
                .pointerInput(Unit) {
                    detectTransformGestures { _, pan, zoomChange, _ ->
                        framing = framing.transformed(
                            zoomChange = zoomChange,
                            panX = pan.x / (size.width * WallpaperLayout.SYSTEM_WALLPAPER_ZOOM),
                            panY = pan.y / (size.height * WallpaperLayout.SYSTEM_WALLPAPER_ZOOM),
                        )
                    }
                }
                .testTag(WallpaperAdjustTestTags.FRAME),
        ) {
            val image = uiState.adjustmentPreview
            if (image != null) {
                Image(
                    bitmap = image,
                    contentDescription = stringResource(R.string.home_preview_description),
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            val scale = WallpaperLayout.SYSTEM_WALLPAPER_ZOOM * framing.zoom
                            scaleX = scale
                            scaleY = scale
                            translationX = WallpaperLayout.SYSTEM_WALLPAPER_ZOOM * framing.offsetX * size.width
                            translationY = WallpaperLayout.SYSTEM_WALLPAPER_ZOOM * framing.offsetY * size.height
                        },
                )
            } else {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }
            if (uiState.settings.target != WallpaperTarget.HOME_SCREEN) {
                LockScreenClock(
                    color = Color(palette.primaryText),
                    modifier = Modifier.align(Alignment.TopCenter),
                )
            }
        }
        AdjustControls(
            isApplying = uiState.isApplying,
            canReset = framing != WallpaperFraming(),
            onReset = { framing = WallpaperFraming() },
            onCancel = onCancel,
            onApply = { onApply(framing) },
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}

@Composable
private fun StatusBarAppearance(lightBackground: Boolean) {
    val view = LocalView.current
    DisposableEffect(view, lightBackground) {
        val window = (view.context as? Activity)?.window
        val controller = window?.let { WindowCompat.getInsetsController(it, view) }
        val previous = controller?.isAppearanceLightStatusBars
        controller?.isAppearanceLightStatusBars = lightBackground
        onDispose {
            if (controller != null && previous != null) controller.isAppearanceLightStatusBars = previous
        }
    }
}

@Composable
private fun LockScreenClock(color: Color, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val locale = LocalResources.current.primaryLocale
    val now = remember { Date() }
    val time = remember(context, now) { AndroidDateFormat.getTimeFormat(context).format(now) }
    val date = remember(locale, now) {
        SimpleDateFormat(AndroidDateFormat.getBestDateTimePattern(locale, DATE_SKELETON), locale).format(now)
    }
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .statusBarsPadding()
            .padding(top = 32.dp)
            .testTag(WallpaperAdjustTestTags.LOCK_SCREEN_CLOCK),
    ) {
        Text(text = time, style = MaterialTheme.typography.displayLarge, color = color)
        Text(text = date, style = MaterialTheme.typography.titleMedium, color = color)
    }
}

@Composable
private fun AdjustControls(
    isApplying: Boolean,
    canReset: Boolean,
    onReset: () -> Unit,
    onCancel: () -> Unit,
    onApply: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(tonalElevation = 3.dp, modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = stringResource(R.string.adjust_hint),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(onClick = onCancel, enabled = !isApplying) {
                    Text(stringResource(R.string.login_cancel))
                }
                OutlinedButton(onClick = onReset, enabled = canReset && !isApplying) {
                    Text(stringResource(R.string.adjust_reset))
                }
                Button(
                    onClick = onApply,
                    enabled = !isApplying,
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp)
                        .testTag(WallpaperAdjustTestTags.APPLY_BUTTON),
                ) {
                    if (isApplying) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary,
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(stringResource(R.string.home_applying))
                    } else {
                        Text(stringResource(R.string.adjust_apply))
                    }
                }
            }
        }
    }
}
