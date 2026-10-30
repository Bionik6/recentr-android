package com.recentr.feature.focus.setup

import com.recentr.core.model.FocusSound
import com.recentr.core.model.FocusSoundCategory
import com.recentr.core.model.FocusSoundLayer
import kotlin.math.roundToInt

/** A sound's row in the sounds sheet (Figma › Specs · Sound row): off, on, or muted at the volume it kept. */
internal enum class SoundState { Off, On, Muted }

/** The link by the sheet's title: "Mute all" while something plays, "Unmute all" once all of it is muted. */
internal enum class MuteLink { MuteAll, UnmuteAll }

/**
 * What the sounds sheet (Figma 93) holds: the sounds [on], in the order they were turned on, and
 * the ones [muted] there, at the volume they come back at, in the order they were muted. The
 * muted ones are forgotten once the sheet closes.
 */
internal data class SoundsMix(val on: List<FocusSoundLayer>, val muted: List<FocusSoundLayer> = emptyList()) {

    fun state(sound: FocusSound): SoundState = when {
        on.any { it.sound == sound } -> SoundState.On
        muted.any { it.sound == sound } -> SoundState.Muted
        else -> SoundState.Off
    }

    /** Where its slider stands: the volume it plays or comes back at, 0 while off. */
    fun volume(sound: FocusSound): Float = (on + muted).firstOrNull { it.sound == sound }?.volume ?: 0f

    /** How many of [category]'s sounds are on, for "2 on" by its title. */
    fun onCount(category: FocusSoundCategory): Int = on.count { it.sound.category == category }

    /** None with nothing on. */
    val muteLink: MuteLink?
        get() = when {
            on.isNotEmpty() -> MuteLink.MuteAll
            muted.isNotEmpty() -> MuteLink.UnmuteAll
            else -> null
        }

    /** Off → on at 60 %, on → muted, muted → on at the volume it kept. */
    fun artTapped(sound: FocusSound): SoundsMix {
        on.firstOrNull { it.sound == sound }?.let { return SoundsMix(on - it, muted + it) }
        muted.firstOrNull { it.sound == sound }?.let { return SoundsMix(on + it, muted - it) }
        return copy(on = on + FocusSoundLayer(sound))
    }

    /** Sets [sound]'s volume, to the percent, turning it on if it was off or muted. Down to 0, it's off. */
    fun volumeChanged(sound: FocusSound, volume: Float): SoundsMix {
        val level = (volume.coerceIn(0f, 1f) * 100).roundToInt() / 100f
        val playing = on.firstOrNull { it.sound == sound }
        return when {
            playing != null && playing.volume == level -> this
            playing != null && level == 0f -> copy(on = on - playing)
            playing != null -> copy(on = on.map { if (it.sound == sound) it.copy(volume = level) else it })
            level == 0f -> copy(muted = muted.filterNot { it.sound == sound })
            else -> SoundsMix(on + FocusSoundLayer(sound, level), muted.filterNot { it.sound == sound })
        }
    }

    fun muteLinkTapped(): SoundsMix = when (muteLink) {
        MuteLink.MuteAll -> SoundsMix(on = emptyList(), muted = muted + on)
        MuteLink.UnmuteAll -> SoundsMix(on = on + muted, muted = emptyList())
        null -> this
    }
}
