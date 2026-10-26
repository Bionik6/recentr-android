package com.recentr.feature.rules.detail

import com.recentr.core.model.Rule
import com.recentr.core.model.RuleChange
import com.recentr.core.model.RuleSchedule
import com.recentr.core.model.RuleType
import com.recentr.feature.rules.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** What a rule's page offers: the one button at the bottom, and Turn off in its ⋯ menu. */
class DetailActionsTest {

    private val now = 1_800_000_000_000L
    private val session = Rule("s", RuleType.Session, "Study", durationMinutes = 30)
    private val limit = Rule("l", RuleType.Limit, "Social", limitMinutes = 30)
    private val lock = Rule("k", RuleType.Lock, "YouTube")

    @Test
    fun `a session or a limit not going starts, a lock that's off activates`() {
        assertEquals(R.string.rules_start, session.startLabel(now))
        assertEquals(R.string.rules_start, limit.startLabel(now))
        assertEquals(R.string.rules_activate, lock.startLabel(now))
    }

    @Test
    fun `once going, or for a routine, nothing is left at the bottom`() {
        assertNull(session.copy(activeSince = now - 60_000L).startLabel(now))
        assertNull(limit.copy(activeSince = now - 60_000L).startLabel(now))
        assertNull(lock.copy(activeSince = now - 60_000L).startLabel(now))
        assertNull(Rule("r", RuleType.Bedtime, "Bedtime", schedule = RuleSchedule(22 * 60, 7 * 60)).startLabel(now))
    }

    @Test
    fun `a lock that's on turns off from the menu, once`() {
        val on = lock.copy(activeSince = now - 60_000L)
        assertTrue(on.canTurnOff())
        // Turned off already: it goes off at midnight.
        assertFalse(on.copy(pending = RuleChange(now + 3_600_000L, on.copy(activeSince = null))).canTurnOff())
        assertFalse(lock.canTurnOff())
        assertFalse(session.copy(activeSince = now - 60_000L).canTurnOff())
    }
}
