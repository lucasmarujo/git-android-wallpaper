package com.lucasmarujo.gitwallpaper.data.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.lucasmarujo.gitwallpaper.domain.UpdateStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.IOException

class UpdateStatusRepository(private val dataStore: DataStore<Preferences>) {

    val status: Flow<UpdateStatus> = dataStore.data
        .catch { exception -> if (exception is IOException) emit(emptyPreferences()) else throw exception }
        .map { preferences ->
            UpdateStatus(
                fingerprint = preferences[FINGERPRINT],
                lastAppliedAtMillis = preferences[LAST_APPLIED_AT],
                lastCheckedAtMillis = preferences[LAST_CHECKED_AT],
            )
        }
        .distinctUntilChanged()

    suspend fun current(): UpdateStatus = status.first()

    suspend fun markApplied(fingerprint: String, atMillis: Long) {
        dataStore.edit { preferences ->
            preferences[FINGERPRINT] = fingerprint
            preferences[LAST_APPLIED_AT] = atMillis
            preferences[LAST_CHECKED_AT] = atMillis
        }
    }

    suspend fun markChecked(atMillis: Long) {
        dataStore.edit { preferences -> preferences[LAST_CHECKED_AT] = atMillis }
    }

    suspend fun clear() {
        dataStore.edit { it.clear() }
    }

    private companion object {
        val FINGERPRINT = stringPreferencesKey("fingerprint")
        val LAST_APPLIED_AT = longPreferencesKey("last_applied_at")
        val LAST_CHECKED_AT = longPreferencesKey("last_checked_at")
    }
}
