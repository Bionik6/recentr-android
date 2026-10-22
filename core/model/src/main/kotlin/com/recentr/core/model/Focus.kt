package com.recentr.core.model

/** Ambient sounds of the focus timer (Figma "Focus sounds"). */
enum class FocusSound { Rain, Forest, Night, Fireplace, Clock, Silence }

/** How a focus session is set up (Figma 91 "Focus — Set up" + 98 "Rounds"). */
data class FocusConfig(
    val label: String = "Deep work",
    /** 25-minute rounds at first. */
    val focusMinutes: Int = 25,
    val shortBreakMinutes: Int = 5,
    val rounds: Int = 4,
    val longBreakMinutes: Int = 15,
    val autoStartNextRound: Boolean = true,
    val sound: FocusSound = FocusSound.Rain,
    /** 0 – 1. */
    val volume: Float = 0.6f,
    /** Apps put on hold during the session, picked from the device's apps (none at first). */
    val blockedApps: List<UsageApp> = emptyList(),
    /**
     * Strict mode: the session can't be paused or ended early, only waited out. Breaks still
     * come. Chosen for each session: it's off again once one starts.
     */
    val strictMode: Boolean = false,
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
        const val MAX_FOCUS = 180
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

    /** Strict mode keeps the session from being paused or ended early. */
    val isStrict: Boolean get() = config.strictMode

    fun remainingMillis(now: Long): Long =
        (phaseEndsAt?.let { it - now } ?: pausedRemainingMillis ?: phaseLengthMillis).coerceIn(0, phaseLengthMillis)

    /** Focus time so far, the current round included. */
    fun focusedMillis(now: Long): Long = completedFocusMillis +
        if (phase == FocusPhase.Focus) phaseLengthMillis - remainingMillis(now) else 0

    /**
     * When the session will be over, from [now]: what's left of this phase, then the breaks and
     * rounds still to come. A projection: a round waiting for "Start round" (auto-start off) or a
     * pause makes it later.
     */
    fun projectedEnd(now: Long): Long {
        var end = now + remainingMillis(now)
        var round = round
        var phase = phase
        while (true) {
            if (phase == FocusPhase.Focus) {
                if (round >= config.rounds) return end
                end += config.breakMinutesAfter(round) * 60_000L
                phase = FocusPhase.Break
            } else {
                round += 1
                end += config.focusMinutes * 60_000L
                phase = FocusPhase.Focus
            }
        }
    }
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
