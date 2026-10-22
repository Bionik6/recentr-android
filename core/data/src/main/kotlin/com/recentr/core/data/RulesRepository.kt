package com.recentr.core.data

import com.recentr.core.model.Rule
import com.recentr.core.model.RuleEdit
import com.recentr.core.model.RuleType
import com.recentr.core.model.applying
import com.recentr.core.model.deleted
import com.recentr.core.model.edit
import com.recentr.core.model.latestWish
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import java.io.File
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID

/**
 * Blocking rules (none until the user creates one). They are kept in memory
 * for the screens and the app blocker, and saved to [file] as JSON after every change, so they
 * survive the app being closed or the phone restarting.
 *
 * Creating a rule turns it on ([create]). Changes go through the edit policy ([save], [delete]):
 * while a rule is engaged, what loosens it
 * is kept on the rule ([Rule.pending]) and applied by [settle] once the engagement ends. The
 * unlocks used start over on a new local day; a session ends with its countdown, a limit at
 * midnight. Each rule's streak counts its windows as they end ([RuleStreaks]).
 */
class RulesRepository(
    /** Where the rules are saved, or null to keep them in memory only (previews, tests). */
    private val file: File? = null,
    private val zone: () -> ZoneId = ZoneId::systemDefault,
    now: Long = System.currentTimeMillis(),
    writeDispatcher: CoroutineDispatcher = Dispatchers.IO.limitedParallelism(1),
    private val clock: () -> Long = System::currentTimeMillis,
) {
    private val saved = file?.let { read(it, now) }

    private val _rules = MutableStateFlow(saved?.rules.orEmpty())
    val rules: StateFlow<List<Rule>> = _rules.asStateFlow()

    /** When the counters were last settled (epoch millis): their day is that of this moment. */
    @Volatile
    private var settledAt: Long = saved?.settledAt ?: saved?.countersDay?.let(::startOfDay) ?: now

    /** One write at a time, each with the latest state. */
    private val writes = CoroutineScope(SupervisorJob() + writeDispatcher)

    init {
        // Rules made before this version were migrated on reading: saved as they are now.
        if (saved != null && saved.version < VERSION) persist()
    }

    fun rule(id: String): Rule? = _rules.value.firstOrNull { it.id == id }

    /** Writes [rule] as it is, over the rule with its id or as a new rule (its streak counting from now). */
    fun upsert(rule: Rule) {
        _rules.update { list ->
            if (list.any { it.id == rule.id }) {
                list.map { if (it.id == rule.id) rule else it }
            } else {
                // A new rule's streak counts its windows from now on.
                list + if (rule.streakFrom == null) rule.copy(streakFrom = clock()) else rule
            }
        }
        persist()
    }

    /**
     * Saves [draft] over the rule with its id through the edit policy ([Rule.edit]): what loosens
     * an engaged rule is kept for when its engagement ends. A new rule is added as it is.
     */
    fun save(draft: Rule, now: Long = clock()): RuleEdit {
        var edit = RuleEdit(now = draft, later = null, blocksMore = false)
        _rules.update { list ->
            val current = list.firstOrNull { it.id == draft.id }
            if (current == null) {
                edit = RuleEdit(now = draft, later = null, blocksMore = false)
                list + if (draft.streakFrom == null) draft.copy(streakFrom = clock()) else draft
            } else {
                edit = current.edit(draft, now, zone())
                val saved = pauseStreak(current, edit.now, now)
                list.map { if (it.id == draft.id) saved else it }
            }
        }
        persist()
        return edit
    }

    /**
     * Creates [rule] and turns it on at [now] ("Hold to create"): a session starts, a lock is on,
     * a limit starts for today ([start]); a routine follows its schedule. Its streak counts from now.
     */
    fun create(rule: Rule, now: Long = clock()) {
        val created = rule.copy(streakFrom = rule.streakFrom ?: now, pending = null).started(now)
        _rules.update { list -> list.filterNot { it.id == rule.id } + created }
        persist()
    }

    /** Applies [transform] to the rule with [id], if it exists: runtime counters, not settings ([save]). */
    fun update(id: String, transform: (Rule) -> Rule) {
        _rules.update { list -> list.map { if (it.id == id) transform(it) else it } }
        persist()
    }

    /**
     * Deletes the rule with [id]: right away when it isn't engaged, otherwise once its engagement
     * ends ([Rule.deleted]). True when it was deleted now.
     */
    fun delete(id: String, now: Long = clock()): Boolean {
        var deleted = true
        _rules.update { list ->
            val kept = list.firstOrNull { it.id == id }?.deleted(now, zone())
            deleted = kept == null
            if (kept == null) list.filterNot { it.id == id } else list.map { if (it.id == id) kept else it }
        }
        persist()
        return deleted
    }

    /** Drops the change kept on the rule ("Drop changes", "Keep rule", "Keep it on"). */
    fun dropPending(id: String) = update(id) { it.copy(pending = null) }

    fun newId(): String = UUID.randomUUID().toString()

    /**
     * Pauses the routine until [untilEpochMillis] ([Long.MAX_VALUE] = until resumed), through the
     * edit policy: during a window, the pause starts when the window ends. A pause over one of its
     * windows breaks its streak.
     */
    fun pause(id: String, untilEpochMillis: Long, now: Long = clock()): RuleEdit? =
        rule(id)?.let { save(it.latestWish.copy(pausedUntil = untilEpochMillis), now) }

    fun resume(id: String, now: Long = clock()): RuleEdit? =
        rule(id)?.let { save(it.latestWish.copy(pausedUntil = null), now) }

    /**
     * Start (a session, for its length; a limit, for today) or Activate (a lock) at [now], again
     * from the rule's page. A pause in the way is lifted; a limit counts its budget from now. A
     * limit started today isn't started again (its budget would start over), nor a lock that is on.
     */
    fun start(id: String, now: Long = clock()) = update(id) { it.started(now) }

    /** [start]ed at [now]: a routine follows its schedule instead. */
    private fun Rule.started(now: Long): Rule = when (type) {
        RuleType.Session -> copy(activeSince = now, activeUntil = now + (durationMinutes ?: 0) * 60_000L, pausedUntil = null)
        RuleType.Lock -> copy(activeSince = activeSince ?: now, pausedUntil = null)
        RuleType.Limit -> if (isEnforcing(now, zone())) this else copy(activeSince = now, usedMinutes = 0, pausedUntil = null)
        RuleType.Bedtime -> this
    }

    /** The rules [ids] were broken at [now] (an emergency unlock past them): their streaks start over. */
    fun breakStreaks(ids: Collection<String>, now: Long) {
        if (ids.isEmpty()) return
        _rules.update { list -> list.map { if (it.id in ids) RuleStreaks.broken(it, now) else it } }
        persist()
    }

    /**
     * The minutes per package since each limit's Start ([AppHoldRepository.refreshUsage]) become
     * the limit's [Rule.usedMinutes], for its card and detail. A limit whose use wasn't read keeps
     * its minutes. Saved only when a limit's minutes changed.
     */
    fun recordUsage(usage: LimitUsage) {
        var changed = false
        _rules.update { list ->
            val updated = list.map { rule ->
                val used = if (rule.type == RuleType.Limit) usage.of(rule) else null
                if (used != null) rule.copy(usedMinutes = used) else rule
            }
            changed = updated != list
            updated
        }
        if (changed) persist()
    }

    /**
     * Streaks count the windows that ended first, while a limit's minutes are still those of its
     * day. Then the changes kept for when an engagement ends apply (a kept delete deletes the
     * rule), expired pauses are cleared, the unlocks used start over on a new day, sessions whose
     * time is up stop running (and stay saved, to be started again) and limits started before
     * midnight end (they don't start again on their own). Call periodically (the rules screens
     * tick every second, the app blocker every few seconds).
     */
    fun settle(now: Long) {
        val zone = zone()
        val since = settledAt
        val newDay = epochDay(now) != epochDay(since)
        // Rules saved before streaks were counted count from the start of today.
        val today = startOfDay(epochDay(now))
        val needsWork = newDay || _rules.value.any { rule ->
            (rule.pending?.let { it.appliesAt <= now } == true) ||
                (rule.sessionEndsAt?.let { it <= now } == true) ||
                (rule.limitEndsAt(zone)?.let { it <= now } == true) ||
                (rule.pausedUntil?.let { it <= now } == true) ||
                rule.streakFrom == null || RuleStreaks.due(rule, now, zone)
        }
        if (!needsWork) return
        settledAt = now
        _rules.update { list ->
            list.mapNotNull { rule ->
                var result = if (rule.streakFrom == null) rule.copy(streakFrom = today) else rule
                result = RuleStreaks.advance(result, now, zone)
                if (newDay) result = result.copy(unlocksUsedToday = 0)
                val change = result.pending?.takeIf { it.appliesAt <= now }
                if (change != null) {
                    val applied = result.applying(change) ?: return@mapNotNull null
                    result = pauseStreak(result, applied, change.appliesAt)
                }
                if (result.pausedUntil?.let { it <= now } == true) result = result.copy(pausedUntil = null)
                val end = result.sessionEndsAt
                if (end != null && end <= now) {
                    result = RuleStreaks.runEnded(result, end, zone).copy(activeSince = null, activeUntil = null)
                }
                if (result.limitEndsAt(zone)?.let { it <= now } == true) result = result.copy(activeSince = null, usedMinutes = 0)
                result
            }
        }
        persist()
    }

    /** Removes every rule ("Delete account"). */
    fun reset() {
        _rules.value = emptyList()
        persist()
    }

    /**
     * [after] replacing [before] at [at]: a new pause breaks the streak when it covers part of a
     * window, a resume counts again from [at].
     */
    private fun pauseStreak(before: Rule, after: Rule, at: Long): Rule {
        val was = before.pausedUntil?.takeIf { it > at }
        val until = after.pausedUntil?.takeIf { it > at }
        return when {
            until != null && until != was -> RuleStreaks.pausedOver(after, at, until, zone())
            until == null && was != null -> RuleStreaks.resumed(after, at)
            else -> after
        }
    }

    private fun epochDay(millis: Long): Long = Instant.ofEpochMilli(millis).atZone(zone()).toLocalDate().toEpochDay()

    private fun startOfDay(epochDay: Long): Long = LocalDate.ofEpochDay(epochDay).atStartOfDay(zone()).toInstant().toEpochMilli()

    private fun persist() {
        val target = file ?: return
        val at = settledAt
        writes.launch { write(target, SavedRules(_rules.value, countersDay = epochDay(at), settledAt = at, version = VERSION)) }
    }

    /**
     * Files written before templates went away still have a "templates" list: it's ignored. Older
     * files only have [countersDay]; it's still written for them. Files without a [version] were
     * written before the four kinds of rule settled ([migrated]).
     */
    @Serializable
    private data class SavedRules(
        val rules: List<Rule> = emptyList(),
        val countersDay: Long? = null,
        val settledAt: Long? = null,
        val version: Int = 0,
    )

    private companion object {
        /** The rules file's format: 1 since the four kinds of rule (docs/RULES.md › "Rules made before this"). */
        const val VERSION = 1

        val json = Json {
            ignoreUnknownKeys = true
            encodeDefaults = true
        }

        /**
         * The saved rules, or null when there are none yet (or the file can't be read). A file of
         * an older [VERSION] is [migrated] first: decoding it as it is would fail on a kind or an
         * unlock method that is gone, and lose every rule.
         */
        fun read(file: File, now: Long): SavedRules? = runCatching {
            if (!file.exists()) return@runCatching null
            val saved = json.parseToJsonElement(file.readText()).jsonObject
            val version = saved["version"]?.jsonPrimitive?.intOrNull ?: 0
            json.decodeFromJsonElement<SavedRules>(if (version < VERSION) migrated(saved, now) else saved)
        }.getOrNull()

        /**
         * Rules made before the four kinds settled, as they are now (version 0 → 1): habits are
         * gone; locks with hours and sessions with a weekly schedule become routines (keeping their
         * sticker); a lock without hours stays on from [now] (off when it was paused until turned
         * back on); a limit waits for Start; only routines pause; Strict mode is off on locks and
         * limits; "No unlocks" becomes the default (wait, five unlocks a day) and "Give a good
         * reason" waits too. Its version stays the file's, so the repository saves it again.
         */
        fun migrated(saved: JsonObject, now: Long): JsonObject {
            val rules = (saved["rules"] as? JsonArray)?.mapNotNull { (it as? JsonObject)?.let { rule -> migratedRule(rule, now) } }
                ?: return saved
            return JsonObject(saved + ("rules" to JsonArray(rules)))
        }

        private fun migratedRule(rule: JsonObject, now: Long): JsonObject? {
            val fields = rule.toMutableMap()
            listOf("limitReset", "limitAction", "habitTasks", "habitRepeat", "oneShot").forEach(fields::remove)
            val type = rule.string("type")
            val scheduled = rule["schedule"].let { it != null && it !is JsonNull }
            val newType = when {
                type == "Habit" -> return null
                (type == "Session" || type == "Lock") && scheduled -> "Bedtime"
                else -> type
            }
            if (newType != type) {
                fields["type"] = JsonPrimitive(newType)
                // Their sticker stays a bolt or a lock, not a routine's moon.
                if (rule["sticker"] == null) fields["sticker"] = JsonPrimitive(if (type == "Session") "Bolt" else "Lock")
            }
            val pausedUntil = rule["pausedUntil"]?.let { it as? JsonPrimitive }?.longOrNull
            when (newType) {
                "Lock" -> fields["activeSince"] = if (pausedUntil == Long.MAX_VALUE) JsonNull else JsonPrimitive(now)
                "Limit" -> {
                    fields["activeSince"] = JsonNull
                    fields["usedMinutes"] = JsonPrimitive(0)
                }
                "Bedtime" -> {
                    fields.remove("activeSince")
                    fields.remove("activeUntil")
                }
            }
            if (newType != "Bedtime") fields.remove("pausedUntil")
            if (newType == "Lock" || newType == "Limit") fields["strictMode"] = JsonPrimitive(false)
            when (rule.string("unlockMethod")) {
                // "No unlocks" becomes the default: wait, five unlocks a day.
                "None", "Strict" -> {
                    fields["unlockMethod"] = JsonPrimitive("Wait")
                    fields["unlocksPerDay"] = JsonPrimitive(5)
                }
                "Reason" -> fields["unlockMethod"] = JsonPrimitive("Wait")
            }
            return JsonObject(fields)
        }

        private fun JsonObject.string(key: String): String? = (get(key) as? JsonPrimitive)?.contentOrNull

        fun write(file: File, saved: SavedRules) = file.writeAtomically(json.encodeToString(saved))
    }
}
