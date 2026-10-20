package com.recentr.core.data

import com.google.zxing.BinaryBitmap
import com.google.zxing.PlanarYUVLuminanceSource
import com.google.zxing.ReaderException
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.qrcode.QRCodeReader
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import com.google.zxing.qrcode.encoder.Encoder
import com.recentr.core.model.AppSettings
import com.recentr.core.model.Rule
import com.recentr.core.model.RuleType
import com.recentr.core.model.UnlockMethod
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime

// The friction between a held app's "Unlock anyway" and the app opening (Figma › Unlock friction,
// 48–55, and Unlock — Walk). Pure functions of the rule, the settings and what was typed, so the hold screen and the
// tests agree.

/** What stands between a held app and "Unlock anyway". */
enum class UnlockStep {
    /** Choose your unlock time (48): wait a few seconds first. */
    Wait,

    /** Type this to unlock (50, 51). */
    Retype,

    /** Scan your unlock pass (52). */
    Scan,

    /** Walk 20 steps to unlock, then a few more steps while they're counted. */
    Walk,

    /** Emergency unlock (54): the only way past Strict mode or a limit. */
    Emergency,
}

/**
 * The first step of unlocking this rule's apps at [now], or null when there's no way past it: no
 * unlocks left today, or only an emergency unlock gets past it ([onlyEmergency]) and this month's
 * are spent.
 */
fun Rule.unlockStep(now: Long, emergencyUnlocksLeft: Int, zone: ZoneId = ZoneId.systemDefault()): UnlockStep? = when {
    onlyEmergency(now, zone) -> UnlockStep.Emergency.takeIf { emergencyUnlocksLeft > 0 }
    !canOpenAnyway(now, zone) -> null
    else -> when (unlockMethod) {
        UnlockMethod.Wait -> UnlockStep.Wait
        UnlockMethod.Retype -> UnlockStep.Retype
        UnlockMethod.ScanCode -> UnlockStep.Scan
        UnlockMethod.Walk -> UnlockStep.Walk
    }
}

/**
 * Only an emergency unlock gets this rule's apps open at [now]: Strict mode while the rule is
 * engaged, or a limit, which has no unlocks.
 */
fun Rule.onlyEmergency(now: Long, zone: ZoneId = ZoneId.systemDefault()): Boolean =
    type == RuleType.Limit || isStrictLocked(now, zone)

/** The ways out of "How do you unlock?", by effort, lightest first. */
val UNLOCK_METHODS: List<UnlockMethod> = UnlockMethod.entries

/** The shortest wait before "Unlock anyway" opens an app. */
const val MIN_WAIT_SECONDS = 5

/** Seconds to wait with [UnlockMethod.Wait]: never less than [MIN_WAIT_SECONDS]. */
val Rule.waitSecondsInUse: Int
    get() = waitSeconds.coerceAtLeast(MIN_WAIT_SECONDS)

/**
 * Whether the rule's unlock method can be changed. Strict mode disables unlocks: while it's on, the
 * method is kept, unused, and comes back when Strict mode is turned off.
 */
val Rule.unlockMethodEnabled: Boolean
    get() = !strictMode

/**
 * What's been typed of the retype text (50), and whether a typo sent it back to the start (51).
 * One wrong letter, or pasted text, is a typo; the text then stays as typed until [startOver].
 */
data class Retype(val typed: String = "", val hasTypo: Boolean = false) {

    /** Takes the field's new text. */
    fun typed(text: String, target: String): Retype = when {
        hasTypo -> this
        target.startsWith(text) && text.length <= typed.length + 1 -> copy(typed = text)
        else -> Retype(typed = text, hasTypo = true)
    }

    fun startOver(): Retype = Retype()

    fun isDone(target: String): Boolean = !hasTypo && typed == target

    /** How much of [typed] matches [target], to show the typo in red. */
    fun matching(target: String): Int = typed.zip(target).takeWhile { (a, b) -> a == b }.size
}

/**
 * The steps walked to unlock, out of [STEPS], from two sources. The accelerometer's
 * ([StepDetector]) show at once while the hold screen is open. The step counter's are slower (it
 * waits for several steps in a row before reporting them all at once) but catch up on steps taken
 * with the screen off. A walk shows whichever has more. Steps never go back down, even when the
 * counter starts over (the phone restarted).
 */
data class Walk(
    /** The step counter's total when the walk started, once its first reading came. */
    val start: Float? = null,
    /** The accelerometer's steps. */
    val detected: Int = 0,
    val steps: Int = 0,
) {
    /**
     * Takes a reading of the step counter: its total since the phone started. It may send its first
     * reading only after a few steps, which the accelerometer already found: the walk started that
     * many steps before it.
     */
    fun counted(total: Float): Walk {
        val from = start ?: (total - detected)
        return copy(start = from, steps = maxOf(steps, (total - from).toInt()).coerceIn(0, STEPS))
    }

    /** The accelerometer's steps so far. */
    fun detected(steps: Int): Walk = copy(detected = steps, steps = maxOf(this.steps, steps).coerceAtMost(STEPS))

    /** One more step, without a step counter (the emulator's pretend walk). */
    fun stepped(): Walk = detected(detected + 1)

    val left: Int get() = STEPS - steps

    val isDone: Boolean get() = steps >= STEPS

    /** How far along the walk is, from 0 to 1. */
    val progress: Float get() = steps / STEPS.toFloat()

    companion object {
        /** The steps to walk before the app opens. */
        const val STEPS = 20
    }
}

/**
 * Steps found in the accelerometer as they're taken.
 *
 * The step counter is sure of its steps but reports them seconds late, in batches: it waits to be
 * sure it's a walk. This counts each step's bounce at once, while the hold screen is open. A bounce
 * counts once a few came in a row at a walking pace, so lifting the phone isn't a step.
 */
