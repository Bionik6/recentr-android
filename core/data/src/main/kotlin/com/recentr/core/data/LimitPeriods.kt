package com.recentr.core.data

import com.recentr.core.model.LimitReset
import com.recentr.core.model.Rule
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters

// The period a daily limit's budget is counted over, from its "Resets" setting. Pure functions of
// the time and the zone, so the app blocker, the rules list and the tests agree.

/**
 * Start (epoch millis) of the limit period holding [now]: the last reset at or before it.
 * [LimitReset.Midnight]: the local day. [LimitReset.FourAm]: from 4:00 to 4:00, so the hours after
 * midnight still count in the evening's budget. [LimitReset.Monday]: the week, from Monday 0:00.
 */
fun LimitReset.periodStart(now: Long, zone: ZoneId): Long {
    val local = Instant.ofEpochMilli(now).atZone(zone)
    val date = local.toLocalDate()
    val start = when (this) {
        LimitReset.Midnight -> date.atStartOfDay(zone)
        LimitReset.FourAm -> {
            val day = if (local.toLocalTime() < FOUR_AM) date.minusDays(1) else date
            day.atTime(FOUR_AM).atZone(zone)
        }
        LimitReset.Monday -> date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).atStartOfDay(zone)
    }
    return start.toInstant().toEpochMilli()
}

/** Whether a reset of [this] happened after [since] and at or before [now]: a new limit period. */
fun LimitReset.resetBetween(since: Long, now: Long, zone: ZoneId): Boolean =
    periodStart(now, zone) != periodStart(since, zone)

/**
 * Minutes per package used so far in the current period of each [LimitReset] ([periodStart]):
 * what a daily limit trips on, and its "used / budget". Only the resets of some limit are read.
 */
data class LimitUsage(val byReset: Map<LimitReset, Map<String, Int>>) {

    /** Minutes [rule]'s apps were used in its current period, or null when that period wasn't read. */
    fun of(rule: Rule): Int? = byReset[rule.limitReset]?.let(rule::usedOf)
}

private val FOUR_AM: LocalTime = LocalTime.of(4, 0)
