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
    fun `the always allowed card stays closed, until everything is erased`() = runBlocking {
        assertFalse(repository.settings.first().alwaysAllowedCardDismissed)
        repository.update { it.copy(alwaysAllowedCardDismissed = true) }
        assertTrue(repository.settings.first().alwaysAllowedCardDismissed)
        repository.reset()
        assertFalse(repository.settings.first().alwaysAllowedCardDismissed)
    }
}
