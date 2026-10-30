package com.recentr.core.model

import org.junit.Assert.assertEquals
import org.junit.Test

/** Focus sounds: the sheet's sections hold each recording once, and a first session plays soft rain. */
class FocusSoundTest {

    @Test
    fun everySoundIsInOneSection() {
        assertEquals(FocusSound.entries, FocusSoundCategory.entries.flatMap { it.sounds })
    }

    @Test
    fun aFirstSessionPlaysSoftRain() {
        assertEquals(listOf(FocusSoundLayer(FocusSound.SoftRain, 0.6f)), FocusConfig().sounds)
    }
}
