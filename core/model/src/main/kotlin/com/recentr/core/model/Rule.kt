package com.recentr.core.model

import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlinx.serialization.Serializable

/**
 * Kind of blocking rule — each maps to a sticker in the UI. Owned by the rules flow; what each one
 * does, starts and ends with is in docs/RULES.md.
 */
enum class RuleType {
    /** Blocks apps for a set time, from Start (Sticker/Bolt). */
    Session,
    /** Keeps apps blocked from Activate until it's turned off, with a few unlocks a day (Sticker/Lock). */
    Lock,
    /** A time budget for today, from Start; once spent, the apps are blocked until midnight (Sticker/Timer). */
    Limit,
    /** A routine: blocks apps during windows that repeat (Sticker/Moon). Saved under its first name. */
    Bedtime,
}

/** Sticker shown for a rule (Figma "Sticker" set). Defaults to the rule type's sticker. */
enum class RuleSticker { Bolt, Lock, Timer, Moon, Leaf, Note, Flame }

val RuleType.defaultSticker: RuleSticker
    get() = when (this) {
        RuleType.Session -> RuleSticker.Bolt
        RuleType.Lock -> RuleSticker.Lock
        RuleType.Limit -> RuleSticker.Timer
        RuleType.Bedtime -> RuleSticker.Moon
    }

/** A weekly time window, minutes from midnight. `days` uses ISO numbering (1 = Monday). */
@Serializable
data class RuleSchedule(
    val startMinute: Int,
    val endMinute: Int,
    val days: Set<Int> = (1..7).toSet(),
) {
    /** True when the window spans midnight (e.g. 11 PM → 7 AM). */
    val overnight: Boolean get() = endMinute <= startMinute

    /** Length of the window in minutes. */
    val lengthMinutes: Int
        get() = if (overnight) MINUTES_PER_DAY - startMinute + endMinute else endMinute - startMinute

    /** A window of a whole day: it holds each of its [days], midnight to midnight. */
    val allDay: Boolean get() = lengthMinutes >= MINUTES_PER_DAY

    /** Whether [epochMillis] falls inside the window (an overnight window belongs to its start day). */
    fun isActiveAt(epochMillis: Long, zone: ZoneId = ZoneId.systemDefault()): Boolean {
        val time = ZonedDateTime.ofInstant(Instant.ofEpochMilli(epochMillis), zone)
        val minute = time.hour * 60 + time.minute
        val day = time.dayOfWeek.value
        if (allDay) return day in days
        return if (!overnight) {
            day in days && minute in startMinute until endMinute
        } else {
            val previousDay = if (day == 1) 7 else day - 1
            (day in days && minute >= startMinute) || (previousDay in days && minute < endMinute)
        }
    }

    /**
     * When the window holding [epochMillis] ends (epoch millis), or null outside the windows. A
     * whole-day window ends at midnight.
     */
    fun windowEndAt(epochMillis: Long, zone: ZoneId = ZoneId.systemDefault()): Long? {
        if (!isActiveAt(epochMillis, zone)) return null
        val time = ZonedDateTime.ofInstant(Instant.ofEpochMilli(epochMillis), zone)
        val today = time.toLocalDate()
        if (allDay) return today.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
        val minute = time.hour * 60 + time.minute
        val endDate = if (overnight && minute >= startMinute) today.plusDays(1) else today
        return endDate.atStartOfDay(zone).plusMinutes(endMinute.toLong()).toInstant().toEpochMilli()
    }

    /** When the window next starts after [epochMillis], within a week; null without days. */
    fun nextStartAfter(epochMillis: Long, zone: ZoneId = ZoneId.systemDefault()): Long? {
        val today = Instant.ofEpochMilli(epochMillis).atZone(zone).toLocalDate()
        return (0..7).asSequence()
            .map { today.plusDays(it.toLong()) }
            .filter { it.dayOfWeek.value in days }
            .map { it.atStartOfDay(zone).plusMinutes(startMinute.toLong()).toInstant().toEpochMilli() }
            .firstOrNull { it > epochMillis }
    }

    /**
     * The minutes of the week inside the window (0 = Monday 0:00), as [isActiveAt] reads them: an
     * overnight window belongs to its start day, Sunday night's runs into Monday morning.
     */
    fun minutesOfWeek(): BooleanArray {
        val week = BooleanArray(MINUTES_PER_WEEK)
        days.filter { it in 1..7 }.forEach { day ->
            val dayStart = (day - 1) * MINUTES_PER_DAY
            if (allDay) {
                for (minute in 0 until MINUTES_PER_DAY) week[dayStart + minute] = true
            } else {
                for (minute in 0 until lengthMinutes) week[(dayStart + startMinute + minute) % MINUTES_PER_WEEK] = true
            }
        }
        return week
    }

    /** Whether this window holds every minute of the week [other] holds (a wider routine window). */
    fun covers(other: RuleSchedule?): Boolean {
        if (other == null) return true
        val mine = minutesOfWeek()
        return other.minutesOfWeek().withIndex().all { (minute, held) -> !held || mine[minute] }
    }

    companion object {
        const val MINUTES_PER_DAY = 24 * 60
        const val MINUTES_PER_WEEK = 7 * MINUTES_PER_DAY
    }
}

