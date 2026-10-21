package com.recentr.feature.rules.model

import com.recentr.core.model.RuleType
import com.recentr.feature.rules.model.BuiltInTemplate.DinnerTogether
import com.recentr.feature.rules.model.BuiltInTemplate.EveningCalm
import com.recentr.feature.rules.model.BuiltInTemplate.FamilyHour
import com.recentr.feature.rules.model.BuiltInTemplate.FocusSprint
import com.recentr.feature.rules.model.BuiltInTemplate.NightPause
import com.recentr.feature.rules.model.BuiltInTemplate.SlowMorning
import com.recentr.feature.rules.model.BuiltInTemplate.StudyHour
import com.recentr.feature.rules.model.BuiltInTemplate.TimeForMe
import com.recentr.feature.rules.model.BuiltInTemplate.WorkRhythm
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

/**
 * A new session's 3 "Suggestions", for the moment the editor opens: a routine holding now (for the
 * minutes left in it), then one starting within the hour, then the sessions suited to the moment of
 * the day, then the other sessions.
 */
class SessionSuggestionsTest {

    private val zone = ZoneId.of("UTC")

    /** [day] October 2026 at [hour]:[minute]:[second]: the 17th is a Saturday, the 18th a Sunday, the 19th a Monday. */
    private fun at(day: Int, hour: Int, minute: Int = 0, second: Int = 0): Long =
        LocalDate.of(2026, 10, day).atTime(hour, minute, second).atZone(zone).toInstant().toEpochMilli()

    private fun suggestions(now: Long): List<Pair<BuiltInTemplate, Int>> =
        BuiltInTemplate.sessionSuggestions(now, zone).map { it.template to it.minutes }

    private val weekdays = 19..23
    private val week = 17..23

    @Test
    fun `saturday 13 30 - the afternoon sessions, then the others`() {
        assertEquals(listOf(FocusSprint to 45, StudyHour to 60, TimeForMe to 30), suggestions(at(17, 13, 30)))
    }

    @Test
    fun `weekday 09 30 - the rest of work rhythm, then the morning session`() {
        weekdays.forEach { day ->
            assertEquals(listOf(WorkRhythm to 150, FocusSprint to 45, StudyHour to 60), suggestions(at(day, 9, 30)))
        }
    }

    @Test
    fun `19 45 - the rest of dinner, then the evening sessions`() {
        week.forEach { day ->
            assertEquals(listOf(DinnerTogether to 30, StudyHour to 60, TimeForMe to 30), suggestions(at(day, 19, 45)))
        }
    }

    @Test
    fun `22 30 - the rest of evening calm, then night pause starting within the hour`() {
        week.forEach { day ->
            assertEquals(listOf(EveningCalm to 30, NightPause to 480, TimeForMe to 30), suggestions(at(day, 22, 30)))
        }
    }

    @Test
    fun `sunday 16 20 - the rest of family hour, then the afternoon sessions`() {
        assertEquals(listOf(FamilyHour to 40, FocusSprint to 45, StudyHour to 60), suggestions(at(18, 16, 20)))
    }

    @Test
    fun `07 10 - the rest of slow morning, then the morning session`() {
        week.forEach { day ->
            assertEquals(listOf(SlowMorning to 50, FocusSprint to 45, StudyHour to 60), suggestions(at(day, 7, 10)))
        }
    }

    @Test
    fun `the minutes left are rounded up to 5, and never under 5`() {
        assertEquals(WorkRhythm to 150, suggestions(at(19, 9, 30, 30)).first())
        assertEquals(WorkRhythm to 130, suggestions(at(19, 9, 53)).first())
        assertEquals(WorkRhythm to 5, suggestions(at(19, 11, 58)).first())
    }

    @Test
    fun `a routine starting in an hour is suggested for its length, not one starting later`() {
        assertEquals(listOf(EveningCalm to 60, StudyHour to 60, TimeForMe to 30), suggestions(at(19, 21, 0)))
        assertEquals(listOf(StudyHour to 60, TimeForMe to 30, FocusSprint to 45), suggestions(at(19, 20, 59)))
        // Work Rhythm on weekdays only.
        assertEquals(listOf(WorkRhythm to 180, FocusSprint to 45, StudyHour to 60), suggestions(at(19, 8, 30)))
        assertEquals(listOf(FocusSprint to 45, StudyHour to 60, TimeForMe to 30), suggestions(at(17, 8, 30)))
    }

    @Test
    fun `always 3 sessions or routines, within the session lengths`() {
        val start = at(17, 0)
        (0 until 7 * 24 * 4).forEach { quarter ->
            val suggestions = BuiltInTemplate.sessionSuggestions(start + quarter * 15 * 60_000L, zone)
            assertEquals(3, suggestions.size)
            suggestions.forEach {
                assertTrue(it.template.type == RuleType.Session || it.template.type == RuleType.Bedtime)
                assertTrue(it.minutes in SESSION_MIN_MINUTES..SESSION_MAX_MINUTES && it.minutes % 5 == 0)
            }
        }
    }
}
