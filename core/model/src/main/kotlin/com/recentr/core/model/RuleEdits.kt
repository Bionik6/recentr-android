package com.recentr.core.model

import java.time.ZoneId

// Changing a rule while it's engaged (docs/RULES.md › "Changing a rule while it's engaged"). Every
// change goes through [Rule.edit] — the editor, the rule page's rows, pause, turn off — and a
// delete through [Rule.deleted], so the iOS and Android apps classify them the same way.

/** What saving a draft over a rule does ([Rule.edit]). */
data class RuleEdit(
    /**
     * The rule from now on: the parts of the draft that apply right away, and the rest kept in
     * [Rule.pending] for when the engagement ends.
     */
    val now: Rule,
    /** The whole draft, kept for when the engagement ends; null when all of it applies now. */
    val later: Rule?,
    /**
     * The rule holds apps now and the change blocks more of them right away (an app added, a
     * budget lowered to what's already used): ask first.
     */
    val blocksMore: Boolean,
)

/**
 * [draft] saved over this rule at [now]. When the rule isn't engaged ([Rule.isEnforcing]), all of
 * it applies now and a kept change is dropped. While it's engaged, each setting is taken from the
 * draft unless it loosens the rule; if one does, the whole draft is kept for the end of the
 * engagement ([Rule.engagementEnd]) and the rule keeps its current value for those settings:
 *
 * - name, sticker, note and reminder: always now;
 * - apps: now when the new choice keeps every app already blocked, otherwise the whole choice waits;
 * - a routine's days and hours: now when they cover every minute the old ones did;
 * - a session's length: always now; the running session never gets shorter, a longer length
 *   makes it last longer;
 * - a limit's budget: smaller or equal now, bigger later;
 * - the unlock method (with its wait): a harder method, or the same with a longer wait, now;
 * - unlocks per day: fewer now (unlimited is the most); each unlock's length: shorter or equal now;
 * - Strict mode: on now, off later;
 * - a routine's pause: resuming now, pausing later;
 * - a lock turned off (no [Rule.activeSince]): later.
 *
 * What the rule has used and counted (minutes used, unlocks used today, its streak, Start) always
 * stays the rule's. A kept delete stays kept: only dropping it keeps the rule.
 *
 * With Settings › Change locks & limits anytime on ([changesAnytime]), a lock or a limit is never
 * engaged for this: every change applies now ([changesWaitUntil]). Asking first when a change
 * blocks more apps right away still holds: blocking itself doesn't change.
 */
fun Rule.edit(draft: Rule, now: Long, zone: ZoneId = ZoneId.systemDefault(), changesAnytime: Boolean = false): RuleEdit {
    val end = changesWaitUntil(now, zone, changesAnytime)
    if (end == null) {
        val applied = settingsOf(draft)
        return RuleEdit(now = applied, later = null, blocksMore = blocksMore(applied, now, zone))
    }
    var loosens = false
    /** [new] when it is [old] or [tighter], else [old], the draft kept for later. */
    fun <T> pick(old: T, new: T, tighter: Boolean): T = when {
        new == old || tighter -> new
        else -> {
            loosens = true
            old
        }
    }

    val keptApps = draft.apps.map { it.packageName }.containsAll(apps.map { it.packageName })
    val newSchedule = if (type == RuleType.Bedtime) {
        pick(schedule, draft.schedule, draft.schedule?.covers(schedule) == true)
    } else {
        draft.schedule
    }
    val harderUnlock = draft.unlockMethod > unlockMethod ||
        draft.unlockMethod == unlockMethod && (unlockMethod != UnlockMethod.Wait || draft.waitSeconds >= waitSeconds)
    val unlock = pick(unlockMethod to waitSeconds, draft.unlockMethod to draft.waitSeconds, harderUnlock)
    val running = sessionEndsAt?.takeIf { isRunning(now) }
    val pause = pausedUntil?.takeIf { it > now }
    val draftPause = draft.pausedUntil?.takeIf { it > now }
    // An engaged lock is on: a draft without Activate turns it off. Start and Activate aren't edits.
    if (type == RuleType.Lock && draft.activeSince == null) loosens = true
    val applied = copy(
        name = draft.name,
        sticker = draft.sticker,
        note = draft.note,
        reminderMinutesBefore = draft.reminderMinutesBefore,
        apps = pick(apps, draft.apps, keptApps),
        schedule = newSchedule,
        durationMinutes = draft.durationMinutes,
        activeUntil = running?.let { maxOf(it, (activeSince ?: now) + (draft.durationMinutes ?: 0) * 60_000L) } ?: activeUntil,
        limitMinutes = pick(limitMinutes, draft.limitMinutes, (draft.limitMinutes ?: Int.MAX_VALUE) <= (limitMinutes ?: Int.MAX_VALUE)),
        unlockMethod = unlock.first,
        waitSeconds = unlock.second,
        unlocksPerDay = pick(unlocksPerDay, draft.unlocksPerDay, (draft.unlocksPerDay ?: Int.MAX_VALUE) <= (unlocksPerDay ?: Int.MAX_VALUE)),
        unlockDurationMinutes = pick(unlockDurationMinutes, draft.unlockDurationMinutes, draft.unlockDurationMinutes <= unlockDurationMinutes),
        strictMode = pick(strictMode, draft.strictMode, draft.strictMode),
        pausedUntil = pick(pause, draftPause, (draftPause ?: Long.MIN_VALUE) <= (pause ?: Long.MIN_VALUE)),
    )
    val later = draft.copy(pending = null).takeIf { loosens }
    val keptDelete = pending?.takeIf { it.rule == null }
    val result = applied.copy(pending = keptDelete ?: later?.let { RuleChange(end, it) })
    return RuleEdit(now = result, later = later, blocksMore = blocksMore(result, now, zone))
}

