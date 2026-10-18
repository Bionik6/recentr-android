package com.recentr.core.data

import com.recentr.core.model.FocusConfig
import com.recentr.core.model.FocusPhase
import com.recentr.core.model.FocusRecordKind
import com.recentr.core.model.FocusRun
import com.recentr.core.model.FocusSessionRecord
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters
import java.util.UUID

/**
 * Focus timer (Figma "Focus timer" 91–98). In-memory like [RulesRepository]: the set-up, the
 * running session and the focus log (seeded with the sessions shown in the design).
 *
 * The session runs on the wall clock ([FocusRun.phaseEndsAt]), so it keeps going while the
 * screen is off; [advance] settles finished phases and is called by the screens' clock.
 */
class FocusRepository(
    private val zone: () -> ZoneId = { ZoneId.systemDefault() },
    now: Long = System.currentTimeMillis(),
) {
    private val _config = MutableStateFlow(FocusConfig())
    val config: StateFlow<FocusConfig> = _config.asStateFlow()

    private val _run = MutableStateFlow<FocusRun?>(null)
    val run: StateFlow<FocusRun?> = _run.asStateFlow()

    private val _history = MutableStateFlow(seed(now))
    /** Most recent first. */
    val history: StateFlow<List<FocusSessionRecord>> = _history.asStateFlow()

    /** The session that just ended, for the "Session complete" screen. */
    private val _lastCompleted = MutableStateFlow<FocusSessionRecord?>(null)
    val lastCompleted: StateFlow<FocusSessionRecord?> = _lastCompleted.asStateFlow()

    fun updateConfig(transform: (FocusConfig) -> FocusConfig) = _config.update(transform)

    /** Starts a session with the current set-up ([rounds] overrides the number of rounds). */
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
    }

    fun pause(now: Long) = _run.update { run ->
        if (run == null || run.isPaused) run else run.copy(phaseEndsAt = null, pausedRemainingMillis = run.remainingMillis(now))
    }

    fun resume(now: Long) = _run.update { run ->
        if (run == null || !run.isPaused) run else run.copy(phaseEndsAt = now + run.remainingMillis(now), pausedRemainingMillis = null)
    }

    /** Ends the break now and starts the next round. */
    fun skipBreak(now: Long) = _run.update { run ->
        if (run == null || run.phase != FocusPhase.Break) run else run.nextRound(startAt = now, autoStart = true)
    }

    /** "Back to focus" on the app-on-hold dialog. */
    fun resistUrge() = _run.update { it?.copy(urgesResisted = it.urgesResisted + 1) }

    /** Ends the session early. Returns the logged session, or null when nothing was focused. */
    fun end(now: Long): FocusSessionRecord? {
        val run = _run.value ?: return null
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

    fun reset(now: Long = System.currentTimeMillis()) {
        _config.value = FocusConfig()
        _run.value = null
        _lastCompleted.value = null
        _history.value = seed(now)
    }

    private fun FocusRun.nextRound(startAt: Long, autoStart: Boolean): FocusRun {
        val length = config.focusMinutes * 60_000L
        return copy(
            round = round + 1,
            phase = FocusPhase.Focus,
            phaseEndsAt = if (autoStart) startAt + length else null,
            pausedRemainingMillis = if (autoStart) null else length,
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

    /** The four sessions of the design (today 9:57 / 8:00, yesterday 9:10 PM, Thursday) + two weeks of history. */
    private fun seed(now: Long): List<FocusSessionRecord> {
        val today = now.toDate()
        fun at(date: LocalDate, hour: Int, minute: Int): Long {
            var millis = date.atTime(LocalTime.of(hour, minute)).atZone(zone()).toInstant().toEpochMilli()
            // Seeded "today" sessions that would start in the future move to the day before.
            if (millis > now) millis -= 24 * 3_600_000L
            return millis
        }
        fun record(id: String, title: String, kind: FocusRecordKind, startedAt: Long, minutes: Int, rounds: Int = 1, urges: Int = 0) =
            FocusSessionRecord(
                id = id,
                title = title,
                kind = kind,
                startedAt = startedAt,
                focusedMinutes = minutes,
                rounds = rounds,
                urgesResisted = urges,
            )

        val lastThursday = today.minusDays(1).with(TemporalAdjusters.previousOrSame(DayOfWeek.THURSDAY))
        val designed = listOf(
            record("seed-thesis", "Deep work · Thesis", FocusRecordKind.Timer, at(today, 9, 57), 45, urges = 2),
            record("seed-work", "Work Hours", FocusRecordKind.RuleSession, at(today, 8, 0), 90, urges = 2),
            record("seed-reading", "Reading, no phone", FocusRecordKind.Habit, at(today.minusDays(1), 21, 10), 30, urges = 1),
            record("seed-slides", "Deep work · Slides", FocusRecordKind.Timer, at(lastThursday, 14, 0), 75, rounds = 2, urges = 2),
        )
        val pattern = listOf(50, 25, 60, 40, 20, 55, 35, 70, 30, 45, 25, 65, 40)
        val older = pattern.mapIndexed { index, minutes ->
            val date = today.minusDays(index + 2L)
            record(
                id = "seed-$index",
                title = if (index % 3 == 1) "Reading, no phone" else "Deep work",
                kind = if (index % 3 == 1) FocusRecordKind.Habit else FocusRecordKind.Timer,
                startedAt = at(date, 9 + index % 4 * 3, if (index % 2 == 0) 0 else 30),
                minutes = minutes,
                // 31 urges over the two weeks: 38 with the designed sessions ("Urges resisted 38").
                urges = index % 4 + 1,
            )
        }
        return (designed + older).sortedByDescending { it.startedAt }
    }
}
