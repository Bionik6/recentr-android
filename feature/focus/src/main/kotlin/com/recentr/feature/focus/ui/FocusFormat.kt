package com.recentr.feature.focus.ui

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.recentr.core.data.RecentrData
import com.recentr.core.designsystem.format.wallClockTime
import com.recentr.core.designsystem.illustration.Sticker
import com.recentr.core.model.AppCategory
import com.recentr.core.model.FocusRecordKind
import com.recentr.core.model.FocusSound
import com.recentr.core.model.UsageApp
import com.recentr.feature.focus.R

/** Countdown "45:00", "3:00", "1:30:00" (seconds rounded up so 0:00 only shows at the end). */
internal fun formatCountdown(millis: Long): String {
    val totalSeconds = ((millis + 999) / 1000).coerceAtLeast(0)
    val h = totalSeconds / 3600
    val m = totalSeconds % 3600 / 60
    val s = totalSeconds % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%d:%02d".format(m, s)
}

/** "10:42 AM" (or "10:42"). */
@Composable
@ReadOnlyComposable
internal fun formatClock(epochMillis: Long): String = wallClockTime(epochMillis)

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

/** Section caption of the "Block during focus" sheet ("SOCIAL"). */
internal val AppCategory.sectionLabel: Int
    get() = when (this) {
        AppCategory.Social -> R.string.block_cat_social
        AppCategory.Entertainment -> R.string.block_cat_entertainment
        AppCategory.Music -> R.string.block_cat_music
        AppCategory.Games -> R.string.block_cat_games
        AppCategory.Productivity -> R.string.block_cat_productivity
        AppCategory.Other -> R.string.block_cat_other
    }

/**
 * The device's launcher apps, read once (null while loading, empty in previews); [refresh]
 * re-reads them, for the app picker: apps may have been installed or removed since.
 */
@Composable
internal fun rememberInstalledApps(refresh: Boolean = false): List<UsageApp>? {
    if (LocalInspectionMode.current) return emptyList()
    val repository = RecentrData.installedApps
    val apps by repository.apps.collectAsStateWithLifecycle()
    LaunchedEffect(repository, refresh) { if (refresh) repository.refresh() else repository.load() }
    return apps
}

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
