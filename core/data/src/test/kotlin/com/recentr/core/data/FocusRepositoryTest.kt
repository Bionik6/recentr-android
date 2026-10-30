package com.recentr.core.data

import com.recentr.core.model.FocusConfig
import com.recentr.core.model.FocusPhase
import com.recentr.core.model.FocusRecordKind
import com.recentr.core.model.FocusSound
import com.recentr.core.model.FocusSoundLayer
import kotlinx.coroutines.Dispatchers
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.time.LocalDate
import java.time.ZoneId

class FocusRepositoryTest {

    @get:Rule
    val folder = TemporaryFolder()

    private val zone = ZoneId.of("Europe/Paris")
    /** Sunday 18 Oct 2026, 11:00. */
    private val now = LocalDate.of(2026, 10, 18).atTime(11, 0).atZone(zone).toInstant().toEpochMilli()
    private val minute = 60_000L

    private fun repository(config: FocusConfig = FocusConfig(focusMinutes = 25, shortBreakMinutes = 5, rounds = 2)) =
        FocusRepository(zone = { zone }).apply { updateConfig { config } }

    @Test
    fun `focus round turns into a break, then the next round starts`() {
        val repo = repository()
        repo.start(now)
        repo.advance(now + 25 * minute)
        val onBreak = repo.run.value!!
        assertEquals(FocusPhase.Break, onBreak.phase)
        assertEquals(1, onBreak.round)
        assertEquals(5 * minute, onBreak.remainingMillis(now + 25 * minute))

        repo.advance(now + 30 * minute)
        val second = repo.run.value!!
        assertEquals(FocusPhase.Focus, second.phase)
        assertEquals(2, second.round)
        assertEquals(25 * minute, second.focusedMillis(now + 30 * minute))
    }

    @Test
    fun `last round ends the session and logs it`() {
        val repo = repository()
        repo.start(now)
        // Back from the background long after the end: every phase settles at once.
        repo.advance(now + 3 * 60 * minute)
        assertNull(repo.run.value)
        val record = repo.lastCompleted.value
        assertNotNull(record)
        assertEquals(50, record!!.focusedMinutes)
        assertEquals(2, record.rounds)
        assertEquals(FocusRecordKind.Timer, record.kind)
        assertEquals(record, repo.history.value.first())
    }

    @Test
    fun `pause keeps the remaining time`() {
        val repo = repository()
        repo.start(now)
        repo.pause(now + 10 * minute)
        repo.advance(now + 60 * minute)
        val paused = repo.run.value!!
        assertTrue(paused.isPaused)
        assertFalse(paused.awaitingStart)
        // "paused for 50:00" counts from here.
        assertEquals(now + 10 * minute, paused.pausedAt)
        assertEquals(15 * minute, paused.remainingMillis(now + 60 * minute))
        repo.resume(now + 60 * minute)
        assertEquals(now + 75 * minute, repo.run.value!!.phaseEndsAt)
        assertNull(repo.run.value!!.pausedAt)
    }

    @Test
    fun `without auto-start the next round waits`() {
        val repo = repository(FocusConfig(focusMinutes = 25, shortBreakMinutes = 5, rounds = 2, autoStartNextRound = false))
        repo.start(now)
        repo.advance(now + 40 * minute)
        val waiting = repo.run.value!!
        assertEquals(2, waiting.round)
        assertTrue(waiting.isPaused)
        // Figma 92E: "Start round", not a pause.
        assertTrue(waiting.awaitingStart)
        assertNull(waiting.pausedAt)
        assertEquals(25 * minute, waiting.remainingMillis(now + 40 * minute))

        repo.resume(now + 42 * minute)
        val started = repo.run.value!!
        assertFalse(started.awaitingStart)
        assertEquals(now + 67 * minute, started.phaseEndsAt)
    }

    @Test
    fun `skip break starts the next round now`() {
        val repo = repository()
        repo.start(now)
        repo.advance(now + 26 * minute)
        repo.skipBreak(now + 26 * minute)
        val next = repo.run.value!!
        assertEquals(FocusPhase.Focus, next.phase)
        assertEquals(now + 51 * minute, next.phaseEndsAt)
    }

