package com.recentr.core.data

import com.recentr.core.model.HabitRepeat
import com.recentr.core.model.LimitReset
import com.recentr.core.model.Rule
import com.recentr.core.model.RuleSchedule
import com.recentr.core.model.RuleType
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * A rule's streak (Rule detail, rule card): the days in a row it was kept.
 *
 * Each window of the rule counts once it is over, if it started at or after [Rule.streakFrom]
 * (when the rule was created, the end of the last window counted, or when the streak broke):
 * a session or routine's scheduled window, a lock's day, a habit's day with every task done, a
 * daily limit's period kept within its budget (a weekly limit's week counts 7 days). A manual
 * session counts its first run of each day. Days the rule doesn't run (a weekday routine's
 * weekend) neither count nor break it.
 *
 * Back to 0: a habit day left undone, a limit gone over, a window that ends while the rule is
 * off, a pause over a window ([pausedOver]), an emergency unlock past the rule ([broken]).
 * Unlocks the rule allows don't break it.
 */
internal object RuleStreaks {

    /** One window of a rule; [days] it adds to the streak when kept. */
    data class Window(val start: Long, val end: Long, val days: Int = 1)

    /** Windows are found a day at a time; nothing repeats less often than weekly. */
    private const val LOOKAHEAD_DAYS = 8L

    /** Whether a window of [rule] that wasn't counted yet is over at [now]. */
    fun due(rule: Rule, now: Long, zone: ZoneId): Boolean {
        val from = rule.streakFrom ?: return false
        return !rule.oneShot && windowsFrom(rule, from, now, zone).firstOrNull()?.let { it.end <= now } == true
    }

    /**
     * [rule] with the windows over at [now] counted. [since] is when the rule's counters were
     * last settled: a habit's tasks and a limit's minutes are those of the window holding it.
     */
    fun advance(rule: Rule, since: Long, now: Long, zone: ZoneId): Rule {
        val from = rule.streakFrom ?: return rule
        if (rule.oneShot) return rule
        var days = rule.streakDays
        var counted = from
        for (window in windowsFrom(rule, from, now, zone)) {
            if (window.end > now) break
            val kept = rule.enabled && !rule.isPaused(window.start) && rule.kept(window, since)
            days = if (kept) days + window.days else 0
            counted = window.end
        }
        return if (counted == from) rule else rule.copy(streakDays = days, streakFrom = counted)
    }

    /** A manual session's run that just ended at [end]: the day's first run counts. */
    fun runEnded(rule: Rule, end: Long, zone: ZoneId): Rule {
        val start = rule.activeSince ?: return rule
        val from = rule.streakFrom ?: return rule
        if (rule.oneShot || rule.schedule != null || start < from) return rule
        val nextDay = Instant.ofEpochMilli(end).atZone(zone).toLocalDate().plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
        return rule.copy(streakDays = rule.streakDays + 1, streakFrom = nextDay)
    }

    /** Broken at [now] (an emergency unlock past it): back to 0, counting again from the next window. */
    fun broken(rule: Rule, now: Long): Rule = rule.copy(streakDays = 0, streakFrom = now)

    /**
     * Paused from [now] until [until] ([Long.MAX_VALUE] = until resumed): if the pause covers part
     * of a window (or a running session), the streak breaks and counts again from the pause's end.
     */
    fun pausedOver(rule: Rule, now: Long, until: Long, zone: ZoneId): Rule {
        if (rule.streakFrom == null || rule.oneShot) return rule
        val running = rule.schedule == null && rule.isRunning(now)
        // Windows in progress at [now] started up to a week ago (a weekly limit).
        val overlaps = running || windowsFrom(rule, now - LOOKAHEAD_DAYS * DAY_MILLIS, minOf(until, now + LOOKAHEAD_DAYS * DAY_MILLIS), zone)
            .takeWhile { it.start < until }
            .any { it.end > now }
        return if (overlaps) rule.copy(streakDays = 0, streakFrom = until) else rule
    }

    /** Resumed at [now]: an open-ended pause counts again from here. */
    fun resumed(rule: Rule, now: Long): Rule =
        if ((rule.streakFrom ?: 0) > now) rule.copy(streakFrom = now) else rule

    private fun Rule.kept(window: Window, since: Long): Boolean {
        val settledInside = since >= window.start && since < window.end
        return when (type) {
            // The tasks are those of the day [since] falls in; a day nobody opened the app is undone.
            RuleType.Habit -> settledInside && habitTasks.isNotEmpty() && habitTasks.all { it.done }
            // Only the period [since] falls in has its minutes; the others are taken as kept.
            RuleType.Limit -> !settledInside || usedMinutes <= (limitMinutes ?: Int.MAX_VALUE)
            else -> true
        }
    }

    /**
     * The windows of [rule] starting at or after [from], in order, looked for up to [until] (plus
     * a week so the next one is found).
     */
    fun windowsFrom(rule: Rule, from: Long, until: Long, zone: ZoneId): Sequence<Window> {
        if (from > until) return emptySequence()
        val first = Instant.ofEpochMilli(from).atZone(zone).toLocalDate().minusDays(1)
        val last = Instant.ofEpochMilli(until).atZone(zone).toLocalDate().plusDays(LOOKAHEAD_DAYS)
        return generateSequence(first) { it.plusDays(1) }
            .takeWhile { !it.isAfter(last) }
            .mapNotNull { rule.windowOn(it, zone) }
            .filter { it.start >= from }
    }

    /** The window of [rule] starting on [date], if any. */
    private fun Rule.windowOn(date: LocalDate, zone: ZoneId): Window? {
        fun at(day: LocalDate, minute: Int = 0) = day.atStartOfDay(zone).plusMinutes(minute.toLong()).toInstant().toEpochMilli()
        val schedule = schedule
        val weekday = date.dayOfWeek.value
        return when {
            type == RuleType.Habit -> if (habitRepeat.on(weekday)) Window(at(date), at(date.plusDays(1))) else null
            type == RuleType.Limit -> when (limitReset) {
                LimitReset.Midnight -> Window(at(date), at(date.plusDays(1)))
                LimitReset.FourAm -> Window(at(date, 4 * 60), at(date.plusDays(1), 4 * 60))
                LimitReset.Monday -> if (date.dayOfWeek == DayOfWeek.MONDAY) Window(at(date), at(date.plusDays(7)), days = 7) else null
            }
            schedule != null && schedule.lengthMinutes < RuleSchedule.MINUTES_PER_DAY ->
                if (weekday in schedule.days) {
                    val start = date.atStartOfDay(zone).plusMinutes(schedule.startMinute.toLong())
                    Window(start.toInstant().toEpochMilli(), start.plusMinutes(schedule.lengthMinutes.toLong()).toInstant().toEpochMilli())
                } else {
                    null
                }
            // All-day locks: each of their days.
            type == RuleType.Lock -> if (weekday in (schedule?.days ?: (1..7).toSet())) Window(at(date), at(date.plusDays(1))) else null
            // Manual sessions count their runs (runEnded); a routine always has a window.
            else -> null
        }
    }

    private fun HabitRepeat.on(weekday: Int): Boolean = when (this) {
        HabitRepeat.EveryDay -> true
        HabitRepeat.Weekdays -> weekday <= 5
        HabitRepeat.Weekends -> weekday >= 6
    }

    private const val DAY_MILLIS = 24 * 3_600_000L
}
