package com.recentr.feature.focus.ui

import org.junit.Assert.assertEquals
import org.junit.Test

/** "paused for 0:42": how long a pause has lasted, to the second. */
class FocusFormatTest {

    @Test
    fun `stopwatch shows minutes and seconds, and hours once there are some`() {
        assertEquals("0:00", stopwatch(-4_000))
        assertEquals("0:42", stopwatch(42_900))
        assertEquals("12:05", stopwatch((12 * 60 + 5) * 1_000L))
        assertEquals("1:02:03", stopwatch(3_723_000))
    }
}
