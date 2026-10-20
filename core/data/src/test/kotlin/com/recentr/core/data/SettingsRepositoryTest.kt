package com.recentr.core.data

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

/** Settings › Change locks & limits anytime: off by default, and kept once turned on. */
class SettingsRepositoryTest {

    @get:Rule
    val folder = TemporaryFolder()

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val repository by lazy {
        SettingsRepository(PreferenceDataStoreFactory.create(scope = scope) { folder.root.resolve("settings.preferences_pb") })
    }

    @After
    fun tearDown() = scope.cancel()

    @Test
    fun `changing locks and limits anytime is off by default, and saved`() = runBlocking {
        assertFalse(repository.settings.first().changesLocksAnytime)
        repository.update { it.copy(changesLocksAnytime = true) }
        assertTrue(repository.settings.first().changesLocksAnytime)
        // Other settings don't touch it.
        repository.update { it.copy(strictByDefault = true) }
        assertTrue(repository.settings.first().changesLocksAnytime)
    }
}
