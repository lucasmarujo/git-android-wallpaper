package com.lucasmarujo.gitwallpaper.data.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import com.lucasmarujo.gitwallpaper.domain.CellShape
import com.lucasmarujo.gitwallpaper.domain.ContributionPeriod
import com.lucasmarujo.gitwallpaper.domain.UpdateInterval
import com.lucasmarujo.gitwallpaper.domain.WallpaperSettings
import com.lucasmarujo.gitwallpaper.domain.WallpaperTarget
import com.lucasmarujo.gitwallpaper.domain.WallpaperTheme
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.IOException

class SettingsRepository(private val dataStore: DataStore<Preferences>) {

    val settings: Flow<WallpaperSettings> = dataStore.data
        .catch { exception -> if (exception is IOException) emit(emptyPreferences()) else throw exception }
        .map { preferences -> preferences.toSettings() }
        .distinctUntilChanged()

    suspend fun current(): WallpaperSettings = settings.first()

    suspend fun update(transform: (WallpaperSettings) -> WallpaperSettings): WallpaperSettings {
        val updated = dataStore.edit { preferences -> preferences.write(transform(preferences.toSettings())) }
        return updated.toSettings()
    }

    private fun Preferences.toSettings(): WallpaperSettings {
        val defaults = WallpaperSettings()
        return WallpaperSettings(
            target = this[TARGET].toEnum(defaults.target),
            theme = this[THEME].toEnum(defaults.theme),
            period = this[PERIOD].toEnum(defaults.period),
            cellShape = this[CELL_SHAPE].toEnum(defaults.cellShape),
            showStats = this[SHOW_STATS] ?: defaults.showStats,
            autoUpdate = this[AUTO_UPDATE] ?: defaults.autoUpdate,
            updateInterval = this[UPDATE_INTERVAL].toEnum(defaults.updateInterval),
        )
    }

    private fun MutablePreferences.write(settings: WallpaperSettings) {
        this[TARGET] = settings.target.name
        this[THEME] = settings.theme.name
        this[PERIOD] = settings.period.name
        this[CELL_SHAPE] = settings.cellShape.name
        this[SHOW_STATS] = settings.showStats
        this[AUTO_UPDATE] = settings.autoUpdate
        this[UPDATE_INTERVAL] = settings.updateInterval.name
    }

    private inline fun <reified T : Enum<T>> String?.toEnum(default: T): T =
        enumValues<T>().firstOrNull { it.name == this } ?: default

    private companion object {
        val TARGET = stringPreferencesKey("target")
        val THEME = stringPreferencesKey("theme")
        val PERIOD = stringPreferencesKey("period")
        val CELL_SHAPE = stringPreferencesKey("cell_shape")
        val SHOW_STATS = booleanPreferencesKey("show_stats")
        val AUTO_UPDATE = booleanPreferencesKey("auto_update")
        val UPDATE_INTERVAL = stringPreferencesKey("update_interval")
    }
}
