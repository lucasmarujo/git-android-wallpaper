package com.lucasmarujo.gitwallpaper.wallpaper

import com.lucasmarujo.gitwallpaper.domain.WallpaperSettings
import com.lucasmarujo.gitwallpaper.domain.WallpaperTarget
import com.lucasmarujo.gitwallpaper.domain.WallpaperTheme
import com.lucasmarujo.gitwallpaper.testing.TestCalendars
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Locale

class WallpaperFingerprintTest {

    private val calendar = TestCalendars.calendar()
    private val settings = WallpaperSettings()
    private val size = ScreenSize(1080, 2400)
    private val locale = Locale.US

    @Test
    fun `same inputs produce the same sha256 fingerprint`() {
        val first = WallpaperFingerprint.of(calendar, settings, size, locale)
        val second = WallpaperFingerprint.of(calendar.copy(), settings.copy(), size.copy(), locale)

        assertEquals(first, second)
        assertTrue(first.matches(Regex("[0-9a-f]{64}")))
    }

    @Test
    fun `a new contribution changes the fingerprint`() {
        val changedDays = calendar.days.toMutableList().apply {
            this[lastIndex] = last().copy(count = last().count + 1)
        }

        assertNotEquals(
            WallpaperFingerprint.of(calendar, settings, size, locale),
            WallpaperFingerprint.of(calendar.copy(days = changedDays), settings, size, locale),
        )
    }

    @Test
    fun `appearance and target settings change the fingerprint`() {
        val base = WallpaperFingerprint.of(calendar, settings, size, locale)

        listOf(
            settings.copy(theme = WallpaperTheme.OCEAN),
            settings.copy(target = WallpaperTarget.BOTH),
            settings.copy(showStats = false),
            settings.copy(verticalPosition = 0.3f),
        ).forEach { changed ->
            assertNotEquals(base, WallpaperFingerprint.of(calendar, changed, size, locale))
        }
    }

    @Test
    fun `scheduling settings do not change the fingerprint`() {
        assertEquals(
            WallpaperFingerprint.of(calendar, settings, size, locale),
            WallpaperFingerprint.of(calendar, settings.copy(autoUpdate = false), size, locale),
        )
    }

    @Test
    fun `screen size and locale change the fingerprint`() {
        val base = WallpaperFingerprint.of(calendar, settings, size, locale)

        assertNotEquals(base, WallpaperFingerprint.of(calendar, settings, ScreenSize(1440, 3200), locale))
        assertNotEquals(base, WallpaperFingerprint.of(calendar, settings, size, Locale.forLanguageTag("pt-BR")))
    }
}
