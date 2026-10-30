package com.recentr.feature.focus.ui

import androidx.annotation.DrawableRes
import androidx.annotation.RawRes
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.res.stringResource
import com.recentr.core.designsystem.format.wallClockTime
import com.recentr.core.designsystem.illustration.Sticker
import com.recentr.core.model.FocusRecordKind
import com.recentr.core.model.FocusSound
import com.recentr.core.model.FocusSoundCategory
import com.recentr.core.model.FocusSoundLayer
import com.recentr.feature.focus.R

/** "10:42 AM" (or "10:42"). */
@Composable
@ReadOnlyComposable
internal fun formatClock(epochMillis: Long): String = wallClockTime(epochMillis)

/** "0:42", "12:05", "1:02:03": how long something has lasted, to the second. */
internal fun stopwatch(millis: Long): String {
    val seconds = (millis / 1000).coerceAtLeast(0)
    val (hours, minutes, rest) = Triple(seconds / 3600, seconds / 60 % 60, seconds % 60)
    return if (hours > 0) "%d:%02d:%02d".format(hours, minutes, rest) else "%d:%02d".format(minutes, rest)
}

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

internal val FocusRecordKind.sticker: Sticker
    get() = when (this) {
        FocusRecordKind.Timer -> Sticker.Headphones
        FocusRecordKind.RuleSession -> Sticker.Bolt
        FocusRecordKind.Habit -> Sticker.Leaf
    }

@get:StringRes
internal val FocusSound.label: Int
    get() = when (this) {
        FocusSound.SoftRain -> R.string.sound_soft_rain
        FocusSound.HeavyRain -> R.string.sound_heavy_rain
        FocusSound.ForestRain -> R.string.sound_forest_rain
        FocusSound.RainVillage -> R.string.sound_rain_village
        FocusSound.Thunder -> R.string.sound_thunder
        FocusSound.River -> R.string.sound_river
        FocusSound.SmallRiver -> R.string.sound_small_river
        FocusSound.Beach -> R.string.sound_beach
        FocusSound.SeaBeach -> R.string.sound_sea_beach
        FocusSound.SoftBeach -> R.string.sound_soft_beach
        FocusSound.Whale -> R.string.sound_whale
        FocusSound.ForestNight -> R.string.sound_forest_night
        FocusSound.Birds -> R.string.sound_birds
        FocusSound.Crickets -> R.string.sound_crickets
        FocusSound.CicadaBirds -> R.string.sound_cicada_birds
        FocusSound.Farm -> R.string.sound_farm
        FocusSound.Wind -> R.string.sound_wind
        FocusSound.Campfire -> R.string.sound_campfire
        FocusSound.CoffeeShop -> R.string.sound_coffee_shop
        FocusSound.Office -> R.string.sound_office
        FocusSound.StreetTraffic -> R.string.sound_street_traffic
        FocusSound.Construction -> R.string.sound_construction
        FocusSound.KeyboardTyping -> R.string.sound_keyboard_typing
        FocusSound.ClockTick -> R.string.sound_clock_tick
        FocusSound.ClockStopwatch -> R.string.sound_clock_stopwatch
        FocusSound.WhiteNoise -> R.string.sound_white_noise
        FocusSound.PinkNoise -> R.string.sound_pink_noise
    }

