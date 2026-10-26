package com.recentr.feature.rules.list

import com.recentr.core.model.AllowedChange
import com.recentr.core.model.AlwaysAllowed
import com.recentr.core.model.AppCategory
import com.recentr.core.model.Rule
import com.recentr.core.model.RuleSchedule
import com.recentr.core.model.RuleType
import com.recentr.core.model.UsageApp
import com.recentr.feature.rules.ui.leftCountdown
import com.recentr.feature.rules.ui.startCountdown
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

/** What a rule card reads: its status chip (green while the rule is on) and its value. */
class RuleCardStateTest {

    private companion object {
        const val MINUTE = 60_000L
        const val HOUR = 60 * MINUTE
    }

    private val zone = ZoneId.of("Europe/Paris")

    /** Monday 19 Oct 2026 at [hour]:[minutes]. */
    private fun at(hour: Int, minutes: Int = 0): Long =
        LocalDate.of(2026, 10, 19).atTime(hour, minutes).atZone(zone).toInstant().toEpochMilli()

    /** Started at 8:00 with 30 minutes, 20 used. */
    private val limit = Rule("l", RuleType.Limit, "Limit", limitMinutes = 30, usedMinutes = 20, activeSince = at(8))

    /** Weekdays 20:10 → 9:00. */
    private val routine = Rule("r", RuleType.Bedtime, "Morning Routine", schedule = RuleSchedule(20 * 60 + 10, 9 * 60, (1..5).toSet()))

    @Test
    fun `a limit under its budget is ongoing, in green`() {
        val status = limit.cardStatus(at(13), zone)
        assertEquals(CardStatus.Ongoing, status)
        assertTrue(status.active)
        assertEquals(CardValue.UsedOf(20, 30), limit.cardValue(at(13), zone))
    }

    @Test
    fun `a limit at its budget is time's up, not green`() {
        val status = limit.copy(usedMinutes = 30).cardStatus(at(13), zone)
        assertEquals(CardStatus.TimesUp, status)
        assertFalse(status.active)
    }

    @Test
    fun `a limit waiting for Start is ready, with its budget`() {
        val waiting = limit.copy(activeSince = null, usedMinutes = 0)
        assertEquals(CardStatus.Ready, waiting.cardStatus(at(13), zone))
        assertFalse(waiting.cardStatus(at(13), zone).active)
        assertEquals(CardValue.Minutes(30), waiting.cardValue(at(13), zone))
        // Started yesterday: over at midnight, ready again.
        val yesterday = limit.copy(activeSince = at(8) - 24 * HOUR)
        assertEquals(CardStatus.Ready, yesterday.cardStatus(at(13), zone))
    }

    @Test
    fun `a routine waiting for its window counts down to its start in the last hour`() {
        assertEquals(CardStatus.Upcoming, routine.cardStatus(at(13), zone))
        assertFalse(routine.cardStatus(at(13), zone).active)
        // 19:11 → 20:10 tonight.
        assertEquals(CardValue.StartsIn(59 * MINUTE), routine.cardValue(at(19, 11), zone))
        assertEquals(CardValue.StartsIn(59 * MINUTE), routine.copy(usedMinutes = 12).cardValue(at(19, 11), zone))
        // Further off, only its chip says when it starts.
        assertNull(routine.cardValue(at(19, 10), zone))
        assertNull(routine.cardValue(at(13), zone))
    }

    @Test
    fun `a routine counts down to its next day, past the weekend`() {
        val saturdayNoon = LocalDate.of(2026, 10, 24).atTime(12, 0).atZone(zone).toInstant().toEpochMilli()
        val monday = LocalDate.of(2026, 10, 26).atTime(20, 10).atZone(zone).toInstant().toEpochMilli()
        // 57h10, not 56h10: the clocks go back an hour on Sunday.
        assertEquals(monday, routine.schedule?.nextStartAfter(saturdayNoon, zone))
        assertEquals(57 * HOUR + 10 * MINUTE, monday - saturdayNoon)
        assertNull(routine.cardValue(saturdayNoon, zone))
        assertEquals(CardValue.StartsIn(40 * MINUTE), routine.cardValue(monday - 40 * MINUTE, zone))
    }

    @Test
    fun `a routine turned off doesn't count down`() {
        assertNull(routine.copy(enabled = false).cardValue(at(19, 30), zone))
    }

    @Test
    fun `the start countdown shows seconds only in the last hour`() {
        assertEquals("42s", startCountdown(42_000))
        assertEquals("23m42s", startCountdown(23 * MINUTE + 41_200))
        assertEquals("59m59s", startCountdown(HOUR - 1_000))
        assertEquals("1h00m", startCountdown(HOUR))
        assertEquals("2h32m", startCountdown(2 * HOUR + 31 * MINUTE + 10_000))
        assertEquals("2d 3h", startCountdown(51 * HOUR))
        assertEquals("3d", startCountdown(72 * HOUR))
    }

    @Test
    fun `what's left counts down to the second, as on iOS`() {
        assertEquals("42s", leftCountdown(42_000))
        assertEquals("32m43s", leftCountdown(32 * MINUTE + 42_400))
        assertEquals("1h00m00s", leftCountdown(HOUR))
        assertEquals("6h12m03s", leftCountdown(6 * HOUR + 12 * MINUTE + 3_000))
    }

