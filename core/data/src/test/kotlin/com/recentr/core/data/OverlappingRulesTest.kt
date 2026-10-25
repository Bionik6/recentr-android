package com.recentr.core.data

import com.recentr.core.model.AppCategory
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

/**
 * Several rules holding the same app: a Bedtime routine, a limit and a lock all on YouTube. The
 * strictest rule is the one shown, and an unlock only lets the app past the rules that held it
 * when it was unlocked.
 */
class OverlappingRulesTest {

    private val zone = ZoneId.of("Europe/Paris")
    private val minute = 60_000L

    /** Monday 19 Oct 2026 at [hour]:[minutes]. */
    private fun at(hour: Int, minutes: Int = 0): Long =
        LocalDate.of(2026, 10, 19).atTime(hour, minutes).atZone(zone).toInstant().toEpochMilli()

    private val youTube = UsageApp("com.google.android.youtube", "YouTube", AppCategory.Entertainment)
    private val tiktok = UsageApp("com.zhiliaoapp.musically", "TikTok", AppCategory.Social)
    private val yt = youTube.packageName

    /** Every night from 21:30 to 7:00, in Strict mode. */
    private val bedtime = Rule(
        "bedtime",
        RuleType.Bedtime,
        "Bedtime",
        apps = listOf(youTube, tiktok),
        schedule = RuleSchedule(21 * 60 + 30, 7 * 60),
        strictMode = true,
    )

    /** The same routine without Strict mode: a 5-second wait unlocks it. */
    private val softBedtime = bedtime.copy(strictMode = false, unlockMethod = UnlockMethod.Wait)

    /** One hour of YouTube, started at 8:00: once it's used, the app is held until midnight. */
    private val limit = Rule(
        "limit",
        RuleType.Limit,
        "YouTube",
        apps = listOf(youTube),
        limitMinutes = 60,
        activeSince = at(8),
    )

    /** On since 8:00, five unlocks a day. */
    private val lock = Rule("lock", RuleType.Lock, "No YouTube", apps = listOf(youTube), activeSince = at(8), unlockMethod = UnlockMethod.Wait)

    private fun state(rules: List<Rule>, used: Int = 0, unlocks: AppUnlocks = AppUnlocks()) = HoldState(
        rules = rules,
        focus = null,
        protectedApps = setOf("com.recentr.app"),
        unlocks = unlocks,
        limitUsage = usedSinceStart(used),
    )

    private fun usedSinceStart(minutes: Int) = LimitUsage(mapOf(limit.activeSince!! to mapOf(yt to minutes)))

    private fun HoldState.reason(at: Long, packageName: String = yt) = reasonFor(packageName, at, zone)

    /** What "Unlock anyway" through [rule] does ([AppHoldRepository.openAnyway]). */
    private fun HoldState.unlockThrough(rule: Rule, at: Long, minutes: Int): HoldState =
        copy(unlocks = unlocks.open(yt, liftedBy(rule, yt, at, zone), at + minutes * minute))

    /** What an emergency unlock does ([AppHoldRepository.emergencyUnlock]). */
    private fun HoldState.emergencyUnlock(at: Long): HoldState =
        copy(unlocks = unlocks.open(yt, holding(yt, at, zone), at + EmergencyUnlocks.MINUTES * minute))

    @Test
    fun `the routine holds the app while the limit still has time left`() {
        assertEquals(HoldReason.ByRule(bedtime), state(listOf(limit, bedtime), used = 0).reason(at(22, 50)))
        assertEquals(HoldReason.ByRule(bedtime), state(listOf(bedtime, limit), used = 59).reason(at(22, 50)))
    }

    @Test
    fun `the strict routine wins over a used-up limit, whatever the order`() {
        assertEquals(HoldReason.ByRule(bedtime), state(listOf(limit, bedtime), used = 75).reason(at(22, 50)))
        assertEquals(HoldReason.ByRule(bedtime), state(listOf(bedtime, limit), used = 75).reason(at(22, 50)))
        // So the shield offers the emergency unlock only.
        assertEquals(UnlockStep.Emergency, bedtime.unlockStep(at(22, 50), emergencyUnlocksLeft = 3, zone))
        assertNull(bedtime.unlockStep(at(22, 50), emergencyUnlocksLeft = 0, zone))
    }