/**
 * When changes that loosen the rule apply ([edit], [deleted]): the end of its engagement, or null
 * when they apply now — the rule isn't engaged, or it's a lock or a limit and Settings › Change
 * locks & limits anytime is on ([changesAnytime]).
 */
fun Rule.changesWaitUntil(now: Long, zone: ZoneId = ZoneId.systemDefault(), changesAnytime: Boolean = false): Long? =
    if (changesAnytime && (type == RuleType.Lock || type == RuleType.Limit)) null else engagementEnd(now, zone)

/**
 * Whether [after] blocks more apps right away than this rule: it holds apps now and [after] adds
 * one, or it's a started limit whose budget drops to what's already used.
 */
private fun Rule.blocksMore(after: Rule, now: Long, zone: ZoneId): Boolean {
    val addsApps = (after.apps.map { it.packageName } - apps.map { it.packageName }.toSet()).isNotEmpty()
    val spendsBudget = type == RuleType.Limit && isEnforcing(now, zone) &&
        usedMinutes >= (after.limitMinutes ?: Int.MAX_VALUE) && usedMinutes < (limitMinutes ?: Int.MAX_VALUE)
    return (isHolding(now, zone) && addsApps) || spendsBudget
}

/**
 * Deleting the rule at [now]: null when it goes right away (it isn't engaged, or [changesAnytime]
 * for a lock or a limit), otherwise the rule with the delete kept for the end of its engagement
 * ("Deleted at midnight").
 */
fun Rule.deleted(now: Long, zone: ZoneId = ZoneId.systemDefault(), changesAnytime: Boolean = false): Rule? =
    changesWaitUntil(now, zone, changesAnytime)?.let { copy(pending = RuleChange(it, rule = null)) }

/**
 * The rule as it was last wished: the change kept for later, if any (what the editor opens on and
 * further changes build on), else the rule itself.
 */
val Rule.latestWish: Rule
    get() = pending?.rule ?: this

/** [change] applied once its engagement ended: its settings on this rule, or null when it deletes the rule. */
fun Rule.applying(change: RuleChange): Rule? = change.rule?.let(::settingsOf)

/**
 * [draft]'s settings on this rule: what the rule has used and counted stays its own, and it stays
 * started (a lock is turned off by a draft without [Rule.activeSince]). Nothing is kept for later.
 */
private fun Rule.settingsOf(draft: Rule): Rule {
    val turnedOff = type == RuleType.Lock && draft.activeSince == null
    return draft.copy(
        id = id,
        type = type,
        usedMinutes = usedMinutes,
        unlocksUsedToday = unlocksUsedToday,
        streakDays = streakDays,
        streakFrom = streakFrom,
        activeSince = if (turnedOff) null else activeSince,
        activeUntil = if (turnedOff) null else activeUntil,
        pending = null,
    )
}
