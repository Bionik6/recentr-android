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

/**
 * The apps opened anyway ("Unlock anyway", emergency unlocks): for each app, until when (epoch
 * millis) each rule lets it through. An unlock only lifts the rules that held the app when it was
 * unlocked, so one rule's unlock never opens the app past another rule: a routine starting or a
 * limit running out during the unlock puts the app on hold again.
 */
data class AppUnlocks(private val until: Map<String, Map<String, Long>> = emptyMap()) {

    /** Whether [rule] lets [packageName] through at [now]. */
    fun lifts(packageName: String, rule: Rule, now: Long): Boolean = (until[packageName]?.get(rule.id) ?: 0L) > now

    /** [packageName] let through by [rules] until [end]; an unlock still running is never shortened. */
    fun open(packageName: String, rules: Collection<Rule>, end: Long): AppUnlocks {
        val app = until[packageName].orEmpty()
        return AppUnlocks(until + (packageName to app + rules.associate { it.id to maxOf(end, app[it.id] ?: 0L) }))
    }

    /** "Lock it again" (55): every unlock of [packageName] ends at [now]. */
    fun lockAgain(packageName: String, now: Long): AppUnlocks {
        val app = until[packageName] ?: return this
        return AppUnlocks(until + (packageName to app.mapValues { (_, end) -> minOf(end, now) }))
    }

    /** When [rule] last closed [packageName] again: the end of its last unlock, if over by [now]. */
    fun lastEnd(packageName: String, rule: Rule, now: Long): Long? = until[packageName]?.get(rule.id)?.takeIf { it <= now }
}

/** Everything that decides whether an opened app is put on hold. */
data class HoldState(
    val rules: List<Rule>,
    val focus: FocusRun?,
    /** Never put on hold: Recentr, the home screens, Settings and System UI. */
    val protectedApps: Set<String>,
    /** Apps opened anyway, and past which rules. */
    val unlocks: AppUnlocks = AppUnlocks(),
    /** Today's minutes per package, or null when unknown (no usage access): limits can't trip. */
    val usedToday: Map<String, Int>? = null,
) {
    /**
     * Why [packageName] is on hold at [now], or null when it may be used. A focus session holds
     * its apps during focus rounds; an unlock lifts the rules it was granted past (not the
     * session) until it runs out. When several rules hold the app, one without "Open anyway" wins.
     */
    fun reasonFor(packageName: String, now: Long, zone: ZoneId): HoldReason? {
        if (packageName in protectedApps) return null
        focus?.takeIf { run ->
            run.phase == FocusPhase.Focus && !run.isPaused && run.config.blockedApps.any { it.packageName == packageName }
        }?.let { return HoldReason.Focus(it) }
        val rule = holding(packageName, now, zone)
            .filterNot { unlocks.lifts(packageName, it, now) }
            .minByOrNull { if (it.canOpenAnyway(now, zone)) 1 else 0 }
        return rule?.let(HoldReason::ByRule)
    }

    /** The rules holding [packageName] at [now], unlocks aside. */
    fun holding(packageName: String, now: Long, zone: ZoneId): List<Rule> =
        if (packageName in protectedApps) emptyList() else rules.filter { it.holds(packageName, now, zone) }

    /**
     * The rules an "Unlock anyway" through [through] lifts at [now]: [through], and the other rules
     * holding the app that could be unlocked too. Never one that can't (Strict mode, "No unlocks",
     * none left today): it keeps the app on hold.
     */
    fun liftedBy(through: Rule, packageName: String, now: Long, zone: ZoneId): List<Rule> =
        (listOf(through) + holding(packageName, now, zone).filter { it.canOpenAnyway(now, zone) }).distinctBy { it.id }

    private fun Rule.holds(packageName: String, now: Long, zone: ZoneId): Boolean {
        if (!isEnforcing(now, zone)) return false
        return when (type) {
            RuleType.Lock, RuleType.Session, RuleType.Bedtime -> targets(packageName)
            RuleType.Limit -> {
                val used = usedToday
                targets(packageName) && limitAction != LimitAction.Remind && used != null &&
                    usedOf(used) >= (limitMinutes ?: Int.MAX_VALUE)
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

/** Minutes the rule's apps were used today, given today's minutes per package: a limit's budget spent. */
fun Rule.usedOf(usedToday: Map<String, Int>): Int = apps.sumOf { usedToday[it.packageName] ?: 0 }

/**
 * Whether the hold screen offers "Unlock anyway" for this rule at [now]: an unlock method (or a
 * limit that asks for a reason), unlocks left today, and no strict mode holding. Strict mode is
 * only passed with an emergency unlock ([unlockStep]).
 */
fun Rule.canOpenAnyway(now: Long, zone: ZoneId = ZoneId.systemDefault()): Boolean =
    (unlockMethod in OPENABLE || asksReason) && !isStrictLocked(now, zone) && (unlocksPerDay?.let { unlocksUsedToday < it } ?: true)

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

/**
 * When the rule started holding its apps, for the shield's "You've stayed off for…": the running
 * session's start, the current scheduled window's start, or the start of the day for rules that
 * hold all day. Null for limits: the moment their budget ran out isn't kept.
 */
fun Rule.blockStart(now: Long, zone: ZoneId): Long? {
    activeSince?.let { return it }
    if (type == RuleType.Limit) return null
    val time = ZonedDateTime.ofInstant(Instant.ofEpochMilli(now), zone)
    val window = schedule
    if (window == null || window.lengthMinutes >= RuleSchedule.MINUTES_PER_DAY) {
        return time.toLocalDate().atStartOfDay(zone).toInstant().toEpochMilli()
    }
    val minute = time.hour * 60 + time.minute
    val startDate = if (window.overnight && minute < window.endMinute) time.toLocalDate().minusDays(1) else time.toLocalDate()
    return startDate.atStartOfDay(zone).plusMinutes(window.startMinute.toLong()).toInstant().toEpochMilli()
}

private val OPENABLE = setOf(UnlockMethod.Wait, UnlockMethod.Reason, UnlockMethod.Retype, UnlockMethod.ScanCode)
