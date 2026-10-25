package com.lucasmarujo.gitwallpaper.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class WallpaperFramingTest {

    @Test
    fun `default framing keeps the original size and position`() {
        val framing = WallpaperFraming()

        assertEquals(1f, framing.zoom)
        assertEquals(0f, framing.offsetX)
        assertEquals(0f, framing.offsetY)
    }

    @Test
    fun `gestures multiply the zoom and add the pan`() {
        val framing = WallpaperFraming(zoom = 1.2f, offsetX = 0.1f, offsetY = -0.1f)

        val transformed = framing.transformed(zoomChange = 1.5f, panX = 0.05f, panY = -0.05f)

        assertEquals(1.8f, transformed.zoom, DELTA)
        assertEquals(0.15f, transformed.offsetX, DELTA)
        assertEquals(-0.15f, transformed.offsetY, DELTA)
    }

    @Test
    fun `zoom is limited to the supported range`() {
        val zoomedIn = WallpaperFraming().transformed(zoomChange = 10f, panX = 0f, panY = 0f)
        val zoomedOut = WallpaperFraming().transformed(zoomChange = 0.01f, panX = 0f, panY = 0f)

        assertEquals(WallpaperFraming.MAX_ZOOM, zoomedIn.zoom)
        assertEquals(WallpaperFraming.MIN_ZOOM, zoomedOut.zoom)
    }

    @Test
    fun `the graph center cannot leave the screen`() {
        val framing = WallpaperFraming().transformed(zoomChange = 1f, panX = 3f, panY = -3f)

        assertEquals(WallpaperFraming.MAX_OFFSET, framing.offsetX)
        assertEquals(-WallpaperFraming.MAX_OFFSET, framing.offsetY)
    }

    @Test
    fun `invalid values fall back to the defaults`() {
        val framing = WallpaperFraming(zoom = Float.NaN, offsetX = Float.POSITIVE_INFINITY, offsetY = Float.NaN).coerced()

        assertEquals(WallpaperFraming(), framing)
    }

    private companion object {
        const val DELTA = 0.0001f
    }
}
