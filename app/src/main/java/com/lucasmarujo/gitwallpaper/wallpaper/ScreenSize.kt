package com.lucasmarujo.gitwallpaper.wallpaper

import android.content.Context
import android.hardware.display.DisplayManager
import android.view.Display
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

data class ScreenSize(val width: Int, val height: Int) {
    fun scaled(factor: Float): ScreenSize = ScreenSize(
        width = (width * factor).roundToInt().coerceAtLeast(1),
        height = (height * factor).roundToInt().coerceAtLeast(1),
    )
}

fun interface ScreenSizeProvider {
    fun screenSize(): ScreenSize
}

class DisplayScreenSizeProvider(private val context: Context) : ScreenSizeProvider {
    override fun screenSize(): ScreenSize {
        val display = context.getSystemService(DisplayManager::class.java)?.getDisplay(Display.DEFAULT_DISPLAY)
        val (width, height) = if (display != null) {
            display.mode.physicalWidth to display.mode.physicalHeight
        } else {
            context.resources.displayMetrics.let { it.widthPixels to it.heightPixels }
        }
        return ScreenSize(width = min(width, height), height = max(width, height))
    }
}
