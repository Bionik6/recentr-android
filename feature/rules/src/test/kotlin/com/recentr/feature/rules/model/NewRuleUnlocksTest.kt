package com.recentr.feature.rules.model

import com.recentr.core.data.unlockMethodEnabled
import com.recentr.core.data.unlockMethodInUse
import com.recentr.core.model.LimitAction
import com.recentr.core.model.RuleType
import com.recentr.core.model.UnlockMethod
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** How a new rule's apps open anyway: what its editor shows is what the hold screen asks. */
class NewRuleUnlocksTest {

    private var ids = 0
    private val factory = RuleFactory(string = { _, _ -> "Rule" }, newId = { "id-${ids++}" })

    @Test
    fun `a new lock waits a few seconds`() {
        val lock = factory.newRule(RuleType.Lock)
        assertEquals(UnlockMethod.Wait, lock.unlockMethodInUse)
        assertEquals(5, lock.waitSeconds)
        assertTrue(lock.unlockMethodEnabled)
    }

    @Test
    fun `a new session waits five seconds, and can change it`() {
        val session = factory.newRule(RuleType.Session)
        assertFalse(session.strictMode)
        assertEquals(UnlockMethod.Wait, session.unlockMethod)
        assertEquals(5, session.waitSeconds)
        assertTrue(session.unlockMethodEnabled)
        // Turned strict, it has no unlocks; turned back, it waits again.
        assertFalse(session.copy(strictMode = true).unlockMethodEnabled)
        assertEquals(UnlockMethod.Wait, session.copy(strictMode = true).copy(strictMode = false).unlockMethodInUse)
    }

    @Test
    fun `a new routine starts strict, with a wait for when it isn't`() {
        val routine = factory.newRule(RuleType.Bedtime)
        assertTrue(routine.strictMode)
        assertFalse(routine.unlockMethodEnabled)
        val relaxed = routine.copy(strictMode = false)
        assertTrue(relaxed.unlockMethodEnabled)
        assertEquals(UnlockMethod.Wait, relaxed.unlockMethodInUse)
        assertEquals(5, relaxed.waitSeconds)
    }

    @Test
    fun `a routine from a template keeps the explicit unlock`() {
        val routine = factory.fromTemplate(BuiltInTemplate.WindDown, installed = emptyList())
        assertEquals(UnlockMethod.Wait, routine.unlockMethod)
        assertTrue(routine.strictMode)
    }

    @Test
    fun `a new limit answers with When time is up`() {
        val limit = factory.newRule(RuleType.Limit)
        assertEquals(LimitAction.Lock, limit.limitAction)
        assertEquals(UnlockMethod.None, limit.unlockMethodInUse)
        assertEquals(UnlockMethod.Reason, limit.copy(limitAction = LimitAction.AskReason).unlockMethodInUse)
    }
}
