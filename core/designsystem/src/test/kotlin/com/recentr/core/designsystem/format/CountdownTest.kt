package com.recentr.core.designsystem.format

import org.junit.Assert.assertEquals
import org.junit.Test

/** Countdowns read in hours and minutes, and in seconds only in their last minute. */
class CountdownTest {

    @Test
    fun `seconds show in the last minute only`() {
        assertEquals("0s", countdown(-5_000))
        assertEquals("0s", countdown(0))
        assertEquals("1s", countdown(200))
        assertEquals("42s", countdown(42_000))
        assertEquals("1m", countdown(59_400))
        assertEquals("1m", countdown(60_000))
        assertEquals("1m", countdown(119_000))
    }

    @Test
    fun `hours and minutes drop the seconds`() {
        assertEquals("24m", countdown((24 * 60 + 18) * 1_000L))
        assertEquals("59m", countdown(3_599_000))
        assertEquals("1h00m", countdown(3_600_000))
        assertEquals("1h30m", countdown(5_424_000))
        assertEquals("1h02m", countdown(3_723_000))
        assertEquals("26h00m", countdown(26 * 3_600_000L))
    }

    @Test
    fun `the focus timer's clock shows the seconds, and the hours from an hour on`() {
        assertEquals("00:00", clockCountdown(-5_000))
        assertEquals("00:00", clockCountdown(0))
        assertEquals("00:01", clockCountdown(200))
        assertEquals("01:00", clockCountdown(59_400))
        assertEquals("24:38", clockCountdown((24 * 60 + 38) * 1_000L))
        assertEquals("59:59", clockCountdown(3_599_000))
        assertEquals("01:00:00", clockCountdown(3_600_000))
        assertEquals("03:24:57", clockCountdown((3 * 3_600 + 24 * 60 + 57) * 1_000L))
    }
}
