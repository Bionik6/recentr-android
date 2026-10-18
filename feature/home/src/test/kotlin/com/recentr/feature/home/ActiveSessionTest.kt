package com.recentr.feature.home

import com.recentr.core.model.Rule
import com.recentr.core.model.RuleSchedule
import com.recentr.core.model.RuleType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

/** The Home tile's "Ongoing · 2:04:08 left": running sessions, and sessions or routines in their window. */
class ActiveSessionTest {

    private val zone = ZoneId.of("Europe/Paris")
    private val monday = LocalDate.of(2026, 10, 19)
    private val minute = 60_000L

    private fun LocalDateTime.millis() = atZone(zone).toInstant().toEpochMilli()

    private fun active(rules: List<Rule>, now: LocalDateTime) = HomeViewModel.activeSession(rules, now, zone)

    private val bedtime = Rule("b", RuleType.Bedtime, "Bedtime", schedule = RuleSchedule(22 * 60 + 30, 7 * 60))

    @Test
    fun `a running session counts down to its end`() {
        val now = monday.atTime(10, 0)
        val session = Rule("s", RuleType.Session, "Focus", durationMinutes = 30, activeSince = now.millis() - 5 * minute)
        assertEquals(
            ActiveSessionUi("s", "Focus", RuleType.Session, now.millis() + 25 * minute),
            active(listOf(session), now),
        )
    }

    @Test
    fun `a routine counts down to the end of its window`() {
        val evening = monday.atTime(23, 0)
        assertEquals(
            ActiveSessionUi("b", "Bedtime", RuleType.Bedtime, monday.plusDays(1).atTime(7, 0).millis()),
            active(listOf(bedtime), evening),
        )
        // Still inside the overnight window after midnight.
        val early = monday.plusDays(1).atTime(6, 0)
        assertEquals(monday.plusDays(1).atTime(7, 0).millis(), active(listOf(bedtime), early)?.endsAt)
    }

    @Test
    fun `a scheduled session counts down to the end of its window`() {
        val workHours = Rule("w", RuleType.Session, "Work", schedule = RuleSchedule(9 * 60, 17 * 60, (1..5).toSet()))
        assertEquals(monday.atTime(17, 0).millis(), active(listOf(workHours), monday.atTime(10, 0))?.endsAt)
        assertNull(active(listOf(workHours), monday.plusDays(5).atTime(10, 0)))
    }

    @Test
    fun `routines outside their window, paused or disabled show nothing`() {
        val now = monday.atTime(23, 0)
        assertNull(active(listOf(bedtime), monday.atTime(12, 0)))
        assertNull(active(listOf(bedtime.copy(pausedUntil = now.millis() + 10 * minute)), now))
        assertNull(active(listOf(bedtime.copy(enabled = false)), now))
    }

    @Test
    fun `rules that hold all day have no countdown`() {
        val now = monday.atTime(10, 0)
        val rules = listOf(
            Rule("l", RuleType.Lock, "Lock"),
            Rule("m", RuleType.Limit, "Limit", limitMinutes = 30),
            Rule("h", RuleType.Habit, "Habit"),
            Rule("a", RuleType.Bedtime, "All day", schedule = RuleSchedule(0, 0)),
        )
        assertNull(active(rules, now))
    }

    @Test
    fun `the one that ends first wins`() {
        val now = monday.atTime(23, 0)
        val session = Rule("s", RuleType.Session, "Focus", durationMinutes = 30, activeSince = now.millis())
        assertEquals("s", active(listOf(bedtime, session), now)?.ruleId)
        val long = session.copy(durationMinutes = 12 * 60)
        assertEquals("b", active(listOf(bedtime, long), now)?.ruleId)
    }
}
