package com.recentr.feature.home.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.res.stringResource
import com.recentr.core.designsystem.component.AvatarStyle
import com.recentr.core.designsystem.format.clockTime
import com.recentr.core.designsystem.illustration.DemoApp
import com.recentr.core.designsystem.illustration.Sticker
import com.recentr.core.model.AppId
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

/** "9:00 AM" (or "09:00") for a minute-of-day. */
@Composable
@ReadOnlyComposable
internal fun formatClock(minuteOfDay: Int): String = clockTime(minuteOfDay)

internal val AvatarColor.style: AvatarStyle get() = AvatarStyle.valueOf(name)

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
