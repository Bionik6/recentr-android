package com.recentr.feature.focus.sound

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.os.Handler
import android.os.Looper
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.recentr.core.model.FocusSound
import com.recentr.core.model.FocusSoundLayer
import com.recentr.feature.focus.ui.recording

/**
 * Loops focus sounds, mixed with whatever else plays (it never takes audio focus): one player per
 * sound, each at its own volume, gliding there so sounds fade in and out. The recordings are Opus
 * loops with no padding, so repeating one leaves no gap. Main thread only.
 */
internal class AmbientMixer(private val context: Context) {

    private class Layer(val player: ExoPlayer) {
        var volume = 0f
        var target = 0f
        /** On its way out: let go of once faded. */
        var leaving = false
    }

    private val layers = mutableMapOf<FocusSound, Layer>()
    private var muted = false
    private val handler = Handler(Looper.getMainLooper())
    private var fading = false

    /** Plays [mix]: sounds coming in fade in, sounds going fade out, the others glide to their volume. */
    fun play(mix: List<FocusSoundLayer>) = apply(mix, muted = false)

    /** Fades [mix] to silence and pauses it, ready to come back. */
    fun mute(mix: List<FocusSoundLayer>) = apply(mix, muted = true)

    /** Fades everything out and lets go of it. */
    fun stop() = apply(emptyList(), muted = false)

    /** Stops at once and lets go of everything. */
    fun release() {
        handler.removeCallbacksAndMessages(null)
        fading = false
        layers.values.forEach { it.player.release() }
        layers.clear()
    }

    private fun apply(mix: List<FocusSoundLayer>, muted: Boolean) {
        this.muted = muted
        val volumes = mix.associate { it.sound to it.volume.coerceIn(0f, 1f) }
        volumes.forEach { (sound, volume) ->
            val layer = layers.getOrPut(sound) { Layer(newPlayer(sound)) }
            layer.target = volume
            layer.leaving = false
            if (!muted) layer.player.play()
        }
        layers.forEach { (sound, layer) -> if (sound !in volumes) layer.leaving = true }
        if (!fading) {
            fading = true
            handler.post(step)
        }
    }

    private fun newPlayer(sound: FocusSound): ExoPlayer {
        val attributes = AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .build()
        val uri = Uri.Builder()
            .scheme(ContentResolver.SCHEME_ANDROID_RESOURCE)
            .authority(context.packageName)
            .path(sound.recording.toString())
            .build()
        return ExoPlayer.Builder(context)
            .setAudioAttributes(attributes, /* handleAudioFocus = */ false)
            .build()
            .apply {
                setMediaItem(MediaItem.fromUri(uri))
                repeatMode = Player.REPEAT_MODE_ONE
                volume = 0f
                prepare()
            }
    }

    /** Moves each volume a step towards its target, and lets go of what's faded out. */
    private val step = object : Runnable {
        override fun run() {
            var moving = false
            val iterator = layers.iterator()
            while (iterator.hasNext()) {
                val layer = iterator.next().value
                val target = if (muted || layer.leaving) 0f else layer.target
                layer.volume = when {
                    layer.volume < target -> minOf(target, layer.volume + STEP)
                    layer.volume > target -> maxOf(target, layer.volume - STEP)
                    else -> target
                }
                layer.player.volume = layer.volume
                if (layer.volume != target) {
                    moving = true
                } else if (layer.leaving) {
                    layer.player.release()
                    iterator.remove()
                } else if (muted) {
                    layer.player.pause()
                }
            }
            if (moving) handler.postDelayed(this, FRAME_MILLIS) else fading = false
        }
    }

    private companion object {
        const val FRAME_MILLIS = 33L
        /** A full fade takes 0.8 s. */
        const val STEP = FRAME_MILLIS / 800f
    }
}
