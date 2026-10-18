package com.recentr.core.data

import com.recentr.core.model.FocusConfig
import com.recentr.core.model.FocusPhase
import com.recentr.core.model.FocusRecordKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class FocusRepositoryTest {

    private val zone = ZoneId.of("Europe/Paris")
    /** Sunday 18 Oct 2026, 11:00. */
    private val now = LocalDate.of(2026, 10, 18).atTime(11, 0).atZone(zone).toInstant().toEpochMilli()
    private val minute = 60_000L

    private fun repository(config: FocusConfig = FocusConfig(focusMinutes = 25, shortBreakMinutes = 5, rounds = 2)) =
        FocusRepository(zone = { zone }, now = now).apply {
            updateConfig { config }
            clearHistory()
        }

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
        assertEquals(15 * minute, paused.remainingMillis(now + 60 * minute))
        repo.resume(now + 60 * minute)
        assertEquals(now + 75 * minute, repo.run.value!!.phaseEndsAt)
    }

    @Test
    fun `without auto-start the next round waits`() {
        val repo = repository(FocusConfig(focusMinutes = 25, shortBreakMinutes = 5, rounds = 2, autoStartNextRound = false))
        repo.start(now)
        repo.advance(now + 40 * minute)
        val waiting = repo.run.value!!
        assertEquals(2, waiting.round)
        assertTrue(waiting.isPaused)
        assertEquals(25 * minute, waiting.remainingMillis(now + 40 * minute))
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
    fun `seeded log carries the designed sessions`() {
        val repo = FocusRepository(zone = { zone }, now = now)
        val titles = repo.history.value.map { it.title }
        assertTrue("Deep work · Thesis" in titles)
        assertTrue("Work Hours" in titles)
        assertEquals(38, repo.history.value.sumOf { it.urgesResisted })
        // Nothing seeded in the future.
        assertTrue(repo.history.value.all { it.startedAt <= now })
    }
}
