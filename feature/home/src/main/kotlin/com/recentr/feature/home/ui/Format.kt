package com.recentr.feature.home.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.res.stringResource
import com.recentr.core.designsystem.component.AvatarStyle
import com.recentr.core.designsystem.format.clockTime
import com.recentr.core.designsystem.illustration.Sticker
import com.recentr.core.model.AvatarColor
import com.recentr.core.model.RuleType
import com.recentr.feature.home.R

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

/** "32m43s", "1h02m03s", "42s": the time a running rule has left, to the second, as on its card. */
internal fun timeLeft(millis: Long): String {
    val total = maxOf(0L, (millis + 999) / 1000)
    val h = total / 3600
    val m = (total % 3600) / 60
    val s = total % 60
    return when {
        h > 0 -> "%dh%02dm%02ds".format(h, m, s)
        m > 0 -> "%dm%02ds".format(m, s)
        else -> "${s}s"
    }
}

/** "9:00 AM" (or "09:00") for a minute-of-day. */
@Composable
@ReadOnlyComposable
internal fun formatClock(minuteOfDay: Int): String = clockTime(minuteOfDay)

internal val AvatarColor.style: AvatarStyle get() = AvatarStyle.valueOf(name)

internal val RuleType.sticker: Sticker
    get() = when (this) {
        RuleType.Session -> Sticker.Bolt
        RuleType.Lock -> Sticker.Lock
        RuleType.Limit -> Sticker.Timer
        RuleType.Bedtime -> Sticker.Moon
    }
