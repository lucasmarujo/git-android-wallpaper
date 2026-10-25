package com.lucasmarujo.gitwallpaper.domain

data class WallpaperFraming(
    val zoom: Float = DEFAULT_ZOOM,
    val offsetX: Float = DEFAULT_OFFSET,
    val offsetY: Float = DEFAULT_OFFSET,
) {
    fun transformed(zoomChange: Float, panX: Float, panY: Float): WallpaperFraming =
        WallpaperFraming(zoom = zoom * zoomChange, offsetX = offsetX + panX, offsetY = offsetY + panY).coerced()

    fun coerced(): WallpaperFraming = WallpaperFraming(
        zoom = zoom.finiteOr(DEFAULT_ZOOM).coerceIn(MIN_ZOOM, MAX_ZOOM),
        offsetX = offsetX.finiteOr(DEFAULT_OFFSET).coerceIn(-MAX_OFFSET, MAX_OFFSET),
        offsetY = offsetY.finiteOr(DEFAULT_OFFSET).coerceIn(-MAX_OFFSET, MAX_OFFSET),
    )

    private fun Float.finiteOr(default: Float): Float = if (isFinite()) this else default

    companion object {
        const val DEFAULT_ZOOM = 1f
        const val DEFAULT_OFFSET = 0f
        const val MIN_ZOOM = 0.5f
        const val MAX_ZOOM = 2.5f
        const val MAX_OFFSET = 0.5f
    }
}
