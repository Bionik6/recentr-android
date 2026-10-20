package com.recentr.core.data

import com.recentr.core.model.Rule
import com.recentr.core.model.RuleChange
import com.recentr.core.model.RuleSchedule
import com.recentr.core.model.RuleType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

/** A rule's streak: which windows count, which break it, and when they are counted. */
class RuleStreaksTest {

    private val zone = ZoneId.of("Europe/Paris")

    /** [day] Oct 2026 at [hour]:[minutes]; Monday 19 Oct is the first day of the week. */
    private fun at(day: Int, hour: Int, minutes: Int = 0): Long =
        LocalDate.of(2026, 10, day).atTime(hour, minutes).atZone(zone).toInstant().toEpochMilli()

    /** Weekdays 6:00 → 9:00, counting from Monday 19 Oct, midnight. */
    private val morning = Rule(
        "morning",
        RuleType.Bedtime,
        "Morning Routine",
        schedule = RuleSchedule(6 * 60, 9 * 60, (1..5).toSet()),
        streakFrom = at(19, 0),
    )

    /** Counts the windows over at [now]. */
    private fun Rule.advancedTo(now: Long) = RuleStreaks.advance(this, now, zone)

    // Routines

    @Test
    fun `a routine's window counts once it is over`() {
        assertEquals(0, morning.advancedTo(at(19, 8, 59)).streakDays)
        val done = morning.advancedTo(at(19, 9))
        assertEquals(1, done.streakDays)
        assertEquals(at(19, 9), done.streakFrom)
    }

    @Test
    fun `each window counts once`() {
        val done = morning.advancedTo(at(19, 9)).advancedTo(at(19, 12)).advancedTo(at(19, 23, 59))
        assertEquals(1, done.streakDays)
    }

    @Test
    fun `days in a row add up`() {
        val tuesday = morning.advancedTo(at(19, 9, 30)).advancedTo(at(20, 9, 30))
        assertEquals(2, tuesday.streakDays)
    }

    @Test
    fun `days off neither count nor break it`() {
        // Friday 23 Oct counted, then the weekend, then Monday 26 Oct.
        val friday = morning.copy(streakDays = 4, streakFrom = at(22, 9)).advancedTo(at(23, 10))
        assertEquals(5, friday.streakDays)
        assertEquals(5, friday.advancedTo(at(25, 23)).streakDays)
        assertEquals(6, friday.advancedTo(at(25, 23)).advancedTo(at(26, 9)).streakDays)
    }

    @Test
    fun `windows passed without the app open all count`() {
        // Nothing settled from Monday midnight to Thursday 10:00: four kept mornings.
        assertEquals(4, morning.advancedTo(at(22, 10)).streakDays)
    }

    @Test
    fun `a window already started when the rule was made doesn't count`() {
        val madeDuringIt = morning.copy(streakFrom = at(19, 7))
        assertEquals(0, madeDuringIt.advancedTo(at(19, 9, 30)).streakDays)
        assertEquals(1, madeDuringIt.advancedTo(at(20, 9, 30)).streakDays)
    }

    @Test
    fun `an overnight window counts the morning it ends`() {
        val bedtime = morning.copy(schedule = RuleSchedule(22 * 60, 7 * 60))
        assertEquals(0, bedtime.advancedTo(at(19, 23)).streakDays)
        assertEquals(1, bedtime.advancedTo(at(20, 7)).streakDays)
    }

    @Test
    fun `a window that ends while the rule is off breaks it`() {
        val off = morning.copy(streakDays = 6, enabled = false).advancedTo(at(19, 9))
        assertEquals(0, off.streakDays)
        // Back on: the next window counts again.
        assertEquals(1, off.copy(enabled = true).advancedTo(at(20, 9)).streakDays)
    }

    @Test
    fun `a window starting while the rule is paused breaks it`() {
        val paused = morning.copy(streakDays = 3, pausedUntil = at(19, 8)).advancedTo(at(19, 9))
        assertEquals(0, paused.streakDays)
    }

    @Test
    fun `a whole-day routine counts each of its days`() {
        val weekend = morning.copy(schedule = RuleSchedule(0, 0, setOf(6, 7)), streakFrom = at(23, 0))
        assertEquals(0, weekend.advancedTo(at(24, 23, 59)).streakDays)
        assertEquals(1, weekend.advancedTo(at(25, 0)).streakDays)
        assertEquals(2, weekend.advancedTo(at(26, 0)).streakDays)
    }

    @Test
    fun `nothing to count before the first window ends`() {
        assertSame(morning, morning.advancedTo(at(19, 5)))
        val unread = morning.copy(streakFrom = null)
        assertSame(unread, unread.advancedTo(at(23, 23)))
    }

    @Test
    fun `due once a window wasn't counted yet`() {
        assertFalse(RuleStreaks.due(morning, at(19, 8), zone))
        assertTrue(RuleStreaks.due(morning, at(19, 9), zone))
        assertFalse(RuleStreaks.due(morning.advancedTo(at(19, 9)), at(19, 12), zone))
        assertFalse(RuleStreaks.due(morning.copy(streakFrom = Long.MAX_VALUE), at(30, 12), zone))
        assertFalse(RuleStreaks.due(morning.copy(streakFrom = null), at(30, 12), zone))
    }

    // Breaking

    @Test
    fun `an emergency unlock breaks it and the window it happened in doesn't count`() {
        val broken = RuleStreaks.broken(morning.copy(streakDays = 8), at(19, 7))
        assertEquals(0, broken.streakDays)
        assertEquals(0, broken.advancedTo(at(19, 9)).streakDays)
        assertEquals(1, broken.advancedTo(at(20, 9)).streakDays)
    }

