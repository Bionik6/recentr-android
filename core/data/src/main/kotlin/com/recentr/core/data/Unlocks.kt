package com.recentr.core.data

import com.google.zxing.BinaryBitmap
import com.google.zxing.PlanarYUVLuminanceSource
import com.google.zxing.ReaderException
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.qrcode.QRCodeReader
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import com.google.zxing.qrcode.encoder.Encoder
import com.recentr.core.model.AppSettings
import com.recentr.core.model.LimitAction
import com.recentr.core.model.Rule
import com.recentr.core.model.RuleType
import com.recentr.core.model.UnlockMethod
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime

// The friction between a held app's "Unlock anyway" and the app opening (Figma › Unlock friction,
// 48–55). Pure functions of the rule, the settings and what was typed, so the hold screen and the
// tests agree.

/** What stands between a held app and "Unlock anyway". */
enum class UnlockStep {
    /** Choose your unlock time (48): wait a few seconds first. */
    Wait,

    /** Give a good reason (49). */
    Reason,

    /** Type this to unlock (50, 51). */
    Retype,

    /** Scan your unlock pass (52). */
    Scan,

    /** Emergency unlock (54): the only way past Strict mode. */
    Emergency,
}

/**
 * The first step of unlocking this rule's apps at [now], or null when there's no way past it: no
 * unlocks left today, a limit that locks, or Strict mode with this month's emergency unlocks spent.
 */
fun Rule.unlockStep(now: Long, emergencyUnlocksLeft: Int, zone: ZoneId = ZoneId.systemDefault()): UnlockStep? = when {
    isStrictLocked(now, zone) -> UnlockStep.Emergency.takeIf { emergencyUnlocksLeft > 0 }
    !canOpenAnyway(now, zone) -> null
    else -> when (unlockMethodInUse) {
        UnlockMethod.Wait -> UnlockStep.Wait
        UnlockMethod.Reason -> UnlockStep.Reason
        UnlockMethod.Retype -> UnlockStep.Retype
        UnlockMethod.ScanCode -> UnlockStep.Scan
        UnlockMethod.None, UnlockMethod.Strict -> null
    }
}

/**
 * How this rule's apps are opened anyway, Strict mode aside. Strict mode is the only "no unlocks":
 * locks, sessions and routines left with "No unlocks" (gone from "How do you unlock?") wait a few
 * seconds. A limit has no unlock method: "When time is up" decides — Ask for a reason, or Lock
 * until it resets. A habit opens with its tasks.
 */
val Rule.unlockMethodInUse: UnlockMethod
    get() = when (type) {
        RuleType.Lock, RuleType.Session, RuleType.Bedtime ->
            unlockMethod.takeUnless { it == UnlockMethod.None || it == UnlockMethod.Strict } ?: UnlockMethod.Wait
        RuleType.Limit -> if (limitAction == LimitAction.AskReason) UnlockMethod.Reason else UnlockMethod.None
        RuleType.Habit -> unlockMethod
    }

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

/** Give a good reason (49): long enough to mean it. */
object UnlockReason {
    const val MINIMUM_LENGTH = 20

    fun length(reason: String): Int = reason.trim().length

    fun isEnough(reason: String): Boolean = length(reason) >= MINIMUM_LENGTH

    /** A suggestion chip tapped: "Work: " to start the reason, or added to what's written. */
    fun withSuggestion(reason: String, suggestion: String): String {
        val trimmed = reason.trim()
        return if (trimmed.isEmpty()) "$suggestion: " else "$trimmed ${suggestion.lowercase()}"
    }
}

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
 * Emergency unlocks (54): the way past Strict mode, three a month across every rule. Each one
 * opens the app for [MINUTES] minutes.
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
