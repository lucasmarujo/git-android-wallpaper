package com.lucasmarujo.gitwallpaper.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColors = darkColorScheme(
    primary = Color(0xFF3FB950),
    onPrimary = Color(0xFF04260F),
    primaryContainer = Color(0xFF0E4429),
    onPrimaryContainer = Color(0xFFAFF5B4),
    secondary = Color(0xFF7D8590),
    onSecondary = Color(0xFF0D1117),
    background = Color(0xFF0D1117),
    onBackground = Color(0xFFE6EDF3),
    surface = Color(0xFF0D1117),
    onSurface = Color(0xFFE6EDF3),
    surfaceVariant = Color(0xFF161B22),
    onSurfaceVariant = Color(0xFF9198A1),
    surfaceContainer = Color(0xFF151B23),
    surfaceContainerHigh = Color(0xFF1C232C),
    outline = Color(0xFF3D444D),
    outlineVariant = Color(0xFF262C36),
    error = Color(0xFFF85149),
)

private val LightColors = lightColorScheme(
    primary = Color(0xFF1A7F37),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFDAFBE1),
    onPrimaryContainer = Color(0xFF044F1E),
    secondary = Color(0xFF59636E),
    onSecondary = Color(0xFFFFFFFF),
    background = Color(0xFFFFFFFF),
    onBackground = Color(0xFF1F2328),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF1F2328),
    surfaceVariant = Color(0xFFF6F8FA),
    onSurfaceVariant = Color(0xFF59636E),
    surfaceContainer = Color(0xFFF6F8FA),
    surfaceContainerHigh = Color(0xFFEFF2F5),
    outline = Color(0xFFD1D9E0),
    outlineVariant = Color(0xFFE6EAEF),
    error = Color(0xFFCF222E),
)

@Composable
fun GitWallpaperTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content,
    )
}
