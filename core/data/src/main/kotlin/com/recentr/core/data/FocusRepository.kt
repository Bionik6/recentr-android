package com.recentr.core.data

import com.recentr.core.model.FocusConfig
import com.recentr.core.model.FocusPhase
import com.recentr.core.model.FocusRecordKind
import com.recentr.core.model.FocusRun
import com.recentr.core.model.FocusSessionRecord
import com.recentr.core.model.FocusSoundLayer
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.updateAndGet
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import java.io.File
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters
import java.util.UUID

/**
 * Focus timer (Figma "Focus timer" 91–98). In-memory like [RulesRepository]: the set-up, the
 * running session and the focus log (empty until the first session ends).
 *
 * The session runs on the wall clock ([FocusRun.phaseEndsAt]), so it keeps going while the
 * screen is off; [advance] settles finished phases and is called by the screens' clock.
 *
 * The sounds mixed are saved to [soundsFile] after every change, so each session starts with the
 * last mix, however long ago.
 */
class FocusRepository(
    /** Where the sound mix is saved, or null to keep it in memory only (previews, tests). */
    private val soundsFile: File? = null,
    private val zone: () -> ZoneId = { ZoneId.systemDefault() },
    writeDispatcher: CoroutineDispatcher = Dispatchers.IO.limitedParallelism(1),
) {
    private val _config = MutableStateFlow(FocusConfig(sounds = soundsFile?.let(::readSounds) ?: FocusSoundLayer.DefaultMix))
    val config: StateFlow<FocusConfig> = _config.asStateFlow()

    /** One write at a time, each with the latest mix. */
    private val writes = CoroutineScope(SupervisorJob() + writeDispatcher)

    private val _run = MutableStateFlow<FocusRun?>(null)
    val run: StateFlow<FocusRun?> = _run.asStateFlow()

    private val _history = MutableStateFlow<List<FocusSessionRecord>>(emptyList())
    /** Most recent first. */
    val history: StateFlow<List<FocusSessionRecord>> = _history.asStateFlow()

    /** The session that just ended, for the "Session complete" screen. */
    private val _lastCompleted = MutableStateFlow<FocusSessionRecord?>(null)
    val lastCompleted: StateFlow<FocusSessionRecord?> = _lastCompleted.asStateFlow()

    fun updateConfig(transform: (FocusConfig) -> FocusConfig) {
        val sounds = _config.value.sounds
        if (_config.updateAndGet(transform).sounds != sounds) persistSounds()
    }

    /**
     * Starts a session with the current set-up ([rounds] overrides the number of rounds). Strict
     * mode is chosen for each session: the next one starts without it.
     */
    fun start(now: Long, rounds: Int? = null) {
        val config = _config.value.let { if (rounds != null) it.copy(rounds = rounds) else it }
        _lastCompleted.value = null
        _run.value = FocusRun(
            config = config,
            startedAt = now,
            round = 1,
            phase = FocusPhase.Focus,
            phaseEndsAt = now + config.focusMinutes * 60_000L,
            pausedRemainingMillis = null,
        )
        _config.update { it.copy(strictMode = false) }
    }

    /** Pauses the round, unless Strict mode holds the session. */
    fun pause(now: Long) = _run.update { run ->
        if (run == null || run.isPaused || run.isStrict) {
            run
        } else {
            run.copy(phaseEndsAt = null, pausedRemainingMillis = run.remainingMillis(now), pausedAt = now)
        }
    }

    /** Resumes a paused round, or starts the round waiting for "Start round". */
    fun resume(now: Long) = _run.update { run ->
        if (run == null || !run.isPaused) {
            run
        } else {
            run.copy(phaseEndsAt = now + run.remainingMillis(now), pausedRemainingMillis = null, pausedAt = null, awaitingStart = false)
        }
    }

    /**
     * "Restart" (Figma 92C): the round going on starts over at once, running, its time so far let
     * go of (it doesn't count as focus). Returns the session as it was, for Undo; null when there's
     * no round to restart: a break, a round waiting to start, or Strict mode.
     */
    fun restartRound(now: Long): FocusRun? {
        val before = _run.value ?: return null
        if (before.phase != FocusPhase.Focus || before.awaitingStart || before.isStrict) return null
        _run.value = before.copy(
            phaseEndsAt = now + before.phaseLengthMillis,
            pausedRemainingMillis = null,
            pausedAt = null,
        )
        return before
    }

    /** The restart toast's "Undo": the round's time left, and its pause, as they were before [restartRound]. */
    fun undoRestart(before: FocusRun) = _run.update { run ->
        if (run == null || run.startedAt != before.startedAt || run.round != before.round || run.phase != FocusPhase.Focus) {
            run
        } else {
            before.copy(config = run.config, urgesResisted = run.urgesResisted)
        }
    }

    /** Ends the break now and starts the next round. */
    fun skipBreak(now: Long) = _run.update { run ->
        if (run == null || run.phase != FocusPhase.Break) run else run.nextRound(startAt = now, autoStart = true)
    }

    /** "Back to focus" on the app-on-hold dialog. */
    fun resistUrge() = _run.update { it?.copy(urgesResisted = it.urgesResisted + 1) }

    /**
     * Ends the session early. Returns the logged session, or null when nothing was focused or
     * Strict mode holds the session (it then goes on).
     */
    fun end(now: Long): FocusSessionRecord? {
        val run = _run.value?.takeUnless { it.isStrict } ?: return null
        _run.value = null
        return log(run, run.focusedMillis(now))
    }

    /** Settles the phases that ended before [now] (several when the app was in the background). */
    fun advance(now: Long) {
        var run = _run.value ?: return
        while (true) {
            val endsAt = run.phaseEndsAt ?: break
            if (now < endsAt) break
            when (run.phase) {
                FocusPhase.Focus -> {
                    val focused = run.completedFocusMillis + run.phaseLengthMillis
                    if (run.round >= run.config.rounds) {
                        _run.value = null
                        log(run, focused)
                        return
                    }
                    run = run.copy(
                        phase = FocusPhase.Break,
                        completedFocusMillis = focused,
                        phaseEndsAt = endsAt + run.config.breakMinutesAfter(run.round) * 60_000L,
                    )
                }
                FocusPhase.Break -> run = run.nextRound(startAt = endsAt, autoStart = run.config.autoStartNextRound)
            }
        }
        _run.value = run
    }

    /** Edits the running session (sound and volume changed mid-session). */
    fun updateRun(transform: (FocusRun) -> FocusRun) = _run.update { it?.let(transform) }

    fun clearHistory() {
        _history.value = emptyList()
    }

    fun updateNote(id: String, note: String) {
        _history.update { list -> list.map { if (it.id == id) it.copy(note = note) else it } }
        _lastCompleted.update { if (it?.id == id) it.copy(note = note) else it }
    }

    /** Focus streak: consecutive days, up to today (or yesterday), with at least one timer session. */
    fun streakDays(now: Long): Int {
        val days = _history.value.filter { it.kind == FocusRecordKind.Timer }.map { it.startedAt.toDate() }.toSet()
        var day = now.toDate()
        if (day !in days) day = day.minusDays(1)
        var streak = 0
        while (day in days) {
            streak++
            day = day.minusDays(1)
        }
        return streak
    }

    /** Minutes focused per day of the week starting on [weekStart] (Monday → Sunday). */
    fun minutesPerDay(weekStart: LocalDate): List<Int> {
        val perDay = IntArray(7)
        _history.value.forEach { record ->
            val index = ChronoUnit.DAYS.between(weekStart, record.startedAt.toDate()).toInt()
            if (index in 0..6) perDay[index] += record.focusedMinutes
        }
        return perDay.toList()
    }

    fun weekStart(now: Long): LocalDate = now.toDate().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))

    fun reset() {
        _config.value = FocusConfig()
        _run.value = null
        _lastCompleted.value = null
        _history.value = emptyList()
        persistSounds()
    }

    private fun persistSounds() {
        val target = soundsFile ?: return
        writes.launch { target.writeAtomically(json.encodeToString(_config.value.sounds)) }
    }

    private fun FocusRun.nextRound(startAt: Long, autoStart: Boolean): FocusRun {
        val length = config.focusMinutes * 60_000L
        return copy(
            round = round + 1,
            phase = FocusPhase.Focus,
            phaseEndsAt = if (autoStart) startAt + length else null,
            pausedRemainingMillis = if (autoStart) null else length,
            pausedAt = null,
            awaitingStart = !autoStart,
        )
    }

    /** Logs [run] (its current round counts, finished or not) when at least a minute was focused. */
    private fun log(run: FocusRun, focusedMillis: Long): FocusSessionRecord? {
        val minutes = (focusedMillis / 60_000L).toInt()
        if (minutes < 1) return null
        val record = FocusSessionRecord(
            id = UUID.randomUUID().toString(),
            title = run.config.label,
            kind = FocusRecordKind.Timer,
            startedAt = run.startedAt,
            focusedMinutes = minutes,
            rounds = run.round,
            unlocks = run.unlocks,
            urgesResisted = run.urgesResisted,
        )
        _history.update { (listOf(record) + it).sortedByDescending { r -> r.startedAt } }
        _lastCompleted.value = record
        return record
    }

    private fun Long.toDate(): LocalDate = Instant.ofEpochMilli(this).atZone(zone()).toLocalDate()

    private companion object {
        val json = Json { ignoreUnknownKeys = true }

        /** The saved mix, or null when there's none yet (or the file can't be read). */
        fun readSounds(file: File): List<FocusSoundLayer>? = runCatching {
            if (file.exists()) json.decodeFromString<List<FocusSoundLayer>>(file.readText()) else null
        }.getOrNull()
    }
}
