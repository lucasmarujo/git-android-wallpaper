package com.lucasmarujo.gitwallpaper.wallpaper

import com.lucasmarujo.gitwallpaper.domain.CellShape
import com.lucasmarujo.gitwallpaper.domain.ContributionPeriod
import com.lucasmarujo.gitwallpaper.domain.WallpaperFraming
import com.lucasmarujo.gitwallpaper.domain.WallpaperSettings
import com.lucasmarujo.gitwallpaper.domain.WallpaperTarget
import com.lucasmarujo.gitwallpaper.domain.WallpaperTheme
import com.lucasmarujo.gitwallpaper.testing.TestCalendars
import com.lucasmarujo.gitwallpaper.testing.TestCalendars.TODAY
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
        val first = fingerprint()
        val second = WallpaperFingerprint.of(calendar.copy(), settings.copy(), size.copy(), locale, TODAY)

        assertEquals(first, second)
        assertTrue(first.matches(Regex("[0-9a-f]{64}")))
    }

    @Test
    fun `a new contribution changes the fingerprint`() {
        val changedDays = calendar.days.toMutableList().apply {
            this[lastIndex] = last().copy(count = last().count + 1)
        }

        assertNotEquals(fingerprint(), WallpaperFingerprint.of(calendar.copy(days = changedDays), settings, size, locale, TODAY))
    }

    @Test
    fun `a new day changes the fingerprint even without new contributions`() {
        assertNotEquals(fingerprint(), WallpaperFingerprint.of(calendar, settings, size, locale, TODAY.plusDays(1)))
    }

    @Test
    fun `a new avatar changes the fingerprint`() {
        val withAvatar = calendar.copy(avatarUrl = "https://avatars.githubusercontent.com/u/1?v=4")

        assertNotEquals(fingerprint(), WallpaperFingerprint.of(withAvatar, settings, size, locale, TODAY))
    }

    @Test
    fun `appearance and target settings change the fingerprint`() {
        val base = fingerprint()

        listOf(
            settings.copy(theme = WallpaperTheme.OCEAN),
            settings.copy(commitColor = 0xFFFF0000.toInt()),
            settings.copy(target = WallpaperTarget.BOTH),
            settings.copy(showStats = false),
            settings.copy(period = ContributionPeriod.LAST_12_MONTHS),
            settings.copy(cellShape = CellShape.CIRCLE),
            settings.copy(framing = WallpaperFraming(zoom = 1.3f)),
            settings.copy(framing = WallpaperFraming(offsetX = 0.1f)),
            settings.copy(framing = WallpaperFraming(offsetY = -0.1f)),
        ).forEach { changed ->
            assertNotEquals(changed.toString(), base, WallpaperFingerprint.of(calendar, changed, size, locale, TODAY))
        }
    }

    @Test
    fun `scheduling settings do not change the fingerprint`() {
        assertEquals(fingerprint(), WallpaperFingerprint.of(calendar, settings.copy(autoUpdate = false), size, locale, TODAY))
    }

    @Test
    fun `screen size and locale change the fingerprint`() {
        val base = fingerprint()

        assertNotEquals(base, WallpaperFingerprint.of(calendar, settings, ScreenSize(1440, 3200), locale, TODAY))
        assertNotEquals(base, WallpaperFingerprint.of(calendar, settings, size, Locale.forLanguageTag("pt-BR"), TODAY))
    }

    private fun fingerprint() = WallpaperFingerprint.of(calendar, settings, size, locale, TODAY)
}
