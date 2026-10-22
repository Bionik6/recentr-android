package com.recentr.core.data

import com.recentr.core.model.AllowedChange
import com.recentr.core.model.AlwaysAllowed
import com.recentr.core.model.AppCategory
import com.recentr.core.model.FocusConfig
import com.recentr.core.model.FocusPhase
import com.recentr.core.model.FocusRun
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
    private val hour = 60 * minute
    private val midnight = LocalDate.of(2026, 10, 20).atStartOfDay(zone).toInstant().toEpochMilli()

    private val whatsApp = UsageApp("com.whatsapp", "WhatsApp", AppCategory.Social)
    private val tiktok = UsageApp("com.zhiliaoapp.musically", "TikTok", AppCategory.Social)
    private val settings = "com.android.settings"

    /** Activated at 8:00. */
    private val lock = Rule("l", RuleType.Lock, "WhatsApp", apps = listOf(whatsApp), activeSince = now - 2 * hour)

    /** An hour of WhatsApp and TikTok, started at 8:00. */
    private val limit = Rule("m", RuleType.Limit, "Social time", apps = listOf(whatsApp, tiktok), limitMinutes = 60, activeSince = now - 2 * hour)

    private fun state(
        rules: List<Rule> = emptyList(),
        focus: FocusRun? = null,
        unlocks: AppUnlocks = AppUnlocks(),
        usedSinceStart: Map<String, Int>? = null,
        alwaysAllowed: AlwaysAllowed = AlwaysAllowed(),
    ) = HoldState(
        rules = rules,
        focus = focus,
        protectedApps = setOf("com.recentr.app", settings),
        alwaysAllowed = alwaysAllowed,
        unlocks = unlocks,
        limitUsage = usedSinceStart?.let { LimitUsage(mapOf(limit.activeSince!! to it)) },
    )

    private fun HoldState.reason(packageName: String, at: Long = now) = reasonFor(packageName, at, zone)

    @Test
    fun `a lock holds its apps only, once activated`() {
        assertEquals(HoldReason.ByRule(lock), state(listOf(lock)).reason(whatsApp.packageName))
        assertNull(state(listOf(lock)).reason(tiktok.packageName))
        // Saved but not activated: it waits on its page.
        assertNull(state(listOf(lock.copy(activeSince = null))).reason(whatsApp.packageName))
        // On, it stays on: the next days too.
        assertTrue(state(listOf(lock)).reason(whatsApp.packageName, now + 3 * 24 * hour) is HoldReason.ByRule)
    }

    @Test
    fun `paused, disabled and protected apps are not held`() {
        val paused = Rule("p", RuleType.Bedtime, "P", apps = listOf(whatsApp), schedule = RuleSchedule(9 * 60, 11 * 60), pausedUntil = now + 10 * minute)
        val disabled = lock.copy(id = "d", enabled = false)
        assertNull(state(listOf(paused, disabled)).reason(whatsApp.packageName))

        val settingsApp = UsageApp(settings, "Settings", AppCategory.Other)
        assertNull(state(listOf(lock.copy(apps = listOf(settingsApp)))).reason(settings))
    }

    @Test
    fun `a session holds while it runs`() {
        val session = Rule("s", RuleType.Session, "Focus", apps = listOf(tiktok), durationMinutes = 30, activeSince = now, activeUntil = now + 30 * minute)
        assertTrue(state(listOf(session)).reason(tiktok.packageName) is HoldReason.ByRule)
        assertNull(state(listOf(session)).reason(tiktok.packageName, now + 31 * minute))
        assertEquals(now + 30 * minute, session.heldUntil(now, zone))
        // Saved, not started: nothing is held.
        assertNull(state(listOf(session.copy(activeSince = null, activeUntil = null))).reason(tiktok.packageName))
    }

    @Test
    fun `a routine holds its apps only, inside its window`() {
        val bedtime = Rule("b", RuleType.Bedtime, "Bedtime", apps = listOf(tiktok), schedule = RuleSchedule(9 * 60, 11 * 60))
        val holds = state(listOf(bedtime))
        assertTrue(holds.reason(tiktok.packageName) is HoldReason.ByRule)
        assertNull(holds.reason(whatsApp.packageName))
        assertNull(holds.reason(tiktok.packageName, now + 2 * hour))
        assertEquals(now + hour, bedtime.heldUntil(now, zone))
    }

    @Test
    fun `a limit holds once its apps used the budget since Start, until midnight`() {
        assertNull(state(listOf(limit), usedSinceStart = mapOf(whatsApp.packageName to 20, tiktok.packageName to 30)).reason(tiktok.packageName))
        val spent = state(listOf(limit), usedSinceStart = mapOf(whatsApp.packageName to 20, tiktok.packageName to 40))
        assertTrue(spent.reason(tiktok.packageName) is HoldReason.ByRule)
        assertTrue(spent.reason(tiktok.packageName, midnight - 1) is HoldReason.ByRule)
        // At midnight it's over, and it doesn't start again on its own.
        assertNull(spent.reason(tiktok.packageName, midnight))
        assertEquals(midnight, limit.heldUntil(now, zone))
    }

    @Test
    fun `a limit not started holds nothing`() {
        val waiting = limit.copy(activeSince = null, usedMinutes = 90)
        assertNull(state(listOf(waiting), usedSinceStart = mapOf(tiktok.packageName to 90)).reason(tiktok.packageName))
    }

    @Test
    fun `a limit goes by its last reading until its use is read`() {
        assertNull(state(listOf(limit.copy(usedMinutes = 59))).reason(tiktok.packageName))
        assertTrue(state(listOf(limit.copy(usedMinutes = 60))).reason(tiktok.packageName) is HoldReason.ByRule)
    }

    @Test
    fun `a limit's budget is spent by all its apps together`() {
        assertEquals(50, limit.usedOf(mapOf(whatsApp.packageName to 20, tiktok.packageName to 30, "com.other" to 90)))
        assertEquals(0, limit.usedOf(emptyMap()))
        // Same numbers as the card: the limit holds once they reach the budget, whichever app is opened.
        val used = mapOf(whatsApp.packageName to 59, tiktok.packageName to 1)
        assertEquals(60, LimitUsage(mapOf(limit.activeSince!! to used)).of(limit))
        assertTrue(state(listOf(limit), usedSinceStart = used).reason(whatsApp.packageName) is HoldReason.ByRule)
        // Another Start's use isn't this one's.
        assertNull(LimitUsage(mapOf(now to used)).of(limit))
        assertNull(LimitUsage(mapOf(now to used)).of(limit.copy(activeSince = null)))
    }

    @Test
    fun `open anyway lifts the rules until it runs out`() {
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
    fun `open anyway needs unlocks left, no strict mode, and never a limit`() {
        val wait = lock.copy(unlockMethod = UnlockMethod.Wait, unlocksPerDay = 2)
        assertTrue(wait.canOpenAnyway(now))
        assertFalse(wait.copy(unlocksUsedToday = 2).canOpenAnyway(now))
        assertTrue(wait.copy(unlocksPerDay = null, unlocksUsedToday = 9).canOpenAnyway(now))
        assertTrue(wait.copy(unlockMethod = UnlockMethod.ScanCode).canOpenAnyway(now))
        // A limit has no unlocks, whatever unlock method it was saved with.
        UnlockMethod.entries.forEach { assertFalse(limit.copy(unlockMethod = it, unlocksPerDay = null).canOpenAnyway(now, zone)) }
        val strictSession = Rule("s", RuleType.Session, "S", apps = listOf(whatsApp), durationMinutes = 30, activeSince = now, strictMode = true)
        assertFalse(strictSession.canOpenAnyway(now))
    }

    @Test
    fun `unlocking through a lock doesn't open a spent limit, only an emergency unlock does`() {
        val tiktokLock = lock.copy(apps = listOf(tiktok), unlockMethod = UnlockMethod.Wait)
        val spent = mapOf(tiktok.packageName to 75)
        val holds = state(listOf(tiktokLock, limit), usedSinceStart = spent)
        // The limit can't be opened anyway: it's the one shown, with the emergency unlock.
        assertEquals(HoldReason.ByRule(limit), holds.reason(tiktok.packageName))
        assertEquals(UnlockStep.Emergency, limit.unlockStep(now, emergencyUnlocksLeft = 3, zone = zone))
        assertNull(limit.unlockStep(now, emergencyUnlocksLeft = 0, zone = zone))
        assertEquals(listOf(tiktokLock), holds.liftedBy(tiktokLock, tiktok.packageName, now, zone))
        val open = AppUnlocks().open(tiktok.packageName, holds.liftedBy(tiktokLock, tiktok.packageName, now, zone), now + 5 * minute)
        assertEquals(HoldReason.ByRule(limit), state(listOf(tiktokLock, limit), unlocks = open, usedSinceStart = spent).reason(tiktok.packageName))
        // An emergency unlock gets past both.
        val emergency = AppUnlocks().open(tiktok.packageName, holds.holding(tiktok.packageName, now, zone), now + EmergencyUnlocks.MINUTES * minute)
        assertNull(state(listOf(tiktokLock, limit), unlocks = emergency, usedSinceStart = spent).reason(tiktok.packageName))
    }

    @Test
    fun `a strict routine keeps its apps on hold past its unlock method`() {
        val bedtime = Rule(
            "b", RuleType.Bedtime, "Bedtime", apps = listOf(tiktok),
            schedule = RuleSchedule(9 * 60, 11 * 60), strictMode = true, unlockMethod = UnlockMethod.Wait,
        )
        val tiktokLock = lock.copy(apps = listOf(tiktok), unlockMethod = UnlockMethod.Wait)
        val holds = state(listOf(tiktokLock, bedtime))
        assertEquals(HoldReason.ByRule(bedtime), holds.reason(tiktok.packageName))
        assertEquals(listOf(tiktokLock), holds.liftedBy(tiktokLock, tiktok.packageName, now, zone))
        // Not strict: its unlock method opens it like the lock's.
        val relaxed = bedtime.copy(strictMode = false)
        assertTrue(relaxed.canOpenAnyway(now, zone))
        assertEquals(setOf("l", "b"), state(listOf(tiktokLock, relaxed)).liftedBy(tiktokLock, tiktok.packageName, now, zone).map { it.id }.toSet())
    }

    @Test
    fun `the strictest rule wins`() {
        val openable = lock.copy(id = "a", unlockMethod = UnlockMethod.Wait)
        val closed = lock.copy(id = "b", unlockMethod = UnlockMethod.Wait, unlocksPerDay = 2, unlocksUsedToday = 2)
        assertEquals(HoldReason.ByRule(closed), state(listOf(openable, closed)).reason(whatsApp.packageName))
    }

    // The "Apps" sheet of a rule's page

    private val youTube = UsageApp("com.google.android.youtube", "YouTube", AppCategory.Entertainment)
    private val instagram = UsageApp("com.instagram.android", "Instagram", AppCategory.Social)

    /** On since 8:00: TikTok, WhatsApp, YouTube and Instagram, listed out of order. */
    private val socials = lock.copy(apps = listOf(whatsApp, youTube, tiktok, instagram))

    @Test
    fun `nothing unlocked, every app is blocked, by name`() {
        val apps = AppUnlocks().appsOf(socials, now)
        assertEquals(listOf(instagram, tiktok, whatsApp, youTube), apps.blocked)
        assertTrue(apps.unlocked.isEmpty())
    }

    @Test
    fun `apps unlocked past the rule are listed apart, with when their unlock ends`() {
        val unlocks = AppUnlocks()
            .open(youTube.packageName, listOf(socials), now + 5 * minute)
            .open(instagram.packageName, listOf(socials), now + 90 * minute)
        val apps = unlocks.appsOf(socials, now)
        assertEquals(listOf(tiktok, whatsApp), apps.blocked)
        assertEquals(listOf(UnlockedApp(instagram, now + 90 * minute), UnlockedApp(youTube, now + 5 * minute)), apps.unlocked)
        // Five minutes on, YouTube's unlock is over: back with the blocked apps.
        val later = unlocks.appsOf(socials, now + 5 * minute)
        assertEquals(listOf(tiktok, whatsApp, youTube), later.blocked)
        assertEquals(listOf(UnlockedApp(instagram, now + 90 * minute)), later.unlocked)
        // Instagram locked again: only YouTube is still unlocked.
        assertEquals(listOf(youTube), unlocks.lockAgain(instagram.packageName, now + minute).appsOf(socials, now + minute).unlocked.map { it.app })
    }

    @Test
    fun `another rule's unlock doesn't count`() {
        val other = Rule("o", RuleType.Lock, "Other", apps = listOf(youTube), activeSince = now)
        val unlocks = AppUnlocks().open(youTube.packageName, listOf(other), now + 5 * minute)
        val apps = unlocks.appsOf(socials, now)
        assertEquals(listOf(instagram, tiktok, whatsApp, youTube), apps.blocked)
        assertTrue(apps.unlocked.isEmpty())
        assertEquals(now + 5 * minute, unlocks.endOf(youTube.packageName, other, now))
        assertNull(unlocks.endOf(youTube.packageName, socials, now))
    }

    // Always allowed

    private val focusRun = FocusRun(
        config = FocusConfig(blockedApps = listOf(whatsApp, tiktok), strictMode = true),
        startedAt = now,
        round = 1,
        phase = FocusPhase.Focus,
        phaseEndsAt = now + 25 * minute,
        pausedRemainingMillis = null,
    )

    @Test
    fun `an app always allowed is never held, by any rule or focus session`() {
        val session = Rule("s", RuleType.Session, "Focus", apps = listOf(whatsApp), durationMinutes = 30, activeSince = now, activeUntil = now + 30 * minute, strictMode = true)
        val routine = Rule("b", RuleType.Bedtime, "Work", apps = listOf(whatsApp), schedule = RuleSchedule(9 * 60, 11 * 60), strictMode = true)
        val allowed = AlwaysAllowed(listOf(whatsApp))
        val rules = listOf(lock, session, routine, limit)
        val holds = state(rules, focus = focusRun, usedSinceStart = mapOf(whatsApp.packageName to 90), alwaysAllowed = allowed)
        assertNull(holds.reason(whatsApp.packageName))
        assertTrue(holds.holding(whatsApp.packageName, now, zone).isEmpty())
        // The others' apps stay on hold.
        assertEquals(HoldReason.Focus(focusRun), holds.reason(tiktok.packageName))
        assertTrue(state(rules, usedSinceStart = mapOf(tiktok.packageName to 90), alwaysAllowed = allowed).reason(tiktok.packageName) is HoldReason.ByRule)
        // Without it, every one of them holds WhatsApp.
        assertEquals(4, state(rules, usedSinceStart = mapOf(whatsApp.packageName to 90)).holding(whatsApp.packageName, now, zone).size)
    }

    @Test
    fun `an app waiting to be always allowed stays held until it joins`() {
        val waiting = AlwaysAllowed(pending = AllowedChange(midnight, listOf(whatsApp)))
        assertEquals(HoldReason.ByRule(lock), state(listOf(lock), alwaysAllowed = waiting).reason(whatsApp.packageName))
        assertTrue(state(listOf(lock), alwaysAllowed = waiting).reason(whatsApp.packageName, midnight - 1) is HoldReason.ByRule)
        assertNull(state(listOf(lock), alwaysAllowed = waiting).reason(whatsApp.packageName, midnight))
    }

    @Test
    fun `a rule's apps always allowed are listed apart`() {
        val unlocks = AppUnlocks().open(youTube.packageName, listOf(socials), now + 5 * minute).open(tiktok.packageName, listOf(socials), now + 5 * minute)
        val apps = unlocks.appsOf(socials, now, AlwaysAllowed(listOf(whatsApp, tiktok)))
        assertEquals(listOf(instagram), apps.blocked)
        assertEquals(listOf(UnlockedApp(youTube, now + 5 * minute)), apps.unlocked)
        assertEquals(listOf(tiktok, whatsApp), apps.allowed)
        // Waiting to join, WhatsApp is still blocked.
        val waiting = AlwaysAllowed(pending = AllowedChange(midnight, listOf(whatsApp)))
        assertEquals(listOf(instagram, tiktok, whatsApp, youTube), AppUnlocks().appsOf(socials, now, waiting).blocked)
        assertEquals(listOf(whatsApp), AppUnlocks().appsOf(socials, midnight, waiting).allowed)
    }
}
