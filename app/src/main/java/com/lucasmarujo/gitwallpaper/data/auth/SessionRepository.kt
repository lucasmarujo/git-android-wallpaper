package com.lucasmarujo.gitwallpaper.data.auth

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.IOException
import java.security.GeneralSecurityException
import java.security.ProviderException

data class Session(val login: String, val token: String) {
    override fun toString(): String = "Session(login=$login)"
}

class SessionRepository(
    private val dataStore: DataStore<Preferences>,
    private val cipher: SecretCipher,
) {
    val session: Flow<Session?> = dataStore.data
        .catch { exception -> if (exception is IOException) emit(emptyPreferences()) else throw exception }
        .map { preferences -> preferences.toSession() }
        .distinctUntilChanged()

    suspend fun currentSession(): Session? = session.first()

    suspend fun save(session: Session) {
        val encryptedToken = cipher.encrypt(session.token)
        dataStore.edit { preferences ->
            preferences[LOGIN] = session.login
            preferences[ENCRYPTED_TOKEN] = encryptedToken
        }
    }

    suspend fun clear() {
        dataStore.edit { it.clear() }
    }

    private fun Preferences.toSession(): Session? {
        val login = this[LOGIN] ?: return null
        val encryptedToken = this[ENCRYPTED_TOKEN] ?: return null
        val token = decryptOrNull(encryptedToken) ?: return null
        return Session(login = login, token = token)
    }

    private fun decryptOrNull(encryptedToken: String): String? = try {
        cipher.decrypt(encryptedToken)
    } catch (exception: GeneralSecurityException) {
        null
    } catch (exception: ProviderException) {
        null
    } catch (exception: IllegalArgumentException) {
        null
    }

    private companion object {
        val LOGIN = stringPreferencesKey("login")
        val ENCRYPTED_TOKEN = stringPreferencesKey("encrypted_token")
    }
}
