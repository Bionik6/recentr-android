package com.recentr.feature.rules.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.recentr.core.data.RecentrData
import com.recentr.core.data.Walk
import com.recentr.core.data.unlockMethodInUse
import com.recentr.core.data.waitSecondsInUse
import com.recentr.core.designsystem.format.clockTimeShort
import com.recentr.core.designsystem.format.wallClockTime
import com.recentr.core.designsystem.illustration.Sticker
import com.recentr.core.model.AppCategory
import com.recentr.core.model.HabitRepeat
import com.recentr.core.model.LimitAction
import com.recentr.core.model.LimitReset
import com.recentr.core.model.Rule
import com.recentr.core.model.RuleSchedule
import com.recentr.core.model.RuleSticker
import com.recentr.core.model.RuleType
import com.recentr.core.model.UnlockMethod
import com.recentr.core.model.UsageApp
import com.recentr.feature.rules.R
import kotlin.math.max

/* Mappings between the shared model and the design system. */

internal val RuleSticker.asSticker: Sticker
    get() = when (this) {
        RuleSticker.Bolt -> Sticker.Bolt
        RuleSticker.Lock -> Sticker.Lock
        RuleSticker.Timer -> Sticker.Timer
        RuleSticker.Moon -> Sticker.Moon
        RuleSticker.Leaf -> Sticker.Leaf
        RuleSticker.Note -> Sticker.Note
        RuleSticker.Flame -> Sticker.Flame
    }

/** Category name in values ("Social · 3"). */
internal val AppCategory.title: Int
    get() = when (this) {
        AppCategory.Social -> R.string.rules_category_social
        AppCategory.Entertainment -> R.string.rules_category_entertainment
        AppCategory.Music -> R.string.rules_category_music
        AppCategory.Games -> R.string.rules_category_games
        AppCategory.Productivity -> R.string.rules_category_productivity
        AppCategory.Other -> R.string.rules_category_other
    }

/**
 * The device's launcher apps, read once (empty while loading, and in previews). The app pickers
 * re-read them when they open: apps may have been installed or removed since.
 */
@Composable
internal fun rememberInstalledApps(refresh: Boolean = false): List<UsageApp>? {
    if (LocalInspectionMode.current) return emptyList()
    val repository = RecentrData.installedApps
    val apps by repository.apps.collectAsStateWithLifecycle()
    LaunchedEffect(repository, refresh) { if (refresh) repository.refresh() else repository.load() }
    return apps
}

@Composable
@ReadOnlyComposable
internal fun RuleType.label(): String = stringResource(
    when (this) {
        RuleType.Session -> R.string.rules_type_session
        RuleType.Lock -> R.string.rules_type_lock
        RuleType.Limit -> R.string.rules_type_limit
        RuleType.Bedtime -> R.string.rules_type_routine
        RuleType.Habit -> R.string.rules_type_habit
    },
)

/* Durations. */

/** "30 min", "1 h", "1 h 30 min". */
@Composable
@ReadOnlyComposable
internal fun durationLabel(minutes: Int): String {
    val h = minutes / 60
    val m = minutes % 60
    return when {
        h == 0 -> stringResource(R.string.rules_minutes_short, m)
        m == 0 -> stringResource(R.string.rules_hours_short, h)
        else -> stringResource(R.string.rules_hours_minutes, h, m)
    }
}

/** Non-composable [durationLabel] for ViewModels ([string] resolves resources with args). */
internal fun durationText(minutes: Int, string: (Int, Array<out Any>) -> String): String {
    val h = minutes / 60
    val m = minutes % 60
    return when {
        h == 0 -> string(R.string.rules_minutes_short, arrayOf(m))
        m == 0 -> string(R.string.rules_hours_short, arrayOf(h))
        else -> string(R.string.rules_hours_minutes, arrayOf(h, m))
    }
}

/** Compact budget format used on cards: "0s", "10m", "2h", "2h15". */
@Composable
@ReadOnlyComposable
internal fun compactDuration(minutes: Int): String {
    val h = minutes / 60
    val m = minutes % 60
    return when {
        minutes <= 0 -> stringResource(R.string.rules_compact_seconds, 0)
        h == 0 -> stringResource(R.string.rules_compact_minutes, m)
        m == 0 -> stringResource(R.string.rules_compact_hours, h)
        else -> stringResource(R.string.rules_compact_hours_minutes, h, m)
    }
}

/** Countdown "24:18" (or "1:04:18" over an hour). */
internal fun countdown(millis: Long): String {
    val total = max(0L, (millis + 999) / 1000)
    val h = total / 3600
    val m = (total % 3600) / 60
    val s = total % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%d:%02d".format(m, s)
}

/** "23m42s", "2h32m", "2d 3h": how long until a routine starts, to the second only in its last hour. */
internal fun startCountdown(millis: Long): String {
    val seconds = max(0L, (millis + 999) / 1000)
    if (seconds < 3600) return if (seconds < 60) "${seconds}s" else "%dm%02ds".format(seconds / 60, seconds % 60)
    val minutes = (seconds + 59) / 60
    if (minutes < 24 * 60) return "%dh%02dm".format(minutes / 60, minutes % 60)
    val hours = (minutes + 59) / 60
    return if (hours % 24 == 0L) "${hours / 24}d" else "${hours / 24}d ${hours % 24}h"
}

/* Times & days. */

