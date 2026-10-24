package com.lucasmarujo.gitwallpaper.data.contributions

import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import com.lucasmarujo.gitwallpaper.data.auth.SessionRepository
import com.lucasmarujo.gitwallpaper.data.github.ContributionsSource
import com.lucasmarujo.gitwallpaper.data.github.GitHubException
import com.lucasmarujo.gitwallpaper.data.github.GitHubJson
import com.lucasmarujo.gitwallpaper.domain.ContributionCalendar
import com.lucasmarujo.gitwallpaper.domain.SyncError
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import java.io.IOException

class ContributionRepository(
    private val dataStore: DataStore<Preferences>,
    private val contributionsSource: ContributionsSource,
    private val sessionRepository: SessionRepository,
    private val json: Json = GitHubJson,
) {
    val calendar: Flow<ContributionCalendar?> = dataStore.data
        .catch { exception -> if (exception is IOException) emit(emptyPreferences()) else throw exception }
        .map { preferences -> preferences[CALENDAR_JSON] }
        .distinctUntilChanged()
        .map { encoded -> encoded?.let(::decodeOrNull) }

    suspend fun cachedCalendar(): ContributionCalendar? = calendar.first()

    suspend fun refresh(): RefreshResult {
        val session = sessionRepository.currentSession()
            ?: return RefreshResult.Failure(SyncError.NOT_SIGNED_IN)
        return try {
            val calendar = contributionsSource.fetchCalendar(session.token)
            save(calendar)
            RefreshResult.Success(calendar)
        } catch (exception: GitHubException.Unauthorized) {
            Log.w(TAG, "GitHub rejected the stored session", exception)
            sessionRepository.clear()
            RefreshResult.Failure(SyncError.SESSION_EXPIRED)
        } catch (exception: GitHubException.Network) {
            Log.w(TAG, "Could not reach GitHub", exception)
            RefreshResult.Failure(SyncError.NETWORK)
        } catch (exception: GitHubException) {
            Log.w(TAG, "GitHub returned an error", exception)
            RefreshResult.Failure(SyncError.GITHUB)
        }
    }

    suspend fun save(calendar: ContributionCalendar) {
        val encoded = json.encodeToString(ContributionCalendar.serializer(), calendar)
        dataStore.edit { preferences -> preferences[CALENDAR_JSON] = encoded }
    }

    suspend fun clear() {
        dataStore.edit { preferences -> preferences.remove(CALENDAR_JSON) }
    }

    private fun decodeOrNull(encoded: String): ContributionCalendar? = try {
        json.decodeFromString(ContributionCalendar.serializer(), encoded)
    } catch (exception: IllegalArgumentException) {
        Log.w(TAG, "Discarding unreadable contribution cache", exception)
        null
    }

    private companion object {
        const val TAG = "ContributionRepository"
        val CALENDAR_JSON = stringPreferencesKey("calendar_json")
    }
}