/**
 * How a blocked app can be opened anyway (Figma "How do you unlock?"). Declared by effort, lightest
 * first: a harder one is a reinforcement ([Rule.edit]). There is no "No unlocks": Strict mode is
 * the way to have none.
 */
enum class UnlockMethod {
    /** Wait a few seconds ([Rule.waitSeconds]). */
    Wait,
    /** Re-type a painful text. */
    Retype,
    /** Scan a code you keep elsewhere. */
    ScanCode,
}

/**
 * A change kept for when the rule's engagement ends ([Rule.edit]): at [appliesAt], [rule] replaces
 * the rule's settings, or the rule is deleted when [rule] is null.
 */
@Serializable
data class RuleChange(val appliesAt: Long, val rule: Rule?)

@Serializable
data class Rule(
    val id: String,
    val type: RuleType,
    val name: String,
    /** Apps the rule blocks. */
    val apps: List<UsageApp> = emptyList(),
    /** Routines: their weekly window. Sessions, locks and limits have no days or hours. */
    val schedule: RuleSchedule? = null,
    /** Limits: the budget for today, in minutes. */
    val limitMinutes: Int? = null,
    /** Sessions: length in minutes. */
    val durationMinutes: Int? = null,
    /** Limits: minutes their apps were used since Start. */
    val usedMinutes: Int = 0,
    /** Sessions and routines ([canBeStrict]): while engaged, only an emergency unlock gets past them. */
    val strictMode: Boolean = false,
    val unlockMethod: UnlockMethod = UnlockMethod.Wait,
    val enabled: Boolean = true,
    /** Routines: epoch millis until which the rule is paused, or null. [Long.MAX_VALUE] = until resumed. */
    val pausedUntil: Long? = null,
    val streakDays: Int = 0,
    /**
     * Windows starting before this (epoch millis) don't count toward [streakDays]: when the rule
     * was created, the end of the last window counted, or when the streak broke. Null in rules
     * saved before streaks were counted (they count from the start of the day they're read).
     */
    val streakFrom: Long? = null,
    /**
     * Epoch millis of Start (a running session, a limit started today) or of Activate (a lock that
     * is on), or null.
     */
    val activeSince: Long? = null,
    /**
     * End of the running session (epoch millis), set at Start: a shorter length doesn't shorten it.
     * Null for sessions started before it was kept (they end [durationMinutes] after Start).
     */
    val activeUntil: Long? = null,
    val sticker: RuleSticker = type.defaultSticker,
    /** Unlock method [UnlockMethod.Wait]: seconds to wait. */
    val waitSeconds: Int = 10,
    /** Unlocks allowed per day, or null for unlimited. */
    val unlocksPerDay: Int? = 5,
    val unlocksUsedToday: Int = 0,
    /** How long one unlock lasts, in minutes. */
    val unlockDurationMinutes: Int = 5,
    /** "Why" reason shown on a sticky note. */
    val note: String? = null,
    /** Routines: reminder before a window starts, in minutes, or null for none. */
    val reminderMinutesBefore: Int? = null,
    /** A change kept for when the engagement ends ([edit]), or null. */
    val pending: RuleChange? = null,
) {
    fun isPaused(now: Long): Boolean = pausedUntil != null && pausedUntil > now

    /** Whether the app published under [packageName] is one of [apps]. */
    fun targets(packageName: String): Boolean = apps.any { it.packageName == packageName }

    /** End of the running session, or null when no session is running. */
    val sessionEndsAt: Long?
        get() = if (type != RuleType.Session) null else activeSince?.let { start -> activeUntil ?: (start + (durationMinutes ?: 0) * 60_000L) }

    /** A session running at [now]. */
    fun isRunning(now: Long): Boolean = sessionEndsAt?.let { now < it } == true

    /** A limit started today: when it ends, the first midnight after Start. Null when it isn't started. */
    fun limitEndsAt(zone: ZoneId = ZoneId.systemDefault()): Long? =
        if (type != RuleType.Limit) null else activeSince?.let { nextMidnight(it, zone) }

    /**
     * Whether the rule is engaged at [now] (docs/RULES.md): a session while it runs, a routine
     * inside one of its windows and not paused, a lock from Activate until it's off, a limit from
     * Start until midnight, budget spent or not.
     */
    fun isEnforcing(now: Long, zone: ZoneId = ZoneId.systemDefault()): Boolean = when {
        !enabled || isPaused(now) -> false
        else -> when (type) {
            RuleType.Session -> isRunning(now)
            RuleType.Bedtime -> schedule?.isActiveAt(now, zone) == true
            RuleType.Lock -> activeSince != null
            RuleType.Limit -> limitEndsAt(zone)?.let { now < it } == true
        }
    }

    /** Engaged and blocking its apps now: a limit once its budget is spent, the others while engaged. */
    fun isHolding(now: Long, zone: ZoneId = ZoneId.systemDefault()): Boolean =
        isEnforcing(now, zone) && (type != RuleType.Limit || usedMinutes >= (limitMinutes ?: Int.MAX_VALUE))

    /**
     * When the engagement holding [now] ends, or null when the rule isn't engaged: a session's end,
     * the end of a routine's window, midnight for a lock or a limit. Changes kept by [edit] apply then.
     */
    fun engagementEnd(now: Long, zone: ZoneId = ZoneId.systemDefault()): Long? {
        if (!isEnforcing(now, zone)) return null
        return when (type) {
            RuleType.Session -> sessionEndsAt
            RuleType.Bedtime -> schedule?.windowEndAt(now, zone)
            RuleType.Lock -> nextMidnight(now, zone)
            RuleType.Limit -> limitEndsAt(zone)
        }
    }

    /** Strict mode holds at [now]: the strict rule is engaged, so only an emergency unlock gets past it. */
    fun isStrictLocked(now: Long, zone: ZoneId = ZoneId.systemDefault()): Boolean =
        canBeStrict && strictMode && isEnforcing(now, zone)

    /** Rules that end on their own (sessions, routines) can be made strict. */
    val canBeStrict: Boolean
        get() = type == RuleType.Session || type == RuleType.Bedtime

    companion object {
        /** The first local midnight after [epochMillis]. */
        fun nextMidnight(epochMillis: Long, zone: ZoneId): Long =
            Instant.ofEpochMilli(epochMillis).atZone(zone).toLocalDate().plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
    }
}
