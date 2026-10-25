package com.lucasmarujo.gitwallpaper.ui.home

import androidx.annotation.ColorInt
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.core.graphics.blue
import androidx.core.graphics.green
import androidx.core.graphics.red
import com.lucasmarujo.gitwallpaper.R
import com.lucasmarujo.gitwallpaper.domain.WallpaperTheme
import com.lucasmarujo.gitwallpaper.ui.components.ContributionPattern
import com.lucasmarujo.gitwallpaper.ui.components.contributionPatternOf
import com.lucasmarujo.gitwallpaper.wallpaper.WallpaperPalette
import java.util.Locale
import kotlin.math.roundToInt
import android.graphics.Color as AndroidColor

object CommitColorTestTags {
    const val RED_SLIDER = "commit_color_red"
    const val GREEN_SLIDER = "commit_color_green"
    const val BLUE_SLIDER = "commit_color_blue"
    const val HEX = "commit_color_hex"
}

private val CommitColorPreviewPattern = contributionPatternOf("0123401", "1234012", "2340123")
private const val MAX_CHANNEL = 255f
private const val HEX_FORMAT = "#%06X"
private const val RGB_MASK = 0xFFFFFF
private const val INACTIVE_TRACK_ALPHA = 0.24f

@Composable
fun CommitColorDialog(
    theme: WallpaperTheme,
    @ColorInt initialColor: Int,
    onConfirm: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    var color by rememberSaveable { mutableIntStateOf(initialColor) }
    val palette = WallpaperPalette.of(theme).withCommitColor(color)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.home_section_commit_color)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(88.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(palette.background)),
                    contentAlignment = Alignment.Center,
                ) {
                    ContributionPattern(
                        palette = palette,
                        pattern = CommitColorPreviewPattern,
                        modifier = Modifier.size(width = 168.dp, height = 68.dp),
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(palette.level4)),
                    )
                    Text(
                        text = String.format(Locale.ROOT, HEX_FORMAT, color and RGB_MASK),
                        style = MaterialTheme.typography.titleMedium,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier
                            .padding(start = 12.dp)
                            .testTag(CommitColorTestTags.HEX),
                    )
                }
                ChannelSlider(
                    labelRes = R.string.commit_color_red,
                    value = color.red,
                    activeColor = Color.Red,
                    testTag = CommitColorTestTags.RED_SLIDER,
                    onValueChange = { red -> color = AndroidColor.rgb(red, color.green, color.blue) },
                )
                ChannelSlider(
                    labelRes = R.string.commit_color_green,
                    value = color.green,
                    activeColor = Color.Green,
                    testTag = CommitColorTestTags.GREEN_SLIDER,
                    onValueChange = { green -> color = AndroidColor.rgb(color.red, green, color.blue) },
                )
                ChannelSlider(
                    labelRes = R.string.commit_color_blue,
                    value = color.blue,
                    activeColor = Color.Blue,
                    testTag = CommitColorTestTags.BLUE_SLIDER,
                    onValueChange = { blue -> color = AndroidColor.rgb(color.red, color.green, blue) },
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(color) }) {
                Text(stringResource(R.string.commit_color_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.login_cancel))
            }
        },
    )
}

@Composable
private fun ChannelSlider(
    @StringRes labelRes: Int,
    value: Int,
    activeColor: Color,
    testTag: String,
    onValueChange: (Int) -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = stringResource(labelRes),
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.width(72.dp),
        )
        Slider(
            value = value.toFloat(),
            onValueChange = { onValueChange(it.roundToInt()) },
            valueRange = 0f..MAX_CHANNEL,
            colors = SliderDefaults.colors(
                thumbColor = activeColor,
                activeTrackColor = activeColor,
                inactiveTrackColor = activeColor.copy(alpha = INACTIVE_TRACK_ALPHA),
                activeTickColor = activeColor,
                inactiveTickColor = activeColor,
            ),
            modifier = Modifier
                .weight(1f)
                .testTag(testTag),
        )
        Text(
            text = value.toString(),
            style = MaterialTheme.typography.labelLarge,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.width(36.dp),
        )
    }
}