    @Test
    fun `a routine in its window is blocking, in green, counting down to its end`() {
        val status = routine.cardStatus(at(21), zone)
        assertEquals(CardStatus.Blocking, status)
        assertTrue(status.active)
        // Ends Tuesday 9:00.
        assertEquals(CardValue.Left(at(9) + 24 * 3_600_000L - at(21)), routine.cardValue(at(21), zone))
    }

    @Test
    fun `a paused rule reads paused`() {
        val paused = routine.copy(pausedUntil = Long.MAX_VALUE)
        assertEquals(CardStatus.Paused, paused.cardStatus(at(21), zone))
        assertEquals(CardValue.Paused, paused.cardValue(at(21), zone))
    }

    @Test
    fun `a rule is ongoing, its page says too, while its chip is green`() {
        assertTrue(routine.isOngoing(at(21), zone))
        assertTrue(limit.isOngoing(at(13), zone))
        assertFalse(routine.isOngoing(at(13), zone))
        assertFalse(routine.copy(pausedUntil = Long.MAX_VALUE).isOngoing(at(21), zone))
        assertFalse(limit.copy(usedMinutes = 30).isOngoing(at(13), zone))
    }

    @Test
    fun `a running session is ongoing and counts down`() {
        val session = Rule("s", RuleType.Session, "Deep work", durationMinutes = 30, activeSince = at(10))
        assertEquals(CardStatus.Ongoing, session.cardStatus(at(10, 10), zone))
        assertEquals(CardValue.Left(20 * 60_000L), session.cardValue(at(10, 10), zone))
        // Ended: a saved session, ready to run again, its length on its chip (no "0s / 30m").
        val idle = session.copy(activeSince = null)
        assertEquals(CardStatus.Ready, idle.cardStatus(at(10, 40), zone))
        assertFalse(idle.cardStatus(at(10, 40), zone).active)
        assertNull(idle.cardValue(at(10, 40), zone))
        // A session made a while ago doesn't run until it's started.
        assertEquals(CardStatus.Ready, idle.cardStatus(at(13), zone))
    }

    @Test
    fun `a card shows five app icons, then how many more`() {
        val apps = (1..8).map { UsageApp("app.$it", "App $it", AppCategory.Other) }
        val (shown, more) = limit.copy(apps = apps).cardApps()
        assertEquals(apps.take(5), shown)
        assertEquals(3, more)
        val (few, none) = limit.copy(apps = apps.take(4)).cardApps()
        assertEquals(4, few.size)
        assertEquals(0, none)
    }

    /** The apps always allowed aren't blocked, even when they were picked for the rule. */
    @Test
    fun `a card leaves out the apps always allowed`() {
        val apps = (1..8).map { UsageApp("app.$it", "App $it", AppCategory.Other) }
        val allowed = AlwaysAllowed(listOf(apps[0], apps[6], UsageApp("other", "Other", AppCategory.Other)))
        val (shown, more) = limit.copy(apps = apps).cardApps(allowed, at(10))
        assertEquals(listOf(apps[1], apps[2], apps[3], apps[4], apps[5]), shown)
        assertEquals(1, more)
        val (none, noMore) = limit.copy(apps = apps.take(1)).cardApps(allowed, at(10))
        assertEquals(emptyList<UsageApp>(), none)
        assertEquals(0, noMore)
    }

    /** An app waiting to join stays on the card until it has joined. */
    @Test
    fun `an app waiting to join leaves the card once it joins`() {
        val apps = (1..3).map { UsageApp("app.$it", "App $it", AppCategory.Other) }
        val waiting = AlwaysAllowed(pending = AllowedChange(at(12), listOf(apps[0])))
        assertEquals(apps, limit.copy(apps = apps).cardApps(waiting, at(10)).first)
        assertEquals(apps.drop(1), limit.copy(apps = apps).cardApps(waiting, at(12)).first)
    }

    @Test
    fun `a lock is off until activated, then locked with its unlocks`() {
        val off = Rule("k", RuleType.Lock, "YouTube", unlocksUsedToday = 0)
        assertEquals(CardStatus.Off, off.cardStatus(at(13), zone))
        assertFalse(off.cardStatus(at(13), zone).active)
        assertEquals(CardValue.Off, off.cardValue(at(13), zone))
        val on = off.copy(activeSince = at(9), unlocksUsedToday = 2)
        assertEquals(CardStatus.Unlocks, on.cardStatus(at(13), zone))
        assertEquals(CardValue.Locked, on.cardValue(at(13), zone))
        assertNull(on.runsUntil(at(13), zone))
    }

    @Test
    fun `a lock with apps unlocked for now still reads locked`() {
        // Unlocking two of its apps for a few minutes uses two of its unlocks; the lock stays on.
        val lock = Rule("k", RuleType.Lock, "Socials", activeSince = at(9), unlocksPerDay = 5, unlocksUsedToday = 2)
        assertEquals(CardStatus.Unlocks, lock.cardStatus(at(13), zone))
        assertEquals(CardValue.Locked, lock.cardValue(at(13), zone))
    }
}
