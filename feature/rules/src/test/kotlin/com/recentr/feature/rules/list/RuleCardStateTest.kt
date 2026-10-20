package com.recentr.feature.rules.list

import com.recentr.core.model.AppCategory
import com.recentr.core.model.HabitTask
import com.recentr.core.model.Rule
import com.recentr.core.model.RuleSchedule
import com.recentr.core.model.RuleType
import com.recentr.core.model.UsageApp
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

    private val limit = Rule("l", RuleType.Limit, "Daily limit", limitMinutes = 30, usedMinutes = 20)

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
    fun `a routine waiting for its window counts down to its start`() {
        assertEquals(CardStatus.Upcoming, routine.cardStatus(at(13), zone))
        assertFalse(routine.cardStatus(at(13), zone).active)
        // 13:00 → 20:10 tonight.
        assertEquals(CardValue.StartsIn(7 * HOUR + 10 * MINUTE), routine.cardValue(at(13), zone))
        assertEquals(CardValue.StartsIn(7 * HOUR + 10 * MINUTE), routine.copy(usedMinutes = 12).cardValue(at(13), zone))
    }

    @Test
    fun `a routine counts down to its next day, past the weekend`() {
        val saturdayNoon = LocalDate.of(2026, 10, 24).atTime(12, 0).atZone(zone).toInstant().toEpochMilli()
        val monday = LocalDate.of(2026, 10, 26).atTime(20, 10).atZone(zone).toInstant().toEpochMilli()
        // 57h10, not 56h10: the clocks go back an hour on Sunday.
        assertEquals(CardValue.StartsIn(monday - saturdayNoon), routine.cardValue(saturdayNoon, zone))
        assertEquals(57 * HOUR + 10 * MINUTE, monday - saturdayNoon)
    }

    @Test
    fun `a routine turned off doesn't count down`() {
        assertNull(routine.copy(enabled = false).cardValue(at(13), zone))
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
    fun `a running session is ongoing and counts down`() {
        val session = Rule("s", RuleType.Session, "Deep work", durationMinutes = 30, activeSince = at(10))
        assertEquals(CardStatus.Ongoing, session.cardStatus(at(10, 10), zone))
        assertEquals(CardValue.Left(20 * 60_000L), session.cardValue(at(10, 10), zone))
        // Ended: a saved session, ready to run again, its length on its chip (no "0s / 30m").
        val idle = session.copy(activeSince = null)
        assertEquals(CardStatus.Ready, idle.cardStatus(at(10, 40), zone))
        assertFalse(idle.cardStatus(at(10, 40), zone).active)
        assertNull(idle.cardValue(at(10, 40), zone))
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

    @Test
    fun `locks and habits keep their counters`() {
        val lock = Rule("k", RuleType.Lock, "YouTube")
        assertEquals(CardStatus.Unlocks, lock.cardStatus(at(13), zone))
        assertEquals(CardValue.Locked, lock.cardValue(at(13), zone))
        val habit = Rule("h", RuleType.Habit, "Read first", habitTasks = listOf(HabitTask("a", "Read", done = true), HabitTask("b", "Walk")))
        assertEquals(CardStatus.Tasks, habit.cardStatus(at(13), zone))
        assertEquals(CardValue.Tasks(1, 2), habit.cardValue(at(13), zone))
    }
}
