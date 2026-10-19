package com.recentr.feature.rules.sheets

import com.recentr.core.data.MIN_WAIT_SECONDS
import com.recentr.core.model.UnlockMethod
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** "How do you unlock?" (41) and "Wait before unlocking" (42). */
class UnlockOptionsTest {

    @Test
    fun `no unlocks is strict mode's, not an option`() {
        assertFalse(UnlockMethod.None in UnlockMethodOptions)
        assertFalse(UnlockMethod.Strict in UnlockMethodOptions)
        assertEquals(listOf(UnlockMethod.Wait, UnlockMethod.Reason, UnlockMethod.Retype, UnlockMethod.ScanCode), UnlockMethodOptions)
    }

    @Test
    fun `the wait presets start at five seconds`() {
        assertEquals(MIN_WAIT_SECONDS, WaitPresets.first())
        assertTrue(WaitPresets.all { it >= MIN_WAIT_SECONDS })
    }
}
