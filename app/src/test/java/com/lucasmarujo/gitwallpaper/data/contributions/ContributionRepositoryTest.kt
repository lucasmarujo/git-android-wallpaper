package com.lucasmarujo.gitwallpaper.data.contributions

import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.lucasmarujo.gitwallpaper.data.auth.Session
import com.lucasmarujo.gitwallpaper.data.auth.SessionRepository
import com.lucasmarujo.gitwallpaper.data.github.GitHubException
import com.lucasmarujo.gitwallpaper.domain.SyncError
import com.lucasmarujo.gitwallpaper.testing.FakeContributionsSource
import com.lucasmarujo.gitwallpaper.testing.FakeSecretCipher
import com.lucasmarujo.gitwallpaper.testing.TestCalendars
import com.lucasmarujo.gitwallpaper.testing.preferencesDataStore
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.IOException

class ContributionRepositoryTest {

    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private val source = FakeContributionsSource()

    @Test
    fun `refresh without a session reports not signed in`() = runTest {
        val repository = repository(signedIn = false)

        assertEquals(RefreshResult.Failure(SyncError.NOT_SIGNED_IN), repository.refresh())
        assertTrue(source.requestedTokens.isEmpty())
    }

    @Test
    fun `refresh fetches with the session token and caches the calendar`() = runTest {
        val calendar = TestCalendars.calendar(login = "octocat")
        source.handler = { calendar }
        val repository = repository()

        val result = repository.refresh()

        assertEquals(RefreshResult.Success(calendar), result)
        assertEquals(listOf(TOKEN), source.requestedTokens)
        assertEquals(calendar, repository.cachedCalendar())
    }

    @Test
    fun `unauthorized responses clear the session`() = runTest {
        source.handler = { throw GitHubException.Unauthorized() }
        val repository = repository()

        val result = repository.refresh()

        assertEquals(RefreshResult.Failure(SyncError.SESSION_EXPIRED), result)
        assertNull(sessionRepository.currentSession())
    }

    @Test
    fun `network and api failures keep the cached calendar`() = runTest {
        val cached = TestCalendars.calendar()
        val repository = repository()
        repository.save(cached)

        source.handler = { throw GitHubException.Network(IOException("offline")) }
        assertEquals(RefreshResult.Failure(SyncError.NETWORK), repository.refresh())

        source.handler = { throw GitHubException.Http(500) }
        assertEquals(RefreshResult.Failure(SyncError.GITHUB), repository.refresh())

        assertEquals(cached, repository.cachedCalendar())
    }

    @Test
    fun `corrupted cache entries are ignored`() = runTest {
        val dataStore = temporaryFolder.preferencesDataStore(backgroundScope)
        dataStore.edit { it[stringPreferencesKey("calendar_json")] = "{not json" }
        val repository = ContributionRepository(dataStore, source, sessionRepository(signedIn = true))

        assertNull(repository.cachedCalendar())
    }

    @Test
    fun `clear removes the cached calendar`() = runTest {
        val repository = repository()
        repository.save(TestCalendars.calendar())

        repository.clear()

        assertNull(repository.cachedCalendar())
    }

    private lateinit var sessionRepository: SessionRepository

    private suspend fun TestScope.sessionRepository(signedIn: Boolean): SessionRepository {
        sessionRepository = SessionRepository(temporaryFolder.preferencesDataStore(backgroundScope), FakeSecretCipher())
        if (signedIn) sessionRepository.save(Session(login = "octocat", token = TOKEN))
        return sessionRepository
    }

    private suspend fun TestScope.repository(signedIn: Boolean = true): ContributionRepository =
        ContributionRepository(
            dataStore = temporaryFolder.preferencesDataStore(backgroundScope),
            contributionsSource = source,
            sessionRepository = sessionRepository(signedIn),
        )

    private companion object {
        const val TOKEN = "gho_session"
    }
}