    @Test
    fun `a strict session can't be paused or ended early, and the next one isn't strict`() {
        val repo = repository(FocusConfig(focusMinutes = 25, rounds = 2, strictMode = true))
        repo.start(now)
        assertTrue(repo.run.value!!.isStrict)
        assertFalse(repo.config.value.strictMode)

        repo.pause(now + minute)
        assertFalse(repo.run.value!!.isPaused)
        assertNull(repo.end(now + 10 * minute))
        assertNotNull(repo.run.value)

        // It still runs out on its own.
        repo.advance(now + (25 + 5 + 25) * minute)
        assertNull(repo.run.value)
        assertEquals(50, repo.lastCompleted.value!!.focusedMinutes)
    }

    @Test
    fun `the timer starts at 25 minutes and goes up to 3 hours`() {
        assertEquals(25, FocusConfig().focusMinutes)
        assertEquals(180, FocusConfig.MAX_FOCUS)
        assertFalse(FocusConfig().strictMode)
    }

    @Test
    fun `ending early logs the focused time, or nothing under a minute`() {
        val repo = repository()
        repo.start(now)
        assertNull(repo.end(now + 30_000))
        assertNull(repo.lastCompleted.value)

        repo.start(now)
        repo.resistUrge()
        val record = repo.end(now + 12 * minute)
        assertEquals(12, record!!.focusedMinutes)
        assertEquals(1, record.urgesResisted)
    }

    @Test
    fun `long break every fourth round and total with breaks`() {
        val config = FocusConfig(focusMinutes = 45, shortBreakMinutes = 5, longBreakMinutes = 15, rounds = 5)
        assertEquals(15, config.breakMinutesAfter(4))
        assertEquals(5, config.breakMinutesAfter(3))
        // 5 × 45 + 5 + 5 + 5 + 15.
        assertEquals(255, config.totalMinutes)
    }

    @Test
    fun `streak counts consecutive days with a timer session`() {
        val repo = repository()
        assertEquals(0, repo.streakDays(now))
        val day = 24 * 60 * minute
        listOf(now - 2 * day, now - day, now).forEach { start ->
            repo.start(start)
            repo.end(start + 30 * minute)
        }
        assertEquals(3, repo.streakDays(now + 60 * minute))
        assertFalse(repo.history.value.isEmpty())
    }

    @Test
    fun `starts with an empty log and no blocked apps`() {
        val repo = FocusRepository(zone = { zone })
        assertTrue(repo.history.value.isEmpty())
        assertTrue(repo.config.value.blockedApps.isEmpty())
        assertEquals(0, repo.streakDays(now))
    }

    @Test
    fun `the sounds mixed are kept for the next sessions, after Recentr closes too`() {
        val file = folder.root.resolve("focus_sounds.json")
        val mix = listOf(FocusSoundLayer(FocusSound.HeavyRain, 0.3f), FocusSoundLayer(FocusSound.Campfire))
        val repo = FocusRepository(file, zone = { zone }, writeDispatcher = Dispatchers.Unconfined)
        assertEquals(FocusSoundLayer.DefaultMix, repo.config.value.sounds)

        repo.updateConfig { it.copy(sounds = mix) }
        repo.start(now)
        repo.end(now + 30 * minute)

        assertEquals(mix, repo.config.value.sounds)
        assertEquals(mix, FocusRepository(file, zone = { zone }).config.value.sounds)
    }

    @Test
    fun `muted sounds are kept too, and a mix saved before them still reads`() {
        val file = folder.root.resolve("focus_sounds.json")
        file.writeText("""[{"sound":"Thunder","volume":0.3}]""")
        val repo = FocusRepository(file, zone = { zone }, writeDispatcher = Dispatchers.Unconfined)
        assertEquals(listOf(FocusSoundLayer(FocusSound.Thunder, 0.3f)), repo.config.value.sounds)
        assertEquals(emptyList<FocusSoundLayer>(), repo.config.value.mutedSounds)

        val muted = listOf(FocusSoundLayer(FocusSound.Thunder, 0.3f))
        repo.updateConfig { it.copy(sounds = emptyList(), mutedSounds = muted) }

        val reopened = FocusRepository(file, zone = { zone }).config.value
        assertEquals(emptyList<FocusSoundLayer>(), reopened.sounds)
        assertEquals(muted, reopened.mutedSounds)
    }

