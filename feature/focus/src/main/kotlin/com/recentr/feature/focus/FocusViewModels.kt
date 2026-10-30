package com.recentr.feature.focus

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.recentr.core.data.FocusRepository
import com.recentr.core.data.RecentrData
import com.recentr.core.model.FocusConfig
import com.recentr.core.model.FocusRun
import com.recentr.core.model.FocusSessionRecord
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Set-up (91) and its sheets: edits the [FocusConfig] and starts a session. */
internal class FocusSetupViewModel(private val repository: FocusRepository) : ViewModel() {
    val config: StateFlow<FocusConfig> = repository.config

    /** The running session, if any (the Timer tab offers to go back to it). */
    val run: StateFlow<FocusRun?> = repository.run

    fun update(transform: (FocusConfig) -> FocusConfig) = repository.updateConfig(transform)

    fun start() = repository.start(System.currentTimeMillis())

    companion object {
        val Factory = viewModelFactory { initializer { FocusSetupViewModel(RecentrData.focus) } }
    }
}

@Immutable
internal data class FocusSessionUiState(
    val run: FocusRun?,
    val now: Long,
    val reduceMotion: Boolean = false,
    /** The round just restarted, while its toast offers to undo it (Figma 92C). */
    val restartedRound: Int? = null,
)

/**
 * Running (92) / Break (94) / App on hold (96). A 4 Hz clock drives the countdown and settles
 * finished phases ([FocusRepository.advance]).
 */
internal class FocusSessionViewModel(private val repository: FocusRepository) : ViewModel() {

    private val clock = flow {
        while (true) {
            emit(System.currentTimeMillis())
            delay(250)
        }
    }.onEach { repository.advance(it) }

    /** The session before its round was restarted, for Undo, while the toast shows. */
    private val restarted = MutableStateFlow<FocusRun?>(null)
    private var restartToast: Job? = null

    val state: StateFlow<FocusSessionUiState> = combine(
        repository.run,
        clock,
        RecentrData.settings.settings.map { it.reduceMotion },
        restarted,
    ) { run, now, reduceMotion, before ->
        FocusSessionUiState(run = run, now = now, reduceMotion = reduceMotion, restartedRound = before?.round)
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        FocusSessionUiState(run = repository.run.value, now = System.currentTimeMillis()),
    )

    val lastCompleted: StateFlow<FocusSessionRecord?> = repository.lastCompleted

    fun pause() = repository.pause(System.currentTimeMillis())

    fun resume() = repository.resume(System.currentTimeMillis())

    fun skipBreak() = repository.skipBreak(System.currentTimeMillis())

    /** "Restart": the round starts over at once; a toast offers Undo for [RESTART_TOAST_MILLIS]. */
    fun restartRound() {
        val before = repository.restartRound(System.currentTimeMillis()) ?: return
        restarted.value = before
        restartToast?.cancel()
        restartToast = viewModelScope.launch {
            delay(RESTART_TOAST_MILLIS)
            restarted.value = null
        }
    }

    fun undoRestart() {
        val before = restarted.value ?: return
        restarted.value = null
        restartToast?.cancel()
        repository.undoRestart(before)
    }

    /** Ends now; returns true when something was logged (→ "Session complete"). */
    fun end(): Boolean = repository.end(System.currentTimeMillis()) != null

    fun updateConfig(transform: (FocusConfig) -> FocusConfig) {
        repository.updateConfig(transform)
        // Sound changes apply to the running session too.
        val run = repository.run.value ?: return
        val updated = transform(run.config)
        repository.updateRun { it.copy(config = run.config.copy(sounds = updated.sounds)) }
    }

    companion object {
        val Factory = viewModelFactory { initializer { FocusSessionViewModel(RecentrData.focus) } }
        const val RESTART_TOAST_MILLIS = 5_000L
    }
}

@Immutable
internal data class FocusCompleteUiState(
    val record: FocusSessionRecord? = null,
    val streakDays: Int = 0,
    val longestThisWeek: Boolean = false,
)

/** Session complete (95). */
internal class FocusCompleteViewModel(private val repository: FocusRepository) : ViewModel() {

    val state: StateFlow<FocusCompleteUiState> = combine(repository.lastCompleted, repository.history) { record, history ->
        val now = System.currentTimeMillis()
        val weekStart = repository.weekStart(now)
        val longest = record != null && history
            .filter { it.id != record.id && !it.startedAt.toDate().isBefore(weekStart) }
            .all { it.focusedMinutes < record.focusedMinutes }
        FocusCompleteUiState(record = record, streakDays = repository.streakDays(now), longestThisWeek = longest)
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        FocusCompleteUiState(record = repository.lastCompleted.value),
    )

    fun updateNote(note: String) {
        val record = repository.lastCompleted.value ?: return
        repository.updateNote(record.id, note)
    }

    /** "Start another round": one more round with the same set-up. */
    fun startAnotherRound() = repository.start(System.currentTimeMillis(), rounds = 1)

    companion object {
        val Factory = viewModelFactory { initializer { FocusCompleteViewModel(RecentrData.focus) } }
    }
}

@Immutable
internal data class FocusHistoryUiState(
    val sessions: List<FocusSessionRecord> = emptyList(),
    /** Minutes per day, Monday → Sunday of this week. */
    val week: List<Int> = List(7) { 0 },
    val lastWeekMinutes: Int = 0,
    /** 0 = Monday. */
    val todayIndex: Int = 0,
) {
    val weekMinutes: Int get() = week.sum()
}

/** Focus log (97). */
internal class FocusHistoryViewModel(private val repository: FocusRepository) : ViewModel() {

    val state: StateFlow<FocusHistoryUiState> = repository.history.map { history ->
        val now = System.currentTimeMillis()
        val weekStart = repository.weekStart(now)
        FocusHistoryUiState(
            sessions = history,
            week = repository.minutesPerDay(weekStart),
            lastWeekMinutes = repository.minutesPerDay(weekStart.minusWeeks(1)).sum(),
            todayIndex = (LocalDate.now().dayOfWeek.value - 1),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), FocusHistoryUiState())

    fun clear() = repository.clearHistory()

    companion object {
        val Factory = viewModelFactory { initializer { FocusHistoryViewModel(RecentrData.focus) } }
    }
}

private fun Long.toDate(): LocalDate = Instant.ofEpochMilli(this).atZone(ZoneId.systemDefault()).toLocalDate()
