package com.recentr.core.model

import kotlinx.serialization.Serializable

/** Ambient sounds of the focus timer (Figma "Focus sounds"): recordings that loop while a round runs. */
@Serializable
enum class FocusSound(val category: FocusSoundCategory) {
    SoftRain(FocusSoundCategory.Rain),
    HeavyRain(FocusSoundCategory.Rain),
    ForestRain(FocusSoundCategory.Rain),
    RainVillage(FocusSoundCategory.Rain),
    Thunder(FocusSoundCategory.Rain),
    River(FocusSoundCategory.Water),
    SmallRiver(FocusSoundCategory.Water),
    Beach(FocusSoundCategory.Water),
    SeaBeach(FocusSoundCategory.Water),
    SoftBeach(FocusSoundCategory.Water),
    Whale(FocusSoundCategory.Water),
    ForestNight(FocusSoundCategory.Nature),
    Birds(FocusSoundCategory.Nature),
    Crickets(FocusSoundCategory.Nature),
    CicadaBirds(FocusSoundCategory.Nature),
    Farm(FocusSoundCategory.Nature),
    Wind(FocusSoundCategory.Nature),
    Campfire(FocusSoundCategory.Nature),
    CoffeeShop(FocusSoundCategory.Places),
    Office(FocusSoundCategory.Places),
    StreetTraffic(FocusSoundCategory.Places),
    Construction(FocusSoundCategory.Places),
    KeyboardTyping(FocusSoundCategory.Places),
    ClockTick(FocusSoundCategory.Steady),
    ClockStopwatch(FocusSoundCategory.Steady),
    WhiteNoise(FocusSoundCategory.Steady),
    PinkNoise(FocusSoundCategory.Steady),
}

/** How the sounds sheet groups the [FocusSound]s. */
enum class FocusSoundCategory {
    Rain,
    Water,
    Nature,
    Places,
    Steady,
    ;

    val sounds: List<FocusSound> get() = FocusSound.entries.filter { it.category == this }
}

/** One sound of a focus mix, and how loud it plays (0 – 1). */
@Serializable
data class FocusSoundLayer(val sound: FocusSound, val volume: Float = DEFAULT_VOLUME) {
    companion object {
        /** How loud a sound comes in when it's turned on. */
        const val DEFAULT_VOLUME = 0.6f

        /** What a first session plays. */
        val DefaultMix = listOf(FocusSoundLayer(FocusSound.SoftRain))
    }
}

/** How a focus session is set up (Figma 91 "Focus — Set up" + 98 "Rounds"). */
data class FocusConfig(
    val label: String = "Deep work",
    /** 25-minute rounds at first. */
    val focusMinutes: Int = 25,
    val shortBreakMinutes: Int = 5,
    val rounds: Int = 4,
    val longBreakMinutes: Int = 15,
    val autoStartNextRound: Boolean = true,
    /** The sounds mixed while a round runs, each at its own volume. None for silence. */
    val sounds: List<FocusSoundLayer> = FocusSoundLayer.DefaultMix,
    /** The sounds muted in the sounds sheet, at the volume they come back at: still muted the next time it opens. */
    val mutedSounds: List<FocusSoundLayer> = emptyList(),
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
    /** When the person paused (wall clock): "paused for 0:42". Null while running or waiting. */
    val pausedAt: Long? = null,
    /** Auto-start is off and this round waits for "Start round" (Figma 92E). */
    val awaitingStart: Boolean = false,
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