    @Test
    fun `a pause over a window breaks it and counts again after the pause`() {
        // Paused Monday 7:00 until Tuesday 10:00: Monday's and Tuesday's mornings are lost.
        val paused = RuleStreaks.pausedOver(morning.copy(streakDays = 5), at(19, 7), at(20, 10), zone)
        assertEquals(0, paused.streakDays)
        assertEquals(at(20, 10), paused.streakFrom)
        assertEquals(0, paused.advancedTo(at(20, 23)).streakDays)
        assertEquals(1, paused.advancedTo(at(21, 9)).streakDays)
    }

    @Test
    fun `a pause between windows keeps it`() {
        // Saturday 24 to Sunday 25 Oct evening: no weekday morning in it.
        val rule = morning.copy(streakDays = 5)
        assertSame(rule, RuleStreaks.pausedOver(rule, at(24, 10), at(25, 20), zone))
        // Monday 9:30 to 18:00, after the morning window.
        assertSame(rule, RuleStreaks.pausedOver(rule, at(19, 9, 30), at(19, 18), zone))
    }

    @Test
    fun `a pause until resumed breaks it and counts again from the resume`() {
        val paused = RuleStreaks.pausedOver(morning.copy(streakDays = 5), at(19, 12), Long.MAX_VALUE, zone)
        assertEquals(0, paused.streakDays)
        assertSame(paused, paused.advancedTo(at(23, 23)))
        val resumed = RuleStreaks.resumed(paused.copy(pausedUntil = null), at(21, 12))
        assertEquals(at(21, 12), resumed.streakFrom)
        assertEquals(2, resumed.advancedTo(at(23, 10)).streakDays)
    }

    @Test
    fun `resuming a timed pause that already ended changes nothing`() {
        val rule = morning.copy(streakFrom = at(20, 10))
        assertSame(rule, RuleStreaks.resumed(rule, at(21, 12)))
    }

    // Locks, limits, sessions

    /** Activated Monday 19 Oct at 10:00. */
    private val lock = Rule("lock", RuleType.Lock, "YouTube", activeSince = at(19, 10), streakFrom = at(19, 0))

    @Test
    fun `a lock counts each day it's on, its first from Activate`() {
        assertEquals(0, lock.advancedTo(at(19, 23, 59)).streakDays)
        assertEquals(1, lock.advancedTo(at(20, 0)).streakDays)
        assertEquals(3, lock.advancedTo(at(22, 0, 1)).streakDays)
        // Off, it neither counts nor breaks.
        val off = lock.copy(activeSince = null, streakDays = 3)
        assertSame(off, off.advancedTo(at(25, 0)))
    }

    @Test
    fun `a lock turned off counts its days until then`() {
        val turningOff = lock.copy(pending = RuleChange(at(21, 0), lock.copy(activeSince = null)))
        assertEquals(2, turningOff.advancedTo(at(23, 0)).streakDays)
        val deleted = lock.copy(pending = RuleChange(at(20, 0), null))
        assertEquals(1, deleted.advancedTo(at(23, 0)).streakDays)
        // A kept change that leaves it on doesn't stop it.
        val edited = lock.copy(pending = RuleChange(at(20, 0), lock.copy(unlocksPerDay = 10)))
        assertEquals(3, edited.advancedTo(at(22, 0)).streakDays)
    }

    /** Started Monday at 8:00 with 30 minutes; 20 used. */
    private val limit = Rule("limit", RuleType.Limit, "Social time", limitMinutes = 30, usedMinutes = 20, activeSince = at(19, 8), streakFrom = at(19, 0))

    @Test
    fun `a limit's day kept within its budget counts`() {
        assertEquals(0, limit.advancedTo(at(19, 23, 59)).streakDays)
        assertEquals(1, limit.advancedTo(at(20, 0)).streakDays)
        assertEquals(1, limit.copy(usedMinutes = 30).advancedTo(at(20, 0)).streakDays)
    }

    @Test
    fun `a limit gone over breaks it`() {
        assertEquals(0, limit.copy(usedMinutes = 31, streakDays = 3).advancedTo(at(20, 0)).streakDays)
    }

    @Test
    fun `a limit not started neither counts nor breaks it`() {
        val waiting = limit.copy(activeSince = null, streakDays = 3)
        assertSame(waiting, waiting.advancedTo(at(25, 0)))
    }

    private val session = Rule("focus", RuleType.Session, "Deep work", durationMinutes = 30, streakFrom = at(19, 0))

    @Test
    fun `a session counts its first run of the day`() {
        val first = RuleStreaks.runEnded(session.copy(activeSince = at(19, 10)), at(19, 10, 30), zone)
        assertEquals(1, first.streakDays)
        val second = RuleStreaks.runEnded(first.copy(activeSince = at(19, 15)), at(19, 15, 30), zone)
        assertEquals(1, second.streakDays)
        val tomorrow = RuleStreaks.runEnded(second.copy(activeSince = at(20, 8)), at(20, 8, 30), zone)
        assertEquals(2, tomorrow.streakDays)
    }

    @Test
    fun `a session run broken while it ran doesn't count`() {
        val broken = RuleStreaks.broken(session.copy(activeSince = at(19, 10)), at(19, 10, 10))
        assertEquals(0, RuleStreaks.runEnded(broken, at(19, 10, 30), zone).streakDays)
    }
}
