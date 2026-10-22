package com.recentr.core.model

import java.time.ZoneId
import kotlinx.serialization.Serializable

/**
 * The apps the person always allows (Settings › Always allowed, docs/RULES.md › "Always
 * allowed"): no rule and no focus session puts them on hold, whatever its kind. Maps, a ride, a
 * health app.
 *
 * Allowing an app makes the rules that hold it easier: while one of them is engaged, the app
 * waits in [pending] until the last of those engagements ends, while the apps nothing engaged
 * holds join now ([choosing]). Removing an app applies now, even while others wait.
 */
@Serializable
data class AlwaysAllowed(
    /** The apps allowed now. */
    val apps: List<UsageApp> = emptyList(),
    /**
     * The list once the apps waiting to join have joined, kept for when the engagements they would
     * loosen are over. It holds every app of [apps].
     */
    val pending: AllowedChange? = null,
) {
    /** The apps allowed at [now]: the kept list, once it's due. */
    fun appsAt(now: Long): List<UsageApp> = pending?.takeIf { it.appliesAt <= now }?.apps ?: apps

    /** Whether [packageName] is always allowed at [now]. */
    fun allows(packageName: String, now: Long): Boolean = appsAt(now).any { it.packageName == packageName }

    /**
     * [apps] but for those allowed at [now]: what a rule picking them actually blocks (its page's
     * Blocked card, its Apps sheet). An app waiting to join is still blocked.
     */
    fun blocked(apps: List<UsageApp>, now: Long): List<UsageApp> {
        val allowed = appsAt(now).map { it.packageName }.toSet()
        return apps.filterNot { it.packageName in allowed }
    }

    /** The list as last chosen, the kept one included: what the picker opens on. */
    val latestWish: List<UsageApp>
        get() = pending?.apps ?: apps

    /** The kept list applied, once it's due at [now]. */
    fun settled(now: Long): AlwaysAllowed =
        if (pending != null && pending.appliesAt <= now) AlwaysAllowed(pending.apps) else this

    /** The apps waiting to join the list ([pending]), in the order chosen. */
    val waiting: List<UsageApp>
        get() {
            val allowed = apps.map { it.packageName }.toSet()
            return pending?.apps.orEmpty().filterNot { it.packageName in allowed }
        }

    /**
     * [new] chosen at [now], in place of the list in effect and of what waits. The apps it drops
     * go now. An app it adds makes easier the rules engaged now ([Rule.engagementEnd]) that hold
     * it, and the strict focus session holding it: while there's one, the app waits. The apps
     * that wait join together at the latest of those ends; the other apps added join now.
     */
    fun choosing(new: List<UsageApp>, rules: List<Rule>, focus: FocusRun?, now: Long, zone: ZoneId = ZoneId.systemDefault()): AlwaysAllowed {
        val chosen = new.distinctBy { it.packageName }
        val allowed = appsAt(now).map { it.packageName }.toSet()
        val added = chosen.map { it.packageName }.filterNot { it in allowed }.toSet()
        // When each engagement holding one of the apps added ends, with the apps it holds.
        val engagements = rules.mapNotNull { rule ->
            rule.engagementEnd(now, zone)?.let { end -> end to added.filter(rule::targets) }
        } + listOfNotNull(
            focus?.takeIf { it.isStrict }?.let { run -> run.projectedEnd(now) to run.config.blockedApps.map { it.packageName }.filter { it in added } },
        )
        val holding = engagements.filter { (_, held) -> held.isNotEmpty() }
        val appliesAt = holding.maxOfOrNull { (end, _) -> end } ?: return AlwaysAllowed(chosen)
        val held = holding.flatMap { (_, held) -> held }.toSet()
        return AlwaysAllowed(apps = chosen.filterNot { it.packageName in held }, pending = AllowedChange(appliesAt, chosen))
    }
}

/** A list of always allowed apps kept until [appliesAt] (epoch millis) ([AlwaysAllowed.choosing]). */
@Serializable
data class AllowedChange(val appliesAt: Long, val apps: List<UsageApp>)
