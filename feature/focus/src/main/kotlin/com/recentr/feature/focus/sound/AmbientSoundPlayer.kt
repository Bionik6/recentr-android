package com.recentr.feature.focus.sound

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import com.recentr.core.model.FocusSound
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin
import kotlin.random.Random

/**
 * Focus sounds synthesised on the fly (the kit ships no audio): filtered noise for rain, wind
 * and fire, chirps for birds and crickets, a soft tick for the clock. Runs on its own thread
 * with a streaming [AudioTrack]; [play] switches sound and volume, [stop] releases everything.
 */
internal class AmbientSoundPlayer {

    @Volatile private var sound: FocusSound = FocusSound.Silence
    @Volatile private var volume: Float = 0.6f
    /** Bumped by [stop]: a playback thread only runs while the generation it started with is current. */
    @Volatile private var generation = 0
    private var playing = false

    fun play(sound: FocusSound, volume: Float) {
        this.sound = sound
        this.volume = volume.coerceIn(0f, 1f)
        if (sound == FocusSound.Silence) {
            stop()
            return
        }
        if (playing) return
        playing = true
        val myGeneration = ++generation
        Thread({ loop(myGeneration) }, "focus-sound").apply {
            isDaemon = true
            start()
        }
    }

    fun stop() {
        playing = false
        generation++
    }

    private fun loop(myGeneration: Int) {
        val minBuffer = AudioTrack.getMinBufferSize(SAMPLE_RATE, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT)
        val track = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build(),
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setSampleRate(SAMPLE_RATE)
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build(),
            )
            .setBufferSizeInBytes(maxOf(minBuffer, CHUNK * 4))
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()
        val synth = Synth()
        val buffer = ShortArray(CHUNK)
        var gain = 0f
        try {
            track.play()
            while (generation == myGeneration) {
                val current = sound
                val target = volume
                for (i in buffer.indices) {
                    // Smooth volume changes and the fade-in to avoid clicks.
                    gain += (target - gain) * 0.0005f
                    val sample = synth.next(current) * gain * MASTER
                    buffer[i] = (sample.coerceIn(-1f, 1f) * Short.MAX_VALUE).toInt().toShort()
                }
                track.write(buffer, 0, buffer.size)
            }
        } catch (_: IllegalStateException) {
        } finally {
            runCatching { track.stop() }
            track.release()
        }
    }

    /** All generators keep their state between chunks. */
    private class Synth {
        private val random = Random(42)
        private var t = 0L

        // Noise colours.
        private var brown = 0f
        private var pinkB0 = 0f
        private var pinkB1 = 0f
        private var pinkB2 = 0f
        private var lowpass = 0f

        // Rain drops / fire crackles: decaying bursts.
        private var burst = 0f
        private var burstDecay = 0.99f

        // Bird chirp / cricket pulse.
        private var chirpLeft = 0
        private var chirpFreq = 0f
        private var chirpPhase = 0.0
        private var pulsesLeft = 0
        private var pulseGap = 0

        private fun white() = random.nextFloat() * 2f - 1f

        private fun brownNoise(): Float {
            brown = (brown + white() * 0.02f) / 1.02f
            return brown * 3.5f
        }

        private fun pinkNoise(): Float {
            val w = white()
            pinkB0 = 0.99765f * pinkB0 + w * 0.0990460f
            pinkB1 = 0.96300f * pinkB1 + w * 0.2965164f
            pinkB2 = 0.57000f * pinkB2 + w * 1.0526913f
            return (pinkB0 + pinkB1 + pinkB2 + w * 0.1848f) * 0.11f
        }

        fun next(sound: FocusSound): Float {
            t++
            return when (sound) {
                FocusSound.Rain -> rain()
                FocusSound.Forest -> forest()
                FocusSound.Night -> night()
                FocusSound.Fireplace -> fireplace()
                FocusSound.Clock -> clock()
                FocusSound.Silence -> 0f
            }
        }

        private fun rain(): Float {
            lowpass += (pinkNoise() - lowpass) * 0.35f
            if (random.nextInt(SAMPLE_RATE / 18) == 0) {
                burst = 0.25f + random.nextFloat() * 0.35f
                burstDecay = 0.992f
            }
            burst *= burstDecay
            return lowpass * 0.8f + white() * burst * 0.5f
        }

        private fun forest(): Float {
            val seconds = t / SAMPLE_RATE.toFloat()
            val wind = brownNoise() * (0.45f + 0.25f * sin(2 * PI * seconds / 7.0).toFloat())
            if (chirpLeft <= 0 && random.nextInt(SAMPLE_RATE * 2) == 0) {
                chirpLeft = SAMPLE_RATE / 9
                chirpFreq = 2_600f + random.nextFloat() * 1_400f
            }
            var bird = 0f
            if (chirpLeft > 0) {
                val progress = 1f - chirpLeft / (SAMPLE_RATE / 9f)
                val freq = chirpFreq * (1f + 0.25f * sin(progress * PI.toFloat() * 6f))
                chirpPhase += 2 * PI * freq / SAMPLE_RATE
                bird = sin(chirpPhase).toFloat() * sin(progress * PI.toFloat()) * 0.18f
                chirpLeft--
            }
            return wind + bird
        }

        private fun night(): Float {
            val bed = brownNoise() * 0.25f
            if (pulsesLeft == 0 && pulseGap <= 0 && random.nextInt(SAMPLE_RATE / 2) == 0) {
                pulsesLeft = 3 + random.nextInt(2)
                chirpLeft = 0
            }
            var cricket = 0f
            if (pulsesLeft > 0) {
                if (chirpLeft <= 0) {
                    if (pulseGap > 0) {
                        pulseGap--
                    } else {
                        chirpLeft = (SAMPLE_RATE * 0.022f).toInt()
                        pulseGap = (SAMPLE_RATE * 0.045f).toInt()
                    }
                } else {
                    chirpPhase += 2 * PI * 4_400.0 / SAMPLE_RATE
                    val envelope = sin(PI * chirpLeft / (SAMPLE_RATE * 0.022)).toFloat()
                    cricket = sin(chirpPhase).toFloat() * envelope * 0.12f
                    chirpLeft--
                    if (chirpLeft == 0) {
                        pulsesLeft--
                        if (pulsesLeft == 0) pulseGap = (SAMPLE_RATE * 0.6f).toInt()
                    }
                }
            } else if (pulseGap > 0) {
                pulseGap--
            }
            return bed + cricket
        }

        private fun fireplace(): Float {
            lowpass += (brownNoise() - lowpass) * 0.08f
            if (random.nextInt(SAMPLE_RATE / 7) == 0) {
                burst = 0.3f + random.nextFloat() * 0.6f
                burstDecay = 0.94f + random.nextFloat() * 0.04f
            }
            burst *= burstDecay
            return lowpass * 1.6f + white() * burst * 0.6f
        }

        private fun clock(): Float {
            val position = (t % SAMPLE_RATE).toInt()
            if (position == 0) {
                chirpPhase = 0.0
                chirpFreq = if ((t / SAMPLE_RATE) % 2 == 0L) 2_100f else 1_700f
            }
            if (position > SAMPLE_RATE / 25) return 0f
            chirpPhase += 2 * PI * chirpFreq / SAMPLE_RATE
            val envelope = exp(-position / (SAMPLE_RATE * 0.006f))
            return (sin(chirpPhase).toFloat() * 0.5f + white() * 0.3f) * envelope
        }
    }

    private companion object {
        const val SAMPLE_RATE = 44_100
        const val CHUNK = 2_048
        const val MASTER = 0.5f
    }
}
