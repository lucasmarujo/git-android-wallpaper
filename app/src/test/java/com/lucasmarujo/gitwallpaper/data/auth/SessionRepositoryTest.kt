package com.lucasmarujo.gitwallpaper.data.auth

import androidx.datastore.preferences.core.stringPreferencesKey
import com.lucasmarujo.gitwallpaper.testing.FakeSecretCipher
import com.lucasmarujo.gitwallpaper.testing.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class SessionRepositoryTest {

    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private val cipher = FakeSecretCipher()

    @Test
    fun `no session is stored initially`() = runTest {
        val repository = SessionRepository(temporaryFolder.preferencesDataStore(backgroundScope), cipher)

        assertNull(repository.currentSession())
    }

    @Test
    fun `saved sessions are read back`() = runTest {
        val repository = SessionRepository(temporaryFolder.preferencesDataStore(backgroundScope), cipher)

        repository.save(Session(login = "octocat", token = "gho_secret"))

        assertEquals(Session(login = "octocat", token = "gho_secret"), repository.currentSession())
    }

    @Test
    fun `tokens are never stored in plain text`() = runTest {
        val dataStore = temporaryFolder.preferencesDataStore(backgroundScope)
        val repository = SessionRepository(dataStore, cipher)

        repository.save(Session(login = "octocat", token = "gho_secret"))

        val stored = dataStore.data.first()[stringPreferencesKey("encrypted_token")].orEmpty()
        assertFalse(stored.contains("gho_secret"))
    }

    @Test
    fun `undecryptable tokens are treated as signed out`() = runTest {
        val repository = SessionRepository(temporaryFolder.preferencesDataStore(backgroundScope), cipher)
        repository.save(Session(login = "octocat", token = "gho_secret"))

        cipher.failOnDecrypt = true

        assertNull(repository.currentSession())
    }

    @Test
    fun `clear removes the session`() = runTest {
        val repository = SessionRepository(temporaryFolder.preferencesDataStore(backgroundScope), cipher)
        repository.save(Session(login = "octocat", token = "gho_secret"))

        repository.clear()

        assertNull(repository.currentSession())
    }

    @Test
    fun `session string representation hides the token`() {
        assertFalse(Session(login = "octocat", token = "gho_secret").toString().contains("gho_secret"))
    }
}
