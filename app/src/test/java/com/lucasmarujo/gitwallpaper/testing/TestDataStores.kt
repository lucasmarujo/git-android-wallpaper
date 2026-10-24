package com.lucasmarujo.gitwallpaper.testing

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import kotlinx.coroutines.CoroutineScope
import org.junit.rules.TemporaryFolder
import java.io.File
import java.util.UUID

fun TemporaryFolder.preferencesDataStore(scope: CoroutineScope): DataStore<Preferences> =
    PreferenceDataStoreFactory.create(
        scope = scope,
        produceFile = { File(root, "${UUID.randomUUID()}.preferences_pb") },
    )