    @Test
    fun `the used-up limit holds the app outside the routine, past an emergency unlock only`() {
        val holds = state(listOf(limit, bedtime), used = 75)
        assertEquals(HoldReason.ByRule(limit), holds.reason(at(20, 0)))
        assertEquals(UnlockStep.Emergency, limit.unlockStep(at(20, 0), emergencyUnlocksLeft = 3, zone))
        assertNull(state(listOf(limit, bedtime), used = 30).reason(at(20, 0)))
    }

    @Test
    fun `a used-up limit can't be unlocked through another rule`() {
        val holds = state(listOf(softBedtime, limit), used = 75)
        // Inside the soft routine: the limit is the one without "Unlock anyway", whatever the order.
        assertEquals(HoldReason.ByRule(limit), holds.reason(at(22, 0)))
        assertEquals(HoldReason.ByRule(limit), state(listOf(limit, softBedtime), used = 75).reason(at(22, 0)))
        // Unlocking through the routine leaves it holding.
        assertEquals(HoldReason.ByRule(limit), holds.unlockThrough(softBedtime, at(22, 0), minutes = 15).reason(at(22, 5)))
    }

    @Test
    fun `an emergency unlock gets past the strict routine and the limit, for five minutes`() {
        val unlocked = state(listOf(limit, bedtime), used = 75).emergencyUnlock(at(22, 50))
        assertNull(unlocked.reason(at(22, 51)))
        assertEquals(HoldReason.ByRule(bedtime), unlocked.reason(at(22, 55)))
    }

    @Test
    fun `a paused routine leaves the app to the limit`() {
        val paused = bedtime.copy(pausedUntil = at(23, 0))
        assertEquals(HoldReason.ByRule(limit), state(listOf(paused, limit), used = 75).reason(at(22, 50)))
        assertNull(state(listOf(paused, limit), used = 10).reason(at(22, 50)))
    }

    @Test
    fun `an emergency unlock past the limit doesn't open the app past the routine starting during it`() {
        // 21:27: the hour of YouTube is used up and unlocked in an emergency.
        val unlocked = state(listOf(limit, bedtime), used = 75).emergencyUnlock(at(21, 27))
        assertNull(unlocked.reason(at(21, 29)))
        // 21:30: Bedtime starts and puts YouTube on hold, the unlock notwithstanding.
        assertEquals(HoldReason.ByRule(bedtime), unlocked.reason(at(21, 30)))
    }

    @Test
    fun `an unlock through the routine doesn't open the app past the limit running out during it`() {
        val holds = state(listOf(softBedtime, limit), used = 50)
        assertEquals(HoldReason.ByRule(softBedtime), holds.reason(at(22, 0)))
        val unlocked = holds.unlockThrough(softBedtime, at(22, 0), minutes = 30)
        assertNull(unlocked.reason(at(22, 10)))
        // The hour runs out while the routine's unlock still runs.
        assertEquals(HoldReason.ByRule(limit), unlocked.copy(limitUsage = usedSinceStart(60)).reason(at(22, 10)))
    }

    @Test
    fun `one unlock lifts every rule holding the app that could be unlocked`() {
        val holds = state(listOf(lock, softBedtime))
        assertEquals(HoldReason.ByRule(lock), holds.reason(at(22, 0)))
        val unlocked = holds.unlockThrough(lock, at(22, 0), minutes = 15)
        assertNull(unlocked.reason(at(22, 0)))
        assertNull(unlocked.reason(at(22, 14)))
        assertEquals(HoldReason.ByRule(lock), unlocked.reason(at(22, 15)))
    }

    @Test
    fun `an unlock never lifts a rule that can't be unlocked`() {
        val now = at(22, 0)
        // Strict mode: the lock's unlock leaves the routine holding.
        assertEquals(listOf(lock), state(listOf(lock, bedtime)).liftedBy(lock, yt, now, zone))
        assertEquals(HoldReason.ByRule(bedtime), state(listOf(lock, bedtime)).unlockThrough(lock, now, minutes = 15).reason(now))
        // A lock with no unlocks left today.
        val spent = lock.copy(id = "spent", unlocksPerDay = 2, unlocksUsedToday = 2)
        assertEquals(HoldReason.ByRule(spent), state(listOf(softBedtime, spent)).unlockThrough(softBedtime, now, minutes = 15).reason(now))
        // A limit.
        assertEquals(HoldReason.ByRule(limit), state(listOf(lock, limit), used = 75).unlockThrough(lock, now, minutes = 15).reason(now))
    }

