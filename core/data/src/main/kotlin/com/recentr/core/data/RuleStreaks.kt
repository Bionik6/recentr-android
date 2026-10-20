package com.recentr.core.data

import com.recentr.core.model.Rule
import com.recentr.core.model.RuleType
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * A rule's streak (Rule detail, rule card): the days in a row it was kept.
 *
 * Each window of the rule counts once it is over, if it started at or after [Rule.streakFrom]
 * (when the rule was created, the end of the last window counted, or when the streak broke): a
 * routine's window, each day a lock is on (from Activate on its first day, until it turns off),
 * a limit's day from Start to midnight kept within its budget. A session counts its first run of
 * each day. Days the rule doesn't run (a weekday routine's weekend, a lock that is off, a limit
 * not started) neither count nor break it.
 *
 * Back to 0: a limit gone over, a window that ends while the rule is off, a pause over a window
 * ([pausedOver]), an emergency unlock past the rule ([broken]). Unlocks the rule allows don't
 * break it.
 */
internal object RuleStreaks {

    /** One window of a rule. */
    data class Window(val start: Long, val end: Long)

    /** Windows are found a day at a time; nothing repeats less often than weekly. */
    private const val LOOKAHEAD_DAYS = 8L

    /** Whether a window of [rule] that wasn't counted yet is over at [now]. */
    fun due(rule: Rule, now: Long, zone: ZoneId): Boolean {
        val from = rule.streakFrom ?: return false
        return windowsFrom(rule, from, now, zone).firstOrNull()?.let { it.end <= now } == true
    }

    /**
     * [rule] with the windows over at [now] counted. A limit's minutes are still those of its day:
     * it ends once its day was counted ([RulesRepository.settle]).
     */
    fun advance(rule: Rule, now: Long, zone: ZoneId): Rule {
        val from = rule.streakFrom ?: return rule
        var days = rule.streakDays
        var counted = from
        for (window in windowsFrom(rule, from, now, zone)) {
            if (window.end > now) break
            val kept = rule.enabled && !rule.isPaused(window.start) && rule.kept
            days = if (kept) days + 1 else 0
            counted = window.end
        }
        return if (counted == from) rule else rule.copy(streakDays = days, streakFrom = counted)
    }

    /** A session's run that just ended at [end]: the day's first run counts. */
    fun runEnded(rule: Rule, end: Long, zone: ZoneId): Rule {
        val start = rule.activeSince ?: return rule
        val from = rule.streakFrom ?: return rule
        if (rule.type != RuleType.Session || start < from) return rule
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
        if (rule.streakFrom == null) return rule
        // Windows in progress at [now] started up to a day ago.
        val overlaps = rule.isRunning(now) ||
            windowsFrom(rule, now - LOOKAHEAD_DAYS * DAY_MILLIS, minOf(until, now + LOOKAHEAD_DAYS * DAY_MILLIS), zone)
                .takeWhile { it.start < until }
                .any { it.end > now }
        return if (overlaps) rule.copy(streakDays = 0, streakFrom = until) else rule
    }

    /** Resumed at [now]: an open-ended pause counts again from here. */
    fun resumed(rule: Rule, now: Long): Rule =
        if ((rule.streakFrom ?: 0) > now) rule.copy(streakFrom = now) else rule

    /** A limit is kept within its budget; the other rules are kept unless broken or paused. */
    private val Rule.kept: Boolean
        get() = type != RuleType.Limit || usedMinutes <= (limitMinutes ?: Int.MAX_VALUE)

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
        return when (type) {
            RuleType.Bedtime -> {
                val schedule = schedule?.takeIf { date.dayOfWeek.value in it.days } ?: return null
                if (schedule.allDay) {
                    Window(at(date), at(date.plusDays(1)))
                } else {
                    val start = date.atStartOfDay(zone).plusMinutes(schedule.startMinute.toLong())
                    Window(start.toInstant().toEpochMilli(), start.plusMinutes(schedule.lengthMinutes.toLong()).toInstant().toEpochMilli())
                }
            }
            // Each day the lock is on: its first from Activate, none past a kept turn-off or delete.
            RuleType.Lock -> {
                val since = activeSince ?: return null
                val end = at(date.plusDays(1))
                val start = maxOf(at(date), since)
                val off = pending?.takeIf { it.rule?.activeSince == null }?.appliesAt
                if (start >= end || (off != null && end > off)) null else Window(start, end)
            }
            // The day it was started, from Start to midnight.
            RuleType.Limit -> {
                val since = activeSince ?: return null
                if (Instant.ofEpochMilli(since).atZone(zone).toLocalDate() != date) null else Window(since, Rule.nextMidnight(since, zone))
            }
            // A session's runs count as they end ([runEnded]).
            RuleType.Session -> null
        }
    }

    private const val DAY_MILLIS = 24 * 3_600_000L
}
