package com.recentr.feature.profile.ui

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.res.stringResource
import com.recentr.core.designsystem.component.AvatarStyle
import com.recentr.core.designsystem.format.clockTimeShort
import com.recentr.core.designsystem.illustration.BrandIcon
import com.recentr.core.model.AppIconStyle
import com.recentr.core.model.AvatarColor
import com.recentr.core.model.PlusPlan
import com.recentr.core.model.ThemeMode
import com.recentr.core.model.TimeFormat
import com.recentr.feature.profile.R

/** Compact duration of the Profile cards: "6h", "2h10", "45m". */
@Composable
@ReadOnlyComposable
internal fun compactDuration(minutes: Int): String {
    val h = minutes / 60
    val m = minutes % 60
    return when {
        h == 0 -> stringResource(R.string.duration_m, m)
        m == 0 -> stringResource(R.string.duration_h, h)
        else -> stringResource(R.string.duration_h_m_compact, h, m)
    }
}

/** Spaced duration of the recap tiles: "5h 50m", "45m". */
@Composable
@ReadOnlyComposable
internal fun spacedDuration(minutes: Int): String {
    val h = minutes / 60
    val m = minutes % 60
    return when {
        h == 0 -> stringResource(R.string.duration_m, m)
        m == 0 -> stringResource(R.string.duration_h, h)
        else -> stringResource(R.string.duration_h_m, h, m)
    }
}

/** "10 PM", "7:30 AM" (or "22:00", "07:30") for a minute of the day. */
@Composable
@ReadOnlyComposable
internal fun formatShortClock(minuteOfDay: Int): String = clockTimeShort(minuteOfDay)

internal val AvatarColor.style: AvatarStyle get() = AvatarStyle.valueOf(name)

internal val AppIconStyle.brandIcon: BrandIcon get() = BrandIcon.valueOf(name)

@get:StringRes
internal val ThemeMode.label: Int
    get() = when (this) {
        ThemeMode.Light -> R.string.theme_light
        ThemeMode.Dark -> R.string.theme_dark
        ThemeMode.Auto -> R.string.theme_auto
    }

@get:StringRes
internal val AppIconStyle.label: Int
    get() = when (this) {
        AppIconStyle.Sky -> R.string.icon_sky
        AppIconStyle.Night -> R.string.icon_night
        AppIconStyle.Mint -> R.string.icon_mint
        AppIconStyle.Sunset -> R.string.icon_sunset
    }

@get:StringRes
internal val TimeFormat.label: Int
    get() = when (this) {
        TimeFormat.AmPm -> R.string.time_format_am_pm
        TimeFormat.TwentyFourHour -> R.string.time_format_24h
    }

@get:StringRes
internal val PlusPlan.label: Int
    get() = when (this) {
        PlusPlan.Yearly -> R.string.plus_yearly
        PlusPlan.Monthly -> R.string.plus_monthly
    }

@get:StringRes
internal val PlusPlan.price: Int
    get() = when (this) {
        PlusPlan.Yearly -> R.string.plus_yearly_price
        PlusPlan.Monthly -> R.string.plus_monthly_price
    }
