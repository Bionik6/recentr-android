package com.recentr.feature.focus.setup

import com.recentr.core.model.FocusSound
import com.recentr.core.model.FocusSoundCategory
import com.recentr.core.model.FocusSoundLayer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test

/** The sounds sheet's rows (Figma 93): off, on, or muted at the volume they keep. */
class SoundsMixTest {

    private val softRain = FocusSoundLayer(FocusSound.SoftRain, 0.5f)
    private val birds = FocusSoundLayer(FocusSound.Birds)

    @Test
    fun `the art turns a sound on, mutes it and brings it back`() {
        var sounds = SoundsMix(listOf(softRain))

        sounds = sounds.artTapped(FocusSound.Birds)
        assertEquals(SoundState.On, sounds.state(FocusSound.Birds))
        assertEquals(FocusSoundLayer.DEFAULT_VOLUME, sounds.volume(FocusSound.Birds))
        sounds = sounds.artTapped(FocusSound.SoftRain)
        assertEquals(SoundState.Muted, sounds.state(FocusSound.SoftRain))
        assertEquals(0.5f, sounds.volume(FocusSound.SoftRain))
        assertEquals(listOf(birds), sounds.on)
        sounds = sounds.artTapped(FocusSound.SoftRain)

        assertEquals(SoundsMix(listOf(birds, softRain)), sounds)
    }

    @Test
    fun `the slider turns a sound on, and down to zero off`() {
        val wind = FocusSoundLayer(FocusSound.Wind, 0.3f)
        var sounds = SoundsMix(listOf(wind))

        assertEquals(SoundState.Off, sounds.state(FocusSound.Thunder))
        assertEquals(0f, sounds.volume(FocusSound.Thunder))
        sounds = sounds.volumeChanged(FocusSound.Thunder, 0.254f)
        assertEquals(listOf(wind, FocusSoundLayer(FocusSound.Thunder, 0.25f)), sounds.on)
        assertSame(sounds, sounds.volumeChanged(FocusSound.Thunder, 0.251f))
        sounds = sounds.volumeChanged(FocusSound.Thunder, 0f)
        assertEquals(SoundState.Off, sounds.state(FocusSound.Thunder))

        sounds = sounds.artTapped(FocusSound.Wind).volumeChanged(FocusSound.Wind, 0.7f)
        assertEquals(SoundsMix(listOf(FocusSoundLayer(FocusSound.Wind, 0.7f))), sounds)
        sounds = sounds.artTapped(FocusSound.Wind).volumeChanged(FocusSound.Wind, 0f)
        assertEquals(SoundsMix(emptyList()), sounds)
    }

    @Test
    fun `mute all keeps the volumes and unmute all brings them back`() {
        val mix = listOf(FocusSoundLayer(FocusSound.Office, 0.8f), FocusSoundLayer(FocusSound.WhiteNoise))
        var sounds = SoundsMix(mix)

        assertEquals(MuteLink.MuteAll, sounds.muteLink)
        sounds = sounds.muteLinkTapped()
        assertEquals(SoundsMix(emptyList(), mix), sounds)
        assertEquals(MuteLink.UnmuteAll, sounds.muteLink)
        sounds = sounds.muteLinkTapped()

        assertEquals(SoundsMix(mix), sounds)
        assertNull(SoundsMix(emptyList()).muteLink)
    }

    @Test
    fun `sections count the sounds on`() {
        val sounds = SoundsMix(listOf(softRain, FocusSoundLayer(FocusSound.River)), muted = listOf(FocusSoundLayer(FocusSound.Thunder)))

        assertEquals(1, sounds.onCount(FocusSoundCategory.Rain))
        assertEquals(1, sounds.onCount(FocusSoundCategory.Water))
        assertEquals(0, sounds.onCount(FocusSoundCategory.Nature))
    }
}
