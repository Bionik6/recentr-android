package com.recentr.core.data

import com.recentr.core.model.Achievement
import com.recentr.core.model.AchievementProgress
import com.recentr.core.model.DayStats
import com.recentr.core.model.FocusSessionRecord
import com.recentr.core.model.Rule
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

/** What the badges are evaluated against. */
internal data class AchievementInputs(
    val now: LocalDateTime,
    /** Usage history, oldest first, ending today (partial). */
    val days: List<DayStats>,
    val goalMinutes: Int,
    /** The daily habit reported in the onboarding ("won back" hours). */
    val baselineDailyMinutes: Int,
    val focusSessions: List<FocusSessionRecord>,
    val rules: List<Rule>,
    /** When the user started (onboarding done, or install). */
    val startedOn: LocalDate,
    val zone: ZoneId,
)

/**
 * Pure evaluation of the [Achievement] catalogue: each badge's progress and the day it was earned
 * (the first day its condition held in the history), unit-tested with fake days.
 */
internal object AchievementEngine {

    /** A night (11 PM → 5 AM) or morning (5 → 8 AM) "without screens" allows this much. */
    private const val QUIET_TOLERANCE_MINUTES = 1

    fun evaluate(inputs: AchievementInputs): List<AchievementProgress> {
        val today = inputs.now.toLocalDate()
        val days = inputs.days.filter { !it.date.isAfter(today) }
        val streakDays = days.map { StreakDay(it.date, it.hasData, it.minutes) }
        val newestFirst = streakDays.asReversed().asSequence()
        val current = Streaks.current(newestFirst, inputs.goalMinutes)
        val strictCurrent = Streaks.current(newestFirst, inputs.goalMinutes, saversPerMonth = 0)
        val runs = Streaks.runs(streakDays, inputs.goalMinutes)
        val strictRuns = Streaks.runs(streakDays, inputs.goalMinutes, saversPerMonth = 0)
        val completed = days.filter { it.hasData && it.date.isBefore(today) }

        /**
         * Earned the first day a streak reached the target: in the history (savers spent as days
         * go by) or in the current streak (the one Home shows, savers spent from today back).
         */
        fun streak(achievement: Achievement, runs: List<Int>, current: StreakState): AchievementProgress {
            val inHistory = runs.indexOfFirst { it >= achievement.target }.takeIf { it >= 0 }?.let { days[it].date }
            val inCurrent = current.statuses.keys.sorted().getOrNull(achievement.target - 1)
            return AchievementProgress(
                achievement = achievement,
                progress = current.days.coerceAtMost(achievement.target),
                earnedOn = listOfNotNull(inHistory, inCurrent).minOrNull(),
            )
        }

        fun firstDay(achievement: Achievement, condition: (DayStats) -> Boolean): AchievementProgress {
            val day = completed.firstOrNull(condition)
            return AchievementProgress(achievement, if (day != null) 1 else 0, day?.date)
        }

        val focus = inputs.focusSessions.sortedBy { it.startedAt }
        val nights = quietRun(nights(days, inputs.now))
        val mornings = quietRun(mornings(days, inputs.now))
        val wonBack = completed.map { day -> day.date to (inputs.baselineDailyMinutes - day.minutes).coerceAtLeast(0) }

        // Usage history predates the app: what was already done counts from the first day.
        fun AchievementProgress.fromStart() = copy(earnedOn = earnedOn?.let { maxOf(it, inputs.startedOn) })

        return Achievement.entries.map { achievement ->
            when (achievement) {
                Achievement.FirstLock -> {
                    val done = inputs.rules.any { it.apps.isNotEmpty() }
                    AchievementProgress(achievement, if (done) 1 else 0, inputs.startedOn.takeIf { done })
                }
                Achievement.NoteToSelf -> {
                    val done = inputs.rules.any { !it.note.isNullOrBlank() }
                    AchievementProgress(achievement, if (done) 1 else 0, inputs.startedOn.takeIf { done })
                }
                Achievement.FirstFocus -> AchievementProgress(
                    achievement,
                    focus.size.coerceAtMost(1),
                    focus.firstOrNull()?.let { date(it.startedAt, inputs.zone) },
                )
                Achievement.FocusTenHours -> cumulative(
                    achievement,
                    focus.map { date(it.startedAt, inputs.zone) to it.focusedMinutes },
                    unit = 60,
                )
                Achievement.UrgesResisted -> cumulative(
                    achievement,
                    focus.map { date(it.startedAt, inputs.zone) to it.urgesResisted },
                    unit = 1,
                )
                Achievement.SevenDayStreak, Achievement.TwoCalmWeeks, Achievement.CalmMonth -> streak(achievement, runs, current)
                Achievement.GoalCrusher -> streak(achievement, strictRuns, strictCurrent)
                Achievement.NightOwlRetired -> nights.progress(achievement)
                Achievement.SlowMornings -> mornings.progress(achievement)
                Achievement.ScreenFreeSunday -> firstDay(achievement) { it.date.dayOfWeek == DayOfWeek.SUNDAY && it.minutes < 30 }
                Achievement.GentleDay -> firstDay(achievement) { it.minutes < 60 }
                Achievement.HandsOff -> firstDay(achievement) { it.pickups < 20 }
                Achievement.TenHoursWonBack, Achievement.FiftyHoursWonBack -> cumulative(achievement, wonBack, unit = 60)
            }.fromStart()
        }
    }

