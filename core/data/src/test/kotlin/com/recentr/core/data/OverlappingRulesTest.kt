package com.recentr.core.data

import com.recentr.core.model.AppCategory
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

/**
 * Several rules holding the same app: a Bedtime routine and a daily limit both on YouTube. The
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

    /** One hour of YouTube a day; a 5-second wait unlocks it, five times a day. */
    private val dailyLimit = Rule(
        "limit",
        RuleType.Limit,
        "Daily limit",
        apps = listOf(youTube),
        limitMinutes = 60,
        unlockMethod = UnlockMethod.Wait,
        unlocksPerDay = 5,
    )

    private fun state(rules: List<Rule>, used: Int = 0, unlocks: AppUnlocks = AppUnlocks()) = HoldState(
        rules = rules,
        focus = null,
        protectedApps = setOf("com.recentr.app"),
        unlocks = unlocks,
        limitUsage = usedToday(used),
    )

    private fun usedToday(minutes: Int) = LimitUsage(mapOf(LimitReset.Midnight to mapOf(yt to minutes)))

    private fun HoldState.reason(at: Long, packageName: String = yt) = reasonFor(packageName, at, zone)

    /** What "Unlock anyway" through [rule] does ([AppHoldRepository.openAnyway]). */
    private fun HoldState.unlockThrough(rule: Rule, at: Long, minutes: Int): HoldState =
        copy(unlocks = unlocks.open(yt, liftedBy(rule, yt, at, zone), at + minutes * minute))

    /** What an emergency unlock does ([AppHoldRepository.emergencyUnlock]). */
    private fun HoldState.emergencyUnlock(at: Long): HoldState =
        copy(unlocks = unlocks.open(yt, holding(yt, at, zone), at + EmergencyUnlocks.MINUTES * minute))

    @Test
    fun `the routine holds the app while the limit still has time left`() {
        assertEquals(HoldReason.ByRule(bedtime), state(listOf(dailyLimit, bedtime), used = 0).reason(at(22, 50)))
        assertEquals(HoldReason.ByRule(bedtime), state(listOf(bedtime, dailyLimit), used = 59).reason(at(22, 50)))
    }

    @Test
    fun `the strict routine wins over a used-up limit, whatever the order`() {
        assertEquals(HoldReason.ByRule(bedtime), state(listOf(dailyLimit, bedtime), used = 75).reason(at(22, 50)))
        assertEquals(HoldReason.ByRule(bedtime), state(listOf(bedtime, dailyLimit), used = 75).reason(at(22, 50)))
        // So the shield offers the emergency unlock only, not the limit's "Unlock anyway".
        assertEquals(UnlockStep.Emergency, bedtime.unlockStep(at(22, 50), emergencyUnlocksLeft = 3, zone))
        assertNull(bedtime.unlockStep(at(22, 50), emergencyUnlocksLeft = 0, zone))
    }

    @Test
    fun `the used-up limit holds the app outside the routine`() {
        val holds = state(listOf(dailyLimit, bedtime), used = 75)
        assertEquals(HoldReason.ByRule(dailyLimit), holds.reason(at(20, 0)))
        assertEquals(UnlockStep.Wait, dailyLimit.unlockStep(at(20, 0), emergencyUnlocksLeft = 3, zone))
        assertNull(state(listOf(dailyLimit, bedtime), used = 30).reason(at(20, 0)))
    }

    @Test
    fun `a paused routine leaves the app to the limit`() {
        val paused = bedtime.copy(pausedUntil = at(23, 0))
        assertEquals(HoldReason.ByRule(dailyLimit), state(listOf(paused, dailyLimit), used = 75).reason(at(22, 50)))
        assertNull(state(listOf(paused, dailyLimit), used = 10).reason(at(22, 50)))
    }

    @Test
    fun `an unlock through the limit doesn't open the app past the routine starting during it`() {
        // 21:00: the hour of YouTube is used up and unlocked for an hour through the limit.
        val unlocked = state(listOf(dailyLimit, bedtime), used = 75).unlockThrough(dailyLimit, at(21, 0), minutes = 60)
        assertNull(unlocked.reason(at(21, 10)))
        // 21:30: Bedtime starts and puts YouTube on hold, the limit's unlock notwithstanding.
        assertEquals(HoldReason.ByRule(bedtime), unlocked.reason(at(21, 30)))
        assertEquals(HoldReason.ByRule(bedtime), unlocked.reason(at(21, 59)))
        assertEquals(HoldReason.ByRule(bedtime), unlocked.reason(at(22, 50)))
    }

    @Test
    fun `an unlock through the routine doesn't open the app past the limit running out during it`() {
        val holds = state(listOf(softBedtime, dailyLimit), used = 50)
        assertEquals(HoldReason.ByRule(softBedtime), holds.reason(at(22, 0)))
        val unlocked = holds.unlockThrough(softBedtime, at(22, 0), minutes = 30)
        assertNull(unlocked.reason(at(22, 10)))
        // The daily hour runs out while the routine's unlock still runs.
        assertEquals(HoldReason.ByRule(dailyLimit), unlocked.copy(limitUsage = usedToday(60)).reason(at(22, 10)))
    }

    @Test
    fun `one unlock lifts every rule holding the app that could be unlocked`() {
        val holds = state(listOf(dailyLimit, softBedtime), used = 75)
        assertEquals(HoldReason.ByRule(dailyLimit), holds.reason(at(22, 0)))
        val unlocked = holds.unlockThrough(dailyLimit, at(22, 0), minutes = 15)
        assertNull(unlocked.reason(at(22, 0)))
        assertNull(unlocked.reason(at(22, 14)))
        assertEquals(HoldReason.ByRule(dailyLimit), unlocked.reason(at(22, 15)))
    }

    @Test
    fun `an unlock never lifts a rule that can't be unlocked`() {
        val now = at(22, 0)
        // Strict mode: the limit's unlock leaves the routine holding.
        assertEquals(listOf(dailyLimit), state(listOf(dailyLimit, bedtime), used = 75).liftedBy(dailyLimit, yt, now, zone))
        assertEquals(
            HoldReason.ByRule(bedtime),
            state(listOf(dailyLimit, bedtime), used = 75).unlockThrough(dailyLimit, now, minutes = 15).reason(now),
        )
        // "No unlocks", and no unlocks left today.
        val noUnlocks = softBedtime.copy(unlockMethod = UnlockMethod.None)
        assertEquals(
            HoldReason.ByRule(noUnlocks),
            state(listOf(dailyLimit, noUnlocks), used = 75).unlockThrough(dailyLimit, now, minutes = 15).reason(now),
        )
        val spent = softBedtime.copy(unlocksPerDay = 2, unlocksUsedToday = 2)
        assertEquals(
            HoldReason.ByRule(spent),
            state(listOf(dailyLimit, spent), used = 75).unlockThrough(dailyLimit, now, minutes = 15).reason(now),
        )
    }

    @Test
    fun `an emergency unlock gets past the strict routine and the limit, not a rule starting after it`() {
        val lateLock = Rule("late", RuleType.Lock, "No YouTube late", apps = listOf(youTube), schedule = RuleSchedule(22 * 60 + 52, 23 * 60 + 30))
        val unlocked = state(listOf(bedtime, dailyLimit, lateLock), used = 75).emergencyUnlock(at(22, 50))
        assertNull(unlocked.reason(at(22, 51)))
        assertEquals(HoldReason.ByRule(lateLock), unlocked.reason(at(22, 52)))
        // Over after five minutes.
        assertEquals(HoldReason.ByRule(bedtime), state(listOf(bedtime, dailyLimit), used = 75).emergencyUnlock(at(22, 50)).reason(at(22, 55)))
    }

    @Test
    fun `an unlock opens one app only`() {
        val unlocked = state(listOf(softBedtime)).unlockThrough(softBedtime, at(22, 0), minutes = 15)
        assertNull(unlocked.reason(at(22, 5)))
        assertEquals(HoldReason.ByRule(softBedtime), unlocked.reason(at(22, 5), tiktok.packageName))
    }

    @Test
    fun `lock it again puts every rule back`() {
        val unlocked = state(listOf(dailyLimit, softBedtime), used = 75).unlockThrough(dailyLimit, at(22, 0), minutes = 15)
        val lockedAgain = unlocked.copy(unlocks = unlocked.unlocks.lockAgain(yt, at(22, 5)))
        assertEquals(HoldReason.ByRule(dailyLimit), lockedAgain.reason(at(22, 5)))
        // An ended unlock stays ended.
        assertEquals(unlocked.unlocks.lockAgain(yt, at(22, 20)), unlocked.unlocks.lockAgain(yt, at(22, 20)).lockAgain(yt, at(22, 30)))
    }

    @Test
    fun `a new unlock never shortens one still running`() {
        val long = AppUnlocks().open(yt, listOf(dailyLimit), at(22, 0))
        val both = long.open(yt, listOf(dailyLimit, softBedtime), at(21, 5))
        assertTrue(both.lifts(yt, dailyLimit, at(21, 30)))
        assertFalse(both.lifts(yt, softBedtime, at(21, 30)))
    }

    @Test
    fun `stayed off counts from the end of the rule's own last unlock`() {
        val unlocks = AppUnlocks().open(yt, listOf(dailyLimit), at(21, 44))
        assertNull(unlocks.lastEnd(yt, dailyLimit, at(21, 40)))
        assertEquals(at(21, 44), unlocks.lastEnd(yt, dailyLimit, at(22, 50)))
        // The limit's unlock says nothing about how long Bedtime kept YouTube closed.
        assertNull(unlocks.lastEnd(yt, bedtime, at(22, 50)))
    }
}
