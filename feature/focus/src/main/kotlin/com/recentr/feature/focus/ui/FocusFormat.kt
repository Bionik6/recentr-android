package com.recentr.feature.focus.ui

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.res.stringResource
import com.recentr.core.designsystem.illustration.DemoApp
import com.recentr.core.designsystem.illustration.Sticker
import com.recentr.core.model.AppId
import com.recentr.core.model.FocusRecordKind
import com.recentr.core.model.FocusSound
import com.recentr.feature.focus.R
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Countdown "45:00", "3:00", "1:30:00" (seconds rounded up so 0:00 only shows at the end). */
internal fun formatCountdown(millis: Long): String {
    val totalSeconds = ((millis + 999) / 1000).coerceAtLeast(0)
    val h = totalSeconds / 3600
    val m = totalSeconds % 3600 / 60
    val s = totalSeconds % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%d:%02d".format(m, s)
}

private val ClockFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("h:mm a", Locale.US)

/** "10:42 AM". */
internal fun formatClock(epochMillis: Long): String =
    Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()).toLocalTime().format(ClockFormatter)

/** "6h 40m", "45m", "2h". */
@Composable
@ReadOnlyComposable
internal fun formatDuration(minutes: Int): String {
    val h = minutes / 60
    val m = minutes % 60
    return when {
        h == 0 -> stringResource(R.string.duration_m, m)
        m == 0 -> stringResource(R.string.duration_h, h)
        else -> stringResource(R.string.duration_h_m, h, m)
    }
}

internal val AppId.demoApp: DemoApp get() = DemoApp.valueOf(name)

internal val FocusRecordKind.sticker: Sticker
    get() = when (this) {
        FocusRecordKind.Timer -> Sticker.Headphones
        FocusRecordKind.RuleSession -> Sticker.Bolt
        FocusRecordKind.Habit -> Sticker.Leaf
    }

internal val FocusSound.sticker: Sticker
    get() = when (this) {
        FocusSound.Rain -> Sticker.Headphones
        FocusSound.Forest -> Sticker.Leaf
        FocusSound.Night -> Sticker.Moon
        FocusSound.Fireplace -> Sticker.Flame
        FocusSound.Clock -> Sticker.Timer
        FocusSound.Silence -> Sticker.Lock
    }

@get:StringRes
internal val FocusSound.label: Int
    get() = when (this) {
        FocusSound.Rain -> R.string.sound_rain
        FocusSound.Forest -> R.string.sound_forest
        FocusSound.Night -> R.string.sound_night
        FocusSound.Fireplace -> R.string.sound_fireplace
        FocusSound.Clock -> R.string.sound_clock
        FocusSound.Silence -> R.string.sound_silence
    }

@get:StringRes
internal val FocusSound.hint: Int
    get() = when (this) {
        FocusSound.Rain -> R.string.sound_rain_hint
        FocusSound.Forest -> R.string.sound_forest_hint
        FocusSound.Night -> R.string.sound_night_hint
        FocusSound.Fireplace -> R.string.sound_fireplace_hint
        FocusSound.Clock -> R.string.sound_clock_hint
        FocusSound.Silence -> R.string.sound_silence_hint
    }
