package com.lucasmarujo.gitwallpaper.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import com.lucasmarujo.gitwallpaper.domain.ContributionLevel
import com.lucasmarujo.gitwallpaper.wallpaper.WallpaperPalette
import kotlin.math.min

private const val GAP_RATIO = 0.2f
private const val CORNER_RATIO = 0.22f

@Composable
fun ContributionPattern(
    palette: WallpaperPalette,
    pattern: List<List<ContributionLevel>>,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier) {
        val rows = pattern.size
        val columns = pattern.maxOfOrNull { it.size } ?: 0
        if (rows == 0 || columns == 0) return@Canvas
        val cell = min(
            size.width / (columns + (columns - 1) * GAP_RATIO),
            size.height / (rows + (rows - 1) * GAP_RATIO),
        )
        val gap = cell * GAP_RATIO
        val offsetX = (size.width - (columns * cell + (columns - 1) * gap)) / 2f
        val offsetY = (size.height - (rows * cell + (rows - 1) * gap)) / 2f
        pattern.forEachIndexed { row, levels ->
            levels.forEachIndexed { column, level ->
                drawRoundRect(
                    color = Color(palette.colorFor(level)),
                    topLeft = Offset(offsetX + column * (cell + gap), offsetY + row * (cell + gap)),
                    size = Size(cell, cell),
                    cornerRadius = CornerRadius(cell * CORNER_RATIO),
                )
            }
        }
    }
}

fun contributionPatternOf(vararg rows: String): List<List<ContributionLevel>> =
    rows.map { row -> row.map { digit -> ContributionLevel.entries[digit.digitToInt()] } }
