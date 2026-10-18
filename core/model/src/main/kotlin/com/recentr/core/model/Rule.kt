package com.recentr.core.model

import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime

/** Kind of blocking rule — each maps to a sticker in the UI. Owned by the rules flow. */
enum class RuleType {
    /** Focus now for a fixed duration, or on a weekly schedule (Sticker/Bolt). */
    Session,
    /** Block apps entirely, all day or on a schedule (Sticker/Lock). */
    Lock,
    /** Daily time budget (Sticker/Timer). */
    Limit,
    /** Evenings & nights routine (Sticker/Moon). */
    Bedtime,
    /** Grow a good habit: finish small tasks to unlock apps (Sticker/Leaf). */
    Habit,
}

/** Sticker shown for a rule (Figma "Sticker" set). Defaults to the rule type's sticker. */
enum class RuleSticker { Bolt, Lock, Timer, Moon, Leaf, Note, Flame }

val RuleType.defaultSticker: RuleSticker
    get() = when (this) {
        RuleType.Session -> RuleSticker.Bolt
        RuleType.Lock -> RuleSticker.Lock
        RuleType.Limit -> RuleSticker.Timer
        RuleType.Bedtime -> RuleSticker.Moon
        RuleType.Habit -> RuleSticker.Leaf
    }

/** A weekly time window, minutes from midnight. `days` uses ISO numbering (1 = Monday). */
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

    /** Whether [epochMillis] falls inside the window (an overnight window belongs to its start day). */
    fun isActiveAt(epochMillis: Long, zone: ZoneId = ZoneId.systemDefault()): Boolean {
        val time = ZonedDateTime.ofInstant(Instant.ofEpochMilli(epochMillis), zone)
        val minute = time.hour * 60 + time.minute
        val day = time.dayOfWeek.value
        if (lengthMinutes >= MINUTES_PER_DAY) return day in days
        return if (!overnight) {
            day in days && minute in startMinute until endMinute
        } else {
            val previousDay = if (day == 1) 7 else day - 1
            (day in days && minute >= startMinute) || (previousDay in days && minute < endMinute)
        }
    }

    companion object {
        const val MINUTES_PER_DAY = 24 * 60
    }
}

/** How a blocked app can be opened anyway (Figma "How do you unlock?"). */
enum class UnlockMethod {
    /** No unlocks while the rule is active. */
    None,
    /** Wait a few seconds ([Rule.waitSeconds]). */
    Wait,
    /** Give a good reason. */
    Reason,
    /** Kept for compatibility: same as [None]. */
    Strict,
    /** Re-type a painful text. */
    Retype,
    /** Scan a code you keep elsewhere. */
    ScanCode,
}

/** Strict mode scope (Figma "Strict mode" segmented control). */
enum class StrictLevel { StartTime, During, Always }

/** When a daily limit resets. */
enum class LimitReset { Midnight, FourAm, Monday }

/** What happens when a daily limit is used up. */
enum class LimitAction { Lock, Remind, AskReason }

/** Repeat pattern of a habit rule. */
enum class HabitRepeat { EveryDay, Weekdays, Weekends }

/** One task of a habit rule ("Read 10 pages"). */
data class HabitTask(val id: String, val title: String, val done: Boolean = false)

data class Rule(
    val id: String,
    val type: RuleType,
    val name: String,
    val apps: List<AppId> = emptyList(),
    /** Weekly window, or null when the rule is active all day / is a one-shot session. */
    val schedule: RuleSchedule? = null,
    /** Limit rules: daily budget in minutes. */
    val limitMinutes: Int? = null,
    /** Session rules: duration in minutes. */
    val durationMinutes: Int? = null,
    /** Minutes already used today (limit rules) or during the current window. */
    val usedMinutes: Int = 0,
    val strictMode: Boolean = false,
    val unlockMethod: UnlockMethod = UnlockMethod.Wait,
    val enabled: Boolean = true,
    /** Epoch millis until which the rule is paused, or null. [Long.MAX_VALUE] = until resumed. */
    val pausedUntil: Long? = null,
    val streakDays: Int = 0,
    /** Epoch millis when an active session started, or null. */
    val activeSince: Long? = null,
    val sticker: RuleSticker = type.defaultSticker,
    val strictLevel: StrictLevel = StrictLevel.During,
    /** Unlock method [UnlockMethod.Wait]: seconds to wait. */
    val waitSeconds: Int = 5,
    /** Unlocks allowed per day, or null for unlimited. */
    val unlocksPerDay: Int? = 5,
    val unlocksUsedToday: Int = 0,
    /** How long one unlock lasts, in minutes. */
    val unlockDurationMinutes: Int = 5,
    /** "Why" reason shown on a sticky note. */
    val note: String? = null,
    /** Reminder before the window starts, in minutes, or null for none. */
    val reminderMinutesBefore: Int? = null,
    /** Bedtime: block every app except [allowedApps] (plus calls & maps). */
    val blockAllApps: Boolean = false,
    val allowedApps: List<AppId> = emptyList(),
    val limitReset: LimitReset = LimitReset.Midnight,
    val limitAction: LimitAction = LimitAction.Lock,
    val habitTasks: List<HabitTask> = emptyList(),
    val habitRepeat: HabitRepeat = HabitRepeat.EveryDay,
    /** One-shot session started with "Start blocking": removed once it ends. */
    val oneShot: Boolean = false,
) {
    fun isPaused(now: Long): Boolean = pausedUntil != null && pausedUntil > now

    /** End of the running session, or null when no session is running. */
    val sessionEndsAt: Long?
        get() = activeSince?.let { start -> start + (durationMinutes ?: 0) * 60_000L }

    fun isRunning(now: Long): Boolean {
        val end = sessionEndsAt ?: return false
        return now < end && !isPaused(now)
    }

    /** True while the rule is enforcing (running session, scheduled window or all-day rule). */
    fun isEnforcing(now: Long): Boolean = when {
        !enabled || isPaused(now) -> false
        activeSince != null -> isRunning(now)
        type == RuleType.Limit || type == RuleType.Habit -> true
        schedule == null -> type == RuleType.Lock
        else -> schedule.isActiveAt(now)
    }

    /** Strict mode currently prevents edits, deletes and unlocks. */
    fun isStrictLocked(now: Long): Boolean = strictMode && when (strictLevel) {
        StrictLevel.Always -> true
        StrictLevel.During, StrictLevel.StartTime -> isEnforcing(now)
    }
}
