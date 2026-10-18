package com.recentr.feature.home.ui

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.res.stringResource
import com.recentr.core.designsystem.illustration.DemoApp
import com.recentr.core.designsystem.illustration.Sticker
import com.recentr.core.model.AppId
import com.recentr.core.model.RuleType
import com.recentr.core.model.UsagePeriod
import com.recentr.feature.home.R
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Formats minutes like the design: "1h12", "4h", "38m". */
@Composable
@ReadOnlyComposable
internal fun formatDuration(minutes: Int): String {
    val h = minutes / 60
    val m = minutes % 60
    return when {
        h == 0 -> stringResource(R.string.home_duration_minutes, m)
        m == 0 -> stringResource(R.string.home_duration_hours, h)
        else -> stringResource(R.string.home_duration_hours_minutes, h, m)
    }
}

private val TimeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("h:mm a", Locale.US)

/** "9:00 AM" for a minute-of-day. */
internal fun formatClock(minuteOfDay: Int): String =
    LocalTime.of((minuteOfDay / 60) % 24, minuteOfDay % 60).format(TimeFormatter)

@get:StringRes
internal val UsagePeriod.labelRes: Int
    get() = when (this) {
        UsagePeriod.Today -> R.string.home_period_today
        UsagePeriod.Yesterday -> R.string.home_period_yesterday
        UsagePeriod.ThisWeek -> R.string.home_period_this_week
        UsagePeriod.ThisMonth -> R.string.home_period_this_month
    }

/** `DemoApp` ↔ `AppId` by enum name. */
internal val AppId.demoApp: DemoApp get() = DemoApp.valueOf(name)

internal val RuleType.sticker: Sticker
    get() = when (this) {
        RuleType.Session -> Sticker.Bolt
        RuleType.Lock -> Sticker.Lock
        RuleType.Limit -> Sticker.Timer
        RuleType.Bedtime -> Sticker.Moon
        RuleType.Habit -> Sticker.Leaf
    }