/** "9 AM", "9:30 PM" (or "09:00", "21:30" on the 24-hour clock). */
@Composable
@ReadOnlyComposable
internal fun clockLabel(minuteOfDay: Int): String = clockTimeShort(minuteOfDay)

internal fun isPm(minuteOfDay: Int): Boolean = (minuteOfDay / 60) % 24 >= 12

/** "Every day", "Weekdays", "Weekends", "Mon, Wed". */
@Composable
@ReadOnlyComposable
internal fun daysLabel(days: Set<Int>): String = when (days) {
    (1..7).toSet() -> stringResource(R.string.rules_days_every_day)
    (1..5).toSet() -> stringResource(R.string.rules_days_weekdays)
    setOf(6, 7) -> stringResource(R.string.rules_days_weekends)
    else -> {
        val names = LocalContext.current.resources.getStringArray(R.array.rules_day_short)
        days.sorted().joinToString(", ") { names[it - 1] }
    }
}

/** "9 AM – 5 PM". */
@Composable
@ReadOnlyComposable
internal fun timeRangeLabel(schedule: RuleSchedule): String =
    stringResource(R.string.rules_time_range, clockLabel(schedule.startMinute), clockLabel(schedule.endMinute))

/** "9 AM – 5 PM · Weekdays", or "All day" for a full-day window. */
@Composable
@ReadOnlyComposable
internal fun scheduleLabel(schedule: RuleSchedule?): String = when {
    schedule == null -> stringResource(R.string.rules_all_day)
    schedule.lengthMinutes >= RuleSchedule.MINUTES_PER_DAY ->
        stringResource(R.string.rules_schedule_format, stringResource(R.string.rules_all_day), daysLabel(schedule.days))
    else -> stringResource(R.string.rules_schedule_format, timeRangeLabel(schedule), daysLabel(schedule.days))
}

/** "3:45 PM" (or "15:45") for an epoch time. */
@Composable
@ReadOnlyComposable
internal fun wallClock(epochMillis: Long): String = wallClockTime(epochMillis)

/* Apps. */

/** "Instagram, X" for up to two apps, else "3 apps". */
@Composable
@ReadOnlyComposable
internal fun appsLabel(apps: List<UsageApp>): String = when {
    apps.isEmpty() -> stringResource(R.string.rules_none)
    apps.size <= 2 -> apps.joinToString(", ") { it.label }
    else -> pluralStringResource(R.plurals.rules_apps_count, apps.size, apps.size)
}

/** "Social · 3" when all apps share a category (other than Other), else "3 apps". */
@Composable
@ReadOnlyComposable
internal fun appsCategoryLabel(apps: List<UsageApp>): String {
    val category = apps.map { it.category }.distinct().singleOrNull()
    return if (apps.size > 1 && category != null && category != AppCategory.Other) {
        stringResource(R.string.rules_category_count, stringResource(category.title), apps.size)
    } else {
        appsLabel(apps)
    }
}

/** Default rule name built from the blocked apps: "Instagram & X", "Instagram & 2 more". */
internal fun appsName(apps: List<UsageApp>, two: (String, String) -> String, more: (String, Int) -> String): String? = when {
    apps.isEmpty() -> null
    apps.size == 1 -> apps.single().label
    apps.size == 2 -> two(apps[0].label, apps[1].label)
    else -> more(apps[0].label, apps.size - 1)
}

/* Rule options. */

@Composable
@ReadOnlyComposable
internal fun unlockLabel(rule: Rule): String = when (rule.unlockMethodInUse) {
    UnlockMethod.None, UnlockMethod.Strict -> stringResource(R.string.rules_unlock_none)
    UnlockMethod.Wait -> stringResource(R.string.rules_unlock_wait, rule.waitSecondsInUse)
    UnlockMethod.Reason -> stringResource(R.string.rules_unlock_reason)
    UnlockMethod.Retype -> stringResource(R.string.rules_unlock_retype)
    UnlockMethod.ScanCode -> stringResource(R.string.rules_unlock_scan)
    UnlockMethod.Walk -> stringResource(R.string.rules_unlock_walk, Walk.STEPS)
}

@Composable
@ReadOnlyComposable
internal fun unlocksPerDayLabel(perDay: Int?): String =
    perDay?.toString() ?: stringResource(R.string.rules_unlimited)

@Composable
@ReadOnlyComposable
internal fun limitResetLabel(reset: LimitReset): String = when (reset) {
    LimitReset.Midnight -> stringResource(R.string.rules_reset_midnight)
    LimitReset.FourAm -> clockLabel(4 * 60)
    LimitReset.Monday -> stringResource(R.string.rules_reset_monday)
}

@Composable
@ReadOnlyComposable
internal fun limitActionLabel(action: LimitAction): String = stringResource(
    when (action) {
        LimitAction.Lock -> R.string.rules_action_lock
        LimitAction.Remind -> R.string.rules_action_remind
        LimitAction.AskReason -> R.string.rules_action_ask
    },
)

@Composable
@ReadOnlyComposable
internal fun habitRepeatLabel(repeat: HabitRepeat): String = stringResource(
    when (repeat) {
        HabitRepeat.EveryDay -> R.string.rules_days_every_day
        HabitRepeat.Weekdays -> R.string.rules_days_weekdays
        HabitRepeat.Weekends -> R.string.rules_days_weekends
    },
)

@Composable
@ReadOnlyComposable
internal fun reminderLabel(minutes: Int?): String =
    if (minutes == null) stringResource(R.string.rules_reminder_off) else stringResource(R.string.rules_reminder_before, minutes)
