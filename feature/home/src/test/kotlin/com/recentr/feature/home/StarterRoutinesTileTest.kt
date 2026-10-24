package com.recentr.feature.home

import com.recentr.core.model.AppCategory
import com.recentr.core.model.RuleType
import com.recentr.core.model.StarterRoutine
import com.recentr.core.model.UsageApp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId

/**
 * Home's tile after onboarding set up the starter routines: the routine next, or the one blocking
 * now, with its id, so tapping the tile shows that rule on Rules.
 */
class StarterRoutinesTileTest {

    private val zone = ZoneId.systemDefault()
    private val instagram = UsageApp("com.instagram.android", "Instagram", AppCategory.Social)
    private val routines = listOf(
        StarterRoutine.WorkingTime.rule("work", "Working Time", listOf(instagram)),
        StarterRoutine.NightPause.rule("night", "Night Pause", listOf(instagram)),
        StarterRoutine.WeekendZen.rule("zen", "Weekend Zen", listOf(instagram)),
    )

    /** [day] Oct 2026 at [hour]:[minutes]: Monday 19, Friday 23, Saturday 24, Sunday 25. */
    private fun at(day: Int, hour: Int, minutes: Int = 0) = LocalDate.of(2026, 10, day).atTime(hour, minutes)

    @Test
    fun `on a weekday morning, working time is next`() {
        assertEquals(
            NextRuleUi("work", "Working Time", RuleType.Bedtime, 9 * 60, null),
            HomeViewModel.nextRule(routines, at(19, 8)),
        )
    }

    @Test
    fun `after work, night pause is next`() {
        assertEquals(
            NextRuleUi("night", "Night Pause", RuleType.Bedtime, 23 * 60, null),
            HomeViewModel.nextRule(routines, at(23, 17, 30)),
        )
    }

    @Test
    fun `on a weekend morning, weekend zen is next`() {
        assertEquals("zen", HomeViewModel.nextRule(routines, at(24, 8))?.ruleId)
    }

    @Test
    fun `on a sunday evening, the next one is monday's working time after the night`() {
        assertEquals(
            NextRuleUi("night", "Night Pause", RuleType.Bedtime, 23 * 60, null),
            HomeViewModel.nextRule(routines, at(25, 18)),
        )
        val afterMidnight = HomeViewModel.nextRule(routines, at(25, 23, 30))
        assertEquals(NextRuleUi("work", "Working Time", RuleType.Bedtime, 9 * 60, DayOfWeek.MONDAY), afterMidnight)
    }

    @Test
    fun `the routine blocking now takes the tile, with its id`() {
        assertEquals("work", HomeViewModel.activeSession(routines, at(19, 10), zone)?.ruleId)
        assertEquals("night", HomeViewModel.activeSession(routines, at(20, 2), zone)?.ruleId)
        assertEquals("zen", HomeViewModel.activeSession(routines, at(24, 10), zone)?.ruleId)
        assertNull(HomeViewModel.activeSession(routines, at(19, 20), zone))
    }
}
