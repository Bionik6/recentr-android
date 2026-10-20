package com.recentr.feature.rules.sheets

import com.recentr.core.data.MIN_WAIT_SECONDS
import com.recentr.core.data.UNLOCK_METHODS
import com.recentr.core.model.UnlockMethod
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** "How do you unlock?" (41) and "Wait before unlocking" (42). */
class UnlockOptionsTest {

    @Test
    fun `the unlock methods go as Figma lists them, no unlocks is strict mode's`() {
        assertEquals(
            listOf(UnlockMethod.Wait, UnlockMethod.Retype, UnlockMethod.ScanCode, UnlockMethod.Walk),
            UnlockMethodOptions,
        )
        // Every way out is offered.
        assertEquals(UNLOCK_METHODS.toSet(), UnlockMethodOptions.toSet())
    }

    @Test
    fun `the wait presets start at five seconds`() {
        assertEquals(MIN_WAIT_SECONDS, WaitPresets.first())
        assertTrue(WaitPresets.all { it >= MIN_WAIT_SECONDS })
    }
}
