package com.recentr.core.data

import com.recentr.core.model.Rule
import com.recentr.core.model.RuleSchedule
import com.recentr.core.model.RuleType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

/** "Starts in N min" reminders: when the next one is due, and for which rules. */
class RuleRemindersTest {

    private val zone = ZoneId.of("Europe/Paris")

    /** [day] Oct 2026 at [hour]:[minutes] (Monday 19 Oct). */
    private fun at(hour: Int, minutes: Int = 0, day: Int = 19): Long =
        LocalDate.of(2026, 10, day).atTime(hour, minutes).atZone(zone).toInstant().toEpochMilli()

    /** Weekdays 20:10 → 09:00, reminded 10 minutes before. */
    private val morning = Rule(
        "morning",
        RuleType.Bedtime,
        "Morning Routine",
        schedule = RuleSchedule(20 * 60 + 10, 9 * 60, (1..5).toSet()),
        reminderMinutesBefore = 10,
    )

    @Test
    fun remindsBeforeTodaysStart() {
        val next = nextRuleReminder(listOf(morning), at(13), zone)!!
        assertEquals(at(20), next.atMillis)
        assertEquals(mapOf("morning" to at(20, 10)), next.starts)
    }

    @Test
    fun movesToTheNextScheduledDayOnceTodaysReminderIsPast() {
        // Friday 23 Oct after the reminder: the weekend is skipped, Monday 26 Oct is next.
        val next = nextRuleReminder(listOf(morning), at(20, 5, day = 23), zone)!!
        assertEquals(at(20, day = 26), next.atMillis)
    }

    @Test
    fun aReminderCanFallOnTheDayBefore() {
        val midnight = morning.copy(schedule = RuleSchedule(10, 7 * 60, setOf(2)), reminderMinutesBefore = 30)
        // Starts Tuesday 00:10: reminded Monday 23:40.
        assertEquals(at(23, 40), nextRuleReminder(listOf(midnight), at(13), zone)!!.atMillis)
    }

    @Test
    fun noReminderWithoutOneOffOrPausedOrAllDay() {
        assertNull(nextRuleReminder(listOf(morning.copy(reminderMinutesBefore = null)), at(13), zone))
        assertNull(nextRuleReminder(listOf(morning.copy(enabled = false)), at(13), zone))
        assertNull(nextRuleReminder(listOf(morning.copy(pausedUntil = Long.MAX_VALUE)), at(13), zone))
        assertNull(nextRuleReminder(listOf(morning.copy(schedule = RuleSchedule(0, 0))), at(13), zone))
    }

    @Test
    fun onlyRoutinesHaveReminders() {
        // Sessions, locks and limits have no days or hours.
        val rules = RuleType.entries.filter { it != RuleType.Bedtime }.map { type -> morning.copy(id = type.name, type = type) }
        assertNull(nextRuleReminder(rules, at(13), zone))
    }

    @Test
    fun skipsAStartInsideAPause() {
        // Paused until Tuesday morning: Monday's start is skipped.
        val paused = morning.copy(pausedUntil = at(8, day = 20))
        assertEquals(at(20, day = 20), nextRuleReminder(listOf(paused), at(13), zone)!!.atMillis)
    }

    @Test
    fun rulesDueTogetherShareTheReminder() {
        val focus = Rule(
            "focus",
            RuleType.Bedtime,
            "Focus",
            schedule = RuleSchedule(20 * 60 + 5, 22 * 60),
            reminderMinutesBefore = 5,
        )
        val next = nextRuleReminder(listOf(morning, focus), at(13), zone)!!
        assertEquals(at(20), next.atMillis)
        assertEquals(setOf("morning", "focus"), next.starts.keys)
        assertEquals(at(20, 5), next.starts["focus"])
    }

    @Test
    fun minutesLeftRoundUpAndNeverReadZero() {
        val start = at(20, 10)
        assertEquals(10, reminderMinutesLeft(start, at(20)))
        assertEquals(10, reminderMinutesLeft(start, at(20) + 30))
        // An approximate alarm 2.5 minutes late: "starts in 8 min".
        assertEquals(8, reminderMinutesLeft(start, at(20, 2) + 30_000))
        assertEquals(1, reminderMinutesLeft(start, start - 1))
        assertEquals(1, reminderMinutesLeft(start, start))
    }
}
