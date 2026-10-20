package com.recentr.feature.rules.model

import com.recentr.core.data.UnlockStep
import com.recentr.core.data.unlockMethodEnabled
import com.recentr.core.data.unlockStep
import com.recentr.core.model.RuleType
import com.recentr.core.model.UnlockMethod
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** How a new rule's apps open anyway, and what it starts with: creating a rule only saves it. */
class NewRuleUnlocksTest {

    private var ids = 0
    private val factory = RuleFactory(string = { _, _ -> "Rule" }, newId = { "id-${ids++}" })
    private val now = 1_800_000_000_000L

    @Test
    fun `a new lock waits ten seconds, once activated`() {
        val lock = factory.newRule(RuleType.Lock)
        assertEquals(UnlockMethod.Wait, lock.unlockMethod)
        assertEquals(10, lock.waitSeconds)
        assertTrue(lock.unlockMethodEnabled)
        assertNull(lock.activeSince)
        assertFalse(lock.strictMode)
    }

    @Test
    fun `a new session waits ten seconds, and can change it`() {
        val session = factory.newRule(RuleType.Session)
        assertFalse(session.strictMode)
        assertNull(session.activeSince)
        assertNull(session.schedule)
        assertEquals(UnlockMethod.Wait, session.unlockMethod)
        assertEquals(10, session.waitSeconds)
        assertTrue(session.unlockMethodEnabled)
        // Turned strict, it has no unlocks; turned back, it waits again.
        assertFalse(session.copy(strictMode = true).unlockMethodEnabled)
        assertEquals(UnlockMethod.Wait, session.copy(strictMode = true).copy(strictMode = false).unlockMethod)
    }

    @Test
    fun `a new routine isn't strict, Settings decides`() {
        val routine = factory.newRule(RuleType.Bedtime)
        assertFalse(routine.strictMode)
        assertTrue(routine.unlockMethodEnabled)
        assertEquals(UnlockMethod.Wait, routine.unlockMethod)
        assertEquals(10, routine.waitSeconds)
        assertTrue(routine.canBeStrict)
    }

    @Test
    fun `a routine from a template keeps the explicit unlock`() {
        val routine = factory.fromTemplate(BuiltInTemplate.WindDown, installed = emptyList())
        assertEquals(RuleType.Bedtime, routine.type)
        assertEquals(UnlockMethod.Wait, routine.unlockMethod)
        assertFalse(routine.strictMode)
    }

    @Test
    fun `a new limit waits for Start, and has no unlocks but emergency ones`() {
        val limit = factory.newRule(RuleType.Limit)
        assertNull(limit.activeSince)
        assertFalse(limit.canBeStrict)
        assertEquals(2 * 60 + 15, limit.limitMinutes)
        assertEquals(UnlockStep.Emergency, limit.copy(activeSince = now).unlockStep(now, emergencyUnlocksLeft = 3))
    }
}
