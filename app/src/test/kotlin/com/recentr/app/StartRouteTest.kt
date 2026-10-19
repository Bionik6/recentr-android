package com.recentr.app

import com.recentr.core.designsystem.component.RecentrTab
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Where the app opens: the stored onboarding progress, and the debug QA shortcuts of a launch. */
class StartRouteTest {

    private fun opensOnOnboarding(stored: StartState, debuggable: Boolean, freshLaunch: Boolean, start: String?): Boolean =
        showsOnboarding(stored, QaShortcuts.of(debuggable, freshLaunch, start, tab = null).start)

    @Test
    fun `a finished onboarding opens Home, an unfinished one the onboarding`() {
        assertFalse(opensOnOnboarding(StartState.Main, debuggable = false, freshLaunch = true, start = null))
        assertTrue(opensOnOnboarding(StartState.Onboarding, debuggable = false, freshLaunch = true, start = null))
    }

    @Test
    fun `the QA shortcut picks the start of the launch that carries it`() {
        assertTrue(opensOnOnboarding(StartState.Main, debuggable = true, freshLaunch = true, start = "onboarding"))
        assertFalse(opensOnOnboarding(StartState.Onboarding, debuggable = true, freshLaunch = true, start = "main"))
    }

    @Test
    fun `a task brought back after the night doesn't replay an old onboarding shortcut`() {
        // Opened last night with "--es start onboarding", onboarding finished, process gone by
        // morning: the activity comes back with that intent, and must open Home.
        assertFalse(opensOnOnboarding(StartState.Main, debuggable = true, freshLaunch = false, start = "onboarding"))
        // Nor skip an onboarding left unfinished with an old "--es start main".
        assertTrue(opensOnOnboarding(StartState.Onboarding, debuggable = true, freshLaunch = false, start = "main"))
    }

    @Test
    fun `release builds ignore the shortcuts`() {
        assertFalse(opensOnOnboarding(StartState.Main, debuggable = false, freshLaunch = true, start = "onboarding"))
        assertEquals(QaShortcuts(), QaShortcuts.of(debuggable = false, freshLaunch = true, start = "main", tab = "rules"))
    }

    @Test
    fun `unknown starts and tabs are ignored`() {
        assertNull(QaShortcuts.of(debuggable = true, freshLaunch = true, start = "settings", tab = "squads").start)
        assertNull(QaShortcuts.of(debuggable = true, freshLaunch = true, start = null, tab = "squads").tab)
    }

    @Test
    fun `the tab shortcut follows the same rule`() {
        assertEquals(RecentrTab.Insights, QaShortcuts.of(debuggable = true, freshLaunch = true, start = "main", tab = "INSIGHTS").tab)
        assertNull(QaShortcuts.of(debuggable = true, freshLaunch = false, start = "main", tab = "insights").tab)
    }

    @Test
    fun `a screen restored on a finished onboarding goes to Home`() {
        // The QA shortcut left a screen on the onboarding; the profile finished it since.
        assertTrue(leavesRestoredOnboarding(showsOnboarding = false, restoredOnOnboarding = true))
    }

    @Test
    fun `an onboarding meant to run is never left`() {
        // Not finished yet, replayed, or asked for by a fresh QA shortcut.
        assertFalse(leavesRestoredOnboarding(showsOnboarding = true, restoredOnOnboarding = true))
        // Already on Home: nothing to do.
        assertFalse(leavesRestoredOnboarding(showsOnboarding = false, restoredOnOnboarding = false))
    }

    @Test
    fun `the full path of a phone left overnight on an old onboarding screen`() {
        // Launched last night with "--es start onboarding", finished elsewhere, process gone: the
        // screen is recreated with that intent (not a fresh launch) on its saved onboarding.
        val shows = showsOnboarding(StartState.Main, QaShortcuts.of(debuggable = true, freshLaunch = false, start = "onboarding", tab = null).start)
        assertFalse(shows)
        assertTrue(leavesRestoredOnboarding(shows, restoredOnOnboarding = true))
    }
}
