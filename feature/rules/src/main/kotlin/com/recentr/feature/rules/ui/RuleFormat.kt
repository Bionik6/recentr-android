package com.recentr.feature.rules.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.recentr.core.designsystem.illustration.DemoApp
import com.recentr.core.designsystem.illustration.Sticker
import com.recentr.core.model.AppId
import com.recentr.core.model.HabitRepeat
import com.recentr.core.model.LimitAction
import com.recentr.core.model.LimitReset
import com.recentr.core.model.Rule
import com.recentr.core.model.RuleSchedule
import com.recentr.core.model.RuleSticker
import com.recentr.core.model.RuleType
import com.recentr.core.model.UnlockMethod
import com.recentr.feature.rules.R
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
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

internal val AppId.asDemoApp: DemoApp get() = DemoApp.valueOf(name)

/** Category of the demo app catalogue (Figma "Choose apps" sections). */
internal enum class AppCategory(val label: Int, val title: Int, val apps: List<AppId>) {
    Social(R.string.rules_cat_social, R.string.rules_category_social, listOf(AppId.X, AppId.Instagram, AppId.TikTok, AppId.Facebook)),
    Entertainment(R.string.rules_cat_entertainment, R.string.rules_category_entertainment, listOf(AppId.YouTube, AppId.Roblox, AppId.Spotify)),
    Productivity(R.string.rules_cat_productivity, R.string.rules_category_productivity, listOf(AppId.Gmail)),
}

internal val AppId.category: AppCategory get() = AppCategory.entries.first { this in it.apps }

/** Apps a Bedtime "all except" rule ends up blocking (for icons). */
internal fun Rule.blockedApps(): List<AppId> =
    if (blockAllApps) AppCategory.entries.flatMap { it.apps }.filterNot { it in allowedApps } else apps

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

/* Times & days. */

/** "9 AM", "9:30 PM". */
@Composable
@ReadOnlyComposable
internal fun clockLabel(minuteOfDay: Int): String {
    val hour24 = (minuteOfDay / 60) % 24
    val minute = minuteOfDay % 60
    val hour = hour12(hour24)
    val pm = hour24 >= 12
    return when {
        minute == 0 && pm -> stringResource(R.string.rules_time_pm, hour)
        minute == 0 -> stringResource(R.string.rules_time_am, hour)
        pm -> stringResource(R.string.rules_time_pm_min, hour, minute)
        else -> stringResource(R.string.rules_time_am_min, hour, minute)
    }
}

/** "11:00" — 12-hour clock without the AM/PM suffix (time cards). */
internal fun clockDigits(minuteOfDay: Int): String =
    "%d:%02d".format(hour12((minuteOfDay / 60) % 24), minuteOfDay % 60)

/** "9" or "9:30" — 12-hour clock, minutes only when needed (detail tile "9 – 5"). */
internal fun clockShort(minuteOfDay: Int): String {
    val hour = hour12((minuteOfDay / 60) % 24)
    val minute = minuteOfDay % 60
    return if (minute == 0) hour.toString() else "%d:%02d".format(hour, minute)
}

internal fun isPm(minuteOfDay: Int): Boolean = (minuteOfDay / 60) % 24 >= 12

private fun hour12(hour24: Int): Int = when {
    hour24 == 0 -> 12
    hour24 > 12 -> hour24 - 12
    else -> hour24
}

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

/** "3:45 PM" for an epoch time. */
@Composable
@ReadOnlyComposable
internal fun wallClock(epochMillis: Long): String {
    val time = ZonedDateTime.ofInstant(Instant.ofEpochMilli(epochMillis), ZoneId.systemDefault())
    return clockLabelWithMinutes(time.hour * 60 + time.minute)
}

/** "3:45 PM" / "3:00 PM" (always with minutes). */
@Composable
@ReadOnlyComposable
internal fun clockLabelWithMinutes(minuteOfDay: Int): String {
    val hour24 = (minuteOfDay / 60) % 24
    val hour = hour12(hour24)
    return if (hour24 >= 12) {
        stringResource(R.string.rules_time_pm_min, hour, minuteOfDay % 60)
    } else {
        stringResource(R.string.rules_time_am_min, hour, minuteOfDay % 60)
    }
}

/* Apps. */

/** "Instagram, X" for up to two apps, else "3 apps". */
@Composable
@ReadOnlyComposable
internal fun appsLabel(apps: List<AppId>): String = when {
    apps.isEmpty() -> stringResource(R.string.rules_none)
    apps.size <= 2 -> apps.joinToString(", ") { it.displayName }
    else -> pluralStringResource(R.plurals.rules_apps_count, apps.size, apps.size)
}

/** "Social · 3" when all apps share a category, else "3 apps". */
@Composable
@ReadOnlyComposable
internal fun appsCategoryLabel(apps: List<AppId>): String {
    val categories = apps.map { it.category }.distinct()
    return if (apps.isNotEmpty() && categories.size == 1) {
        stringResource(R.string.rules_category_count, stringResource(categories.single().title), apps.size)
    } else {
        appsLabel(apps)
    }
}

/** Default rule name built from the blocked apps: "Instagram & X", "Instagram & 2 more". */
internal fun appsName(apps: List<AppId>, two: (String, String) -> String, more: (String, Int) -> String): String? = when {
    apps.isEmpty() -> null
    apps.size == 1 -> apps.single().displayName
    apps.size == 2 -> two(apps[0].displayName, apps[1].displayName)
    else -> more(apps[0].displayName, apps.size - 1)
}

/* Rule options. */

@Composable
@ReadOnlyComposable
internal fun unlockLabel(rule: Rule): String = when (rule.unlockMethod) {
    UnlockMethod.None, UnlockMethod.Strict -> stringResource(R.string.rules_unlock_none)
    UnlockMethod.Wait -> stringResource(R.string.rules_unlock_wait, rule.waitSeconds)
    UnlockMethod.Reason -> stringResource(R.string.rules_unlock_reason)
    UnlockMethod.Retype -> stringResource(R.string.rules_unlock_retype)
    UnlockMethod.ScanCode -> stringResource(R.string.rules_unlock_scan)
}

@Composable
@ReadOnlyComposable
internal fun unlocksPerDayLabel(perDay: Int?): String =
    perDay?.toString() ?: stringResource(R.string.rules_unlimited)

@Composable
@ReadOnlyComposable
internal fun limitResetLabel(reset: LimitReset): String = stringResource(
    when (reset) {
        LimitReset.Midnight -> R.string.rules_reset_midnight
        LimitReset.FourAm -> R.string.rules_reset_four_am
        LimitReset.Monday -> R.string.rules_reset_monday
    },
)

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

/** Value of the Bedtime "Block" row: "All except 3" or the chosen apps. */
@Composable
@ReadOnlyComposable
internal fun bedtimeBlockLabel(rule: Rule): String =
    if (rule.blockAllApps) stringResource(R.string.rules_all_except, ESSENTIAL_APPS + rule.allowedApps.size) else appsLabel(rule.apps)

/** Value of the Bedtime "Always allowed" row: "Calls, Maps, Gmail". */
@Composable
@ReadOnlyComposable
internal fun allowedAppsLabel(rule: Rule): String {
    val essentials = stringResource(R.string.rules_calls_maps)
    return (listOf(essentials) + rule.allowedApps.map { it.displayName }).joinToString(", ")
}

/** Calls and Maps are always allowed during routines (not part of the app catalogue). */
internal const val ESSENTIAL_APPS = 2
