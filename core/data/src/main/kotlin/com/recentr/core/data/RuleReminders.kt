package com.recentr.core.data

import com.recentr.core.model.Rule
import com.recentr.core.model.RuleType
import java.time.Instant
import java.time.ZoneId

/** The next "starts in N min" reminder: when it is due, and the window start of each rule it is for. */
data class DueReminder(
    val atMillis: Long,
    /** Rule id → epoch millis when its window starts. */
    val starts: Map<String, Long>,
)

/**
 * The next reminder after [now] among the routines with one (Rule detail › Reminders): enabled
 * routines, [Rule.reminderMinutesBefore] a window's start, skipping the starts that fall in a
 * pause. Whole-day windows have no start to announce. Rules due at the same moment share it.
 */
fun nextRuleReminder(rules: List<Rule>, now: Long, zone: ZoneId = ZoneId.systemDefault()): DueReminder? {
    val today = Instant.ofEpochMilli(now).atZone(zone).toLocalDate()
    val due = rules.mapNotNull { rule ->
        val before = rule.reminderMinutesBefore ?: return@mapNotNull null
        val schedule = rule.schedule?.takeIf { rule.type == RuleType.Bedtime } ?: return@mapNotNull null
        if (!rule.enabled || schedule.allDay) return@mapNotNull null
        // Eight days: a reminder can fall on the evening before its start, a week ahead at most.
        (0..8).asSequence()
            .map { today.plusDays(it.toLong()) }
            .filter { it.dayOfWeek.value in schedule.days }
            .map { it.atStartOfDay(zone).plusMinutes(schedule.startMinute.toLong()).toInstant().toEpochMilli() }
            .filter { start -> !rule.isPaused(start) }
            .map { start -> start - before * 60_000L to start }
            .firstOrNull { (at, _) -> at > now }
            ?.let { (at, start) -> Triple(rule.id, at, start) }
    }
    val at = due.minOfOrNull { it.second } ?: return null
    return DueReminder(at, due.filter { it.second == at }.associate { (id, _, start) -> id to start })
}

/** Whole minutes left until [start] for a reminder posted at [now], rounded up ("starts in 10 min"). */
fun reminderMinutesLeft(start: Long, now: Long): Int = ((start - now + 59_999) / 60_000).toInt().coerceAtLeast(1)