    /** Badges counting a total over time: earned the day the running total reaches the target. */
    private fun cumulative(achievement: Achievement, amounts: List<Pair<LocalDate, Int>>, unit: Int): AchievementProgress {
        var total = 0
        var earnedOn: LocalDate? = null
        for ((date, amount) in amounts) {
            total += amount
            if (earnedOn == null && total >= achievement.target * unit) earnedOn = date
        }
        return AchievementProgress(achievement, (total / unit).coerceAtMost(achievement.target), earnedOn)
    }

    /**
     * Nights of [days] that are over (11 PM of a day → 5 AM of the next), with whether they were
     * quiet, oldest first, dated by the morning they end on.
     */
    private fun nights(days: List<DayStats>, now: LocalDateTime): List<Pair<LocalDate, Boolean?>> =
        days.zipWithNext().filter { (_, morning) -> morning.date.isBefore(now.toLocalDate()) || now.hour >= 5 }.map { (evening, morning) ->
            val quiet = if (!evening.hasData || !morning.hasData) {
                null
            } else {
                evening.hourlyMinutes[23] + morning.hourlyMinutes.subList(0, 5).sum() <= QUIET_TOLERANCE_MINUTES
            }
            morning.date to quiet
        }

    /** Mornings (5 → 8 AM) of [days] that are over, with whether they were quiet, oldest first. */
    private fun mornings(days: List<DayStats>, now: LocalDateTime): List<Pair<LocalDate, Boolean?>> =
        days.filter { it.date.isBefore(now.toLocalDate()) || now.hour >= 8 }.map { day ->
            day.date to if (day.hasData) day.hourlyMinutes.subList(5, 8).sum() <= QUIET_TOLERANCE_MINUTES else null
        }

    /** Consecutive quiet [periods]: the run so far and the day each length was first reached. */
    private class QuietRun(val current: Int, val reachedOn: Map<Int, LocalDate>) {
        fun progress(achievement: Achievement) = AchievementProgress(
            achievement = achievement,
            progress = current.coerceAtMost(achievement.target),
            earnedOn = reachedOn.filterKeys { it >= achievement.target }.values.minOrNull(),
        )
    }

    private fun quietRun(periods: List<Pair<LocalDate, Boolean?>>): QuietRun {
        var run = 0
        val reachedOn = HashMap<Int, LocalDate>()
        for ((date, quiet) in periods) {
            run = if (quiet == true) run + 1 else 0
            if (run > 0) reachedOn.putIfAbsent(run, date)
        }
        return QuietRun(run, reachedOn)
    }

    private fun date(millis: Long, zone: ZoneId): LocalDate = Instant.ofEpochMilli(millis).atZone(zone).toLocalDate()
}
