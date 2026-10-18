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
import com.recentr.core.designsystem.illustration.AppIcon
import com.recentr.core.designsystem.illustration.DemoApp
import com.recentr.core.designsystem.illustration.InstalledAppIcon
import com.recentr.core.model.AppCategory
import com.recentr.core.model.UsageApp
import com.recentr.core.model.UsagePeriod
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

/** Figma icon of a catalogue app, launcher icon of any other app. */
@Composable
internal fun UsageAppIcon(app: UsageApp, size: Dp, modifier: Modifier = Modifier) {
    val catalogue = app.catalogue
    if (catalogue != null) {
        AppIcon(DemoApp.valueOf(catalogue.name), modifier = modifier, size = size, contentDescription = null)
    } else {
        InstalledAppIcon(app.packageName, size = size, modifier = modifier)
    }
}
