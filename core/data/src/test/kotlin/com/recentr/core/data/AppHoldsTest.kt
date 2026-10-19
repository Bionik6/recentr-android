package com.recentr.core.data

import com.recentr.core.model.AppCategory
import com.recentr.core.model.FocusConfig
import com.recentr.core.model.FocusPhase
import com.recentr.core.model.FocusRun
import com.recentr.core.model.HabitTask
import com.recentr.core.model.LimitAction
import com.recentr.core.model.LimitReset
import com.recentr.core.model.Rule
import com.recentr.core.model.RuleSchedule
import com.recentr.core.model.RuleType
import com.recentr.core.model.UnlockMethod
import com.recentr.core.model.UsageApp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class AppHoldsTest {

    private val zone = ZoneId.of("Europe/Paris")
    /** Monday 19 Oct 2026, 10:00. */
    private val now = LocalDate.of(2026, 10, 19).atTime(10, 0).atZone(zone).toInstant().toEpochMilli()
    private val minute = 60_000L

    private val whatsApp = UsageApp("com.whatsapp", "WhatsApp", AppCategory.Social)
    private val tiktok = UsageApp("com.zhiliaoapp.musically", "TikTok", AppCategory.Social)
    private val settings = "com.android.settings"

    private fun state(
        rules: List<Rule> = emptyList(),
        focus: FocusRun? = null,
        unlocks: AppUnlocks = AppUnlocks(),
        usedToday: Map<String, Int>? = null,
        usedThisWeek: Map<String, Int>? = null,
    ) = HoldState(
        rules = rules,
        focus = focus,
        protectedApps = setOf("com.recentr.app", settings),
        unlocks = unlocks,
        limitUsage = if (usedToday == null && usedThisWeek == null) {
            null
        } else {
            LimitUsage(
                listOfNotNull(usedToday?.let { LimitReset.Midnight to it }, usedThisWeek?.let { LimitReset.Monday to it }).toMap(),
            )
        },
    )

    private fun HoldState.reason(packageName: String, at: Long = now) = reasonFor(packageName, at, zone)

    @Test
    fun `a lock holds its apps only`() {
        val lock = Rule("l", RuleType.Lock, "WhatsApp", apps = listOf(whatsApp))
        assertEquals(HoldReason.ByRule(lock), state(listOf(lock)).reason(whatsApp.packageName))
        assertNull(state(listOf(lock)).reason(tiktok.packageName))
    }

    @Test
    fun `paused, disabled and protected apps are not held`() {
        val paused = Rule("p", RuleType.Lock, "P", apps = listOf(whatsApp), pausedUntil = now + 10 * minute)
        val disabled = Rule("d", RuleType.Lock, "D", apps = listOf(whatsApp), enabled = false)
        assertNull(state(listOf(paused, disabled)).reason(whatsApp.packageName))

        val settingsApp = UsageApp(settings, "Settings", AppCategory.Other)
        assertNull(state(listOf(Rule("s", RuleType.Lock, "S", apps = listOf(settingsApp)))).reason(settings))
    }

    @Test
    fun `a session holds while it runs`() {
        val session = Rule("s", RuleType.Session, "Focus", apps = listOf(tiktok), durationMinutes = 30, activeSince = now)
        assertTrue(state(listOf(session)).reason(tiktok.packageName) is HoldReason.ByRule)
        assertNull(state(listOf(session)).reason(tiktok.packageName, now + 31 * minute))
        assertEquals(now + 30 * minute, session.heldUntil(now, zone))
    }

    @Test
    fun `a scheduled session holds inside its window`() {
        val workHours = Rule("w", RuleType.Session, "Work", apps = listOf(tiktok), schedule = RuleSchedule(9 * 60, 17 * 60, (1..5).toSet()))
        assertTrue(state(listOf(workHours)).reason(tiktok.packageName) is HoldReason.ByRule)
        assertNull(state(listOf(workHours)).reason(tiktok.packageName, now + 8 * 60 * minute))
        assertEquals(now + 7 * 60 * minute, workHours.heldUntil(now, zone))
    }

    @Test
    fun `a routine holds its apps only, inside its window`() {
        val bedtime = Rule("b", RuleType.Bedtime, "Bedtime", apps = listOf(tiktok), schedule = RuleSchedule(9 * 60, 11 * 60))
        val holds = state(listOf(bedtime))
        assertTrue(holds.reason(tiktok.packageName) is HoldReason.ByRule)
        assertNull(holds.reason(whatsApp.packageName))
        assertNull(holds.reason(tiktok.packageName, now + 2 * 60 * minute))
    }

    @Test
    fun `a limit holds once its apps used the daily budget`() {
        val limit = Rule("m", RuleType.Limit, "Social time", apps = listOf(whatsApp, tiktok), limitMinutes = 60)
        assertNull(state(listOf(limit), usedToday = mapOf(whatsApp.packageName to 20, tiktok.packageName to 30)).reason(tiktok.packageName))
        assertTrue(state(listOf(limit), usedToday = mapOf(whatsApp.packageName to 20, tiktok.packageName to 40)).reason(tiktok.packageName) is HoldReason.ByRule)
        // Unknown use (no usage access) or a reminder-only limit never holds.
        assertNull(state(listOf(limit)).reason(tiktok.packageName))
        val remind = limit.copy(limitAction = LimitAction.Remind)
        assertNull(state(listOf(remind), usedToday = mapOf(tiktok.packageName to 90)).reason(tiktok.packageName))
    }

    @Test
    fun `a limit's budget is spent by all its apps together`() {
        val limit = Rule("m", RuleType.Limit, "Social time", apps = listOf(whatsApp, tiktok), limitMinutes = 60)
        assertEquals(50, limit.usedOf(mapOf(whatsApp.packageName to 20, tiktok.packageName to 30, "com.other" to 90)))
        assertEquals(0, limit.usedOf(emptyMap()))
        // Same numbers as the card: the limit holds once they reach the budget, whichever app is opened.
        val used = mapOf(whatsApp.packageName to 59, tiktok.packageName to 1)
        assertEquals(60, limit.usedOf(used))
        assertTrue(state(listOf(limit), usedToday = used).reason(whatsApp.packageName) is HoldReason.ByRule)
    }

    @Test
    fun `a weekly limit holds on the week's use, not today's`() {
        val weekly = Rule("w", RuleType.Limit, "TikTok week", apps = listOf(tiktok), limitMinutes = 300, limitReset = LimitReset.Monday)
        val daily = weekly.copy(id = "d", limitMinutes = 60, limitReset = LimitReset.Midnight)
        val today = mapOf(tiktok.packageName to 20)
        val week = mapOf(tiktok.packageName to 300)
        assertEquals(HoldReason.ByRule(weekly), state(listOf(weekly), usedToday = today, usedThisWeek = week).reason(tiktok.packageName))
        assertNull(state(listOf(daily), usedToday = today, usedThisWeek = week).reason(tiktok.packageName))
        // The week not read yet: unknown, so it doesn't hold.
        assertNull(state(listOf(weekly), usedToday = week).reason(tiktok.packageName))
    }

    @Test
    fun `a habit holds until its tasks are done`() {
        val habit = Rule("h", RuleType.Habit, "Before TikTok", apps = listOf(tiktok), habitTasks = listOf(HabitTask("t", "Read")))
        assertTrue(state(listOf(habit)).reason(tiktok.packageName) is HoldReason.ByRule)
        val done = habit.copy(habitTasks = listOf(HabitTask("t", "Read", done = true)))
        assertNull(state(listOf(done)).reason(tiktok.packageName))
    }

    @Test
    fun `open anyway lifts the rules until it runs out`() {
        val lock = Rule("l", RuleType.Lock, "WhatsApp", apps = listOf(whatsApp))
        val open = AppUnlocks().open(whatsApp.packageName, listOf(lock), now + 5 * minute)
        assertNull(state(listOf(lock), unlocks = open).reason(whatsApp.packageName))
        assertTrue(state(listOf(lock), unlocks = open).reason(whatsApp.packageName, now + 6 * minute) is HoldReason.ByRule)
    }

    @Test
    fun `focus rounds hold the session's apps, breaks don't`() {
        val run = FocusRun(
            config = FocusConfig(blockedApps = listOf(tiktok)),
            startedAt = now,
            round = 1,
            phase = FocusPhase.Focus,
            phaseEndsAt = now + 45 * minute,
            pausedRemainingMillis = null,
        )
        assertEquals(HoldReason.Focus(run), state(focus = run).reason(tiktok.packageName))
        // Opening anyway is for rules only.
        assertEquals(HoldReason.Focus(run), state(focus = run, unlocks = AppUnlocks().open(tiktok.packageName, emptyList(), now + minute)).reason(tiktok.packageName))
        assertNull(state(focus = run.copy(phase = FocusPhase.Break)).reason(tiktok.packageName))
        assertNull(state(focus = run).reason(whatsApp.packageName))
    }

    @Test
    fun `open anyway needs an unlock method, unlocks left and no strict mode`() {
        val wait = Rule("l", RuleType.Lock, "L", apps = listOf(whatsApp), unlockMethod = UnlockMethod.Wait, unlocksPerDay = 2)
        assertTrue(wait.canOpenAnyway(now))
        assertFalse(wait.copy(unlocksUsedToday = 2).canOpenAnyway(now))
        assertTrue(wait.copy(unlocksPerDay = null, unlocksUsedToday = 9).canOpenAnyway(now))
        // "No unlocks" is Strict mode's: a lock left with it waits a few seconds.
        assertTrue(wait.copy(unlockMethod = UnlockMethod.None).canOpenAnyway(now))
        assertTrue(wait.copy(unlockMethod = UnlockMethod.ScanCode).canOpenAnyway(now))
        val askReason = Rule("m", RuleType.Limit, "M", apps = listOf(whatsApp), unlockMethod = UnlockMethod.None, limitAction = LimitAction.AskReason)
        assertTrue(askReason.canOpenAnyway(now))
        // A limit that locks can't be opened, whatever unlock method it was saved with.
        assertFalse(askReason.copy(limitAction = LimitAction.Lock, unlockMethod = UnlockMethod.Wait).canOpenAnyway(now))
        val strictSession = Rule("s", RuleType.Session, "S", apps = listOf(whatsApp), durationMinutes = 30, activeSince = now, strictMode = true)
        assertFalse(strictSession.copy(unlockMethod = UnlockMethod.Wait).canOpenAnyway(now))
    }

    @Test
    fun `unlocking through a lock doesn't open a spent limit that locks`() {
        val lock = Rule("l", RuleType.Lock, "TikTok", apps = listOf(tiktok), unlockMethod = UnlockMethod.Wait)
        val limit = Rule("m", RuleType.Limit, "Social time", apps = listOf(tiktok), limitMinutes = 30, limitAction = LimitAction.Lock)
        val spent = mapOf(tiktok.packageName to 45)
        val holds = state(listOf(lock, limit), usedToday = spent)
        // The limit can't be opened anyway: it's the one shown.
        assertEquals(HoldReason.ByRule(limit), holds.reason(tiktok.packageName))
        assertEquals(listOf(lock), holds.liftedBy(lock, tiktok.packageName, now, zone))
        val open = AppUnlocks().open(tiktok.packageName, holds.liftedBy(lock, tiktok.packageName, now, zone), now + 5 * minute)
        assertEquals(HoldReason.ByRule(limit), state(listOf(lock, limit), unlocks = open, usedToday = spent).reason(tiktok.packageName))
        // Asking why instead: both are lifted.
        val asks = limit.copy(limitAction = LimitAction.AskReason)
        val liftedWithReason = state(listOf(lock, asks), usedToday = spent).liftedBy(asks, tiktok.packageName, now, zone)
        assertEquals(setOf("l", "m"), liftedWithReason.map { it.id }.toSet())
    }

    @Test
    fun `a strict routine keeps its apps on hold past its unlock method`() {
        val bedtime = Rule(
            "b", RuleType.Bedtime, "Bedtime", apps = listOf(tiktok),
            schedule = RuleSchedule(9 * 60, 11 * 60), strictMode = true, unlockMethod = UnlockMethod.Wait,
        )
        val lock = Rule("l", RuleType.Lock, "TikTok", apps = listOf(tiktok), unlockMethod = UnlockMethod.Wait)
        val holds = state(listOf(lock, bedtime))
        assertEquals(HoldReason.ByRule(bedtime), holds.reason(tiktok.packageName))
        assertEquals(listOf(lock), holds.liftedBy(lock, tiktok.packageName, now, zone))
        // Not strict: its unlock method opens it like the lock's.
        val relaxed = bedtime.copy(strictMode = false)
        assertTrue(relaxed.canOpenAnyway(now, zone))
        assertEquals(setOf("l", "b"), state(listOf(lock, relaxed)).liftedBy(lock, tiktok.packageName, now, zone).map { it.id }.toSet())
    }

    @Test
    fun `the strictest rule wins`() {
        val openable = Rule("a", RuleType.Lock, "A", apps = listOf(whatsApp), unlockMethod = UnlockMethod.Wait)
        val closed = Rule("b", RuleType.Lock, "B", apps = listOf(whatsApp), unlockMethod = UnlockMethod.Wait, unlocksPerDay = 2, unlocksUsedToday = 2)
        assertEquals(HoldReason.ByRule(closed), state(listOf(openable, closed)).reason(whatsApp.packageName))
    }
}
