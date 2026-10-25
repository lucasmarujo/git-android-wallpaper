package com.lucasmarujo.gitwallpaper.data.settings

import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.lucasmarujo.gitwallpaper.domain.CellShape
import com.lucasmarujo.gitwallpaper.domain.ContributionPeriod
import com.lucasmarujo.gitwallpaper.domain.UpdateInterval
import com.lucasmarujo.gitwallpaper.domain.WallpaperFraming
import com.lucasmarujo.gitwallpaper.domain.WallpaperSettings
import com.lucasmarujo.gitwallpaper.domain.WallpaperTarget
import com.lucasmarujo.gitwallpaper.domain.WallpaperTheme
import com.lucasmarujo.gitwallpaper.testing.preferencesDataStore
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class SettingsRepositoryTest {

    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @Test
    fun `defaults are returned when nothing is stored`() = runTest {
        val repository = SettingsRepository(temporaryFolder.preferencesDataStore(backgroundScope))

        assertEquals(WallpaperSettings(), repository.current())
    }

    @Test
    fun `updates are persisted and returned`() = runTest {
        val repository = SettingsRepository(temporaryFolder.preferencesDataStore(backgroundScope))
        val expected = WallpaperSettings(
            target = WallpaperTarget.BOTH,
            theme = WallpaperTheme.SUNSET,
            commitColor = 0xFF8A2BE2.toInt(),
            period = ContributionPeriod.LAST_12_MONTHS,
            cellShape = CellShape.CIRCLE,
            showStats = false,
            framing = WallpaperFraming(zoom = 1.4f, offsetX = -0.1f, offsetY = 0.2f),
            autoUpdate = false,
            updateInterval = UpdateInterval.SIX_HOURS,
        )

        val returned = repository.update { expected }

        assertEquals(expected, returned)
        assertEquals(expected, repository.current())
    }

    @Test
    fun `updates are applied on top of the stored settings`() = runTest {
        val repository = SettingsRepository(temporaryFolder.preferencesDataStore(backgroundScope))
        repository.update { it.copy(theme = WallpaperTheme.OCEAN) }

        repository.update { it.copy(period = ContributionPeriod.LAST_12_MONTHS) }

        val current = repository.current()
        assertEquals(WallpaperTheme.OCEAN, current.theme)
        assertEquals(ContributionPeriod.LAST_12_MONTHS, current.period)
    }

    @Test
    fun `periods saved by older versions fall back to the current year`() = runTest {
        val dataStore = temporaryFolder.preferencesDataStore(backgroundScope)
        dataStore.edit { it[stringPreferencesKey("period")] = "SIX_MONTHS" }

        val repository = SettingsRepository(dataStore)

        assertEquals(ContributionPeriod.CURRENT_YEAR, repository.current().period)
    }

    @Test
    fun `clearing the commit color goes back to the theme colors`() = runTest {
        val repository = SettingsRepository(temporaryFolder.preferencesDataStore(backgroundScope))
        repository.update { it.copy(commitColor = 0xFFFF0000.toInt()) }

        repository.update { it.copy(commitColor = null) }

        assertNull(repository.current().commitColor)
    }

    @Test
    fun `out of range framing is limited when saved`() = runTest {
        val repository = SettingsRepository(temporaryFolder.preferencesDataStore(backgroundScope))

        val saved = repository.update { it.copy(framing = WallpaperFraming(zoom = 9f, offsetX = -2f, offsetY = 2f)) }

        val expected = WallpaperFraming(
            zoom = WallpaperFraming.MAX_ZOOM,
            offsetX = -WallpaperFraming.MAX_OFFSET,
            offsetY = WallpaperFraming.MAX_OFFSET,
        )
        assertEquals(expected, saved.framing)
        assertEquals(expected, repository.current().framing)
    }

    @Test
    fun `invalid stored framing falls back to safe values`() = runTest {
        val dataStore = temporaryFolder.preferencesDataStore(backgroundScope)
        dataStore.edit {
            it[floatPreferencesKey("framing_zoom")] = Float.NaN
            it[floatPreferencesKey("framing_offset_y")] = -7f
        }

        val repository = SettingsRepository(dataStore)

        assertEquals(WallpaperFraming(offsetY = -WallpaperFraming.MAX_OFFSET), repository.current().framing)
    }

    @Test
    fun `unknown stored values fall back to defaults`() = runTest {
        val dataStore = temporaryFolder.preferencesDataStore(backgroundScope)
        dataStore.edit { it[stringPreferencesKey("theme")] = "REMOVED_THEME" }

        val repository = SettingsRepository(dataStore)

        assertEquals(WallpaperSettings().theme, repository.current().theme)
    }
}