class StepDetector {
    /** Steps walked so far. */
    var steps = 0
        private set

    /** The last few readings, averaged to smooth out the jolts within a step. */
    private val recent = ArrayDeque<Double>()
    private var sum = 0.0
    /** The level the bounces rise above: gravity, give or take the sensor's offset. */
    private var baseline: Double? = null
    /** Whether the readings fell back since the last bounce, so the next one can count. */
    private var armed = true
    private var lastBounce: Double? = null
    /** Bounces in a row not counted yet, until there are enough of them to be a walk. */
    private var pending = 0
    private var walking = false

    /**
     * Takes a reading of the accelerometer at [time] (seconds): the size of its acceleration in g,
     * gravity included, so it doesn't matter how the phone is held.
     */
    fun add(magnitude: Double, time: Double) {
        recent.addLast(magnitude)
        sum += magnitude
        if (recent.size > WINDOW) sum -= recent.removeFirst()
        val smoothed = sum / recent.size
        val base = baseline?.let { it + (smoothed - it) * 0.02 } ?: smoothed
        baseline = base
        val level = smoothed - base

        if (!armed) {
            if (level < 0) armed = true
            return
        }
        if (level <= RISE) return
        val last = lastBounce
        if (last != null && time - last < SHORTEST_STEP) return
        armed = false
        if (last != null && time - last > LONGEST_STEP) {
            pending = 0
            walking = false
        }
        lastBounce = time
        if (walking) {
            steps++
        } else if (++pending >= BOUNCES_TO_WALK) {
            steps += pending
            pending = 0
            walking = true
        }
    }

    companion object {
        /** Readings averaged: 0.16 s at 50 a second. */
        const val WINDOW = 8

        /** How far above the baseline a bounce rises, in g. */
        const val RISE = 0.04

        /** Bounces closer than this (seconds) are the same step. */
        const val SHORTEST_STEP = 0.27

        /** A longer pause (seconds) ends the walk: the next bounces must show it's a walk again. */
        const val LONGEST_STEP = 2.0

        /** Bounces in a row before they count. */
        const val BOUNCES_TO_WALK = 3
    }
}

/**
 * The unlock pass of a "Scan a code" rule (53): a QR code to print and keep far away, with four
 * digits printed under it that can be typed instead.
 */
object UnlockPass {
    private const val SCHEME = "recentr-pass"

    /** The four digits printed on the pass. They come from the rule's id, so a pass never changes. */
    fun code(rule: Rule): String {
        val number = rule.id.fold(0) { acc, char -> (acc * 31 + char.code) % 9000 }
        return (1000 + number).toString()
    }

    /** What the pass's QR code says: the rule it opens and its code. */
    fun payload(rule: Rule): String = "$SCHEME:${rule.id}:${code(rule)}"

    /** Whether a scanned QR code, or a typed code ("4821", "#4821"), opens [rule]. */
    fun opens(rule: Rule, scanned: String): Boolean {
        val value = scanned.trim()
        return value == payload(rule) || value.removePrefix("#") == code(rule)
    }

    /**
     * The pass's QR code, row by row (true = dark module), without its quiet zone. Error correction
     * is high enough for the logo drawn over its centre.
     */
    fun modules(rule: Rule): List<BooleanArray> {
        val matrix = Encoder.encode(payload(rule), ErrorCorrectionLevel.Q).matrix
        return List(matrix.height) { y -> BooleanArray(matrix.width) { x -> matrix.get(x, y).toInt() == 1 } }
    }
}

/** Reads QR codes in camera frames. Not thread-safe: one per camera analyzer. */
class QrReader {
    private val reader = QRCodeReader()

    /**
     * The text of the QR code in a frame's luminance (its Y plane, [rowStride] bytes a row), or
     * null when there's none.
     */
    fun read(luminance: ByteArray, width: Int, height: Int, rowStride: Int = width): String? {
        val source = PlanarYUVLuminanceSource(luminance, rowStride, height, 0, 0, width, height, false)
        return try {
            reader.decode(BinaryBitmap(HybridBinarizer(source))).text
        } catch (_: ReaderException) {
            null
        } finally {
            reader.reset()
        }
    }
}

/**
 * Emergency unlocks (54): the way past Strict mode and limits, three a month across every rule.
 * Each one opens the app for [MINUTES] minutes.
 */
object EmergencyUnlocks {
    const val PER_MONTH = 3
    const val MINUTES = 5

    /** How long "Hold to unlock" has to be held. */
    const val HOLD_MILLIS = 3_000L

    fun left(settings: AppSettings, now: Long, zone: ZoneId): Int {
        val used = if (settings.emergencyUnlocksMonth == month(now, zone)) settings.emergencyUnlocksUsed else 0
        return (PER_MONTH - used).coerceAtLeast(0)
    }

    /** [settings] with one more emergency unlock spent this month. */
    fun spend(settings: AppSettings, now: Long, zone: ZoneId): AppSettings {
        val month = month(now, zone)
        val used = if (settings.emergencyUnlocksMonth == month) settings.emergencyUnlocksUsed else 0
        return settings.copy(emergencyUnlocksUsed = used + 1, emergencyUnlocksMonth = month)
    }

    /** When this month's emergency unlocks come back: the 1st of next month (epoch millis). */
    fun resetAt(now: Long, zone: ZoneId): Long =
        local(now, zone).toLocalDate().withDayOfMonth(1).plusMonths(1).atStartOfDay(zone).toInstant().toEpochMilli()

    private fun month(now: Long, zone: ZoneId): Int = local(now, zone).let { it.year * 12 + it.monthValue - 1 }

    private fun local(now: Long, zone: ZoneId): ZonedDateTime = Instant.ofEpochMilli(now).atZone(zone)
}
