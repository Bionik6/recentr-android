package com.recentr.feature.rules.model

import com.recentr.core.model.AppCategory
import com.recentr.core.model.HabitTask
import com.recentr.core.model.Rule
import com.recentr.core.model.RuleSchedule
import com.recentr.core.model.RuleType
import com.recentr.core.model.UnlockMethod
import com.recentr.core.model.UsageApp
import com.recentr.feature.rules.R
import com.recentr.feature.rules.ui.appsName

/**
 * Builds new rules with the defaults of the Figma creation sheets — without apps: the user picks
 * them from the device — and the names derived from their content. [string] resolves string
 * resources (with format args).
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
            name = lockName(emptyList()),
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
            strictMode = true,
            // Once Strict mode is turned off.
            unlockMethod = UnlockMethod.Wait,
            waitSeconds = 10,
        )
        RuleType.Habit -> Rule(
            id = newId(),
            type = RuleType.Habit,
            name = habitName(emptyList()),
            unlockMethod = UnlockMethod.None,
            habitTasks = listOf(
                HabitTask(newId(), s(R.string.rules_task_read)),
                HabitTask(newId(), s(R.string.rules_task_water)),
                HabitTask(newId(), s(R.string.rules_task_stretch)),
            ),
        )
    }

    /** A Routine with the template's schedule and sticker, blocking the template's [installed] apps. */
    fun fromTemplate(template: BuiltInTemplate, installed: List<UsageApp>): Rule {
        val base = newRule(template.type)
        val proto = template.prototype(base.id, s(template.title), installed)
        return base.copy(
            name = proto.name,
            apps = proto.apps,
            schedule = proto.schedule,
            sticker = proto.sticker,
        )
    }

    /** "Instagram & X", "Instagram & 2 more". */
    fun lockName(apps: List<UsageApp>): String = appsName(
        apps,
        two = { a, b -> s(R.string.rules_name_two, a, b) },
        more = { a, n -> s(R.string.rules_name_more, a, n) },
    ) ?: s(R.string.rules_lock_default_name)

    /** "Social time" when only social apps are limited, else "Daily limit". */
    fun limitName(apps: List<UsageApp>): String =
        if (apps.isNotEmpty() && apps.all { it.category == AppCategory.Social }) {
            s(R.string.rules_limit_social_name)
        } else {
            s(R.string.rules_limit_default_name)
        }

    /** "Before X opens". */
    fun habitName(apps: List<UsageApp>): String =
        apps.firstOrNull()?.let { s(R.string.rules_habit_before, it.label) } ?: s(R.string.rules_habit_default_name)
}
