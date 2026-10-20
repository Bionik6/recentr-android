package com.recentr.core.data

import com.recentr.core.model.FocusPhase
import com.recentr.core.model.FocusRun
import com.recentr.core.model.Rule
import com.recentr.core.model.RuleType
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
 * limit's budget running out during the unlock puts the app on hold again.
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
    /**
     * Use of the started limits' apps since their Start, or null when it wasn't read (no usage
     * access): a limit then goes by the minutes it last recorded ([Rule.usedMinutes]).
     */
    val limitUsage: LimitUsage? = null,
) {
    /**
     * Why [packageName] is on hold at [now], or null when it may be used. A focus session holds
     * its apps during focus rounds; an unlock lifts the rules it was granted past (not the
     * session) until it runs out. When several rules hold the app, the strictest wins: one in
     * Strict mode (only an emergency unlock gets past it, and past the others), then one without
     * "Open anyway" (a limit, no unlocks left today), whatever their order.
     */
    fun reasonFor(packageName: String, now: Long, zone: ZoneId): HoldReason? {
        if (packageName in protectedApps) return null
        focus?.takeIf { run ->
            run.phase == FocusPhase.Focus && !run.isPaused && run.config.blockedApps.any { it.packageName == packageName }
        }?.let { return HoldReason.Focus(it) }
        val rule = holding(packageName, now, zone)
            .filterNot { unlocks.lifts(packageName, it, now) }
            .minByOrNull {
                when {
                    it.isStrictLocked(now, zone) -> 0
                    !it.canOpenAnyway(now, zone) -> 1
                    else -> 2
                }
            }
        return rule?.let(HoldReason::ByRule)
    }

    /** The rules holding [packageName] at [now], unlocks aside. */
    fun holding(packageName: String, now: Long, zone: ZoneId): List<Rule> =
        if (packageName in protectedApps) emptyList() else rules.filter { it.holds(packageName, now, zone) }

    /**
     * The rules an "Unlock anyway" through [through] lifts at [now]: [through], and the other rules
     * holding the app that could be unlocked too. Never one that can't (Strict mode, a limit, none
     * left today): it keeps the app on hold.
     */
    fun liftedBy(through: Rule, packageName: String, now: Long, zone: ZoneId): List<Rule> =
        (listOf(through) + holding(packageName, now, zone).filter { it.canOpenAnyway(now, zone) }).distinctBy { it.id }

    /** A limit holds its apps once its budget is spent, until midnight; the other rules while engaged. */
    private fun Rule.holds(packageName: String, now: Long, zone: ZoneId): Boolean {
        if (!targets(packageName) || !isEnforcing(now, zone)) return false
        if (type != RuleType.Limit) return true
        val used = limitUsage?.of(this) ?: usedMinutes
        return used >= (limitMinutes ?: Int.MAX_VALUE)
    }
}

/** Minutes the rule's apps were used, given minutes per package since a limit's Start: the budget spent. */
fun Rule.usedOf(minutesByPackage: Map<String, Int>): Int = apps.sumOf { minutesByPackage[it.packageName] ?: 0 }

/**
 * Whether the hold screen offers "Unlock anyway" for this rule at [now]: unlocks left today, no
 * Strict mode holding and not a limit (a limit has no unlocks). Strict mode and limits are only
 * passed with an emergency unlock ([unlockStep]).
 */
fun Rule.canOpenAnyway(now: Long, zone: ZoneId = ZoneId.systemDefault()): Boolean =
    type != RuleType.Limit && !isStrictLocked(now, zone) && (unlocksPerDay?.let { unlocksUsedToday < it } ?: true)

/** Unlocks left today, or null when unlimited. */
val Rule.unlocksLeft: Int?
    get() = unlocksPerDay?.let { (it - unlocksUsedToday).coerceAtLeast(0) }

/**
 * When the rule stops holding apps (epoch millis): the end of the running session or of the
 * routine's current window, midnight for a limit. Null when nothing ends it on its own: a lock,
 * a routine's whole-day window.
 */
fun Rule.heldUntil(now: Long, zone: ZoneId): Long? = when (type) {
    RuleType.Session -> sessionEndsAt
    RuleType.Bedtime -> schedule?.takeIf { !it.allDay }?.windowEndAt(now, zone)
    RuleType.Lock -> null
    RuleType.Limit -> limitEndsAt(zone)
}

/**
 * When the rule started holding its apps, for the shield's "You've stayed off for…": the running
 * session's start, the routine's current window's start (its day for a whole-day window), the
 * start of the day for a lock (or its Activate, if later). Null for limits: the moment their
 * budget ran out isn't kept.
 */
fun Rule.blockStart(now: Long, zone: ZoneId): Long? {
    val time = ZonedDateTime.ofInstant(Instant.ofEpochMilli(now), zone)
    val today = time.toLocalDate().atStartOfDay(zone).toInstant().toEpochMilli()
    return when (type) {
        RuleType.Session -> activeSince
        RuleType.Limit -> null
        RuleType.Lock -> activeSince?.let { maxOf(it, today) }
        RuleType.Bedtime -> {
            val window = schedule ?: return null
            if (window.allDay) return today
            val minute = time.hour * 60 + time.minute
            val startDate = if (window.overnight && minute < window.endMinute) time.toLocalDate().minusDays(1) else time.toLocalDate()
            startDate.atStartOfDay(zone).plusMinutes(window.startMinute.toLong()).toInstant().toEpochMilli()
        }
    }
}
