package com.lucasmarujo.gitwallpaper.data.settings

import com.lucasmarujo.gitwallpaper.domain.UpdateStatus
import com.lucasmarujo.gitwallpaper.testing.preferencesDataStore
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class UpdateStatusRepositoryTest {

    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @Test
    fun `applied wallpapers record fingerprint and timestamps`() = runTest {
        val repository = UpdateStatusRepository(temporaryFolder.preferencesDataStore(backgroundScope))

        repository.markApplied("abc", 1_000L)

        assertEquals(UpdateStatus("abc", 1_000L, 1_000L), repository.current())
    }

    @Test
    fun `checks only move the checked timestamp`() = runTest {
        val repository = UpdateStatusRepository(temporaryFolder.preferencesDataStore(backgroundScope))
        repository.markApplied("abc", 1_000L)

        repository.markChecked(5_000L)

        assertEquals(UpdateStatus("abc", 1_000L, 5_000L), repository.current())
    }

    @Test
    fun `clear resets the status`() = runTest {
        val repository = UpdateStatusRepository(temporaryFolder.preferencesDataStore(backgroundScope))
        repository.markApplied("abc", 1_000L)

        repository.clear()

        assertEquals(UpdateStatus(), repository.current())
    }
}
