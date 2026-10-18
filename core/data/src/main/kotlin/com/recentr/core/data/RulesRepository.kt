package com.recentr.core.data

import com.recentr.core.model.AppId
import com.recentr.core.model.LimitAction
import com.recentr.core.model.LimitReset
import com.recentr.core.model.Rule
import com.recentr.core.model.RuleSchedule
import com.recentr.core.model.RuleType
import com.recentr.core.model.UnlockMethod
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.util.UUID

/**
 * Blocking rules. In-memory for now (seeded with the rules shown in the design); the API is
 * the contract the Rules and Home flows rely on.
 */
class RulesRepository {

    private val _rules = MutableStateFlow(seed())
    val rules: StateFlow<List<Rule>> = _rules.asStateFlow()

    /** Rule prototypes saved with "Save as template" (ids are template ids). */
    private val _savedTemplates = MutableStateFlow<List<Rule>>(emptyList())
    val savedTemplates: StateFlow<List<Rule>> = _savedTemplates.asStateFlow()

    fun rule(id: String): Rule? = _rules.value.firstOrNull { it.id == id }

    fun upsert(rule: Rule) {
        _rules.update { list ->
            if (list.any { it.id == rule.id }) list.map { if (it.id == rule.id) rule else it } else list + rule
        }
    }

    /** Applies [transform] to the rule with [id], if it exists. */
    fun update(id: String, transform: (Rule) -> Rule) {
        _rules.update { list -> list.map { if (it.id == id) transform(it) else it } }
    }

    fun delete(id: String) {
        _rules.update { list -> list.filterNot { it.id == id } }
    }

    fun newId(): String = UUID.randomUUID().toString()

    /** Pauses the rule until [untilEpochMillis] ([Long.MAX_VALUE] = until resumed). */
    fun pause(id: String, untilEpochMillis: Long) = update(id) { it.copy(pausedUntil = untilEpochMillis) }

    fun resume(id: String) = update(id) { it.copy(pausedUntil = null) }

    /** Stores [rule] as a reusable template and returns the template id. */
    fun saveTemplate(rule: Rule): String {
        val id = "saved-" + newId()
        _savedTemplates.update { it + rule.copy(id = id, activeSince = null, pausedUntil = null) }
        return id
    }

    fun savedTemplate(id: String): Rule? = _savedTemplates.value.firstOrNull { it.id == id }

    /**
     * Ends sessions whose time is up: one-shot sessions are removed, others just stop running.
     * Expired pauses are cleared. Call periodically (the rules list ticks every second).
     */
    fun settle(now: Long) {
        val current = _rules.value
        val needsWork = current.any { rule ->
            (rule.sessionEndsAt?.let { it <= now } == true) || (rule.pausedUntil?.let { it <= now } == true)
        }
        if (!needsWork) return
        _rules.update { list ->
            list.mapNotNull { rule ->
                var result = rule
                if (result.pausedUntil?.let { it <= now } == true) result = result.copy(pausedUntil = null)
                val end = result.sessionEndsAt
                if (end != null && end <= now) {
                    result = if (result.oneShot) return@mapNotNull null else result.copy(activeSince = null)
                }
                result
            }
        }
    }

    private fun seed(): List<Rule> = listOf(
        Rule(
            id = "instagram-x",
            type = RuleType.Lock,
            name = "Instagram & X",
            apps = listOf(AppId.Instagram, AppId.X),
            unlockMethod = UnlockMethod.Wait,
            waitSeconds = 5,
            unlocksPerDay = 5,
            unlocksUsedToday = 2,
            unlockDurationMinutes = 5,
            note = "Doomscrolling before bed. I want my nights back.",
        ),
        Rule(
            id = "work-hours",
            type = RuleType.Session,
            name = "Work Hours",
            apps = listOf(AppId.X, AppId.Instagram, AppId.TikTok),
            schedule = RuleSchedule(startMinute = 9 * 60, endMinute = 17 * 60, days = (1..5).toSet()),
            unlockMethod = UnlockMethod.Wait,
            waitSeconds = 5,
            reminderMinutesBefore = 10,
            note = "Deep work beats busy work.",
            streakDays = 3,
        ),
        Rule(
            id = "social-time",
            type = RuleType.Limit,
            name = "Social time",
            apps = listOf(AppId.Instagram, AppId.X, AppId.TikTok),
            limitMinutes = 2 * 60 + 15,
            usedMinutes = 10,
            limitReset = LimitReset.Midnight,
            limitAction = LimitAction.Lock,
            streakDays = 3,
        ),
        Rule(
            id = "bedtime",
            type = RuleType.Bedtime,
            name = "Bedtime",
            apps = listOf(AppId.Instagram, AppId.X, AppId.TikTok),
            schedule = RuleSchedule(startMinute = 23 * 60, endMinute = 7 * 60),
            reminderMinutesBefore = 10,
            note = "Sleep first, scroll never.",
            streakDays = 3,
        ),
    )
}