    @Test
    fun `an emergency unlock gets past the rules holding the app, not a routine starting after it`() {
        val late = Rule("late", RuleType.Bedtime, "No YouTube late", apps = listOf(youTube), schedule = RuleSchedule(22 * 60 + 52, 23 * 60 + 30))
        val unlocked = state(listOf(bedtime, limit, late), used = 75).emergencyUnlock(at(22, 50))
        assertNull(unlocked.reason(at(22, 51)))
        assertEquals(HoldReason.ByRule(late), unlocked.reason(at(22, 52)))
    }

    /**
     * A session's unlocks don't run out: only a lock counts them. Five unlocks through the session
     * still leave it unlockable, and the routine starting meanwhile is lifted with it.
     */
    @Test
    fun `a session unlocked five times still unlocks, and lifts the routine with it`() {
        val session = Rule(
            "session", RuleType.Session, "Focus", apps = listOf(youTube), durationMinutes = 120,
            activeSince = at(21, 0), unlocksUsedToday = 5, unlockMethod = UnlockMethod.Wait,
        )
        val holds = state(listOf(session, softBedtime))
        assertEquals(HoldReason.ByRule(session), holds.reason(at(22, 0)))
        assertEquals(UnlockStep.Wait, session.unlockStep(at(22, 0), emergencyUnlocksLeft = 3, zone))
        assertEquals(listOf(session, softBedtime), holds.liftedBy(session, yt, at(22, 0), zone))
        assertNull(holds.unlockThrough(session, at(22, 0), minutes = 5).reason(at(22, 4)))
    }

    /** A lock that's run out is the one shown: the routine's unlock would leave it holding. */
    @Test
    fun `a lock with no unlocks left wins over a routine`() {
        val spent = lock.copy(unlocksPerDay = 5, unlocksUsedToday = 5)
        assertEquals(HoldReason.ByRule(spent), state(listOf(softBedtime, spent)).reason(at(22, 0)))
        assertNull(spent.unlockStep(at(22, 0), emergencyUnlocksLeft = 3, zone))
        assertEquals(HoldReason.ByRule(softBedtime), state(listOf(softBedtime, lock)).reason(at(22, 0)))
    }

    @Test
    fun `an unlock opens one app only`() {
        val unlocked = state(listOf(softBedtime)).unlockThrough(softBedtime, at(22, 0), minutes = 15)
        assertNull(unlocked.reason(at(22, 5)))
        assertEquals(HoldReason.ByRule(softBedtime), unlocked.reason(at(22, 5), tiktok.packageName))
    }

    @Test
    fun `lock it again puts every rule back`() {
        val unlocked = state(listOf(lock, softBedtime)).unlockThrough(lock, at(22, 0), minutes = 15)
        val lockedAgain = unlocked.copy(unlocks = unlocked.unlocks.lockAgain(yt, at(22, 5)))
        assertEquals(HoldReason.ByRule(lock), lockedAgain.reason(at(22, 5)))
        // An ended unlock stays ended.
        assertEquals(unlocked.unlocks.lockAgain(yt, at(22, 20)), unlocked.unlocks.lockAgain(yt, at(22, 20)).lockAgain(yt, at(22, 30)))
    }

    @Test
    fun `a new unlock never shortens one still running`() {
        val long = AppUnlocks().open(yt, listOf(lock), at(22, 0))
        val both = long.open(yt, listOf(lock, softBedtime), at(21, 5))
        assertTrue(both.lifts(yt, lock, at(21, 30)))
        assertFalse(both.lifts(yt, softBedtime, at(21, 30)))
    }

    @Test
    fun `stayed off counts from the end of the rule's own last unlock`() {
        val unlocks = AppUnlocks().open(yt, listOf(lock), at(21, 44))
        assertNull(unlocks.lastEnd(yt, lock, at(21, 40)))
        assertEquals(at(21, 44), unlocks.lastEnd(yt, lock, at(22, 50)))
        // The lock's unlock says nothing about how long Bedtime kept YouTube closed.
        assertNull(unlocks.lastEnd(yt, bedtime, at(22, 50)))
    }
}
