package com.recentr.core.data

import com.recentr.core.model.FocusPhase
import com.recentr.core.model.FocusRun
import com.recentr.core.model.HabitRepeat
import com.recentr.core.model.LimitAction
import com.recentr.core.model.Rule
import com.recentr.core.model.RuleSchedule
import com.recentr.core.model.RuleType
import com.recentr.core.model.UnlockMethod
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime

/** Why an opened app is put on hold. */
sealed interface HoldReason {
    /** The app is blocked by the running focus session. */
    data class Focus(val run: FocusRun) : HoldReason

    /** The app is blocked by an active [rule]. */
    data class ByRule(val rule: Rule) : HoldReason
}

/** Everything that decides whether an opened app is put on hold. */
data class HoldState(
    val rules: List<Rule>,
    val focus: FocusRun?,
    /** Never put on hold: Recentr, the home screens, Settings and System UI. */
    val protectedApps: Set<String>,
    /** Apps opened anyway, and until when (epoch millis). */
    val openUntil: Map<String, Long> = emptyMap(),
    /** Today's minutes per package, or null when unknown (no usage access): limits can't trip. */
    val usedToday: Map<String, Int>? = null,
) {
    /**
     * Why [packageName] is on hold at [now], or null when it may be used. A focus session holds
     * its apps during focus rounds; an "Open anyway" lifts the rules' hold (not the session's)
     * until it runs out. When several rules hold the app, one without "Open anyway" wins.
     */
    fun reasonFor(packageName: String, now: Long, zone: ZoneId): HoldReason? {
        if (packageName in protectedApps) return null
        focus?.takeIf { run ->
            run.phase == FocusPhase.Focus && !run.isPaused && run.config.blockedApps.any { it.packageName == packageName }
        }?.let { return HoldReason.Focus(it) }
        if ((openUntil[packageName] ?: 0L) > now) return null
        val rule = rules.filter { it.holds(packageName, now, zone) }.minByOrNull { if (it.canOpenAnyway(now)) 1 else 0 }
        return rule?.let(HoldReason::ByRule)
    }

    private fun Rule.holds(packageName: String, now: Long, zone: ZoneId): Boolean {
        if (!isEnforcing(now, zone)) return false
        return when (type) {
            RuleType.Lock, RuleType.Session, RuleType.Bedtime -> targets(packageName)
            RuleType.Limit -> {
                val used = usedToday
                targets(packageName) && limitAction != LimitAction.Remind && used != null &&
                    apps.sumOf { used[it.packageName] ?: 0 } >= (limitMinutes ?: Int.MAX_VALUE)
            }
            RuleType.Habit -> targets(packageName) && habitTasks.any { !it.done } && habitRepeat.includes(now, zone)
        }
    }

    private fun HabitRepeat.includes(now: Long, zone: ZoneId): Boolean {
        val day = Instant.ofEpochMilli(now).atZone(zone).dayOfWeek.value
        return when (this) {
            HabitRepeat.EveryDay -> true
            HabitRepeat.Weekdays -> day <= 5
            HabitRepeat.Weekends -> day >= 6
        }
    }
}

/**
 * Whether the hold screen offers "Open anyway" for this rule at [now]: a wait, reason or retype
 * unlock with unlocks left today, and no strict mode holding. (Scanning the unlock pass is not
 * available yet, so those rules can't be opened.)
 */
fun Rule.canOpenAnyway(now: Long): Boolean =
    unlockMethod in OPENABLE && !isStrictLocked(now) && (unlocksPerDay?.let { unlocksUsedToday < it } ?: true)

/** Unlocks left today, or null when unlimited. */
val Rule.unlocksLeft: Int?
    get() = unlocksPerDay?.let { (it - unlocksUsedToday).coerceAtLeast(0) }

/**
 * When the rule stops holding apps (epoch millis): the end of the running session or of the
 * current scheduled window, or null when it holds all day (locks, limits, habits).
 */
fun Rule.heldUntil(now: Long, zone: ZoneId): Long? {
    if (activeSince != null) return sessionEndsAt
    val window = schedule ?: return null
    if (window.lengthMinutes >= RuleSchedule.MINUTES_PER_DAY) return null
    val time = ZonedDateTime.ofInstant(Instant.ofEpochMilli(now), zone)
    val minute = time.hour * 60 + time.minute
    val endDate = if (window.overnight && minute >= window.startMinute) time.toLocalDate().plusDays(1) else time.toLocalDate()
    return endDate.atStartOfDay(zone).plusMinutes(window.endMinute.toLong()).toInstant().toEpochMilli()
}

private val OPENABLE = setOf(UnlockMethod.Wait, UnlockMethod.Reason, UnlockMethod.Retype)
