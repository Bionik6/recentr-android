package com.recentr.core.model

/** Ambient sounds of the focus timer (Figma "Focus sounds"). */
enum class FocusSound { Rain, Forest, Night, Fireplace, Clock, Silence }

/** How a focus session is set up (Figma 91 "Focus — Set up" + 98 "Rounds"). */
data class FocusConfig(
    val label: String = "Deep work",
    val focusMinutes: Int = 45,
    val shortBreakMinutes: Int = 5,
    val rounds: Int = 4,
    val longBreakMinutes: Int = 15,
    val autoStartNextRound: Boolean = true,
    val sound: FocusSound = FocusSound.Rain,
    /** 0 – 1. */
    val volume: Float = 0.6f,
    /** Apps put on hold during the session, picked from the device's apps (none at first). */
    val blockedApps: List<UsageApp> = emptyList(),
) {
    /** Break after [round] (1-based): a long break every [LONG_BREAK_EVERY] rounds, short otherwise. */
    fun breakMinutesAfter(round: Int): Int =
        if (round % LONG_BREAK_EVERY == 0) longBreakMinutes else shortBreakMinutes

    /** Whole session, breaks between rounds included ("Total: 3h 15m with breaks"). */
    val totalMinutes: Int
        get() = rounds * focusMinutes + (1 until rounds).sumOf { breakMinutesAfter(it) }

    companion object {
        const val LONG_BREAK_EVERY = 4
        const val MIN_FOCUS = 5
        const val MAX_FOCUS = 120
        const val MAX_ROUNDS = 8
    }
}

enum class FocusPhase { Focus, Break }

/**
 * A running focus session. While running, the current phase ends at [phaseEndsAt] (wall clock);
 * while paused, [pausedRemainingMillis] holds what is left of it.
 */
data class FocusRun(
    val config: FocusConfig,
    val startedAt: Long,
    /** 1-based round being focused on (or the round just finished, during its break). */
    val round: Int,
    val phase: FocusPhase,
    val phaseEndsAt: Long?,
    val pausedRemainingMillis: Long?,
    /** Focus time of the rounds already completed. */
    val completedFocusMillis: Long = 0,
    /** "Back to focus" taps on the app-on-hold dialog. */
    val urgesResisted: Int = 0,
    /** "Open anyway" / early ends — always 0 for now. */
    val unlocks: Int = 0,
) {
    val isPaused: Boolean get() = phaseEndsAt == null

    val phaseLengthMillis: Long
        get() = when (phase) {
            FocusPhase.Focus -> config.focusMinutes * 60_000L
            FocusPhase.Break -> config.breakMinutesAfter(round) * 60_000L
        }

    fun remainingMillis(now: Long): Long =
        (phaseEndsAt?.let { it - now } ?: pausedRemainingMillis ?: phaseLengthMillis).coerceIn(0, phaseLengthMillis)

    /** Focus time so far, the current round included. */
    fun focusedMillis(now: Long): Long = completedFocusMillis +
        if (phase == FocusPhase.Focus) phaseLengthMillis - remainingMillis(now) else 0
}

/** A finished focus session (Figma 97 "Focus log"). */
data class FocusSessionRecord(
    val id: String,
    val title: String,
    val kind: FocusRecordKind,
    val startedAt: Long,
    val focusedMinutes: Int,
    val rounds: Int,
    val unlocks: Int = 0,
    val urgesResisted: Int = 0,
    /** "What did you finish?" note. */
    val note: String = "",
)

/** Sticker of a focus-log row: timer sessions, rule sessions, offline habits. */
enum class FocusRecordKind { Timer, RuleSession, Habit }