/** The sound's illustration. */
@get:DrawableRes
internal val FocusSound.art: Int
    get() = when (this) {
        FocusSound.SoftRain -> R.drawable.ambient_soft_rain
        FocusSound.HeavyRain -> R.drawable.ambient_heavy_rain
        FocusSound.ForestRain -> R.drawable.ambient_forest_rain
        FocusSound.RainVillage -> R.drawable.ambient_rain_village
        FocusSound.Thunder -> R.drawable.ambient_thunder
        FocusSound.River -> R.drawable.ambient_river
        FocusSound.SmallRiver -> R.drawable.ambient_small_river
        FocusSound.Beach -> R.drawable.ambient_beach
        FocusSound.SeaBeach -> R.drawable.ambient_sea_beach
        FocusSound.SoftBeach -> R.drawable.ambient_soft_beach
        FocusSound.Whale -> R.drawable.ambient_whale
        FocusSound.ForestNight -> R.drawable.ambient_forest_night
        FocusSound.Birds -> R.drawable.ambient_birds
        FocusSound.Crickets -> R.drawable.ambient_crickets
        FocusSound.CicadaBirds -> R.drawable.ambient_cicada_birds
        FocusSound.Farm -> R.drawable.ambient_farm
        FocusSound.Wind -> R.drawable.ambient_wind
        FocusSound.Campfire -> R.drawable.ambient_campfire
        FocusSound.CoffeeShop -> R.drawable.ambient_coffee_shop
        FocusSound.Office -> R.drawable.ambient_office
        FocusSound.StreetTraffic -> R.drawable.ambient_street_traffic
        FocusSound.Construction -> R.drawable.ambient_construction
        FocusSound.KeyboardTyping -> R.drawable.ambient_keyboard_typing
        FocusSound.ClockTick -> R.drawable.ambient_clock_tick
        FocusSound.ClockStopwatch -> R.drawable.ambient_clock_stopwatch
        FocusSound.WhiteNoise -> R.drawable.ambient_white_noise
        FocusSound.PinkNoise -> R.drawable.ambient_pink_noise
    }

/** The sound's recording: an Opus loop. */
@get:RawRes
internal val FocusSound.recording: Int
    get() = when (this) {
        FocusSound.SoftRain -> R.raw.ambient_soft_rain
        FocusSound.HeavyRain -> R.raw.ambient_heavy_rain
        FocusSound.ForestRain -> R.raw.ambient_forest_rain
        FocusSound.RainVillage -> R.raw.ambient_rain_village
        FocusSound.Thunder -> R.raw.ambient_thunder
        FocusSound.River -> R.raw.ambient_river
        FocusSound.SmallRiver -> R.raw.ambient_small_river
        FocusSound.Beach -> R.raw.ambient_beach
        FocusSound.SeaBeach -> R.raw.ambient_sea_beach
        FocusSound.SoftBeach -> R.raw.ambient_soft_beach
        FocusSound.Whale -> R.raw.ambient_whale
        FocusSound.ForestNight -> R.raw.ambient_forest_night
        FocusSound.Birds -> R.raw.ambient_birds
        FocusSound.Crickets -> R.raw.ambient_crickets
        FocusSound.CicadaBirds -> R.raw.ambient_cicada_birds
        FocusSound.Farm -> R.raw.ambient_farm
        FocusSound.Wind -> R.raw.ambient_wind
        FocusSound.Campfire -> R.raw.ambient_campfire
        FocusSound.CoffeeShop -> R.raw.ambient_coffee_shop
        FocusSound.Office -> R.raw.ambient_office
        FocusSound.StreetTraffic -> R.raw.ambient_street_traffic
        FocusSound.Construction -> R.raw.ambient_construction
        FocusSound.KeyboardTyping -> R.raw.ambient_keyboard_typing
        FocusSound.ClockTick -> R.raw.ambient_clock_tick
        FocusSound.ClockStopwatch -> R.raw.ambient_clock_stopwatch
        FocusSound.WhiteNoise -> R.raw.ambient_white_noise
        FocusSound.PinkNoise -> R.raw.ambient_pink_noise
    }

@get:StringRes
internal val FocusSoundCategory.label: Int
    get() = when (this) {
        FocusSoundCategory.Rain -> R.string.sound_category_rain
        FocusSoundCategory.Water -> R.string.sound_category_water
        FocusSoundCategory.Nature -> R.string.sound_category_nature
        FocusSoundCategory.Places -> R.string.sound_category_places
        FocusSoundCategory.Steady -> R.string.sound_category_steady
    }

/** "Soft rain", "Soft rain + 2", or "Off" with nothing on. */
@Composable
internal fun mixTitle(mix: List<FocusSoundLayer>): String {
    val first = mix.firstOrNull() ?: return stringResource(R.string.sound_mix_off)
    val name = stringResource(first.sound.label)
    return if (mix.size == 1) name else stringResource(R.string.sound_mix_more, name, mix.size - 1)
}
