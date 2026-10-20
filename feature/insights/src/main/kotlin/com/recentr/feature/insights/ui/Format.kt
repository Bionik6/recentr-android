package com.recentr.feature.insights.ui

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import com.recentr.core.designsystem.format.clockTime
import com.recentr.core.designsystem.format.clockTimeShort
import com.recentr.core.designsystem.illustration.InstalledAppIcon
import com.recentr.core.model.AppCategory
import com.recentr.core.model.RangeUnit
import com.recentr.core.model.UsageApp
import com.recentr.core.model.UsagePeriod
import com.recentr.core.model.UsageRange
import com.recentr.feature.insights.R
import java.time.LocalDate
import java.time.YearMonth
import kotlin.math.absoluteValue
import kotlin.math.roundToInt

/** Spaced duration of the Insights screens: "2h 05m", "3h", "54m". */
@Composable
@ReadOnlyComposable
internal fun duration(minutes: Int): String {
    val h = minutes / 60
    val m = minutes % 60
    return when {
        h == 0 -> stringResource(R.string.insights_duration_m, m)
        m == 0 -> stringResource(R.string.insights_duration_h, h)
        else -> stringResource(R.string.insights_duration_h_m, h, m)
    }
}

/** Compact duration of the goal editor, like Home: "4h20", "3h", "45m". */
@Composable
@ReadOnlyComposable
internal fun compactDuration(minutes: Int): String {
    val h = minutes / 60
    val m = minutes % 60
    return when {
        h == 0 -> stringResource(R.string.insights_duration_m, m)
        m == 0 -> stringResource(R.string.insights_duration_h, h)
        else -> stringResource(R.string.insights_duration_compact, h, m)
    }
}

/** "−48m" / "+12m" with a typographic minus, like the design. */
@Composable
@ReadOnlyComposable
internal fun signedDuration(minutes: Int): String =
    if (minutes < 0) stringResource(R.string.insights_minus, duration(minutes.absoluteValue)) else stringResource(R.string.insights_plus, duration(minutes))

/** "−12" / "+3". */
@Composable
@ReadOnlyComposable
internal fun signedCount(value: Int): String =
    if (value < 0) stringResource(R.string.insights_minus, value.absoluteValue.toString()) else stringResource(R.string.insights_plus, value.toString())

/** "−24%" / "+8%". */
@Composable
@ReadOnlyComposable
internal fun signedPercent(percent: Int): String {
    val value = stringResource(R.string.insights_percent, percent.absoluteValue)
    return if (percent < 0) stringResource(R.string.insights_minus, value) else stringResource(R.string.insights_plus, value)
}

/** Change from [previous] to [current] in percent, or null without a usable previous value. */
internal fun percentChange(current: Int, previous: Int?): Int? =
    previous?.takeIf { it > 0 }?.let { ((current - it) * 100f / it).roundToInt() }

/** "7:12 AM" (or "07:12") for a minute of the day. */
@Composable
@ReadOnlyComposable
internal fun clock(minuteOfDay: Int): String = clockTime(minuteOfDay)

/** "7 AM" (or "07:00") for an hour of the day. */
@Composable
@ReadOnlyComposable
internal fun hourLabel(hour: Int): String = clockTimeShort(hour * 60)

@Composable
@ReadOnlyComposable
internal fun monthName(month: YearMonth): String = stringArrayResource(R.array.insights_months)[month.monthValue - 1]

/** "Oct 12". */
@Composable
@ReadOnlyComposable
internal fun shortDate(date: LocalDate): String =
    stringResource(R.string.insights_date, stringArrayResource(R.array.insights_months_short)[date.monthValue - 1], date.dayOfMonth)

/** How the date selector names a day, week or month. */
internal sealed interface RangeName {
    data object Today : RangeName
    data object Yesterday : RangeName
    data object ThisWeek : RangeName
    data object LastWeek : RangeName
    data class Day(val date: LocalDate, val withYear: Boolean) : RangeName
    data class Week(val start: LocalDate, val end: LocalDate, val withYear: Boolean) : RangeName
    data class Month(val month: YearMonth, val withYear: Boolean) : RangeName
}

/** Today, yesterday, this week and last week by name; other days and weeks by date, months by name (the year when not this one). */
internal fun UsageRange.name(today: LocalDate): RangeName = when (unit) {
    RangeUnit.Day -> when (start) {
        today -> RangeName.Today
        today.minusDays(1) -> RangeName.Yesterday
        else -> RangeName.Day(start, withYear = start.year != today.year)
    }
    RangeUnit.Week -> {
        val thisWeek = UsageRange.of(RangeUnit.Week, today)
        when (this) {
            thisWeek -> RangeName.ThisWeek
            thisWeek.previous() -> RangeName.LastWeek
            else -> RangeName.Week(start, end, withYear = start.year != today.year || end.year != today.year)
        }
    }
    RangeUnit.Month -> RangeName.Month(month, withYear = month.year != today.year)
}

/** "Today", "Sun, Oct 18", "Last week", "Oct 5 – 11", "Sep 28 – Oct 4", "October", "December 2025". */
@Composable
@ReadOnlyComposable
internal fun rangeLabel(range: UsageRange, today: LocalDate): String = when (val name = range.name(today)) {
    RangeName.Today -> stringResource(R.string.insights_period_today)
    RangeName.Yesterday -> stringResource(R.string.insights_period_yesterday)
    RangeName.ThisWeek -> stringResource(R.string.insights_period_week)
    RangeName.LastWeek -> stringResource(R.string.insights_period_last_week)
    is RangeName.Day -> {
        val day = stringResource(R.string.insights_range_day, stringArrayResource(R.array.insights_weekday_short)[name.date.dayOfWeek.value - 1], shortDate(name.date))
        if (name.withYear) stringResource(R.string.insights_with_year, day, name.date.year) else day
    }
    is RangeName.Week -> {
        val first = shortDate(name.start)
        val last = if (name.end.month == name.start.month) name.end.dayOfMonth.toString() else shortDate(name.end)
        val span = stringResource(R.string.insights_range_span, first, last)
        if (name.withYear) stringResource(R.string.insights_with_year, span, name.end.year) else span
    }
    is RangeName.Month -> if (name.withYear) stringResource(R.string.insights_with_year, monthName(name.month), name.month.year) else monthName(name.month)
}

/** "Wednesday". */
@Composable
@ReadOnlyComposable
internal fun weekdayName(date: LocalDate): String = stringArrayResource(R.array.insights_weekday_full)[date.dayOfWeek.value - 1]

@get:StringRes
internal val AppCategory.labelRes: Int
    get() = when (this) {
        AppCategory.Social -> R.string.insights_category_social
        AppCategory.Entertainment -> R.string.insights_category_video
        AppCategory.Music -> R.string.insights_category_music
        AppCategory.Games -> R.string.insights_category_games
        AppCategory.Productivity -> R.string.insights_category_productivity
        AppCategory.Other -> R.string.insights_category_other
    }

@get:StringRes
internal val UsagePeriod.labelRes: Int
    get() = when (this) {
        UsagePeriod.Today -> R.string.insights_period_today
        UsagePeriod.Yesterday -> R.string.insights_period_yesterday
        UsagePeriod.ThisWeek -> R.string.insights_period_week
        UsagePeriod.ThisMonth -> R.string.insights_period_month
    }

/** The app's own launcher icon, as installed on the device. */
@Composable
internal fun UsageAppIcon(app: UsageApp, size: Dp, modifier: Modifier = Modifier) {
    InstalledAppIcon(app.packageName, size = size, modifier = modifier)
}
