package com.recentr.core.data

import com.recentr.core.model.Achievement
import com.recentr.core.model.AppId
import com.recentr.core.model.CalmLevel
import com.recentr.core.model.DayStats
import com.recentr.core.model.FocusRecordKind
import com.recentr.core.model.FocusSessionRecord
import com.recentr.core.model.Rule
import com.recentr.core.model.RuleType
import com.recentr.core.model.UsageApp
import java.time.LocalDateTime
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AchievementEngineTest {

    private val zone = ZoneId.of("Europe/Paris")
    /** Sunday 18 October 2026, 10 AM. */
    private val now = LocalDateTime.of(2026, 10, 18, 10, 0)
    private val today = now.toLocalDate()

    /** One day per value, oldest first, ending today: screen time spread from 9 AM. */
    private fun days(vararg minutes: Int, late: Set<Int> = emptySet(), early: Set<Int> = emptySet(), pickups: Int = 30) =
        minutes.mapIndexed { index, total ->
            val date = today.minusDays((minutes.size - 1 - index).toLong())
            val hourly = MutableList(24) { 0 }
            var left = total
            var hour = 9
            while (left > 0) {
                hourly[hour] = minOf(60, left)
                left -= hourly[hour]
                hour++
            }
            if (index in late) hourly[23] += 20
            if (index in early) hourly[6] += 15
            DayStats(date, true, total + (if (index in late) 20 else 0) + (if (index in early) 15 else 0), pickups, hourly)
        }

    private fun evaluate(
        days: List<DayStats>,
        goal: Int = 120,
        baseline: Int = 4 * 60,
        focus: List<FocusSessionRecord> = emptyList(),
        rules: List<Rule> = emptyList(),
    ) = AchievementEngine.evaluate(
        AchievementInputs(
            now = now,
            days = days,
            goalMinutes = goal,
            baselineDailyMinutes = baseline,
            focusSessions = focus,
            rules = rules,
            startedOn = today.minusDays(30),
            zone = zone,
        ),
    ).associateBy { it.achievement }

    private fun session(daysAgo: Long, minutes: Int, urges: Int) = FocusSessionRecord(
        id = "s$daysAgo",
        title = "Deep work",
        kind = FocusRecordKind.Timer,
        startedAt = today.minusDays(daysAgo).atTime(9, 0).atZone(zone).toInstant().toEpochMilli(),
        focusedMinutes = minutes,
        rounds = 1,
        urgesResisted = urges,
    )

    @Test
    fun `streak badges are earned the day the streak reaches them`() {
        // Nine days: the 3rd is over the goal (a saver keeps the streak), then on target.
        val badges = evaluate(days(100, 100, 300, 100, 100, 100, 100, 100, 100))

        val seven = badges.getValue(Achievement.SevenDayStreak)
        assertEquals(today.minusDays(2), seven.earnedOn)
        assertEquals(7, seven.progress)
        // No saver for the goal crusher: its run restarted after the 3rd day (6 days so far).
        val crusher = badges.getValue(Achievement.GoalCrusher)
        assertNull(crusher.earnedOn)
        assertEquals(6, crusher.progress)
        assertEquals(9, badges.getValue(Achievement.TwoCalmWeeks).progress)
    }

    @Test
    fun `the current streak earns its badge even when the history spent the savers earlier`() {
        // Early days over the goal use the month's savers going forward, so the history never
        // reaches 7; walking back from today, the savers cover the recent days instead.
        val badges = evaluate(days(100, 300, 300, 300, 100, 300, 100, 100, 100, 300, 100, 100))

        val seven = badges.getValue(Achievement.SevenDayStreak)
        assertEquals(7, seven.progress)
        assertEquals(today.minusDays(1), seven.earnedOn)
    }

    @Test
    fun `quiet nights and mornings need data on both sides of midnight`() {
        // 8 days, the 1st with screen time after 11 PM: 6 quiet nights end today (the 7th is
        // tonight). Every morning has screen time at 6 AM except the last five.
        val badges = evaluate(days(60, 60, 60, 60, 60, 60, 60, 60, late = setOf(0), early = setOf(0, 1, 2)))

        assertEquals(6, badges.getValue(Achievement.NightOwlRetired).progress)
        assertNull(badges.getValue(Achievement.NightOwlRetired).earnedOn)
        val mornings = badges.getValue(Achievement.SlowMornings)
        assertEquals(today, mornings.earnedOn)
        assertEquals(5, mornings.progress)
    }

    @Test
    fun `single-day badges only look at days that are over`() {
        // Today (Sunday) is under 30 minutes but not over yet; last Sunday counts.
        val badges = evaluate(days(25, 120, 120, 120, 120, 120, 120, 10))

        assertEquals(today.minusDays(7), badges.getValue(Achievement.ScreenFreeSunday).earnedOn)
        assertEquals(today.minusDays(7), badges.getValue(Achievement.GentleDay).earnedOn)
        assertNull(evaluate(days(100, 100, pickups = 25)).getValue(Achievement.HandsOff).earnedOn)
    }

    @Test
    fun `totals count focus hours, urges and the hours won back`() {
        val focus = listOf(session(3, 300, 40), session(2, 200, 40), session(1, 120, 30))
        // 3 days of 1h against a 4h habit: 9 hours won back (today is not over).
        val badges = evaluate(days(60, 60, 60, 60), focus = focus)

        assertEquals(today.minusDays(1), badges.getValue(Achievement.FocusTenHours).earnedOn)
        assertEquals(today.minusDays(1), badges.getValue(Achievement.UrgesResisted).earnedOn)
        assertEquals(today.minusDays(3), badges.getValue(Achievement.FirstFocus).earnedOn)
        val wonBack = badges.getValue(Achievement.TenHoursWonBack)
        assertNull(wonBack.earnedOn)
        assertEquals(9, wonBack.progress)
    }

    @Test
    fun `rule badges date from the start`() {
        val rules = listOf(Rule("r", RuleType.Lock, "Instagram & X", apps = listOf(UsageApp.of(AppId.Instagram), UsageApp.of(AppId.X)), note = "Sleep"))

        val badges = evaluate(days(60), rules = rules)

        assertEquals(today.minusDays(30), badges.getValue(Achievement.FirstLock).earnedOn)
        assertEquals(today.minusDays(30), badges.getValue(Achievement.NoteToSelf).earnedOn)
        assertNull(evaluate(days(60)).getValue(Achievement.FirstLock).earnedOn)
    }

    @Test
    fun `badges met before the start count from the first day`() {
        val badges = AchievementEngine.evaluate(
            AchievementInputs(
                now = now,
                days = days(30, 120, 120),
                goalMinutes = 120,
                baselineDailyMinutes = 240,
                focusSessions = emptyList(),
                rules = emptyList(),
                startedOn = today.minusDays(1),
                zone = zone,
            ),
        ).associateBy { it.achievement }

        // The gentle day was two days ago, before the user started.
        assertEquals(today.minusDays(1), badges.getValue(Achievement.GentleDay).earnedOn)
    }

    @Test
    fun `levels follow the calm points`() {
        assertEquals(CalmLevel(1, 0, 0, 100), CalmLevel.of(0))
        assertEquals(CalmLevel(4, 640, 600, 1_000), CalmLevel.of(640))
        assertEquals(CalmLevel(7, 2_500, 2_100, null), CalmLevel.of(2_500))
    }
}
