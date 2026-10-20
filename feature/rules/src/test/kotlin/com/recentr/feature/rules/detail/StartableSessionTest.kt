package com.recentr.feature.rules.detail

import com.recentr.core.model.Rule
import com.recentr.core.model.RuleSchedule
import com.recentr.core.model.RuleType
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** When a rule's detail offers "Start": a saved session that isn't running. */
class StartableSessionTest {

    private val now = 1_800_000_000_000L
    private val session = Rule("s", RuleType.Session, "Study", durationMinutes = 30)

    @Test
    fun `a saved session can be started again, paused or not`() {
        assertTrue(session.startable(now))
        assertTrue(session.copy(pausedUntil = Long.MAX_VALUE).startable(now))
        // Ended a while ago.
        assertTrue(session.copy(activeSince = now - 40 * 60_000L).startable(now))
    }

    @Test
    fun `a running session, a scheduled one or another rule can't`() {
        assertFalse(session.copy(activeSince = now - 60_000L).startable(now))
        assertFalse(session.copy(schedule = RuleSchedule(9 * 60, 17 * 60)).startable(now))
        assertFalse(Rule("r", RuleType.Bedtime, "Bedtime", schedule = RuleSchedule(22 * 60, 7 * 60)).startable(now))
        assertFalse(Rule("l", RuleType.Lock, "YouTube").startable(now))
    }
}
