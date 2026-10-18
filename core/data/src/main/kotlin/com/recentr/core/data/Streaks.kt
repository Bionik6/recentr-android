package com.recentr.core.data

import java.time.LocalDate
import java.time.YearMonth

/** How a day counts in a streak. */
enum class StreakDayStatus {
    /** Within the daily goal. */
    OnTarget,
    /** Over the goal, covered by a streak saver. */
    Saved,
    /** Over the goal: the streak broke there. */
    Missed,
    /** No usage data (before the retained history). */
    NoData,
}

/** A day as the streak sees it. */
data class StreakDay(val date: LocalDate, val hasData: Boolean, val minutes: Int)

/** The current streak: its length, how each of its days counted and the savers left. */
data class StreakState(
    val days: Int,
    /** The days of the streak, newest first. */
    val statuses: Map<LocalDate, StreakDayStatus>,
    /** Savers left this month (the month of the newest day). */
    val saversLeft: Int,
    /** Most recent day of this month a saver covered, if any. */
    val lastSavedDay: LocalDate?,
)

/**
 * Streak rules shared by Home and the Insights / Achievements screens: consecutive days within the
 * daily goal, where a day over the goal uses one of the month's streak savers instead of breaking
 * the streak. A streak never starts on a saved day.
 */
object Streaks {
    /** Savers granted each calendar month. */
    const val SAVERS_PER_MONTH = 3

    /**
     * The streak ending on the first of [newestFirst] (today, counted while it is within the goal
     * so far), walking back lazily until a day has no data, or is over the goal with no saver left
     * for its month. Today counts even without data.
     */
    fun current(
        newestFirst: Sequence<StreakDay>,
        goalMinutes: Int,
        saversPerMonth: Int = SAVERS_PER_MONTH,
        maxDays: Int = Int.MAX_VALUE,
    ): StreakState {
        val used = HashMap<YearMonth, Int>()
        val counted = ArrayList<Pair<LocalDate, StreakDayStatus>>()
        var thisMonth: YearMonth? = null
        for ((index, day) in newestFirst.take(maxDays).withIndex()) {
            if (index == 0) thisMonth = YearMonth.from(day.date)
            if (index > 0 && !day.hasData) break
            if (day.minutes <= goalMinutes) {
                counted += day.date to StreakDayStatus.OnTarget
                continue
            }
            val month = YearMonth.from(day.date)
            val spent = used[month] ?: 0
            if (spent >= saversPerMonth) break
            used[month] = spent + 1
            counted += day.date to StreakDayStatus.Saved
        }
        // The oldest days of the streak must be on target: trailing saves give their saver back.
        while (counted.lastOrNull()?.second == StreakDayStatus.Saved) {
            val (date, _) = counted.removeAt(counted.lastIndex)
            val month = YearMonth.from(date)
            used[month] = (used[month] ?: 1) - 1
        }
        val month = thisMonth
        return StreakState(
            days = counted.size,
            statuses = counted.toMap(),
            saversLeft = saversPerMonth - (month?.let { used[it] } ?: 0),
            lastSavedDay = counted.firstOrNull { it.second == StreakDayStatus.Saved && YearMonth.from(it.first) == month }?.first,
        )
    }

    /**
     * Length of the streak on each day of [chronological] (0 when it broke that day), applying
     * savers as days go by: a day over the goal uses a saver of its month only while a streak runs.
     */
    fun runs(
        chronological: List<StreakDay>,
        goalMinutes: Int,
        saversPerMonth: Int = SAVERS_PER_MONTH,
    ): List<Int> {
        val used = HashMap<YearMonth, Int>()
        var run = 0
        var previous: LocalDate? = null
        return chronological.map { day ->
            if (previous != null && day.date != previous.plusDays(1)) run = 0
            previous = day.date
            val month = YearMonth.from(day.date)
            run = when {
                !day.hasData -> 0
                day.minutes <= goalMinutes -> run + 1
                run > 0 && (used[month] ?: 0) < saversPerMonth -> {
                    used[month] = (used[month] ?: 0) + 1
                    run + 1
                }
                else -> 0
            }
            run
        }
    }

    /** Longest streak over [chronological]. */
    fun best(chronological: List<StreakDay>, goalMinutes: Int, saversPerMonth: Int = SAVERS_PER_MONTH): Int =
        runs(chronological, goalMinutes, saversPerMonth).maxOrNull() ?: 0
}
