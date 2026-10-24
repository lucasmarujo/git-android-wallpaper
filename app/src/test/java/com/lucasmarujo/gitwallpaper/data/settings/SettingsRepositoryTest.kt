package com.lucasmarujo.gitwallpaper.data.settings

import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.lucasmarujo.gitwallpaper.domain.CellShape
import com.lucasmarujo.gitwallpaper.domain.ContributionPeriod
import com.lucasmarujo.gitwallpaper.domain.UpdateInterval
import com.lucasmarujo.gitwallpaper.domain.WallpaperSettings
import com.lucasmarujo.gitwallpaper.domain.WallpaperTarget
import com.lucasmarujo.gitwallpaper.domain.WallpaperTheme
import com.lucasmarujo.gitwallpaper.testing.preferencesDataStore
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
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
            period = ContributionPeriod.THREE_MONTHS,
            cellShape = CellShape.CIRCLE,
            verticalPosition = 0.4f,
            showStats = false,
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

        repository.update { it.copy(period = ContributionPeriod.SIX_MONTHS) }

        val current = repository.current()
        assertEquals(WallpaperTheme.OCEAN, current.theme)
        assertEquals(ContributionPeriod.SIX_MONTHS, current.period)
    }

    @Test
    fun `vertical position is clamped to the supported range`() = runTest {
        val repository = SettingsRepository(temporaryFolder.preferencesDataStore(backgroundScope))

        repository.update { it.copy(verticalPosition = 2f) }

        assertEquals(WallpaperSettings.MAX_VERTICAL_POSITION, repository.current().verticalPosition)
    }

    @Test
    fun `unknown stored values fall back to defaults`() = runTest {
        val dataStore = temporaryFolder.preferencesDataStore(backgroundScope)
        dataStore.edit { it[stringPreferencesKey("theme")] = "REMOVED_THEME" }

        val repository = SettingsRepository(dataStore)

        assertEquals(WallpaperSettings().theme, repository.current().theme)
    }
}
