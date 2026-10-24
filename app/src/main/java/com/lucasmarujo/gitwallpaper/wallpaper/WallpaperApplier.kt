package com.lucasmarujo.gitwallpaper.wallpaper

import android.app.WallpaperManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Rect
import com.lucasmarujo.gitwallpaper.domain.WallpaperTarget

fun interface WallpaperApplier {
    fun apply(bitmap: Bitmap, target: WallpaperTarget)
}

class SystemWallpaperApplier(context: Context) : WallpaperApplier {
    private val wallpaperManager = WallpaperManager.getInstance(context)

    override fun apply(bitmap: Bitmap, target: WallpaperTarget) {
        if (!wallpaperManager.isWallpaperSupported || !wallpaperManager.isSetWallpaperAllowed) {
            throw SecurityException("Changing the wallpaper is not allowed on this device")
        }
        wallpaperManager.setBitmap(bitmap, Rect(0, 0, bitmap.width, bitmap.height), true, target.toFlags())
    }

    private fun WallpaperTarget.toFlags(): Int = when (this) {
        WallpaperTarget.LOCK_SCREEN -> WallpaperManager.FLAG_LOCK
        WallpaperTarget.HOME_SCREEN -> WallpaperManager.FLAG_SYSTEM
        WallpaperTarget.BOTH -> WallpaperManager.FLAG_LOCK or WallpaperManager.FLAG_SYSTEM
    }
}