    @Test
    fun `restart starts the round over, running, and its minutes don't count`() {
        val repo = repository()
        repo.start(now)

        val before = repo.restartRound(now + 10 * minute)

        assertNotNull(before)
        val restarted = repo.run.value!!
        assertEquals(1, restarted.round)
        assertEquals(now + 35 * minute, restarted.phaseEndsAt)
        assertEquals(25 * minute, restarted.remainingMillis(now + 10 * minute))
        assertEquals(0L, restarted.focusedMillis(now + 10 * minute))

        // The round now ends 25 minutes after the restart.
        repo.advance(now + 35 * minute)
        assertEquals(FocusPhase.Break, repo.run.value!!.phase)
    }

    @Test
    fun `restart runs a paused round again, and undo pauses it as it was`() {
        val repo = repository()
        repo.start(now)
        repo.pause(now + 10 * minute)

        val before = repo.restartRound(now + 12 * minute)!!

        val restarted = repo.run.value!!
        assertFalse(restarted.isPaused)
        assertNull(restarted.pausedAt)
        assertEquals(now + 37 * minute, restarted.phaseEndsAt)

        repo.undoRestart(before)

        val undone = repo.run.value!!
        assertTrue(undone.isPaused)
        assertEquals(now + 10 * minute, undone.pausedAt)
        assertEquals(15 * minute, undone.remainingMillis(now + 13 * minute))
    }

    @Test
    fun `undo gives a running round its time left back`() {
        val repo = repository()
        repo.start(now)
        val before = repo.restartRound(now + 10 * minute)!!
        repo.resistUrge()

        repo.undoRestart(before)

        val undone = repo.run.value!!
        assertEquals(now + 25 * minute, undone.phaseEndsAt)
        assertEquals(12 * minute, undone.remainingMillis(now + 13 * minute))
        // What happened since stays.
        assertEquals(1, undone.urgesResisted)
    }

    @Test
    fun `undo does nothing once the round is over`() {
        val repo = repository()
        repo.start(now)
        val before = repo.restartRound(now + 10 * minute)!!
        repo.advance(now + 36 * minute)

        repo.undoRestart(before)

        assertEquals(FocusPhase.Break, repo.run.value!!.phase)
    }

    /** The session's clock moves on with Recentr in the background: a break starts on time. */
    @Test
    fun `a break starts on time, so its apps open`() {
        val repo = repository()
        repo.start(now)

        repo.advance(now + 25 * minute)

        val onBreak = repo.run.value!!
        assertEquals(FocusPhase.Break, onBreak.phase)
        assertEquals(now + 30 * minute, onBreak.phaseEndsAt)
        repo.advance(now + 30 * minute)
        assertEquals(FocusPhase.Focus, repo.run.value!!.phase)
    }

    @Test
    fun `breaks and strict sessions can't be restarted`() {
        val onBreak = repository()
        onBreak.start(now)
        onBreak.advance(now + 26 * minute)
        assertNull(onBreak.restartRound(now + 26 * minute))
        assertEquals(FocusPhase.Break, onBreak.run.value!!.phase)

        val strict = repository(FocusConfig(focusMinutes = 25, rounds = 2, strictMode = true))
        strict.start(now)
        assertNull(strict.restartRound(now + 10 * minute))
        assertEquals(now + 25 * minute, strict.run.value!!.phaseEndsAt)

        val waiting = repository(FocusConfig(focusMinutes = 25, shortBreakMinutes = 5, rounds = 2, autoStartNextRound = false))
        waiting.start(now)
        waiting.advance(now + 40 * minute)
        assertNull(waiting.restartRound(now + 40 * minute))
        assertTrue(waiting.run.value!!.awaitingStart)
    }
}
