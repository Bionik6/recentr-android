package com.recentr.core.data

import com.recentr.core.model.HabitRepeat
import com.recentr.core.model.HabitTask
import com.recentr.core.model.LimitReset
import com.recentr.core.model.Rule
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

    /** Counts the windows over at [now], the counters settled at [since] (just before by default). */
    private fun Rule.advancedTo(now: Long, since: Long = now - 1) = RuleStreaks.advance(this, since, now, zone)

    // Windowed rules (routines, scheduled sessions)

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
    fun `nothing to count before the first window ends or for a one-shot session`() {
        assertSame(morning, morning.advancedTo(at(19, 5)))
        val oneShot = morning.copy(oneShot = true)
        assertSame(oneShot, oneShot.advancedTo(at(23, 23)))
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

    // Locks, habits, limits, manual sessions

    @Test
    fun `an all-day lock counts each of its days`() {
        val lock = Rule("lock", RuleType.Lock, "YouTube", streakFrom = at(19, 0))
        assertEquals(0, lock.advancedTo(at(19, 23, 59)).streakDays)
        assertEquals(1, lock.advancedTo(at(20, 0)).streakDays)
        assertEquals(3, lock.advancedTo(at(22, 0, 1)).streakDays)
        // An all-day schedule on weekdays: the weekend doesn't count.
        val weekdays = lock.copy(schedule = RuleSchedule(0, 0, (1..5).toSet()), streakFrom = at(23, 0))
        assertEquals(1, weekdays.advancedTo(at(26, 0)).streakDays)
    }

    private val habit = Rule(
        "habit",
        RuleType.Habit,
        "Read first",
        habitTasks = listOf(HabitTask("a", "Read 10 pages", done = true), HabitTask("b", "Stretch", done = true)),
        streakFrom = at(19, 0),
    )

    @Test
    fun `a habit day counts when every task was done`() {
        assertEquals(1, habit.advancedTo(at(20, 0), since = at(19, 21)).streakDays)
    }

    @Test
    fun `a habit day left undone breaks it`() {
        val undone = habit.copy(streakDays = 4, habitTasks = habit.habitTasks.map { it.copy(done = it.id == "a") })
        assertEquals(0, undone.advancedTo(at(20, 0), since = at(19, 21)).streakDays)
        val noTasks = habit.copy(streakDays = 4, habitTasks = emptyList())
        assertEquals(0, noTasks.advancedTo(at(20, 0), since = at(19, 21)).streakDays)
    }

    @Test
    fun `habit days without the app open are undone`() {
        // Tasks done on Monday, then nothing until Thursday: Tuesday and Wednesday break it.
        assertEquals(0, habit.advancedTo(at(22, 9), since = at(19, 21)).streakDays)
    }

    @Test
    fun `a weekday habit skips the weekend`() {
        val weekdays = habit.copy(habitRepeat = HabitRepeat.Weekdays, streakDays = 5, streakFrom = at(24, 0))
        assertEquals(5, weekdays.advancedTo(at(26, 0), since = at(25, 12)).streakDays)
    }

    private val limit = Rule("limit", RuleType.Limit, "Daily limit", limitMinutes = 30, usedMinutes = 20, streakFrom = at(19, 0))

    @Test
    fun `a limit day kept within its budget counts`() {
        assertEquals(1, limit.advancedTo(at(20, 0), since = at(19, 22)).streakDays)
        assertEquals(1, limit.copy(usedMinutes = 30).advancedTo(at(20, 0), since = at(19, 22)).streakDays)
    }

    @Test
    fun `a limit gone over breaks it`() {
        assertEquals(0, limit.copy(usedMinutes = 31, streakDays = 3).advancedTo(at(20, 0), since = at(19, 22)).streakDays)
    }

    @Test
    fun `a limit resetting at 4 am counts its period then`() {
        val fourAm = limit.copy(limitReset = LimitReset.FourAm)
        assertEquals(0, fourAm.advancedTo(at(20, 3, 59), since = at(19, 22)).streakDays)
        assertEquals(1, fourAm.advancedTo(at(20, 4), since = at(19, 22)).streakDays)
    }

    @Test
    fun `a weekly limit counts its week as seven days`() {
        val weekly = limit.copy(limitReset = LimitReset.Monday)
        assertEquals(0, weekly.advancedTo(at(25, 23), since = at(25, 22)).streakDays)
        assertEquals(7, weekly.advancedTo(at(26, 0), since = at(25, 22)).streakDays)
    }

    private val manual = Rule("focus", RuleType.Session, "Deep work", durationMinutes = 30, streakFrom = at(19, 0))

    @Test
    fun `a manual session counts its first run of the day`() {
        val first = RuleStreaks.runEnded(manual.copy(activeSince = at(19, 10)), at(19, 10, 30), zone)
        assertEquals(1, first.streakDays)
        val second = RuleStreaks.runEnded(first.copy(activeSince = at(19, 15)), at(19, 15, 30), zone)
        assertEquals(1, second.streakDays)
        val tomorrow = RuleStreaks.runEnded(second.copy(activeSince = at(20, 8)), at(20, 8, 30), zone)
        assertEquals(2, tomorrow.streakDays)
    }

    @Test
    fun `a manual session run broken while it ran doesn't count`() {
        val broken = RuleStreaks.broken(manual.copy(activeSince = at(19, 10)), at(19, 10, 10))
        assertEquals(0, RuleStreaks.runEnded(broken, at(19, 10, 30), zone).streakDays)
    }

    @Test
    fun `a pause while a manual session runs breaks it`() {
        val running = manual.copy(streakDays = 2, activeSince = at(19, 10))
        assertEquals(0, RuleStreaks.pausedOver(running, at(19, 10, 10), at(19, 11), zone).streakDays)
        assertSame(manual, RuleStreaks.pausedOver(manual, at(19, 12), at(19, 13), zone))
    }
}
