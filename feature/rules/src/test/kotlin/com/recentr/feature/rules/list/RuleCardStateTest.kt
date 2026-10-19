package com.recentr.feature.rules.list

import com.recentr.core.model.HabitTask
import com.recentr.core.model.Rule
import com.recentr.core.model.RuleSchedule
import com.recentr.core.model.RuleType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

/** What a rule card reads: its status chip (green while the rule is on) and its value. */
class RuleCardStateTest {

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
    fun `a routine waiting for its window shows no value`() {
        assertEquals(CardStatus.Upcoming, routine.cardStatus(at(13), zone))
        assertFalse(routine.cardStatus(at(13), zone).active)
        assertNull(routine.cardValue(at(13), zone))
    }

    @Test
    fun `a routine shows its minutes only from a whole one`() {
        assertNull(routine.copy(usedMinutes = 0).cardValue(at(13), zone))
        assertEquals(CardValue.Minutes(1), routine.copy(usedMinutes = 1).cardValue(at(13), zone))
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
        // Not running: its minutes of the duration.
        val idle = session.copy(activeSince = null)
        assertEquals(CardStatus.Upcoming, idle.cardStatus(at(10, 10), zone))
        assertEquals(CardValue.UsedOf(0, 30), idle.cardValue(at(10, 10), zone))
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
