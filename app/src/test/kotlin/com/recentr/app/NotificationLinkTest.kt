package com.recentr.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Where a tapped notification opens Recentr: a rule's reminder opens the rule's page. */
class NotificationLinkTest {

    private val ruleId = "6f1c2a4e-8d0b-4c5e-9f7a-3b2d1e0c9a8b"

    @Test
    fun `a rule's notification carries its page`() {
        assertEquals("recentr://rules/$ruleId", NotificationLink.rule(ruleId))
        assertEquals(ruleId, NotificationLink.ruleId(NotificationLink.rule(ruleId)))
    }

    @Test
    fun `other links open no rule`() {
        listOf(null, "", "recentr://rules", "recentr://rules/", "recentr://home", "https://rules/$ruleId", "recentr://rules/$ruleId/edit")
            .forEach { assertNull("$it", NotificationLink.ruleId(it)) }
    }

    @Test
    fun `the launch a tap makes opens the rule's page`() {
        assertEquals(ruleId, NotificationLink.ruleToOpen(freshLaunch = true, link = NotificationLink.rule(ruleId)))
        // Opened from the launcher: nothing to open.
        assertNull(NotificationLink.ruleToOpen(freshLaunch = true, link = null))
    }

    @Test
    fun `a task brought back with the tap's intent doesn't open the page again`() {
        // Opened from the reminder last night, process gone by morning: the activity comes back
        // from Recents with that intent, on the screen it was left on.
        assertNull(NotificationLink.ruleToOpen(freshLaunch = false, link = NotificationLink.rule(ruleId)))
    }

    @Test
    fun `a rule's link opens its page over the tabs or over another page`() {
        assertTrue(NotificationLink.opensRulePage(onOnboarding = false, ruleShown = null, ruleId = ruleId))
        assertTrue(NotificationLink.opensRulePage(onOnboarding = false, ruleShown = "another", ruleId = ruleId))
    }

    @Test
    fun `the page already showing stays as it is`() {
        assertFalse(NotificationLink.opensRulePage(onOnboarding = false, ruleShown = ruleId, ruleId = ruleId))
    }

    @Test
    fun `the onboarding isn't left for a rule's page`() {
        assertFalse(NotificationLink.opensRulePage(onOnboarding = true, ruleShown = null, ruleId = ruleId))
    }
}
