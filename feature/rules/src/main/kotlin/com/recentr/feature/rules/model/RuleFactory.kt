package com.recentr.feature.rules.model

import com.recentr.core.model.AppCategory
import com.recentr.core.model.Rule
import com.recentr.core.model.RuleSchedule
import com.recentr.core.model.RuleType
import com.recentr.core.model.UnlockMethod
import com.recentr.core.model.UsageApp
import com.recentr.feature.rules.R

/** The session lengths the editor allows ("Active for"), in minutes. */
internal const val SESSION_MIN_MINUTES = 5
internal const val SESSION_MAX_MINUTES = 12 * 60

/**
 * Builds new rules with the defaults of the Figma creation sheets — without apps: the user picks
 * them from the device — or from a built-in template ([fromTemplate], some apps preselected). A
 * lock is called "Lock" (or after its template), never after its apps, so the unlock screen reads
 * "Instagram · locked by Lock" rather than naming the app twice. None starts strict: Settings ›
 * Strict mode decides for new sessions and routines. [string] resolves string resources (with
 * format args).
 */
internal class RuleFactory(
    private val string: (Int, Array<out Any>) -> String,
    private val newId: () -> String,
) {
    private fun s(id: Int, vararg args: Any) = string(id, args)

    fun newRule(type: RuleType): Rule = when (type) {
        RuleType.Session -> Rule(
            id = newId(),
            type = RuleType.Session,
            name = s(R.string.rules_session_default_name),
            durationMinutes = 30,
            unlockMethod = UnlockMethod.Wait,
            waitSeconds = 10,
        )
        RuleType.Lock -> Rule(
            id = newId(),
            type = RuleType.Lock,
            name = s(R.string.rules_lock_default_name),
            unlockMethod = UnlockMethod.Wait,
            waitSeconds = 10,
            unlocksPerDay = 5,
            unlockDurationMinutes = 5,
        )
        RuleType.Limit -> Rule(
            id = newId(),
            type = RuleType.Limit,
            name = limitName(emptyList()),
            limitMinutes = 2 * 60 + 15,
        )
        RuleType.Bedtime -> Rule(
            id = newId(),
            type = RuleType.Bedtime,
            name = s(R.string.rules_bedtime_title),
            schedule = RuleSchedule(23 * 60, 7 * 60, (1..5).toSet()),
            unlockMethod = UnlockMethod.Wait,
            waitSeconds = 10,
        )
    }

    /**
     * A new rule of the [template]'s kind, with its title, sticker and settings
     * ([BuiltInTemplate.setup]), blocking its apps among the [installed] ones. Every other setting
     * keeps the kind's default.
     */
    fun fromTemplate(template: BuiltInTemplate, installed: List<UsageApp>): Rule {
        val base = newRule(template.type).copy(
            name = s(template.title),
            apps = template.apps.pick(installed),
            sticker = template.sticker,
        )
        return when (val setup = template.setup) {
            is TemplateSetup.Session -> base.copy(durationMinutes = setup.minutes)
            is TemplateSetup.Routine -> base.copy(schedule = setup.schedule)
            is TemplateSetup.Lock -> base.copy(
                unlockMethod = setup.unlockMethod,
                waitSeconds = setup.waitSeconds,
                unlocksPerDay = setup.unlocksPerDay,
                unlockDurationMinutes = setup.unlockMinutes,
            )
            is TemplateSetup.Limit -> base.copy(limitMinutes = setup.minutes)
        }
    }

    /** "Social time" when only social apps are limited, else "Limit". */
    fun limitName(apps: List<UsageApp>): String =
        if (apps.isNotEmpty() && apps.all { it.category == AppCategory.Social }) {
            s(R.string.rules_limit_social_name)
        } else {
            s(R.string.rules_limit_default_name)
        }
}
