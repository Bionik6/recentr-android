package com.recentr.core.data

import com.recentr.core.model.Rule
import com.recentr.core.model.RuleType
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
import java.io.File
import java.time.Instant
import java.time.ZoneId
import java.util.UUID

/**
 * Blocking rules (none until the user creates one). They are kept in memory
 * for the screens and the app blocker, and saved to [file] as JSON after every change, so they
 * survive the app being closed or the phone restarting.
 *
 * The daily counters (unlocks used, limit minutes, habit tasks done) start over on a new local day.
 */
class RulesRepository(
    /** Where the rules are saved, or null to keep them in memory only (previews, tests). */
    private val file: File? = null,
    private val zone: () -> ZoneId = ZoneId::systemDefault,
    now: Long = System.currentTimeMillis(),
    writeDispatcher: CoroutineDispatcher = Dispatchers.IO.limitedParallelism(1),
) {
    private val saved = file?.let(::read)

    private val _rules = MutableStateFlow(saved?.rules.orEmpty())
    val rules: StateFlow<List<Rule>> = _rules.asStateFlow()

    /** Local day (epoch day) the daily counters belong to. */
    @Volatile
    private var countersDay: Long = saved?.countersDay ?: epochDay(now)

    /** One write at a time, each with the latest state. */
    private val writes = CoroutineScope(SupervisorJob() + writeDispatcher)

    fun rule(id: String): Rule? = _rules.value.firstOrNull { it.id == id }

    fun upsert(rule: Rule) {
        _rules.update { list ->
            if (list.any { it.id == rule.id }) list.map { if (it.id == rule.id) rule else it } else list + rule
        }
        persist()
    }

    /** Applies [transform] to the rule with [id], if it exists. */
    fun update(id: String, transform: (Rule) -> Rule) {
        _rules.update { list -> list.map { if (it.id == id) transform(it) else it } }
        persist()
    }

    fun delete(id: String) {
        _rules.update { list -> list.filterNot { it.id == id } }
        persist()
    }

    fun newId(): String = UUID.randomUUID().toString()

    /** Pauses the rule until [untilEpochMillis] ([Long.MAX_VALUE] = until resumed). */
    fun pause(id: String, untilEpochMillis: Long) = update(id) { it.copy(pausedUntil = untilEpochMillis) }

    fun resume(id: String) = update(id) { it.copy(pausedUntil = null) }

    /**
     * Today's minutes per package ([AppHoldRepository.refreshUsage]) become each daily limit's
     * [Rule.usedMinutes], for its card and detail. Saved only when a limit's minutes changed.
     */
    fun recordUsage(usedToday: Map<String, Int>) {
        var changed = false
        _rules.update { list ->
            val updated = list.map { rule -> if (rule.type == RuleType.Limit) rule.copy(usedMinutes = rule.usedOf(usedToday)) else rule }
            changed = updated != list
            updated
        }
        if (changed) persist()
    }

    /**
     * Ends sessions whose time is up: one-shot sessions are removed, others just stop running.
     * Expired pauses are cleared, and the daily counters start over on a new day. Call
     * periodically (the rules list ticks every second, the app blocker every few seconds).
     */
    fun settle(now: Long) {
        val day = epochDay(now)
        val newDay = day != countersDay
        val needsWork = newDay || _rules.value.any { rule ->
            (rule.sessionEndsAt?.let { it <= now } == true) || (rule.pausedUntil?.let { it <= now } == true)
        }
        if (!needsWork) return
        countersDay = day
        _rules.update { list ->
            list.mapNotNull { rule ->
                var result = if (newDay) rule.withCountersReset() else rule
                if (result.pausedUntil?.let { it <= now } == true) result = result.copy(pausedUntil = null)
                val end = result.sessionEndsAt
                if (end != null && end <= now) {
                    result = if (result.oneShot) return@mapNotNull null else result.copy(activeSince = null)
                }
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

    private fun Rule.withCountersReset(): Rule = copy(
        unlocksUsedToday = 0,
        usedMinutes = 0,
        habitTasks = habitTasks.map { it.copy(done = false) },
    )

    private fun epochDay(millis: Long): Long = Instant.ofEpochMilli(millis).atZone(zone()).toLocalDate().toEpochDay()

    private fun persist() {
        val target = file ?: return
        writes.launch { write(target, SavedRules(_rules.value, countersDay)) }
    }

    /** Files written before templates went away still have a "templates" list: it's ignored. */
    @Serializable
    private data class SavedRules(
        val rules: List<Rule> = emptyList(),
        val countersDay: Long? = null,
    )

    private companion object {
        val json = Json {
            ignoreUnknownKeys = true
            encodeDefaults = true
        }

        /** The saved rules, or null when there are none yet (or the file can't be read). */
        fun read(file: File): SavedRules? = runCatching {
            if (file.exists()) json.decodeFromString<SavedRules>(file.readText()) else null
        }.getOrNull()

        /** Writes next to [file] first, then renames: a crash mid-write never leaves half a file. */
        fun write(file: File, saved: SavedRules) {
            file.parentFile?.mkdirs()
            val temp = File(file.parentFile, file.name + ".tmp")
            temp.writeText(json.encodeToString(saved))
            if (!temp.renameTo(file)) {
                file.delete()
                temp.renameTo(file)
            }
        }
    }
}
