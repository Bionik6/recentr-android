package com.recentr.feature.rules.model

import com.recentr.core.model.AppId
import com.recentr.core.model.HabitTask
import com.recentr.core.model.Rule
import com.recentr.core.model.RuleSchedule
import com.recentr.core.model.RuleType
import com.recentr.core.model.UnlockMethod
import com.recentr.feature.rules.R
import com.recentr.feature.rules.ui.AppCategory
import com.recentr.feature.rules.ui.appsName
import com.recentr.feature.rules.ui.category

/**
 * Builds new rules with the defaults shown in the Figma creation sheets, and the names derived
 * from their content. [string] resolves string resources (with format args).
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
            apps = listOf(AppId.Instagram, AppId.X, AppId.TikTok),
            durationMinutes = 30,
            unlockMethod = UnlockMethod.None,
        )
        RuleType.Lock -> {
            val apps = listOf(AppId.Instagram, AppId.X)
            Rule(
                id = newId(),
                type = RuleType.Lock,
                name = lockName(apps),
                apps = apps,
                unlockMethod = UnlockMethod.Wait,
                waitSeconds = 5,
                unlocksPerDay = 5,
                unlockDurationMinutes = 5,
                note = s(R.string.rules_lock_default_note),
            )
        }
        RuleType.Limit -> {
            val apps = listOf(AppId.Instagram, AppId.X, AppId.TikTok)
            Rule(
                id = newId(),
                type = RuleType.Limit,
                name = limitName(apps),
                apps = apps,
                limitMinutes = 2 * 60 + 15,
            )
        }
        RuleType.Bedtime -> Rule(
            id = newId(),
            type = RuleType.Bedtime,
            name = s(R.string.rules_bedtime_title),
            schedule = RuleSchedule(23 * 60, 7 * 60, (1..5).toSet()),
            blockAllApps = true,
            allowedApps = listOf(AppId.Gmail),
            strictMode = true,
        )
        RuleType.Habit -> {
            val apps = listOf(AppId.X, AppId.TikTok)
            Rule(
                id = newId(),
                type = RuleType.Habit,
                name = habitName(apps),
                apps = apps,
                unlockMethod = UnlockMethod.None,
                habitTasks = listOf(
                    HabitTask(newId(), s(R.string.rules_task_read), done = true),
                    HabitTask(newId(), s(R.string.rules_task_water)),
                    HabitTask(newId(), s(R.string.rules_task_stretch)),
                ),
            )
        }
    }

    fun fromTemplate(template: BuiltInTemplate): Rule {
        val base = newRule(template.type)
        val proto = template.prototype(base.id, s(template.title))
        return base.copy(
            name = proto.name,
            apps = proto.apps.ifEmpty { base.apps },
            schedule = proto.schedule,
            durationMinutes = proto.durationMinutes ?: base.durationMinutes,
            sticker = proto.sticker,
            blockAllApps = proto.blockAllApps,
            allowedApps = proto.allowedApps,
            unlocksPerDay = if (template.type == RuleType.Lock) proto.unlocksPerDay else base.unlocksPerDay,
            note = if (template.type == RuleType.Lock) null else base.note,
            strictMode = base.strictMode,
        )
    }

    fun fromSavedTemplate(saved: Rule): Rule = saved.copy(id = newId(), activeSince = null, pausedUntil = null, streakDays = 0)

    /** "Instagram & X", "Instagram & 2 more". */
    fun lockName(apps: List<AppId>): String = appsName(
        apps,
        two = { a, b -> s(R.string.rules_name_two, a, b) },
        more = { a, n -> s(R.string.rules_name_more, a, n) },
    ) ?: s(R.string.rules_lock_default_name)

    /** "Social time" when only social apps are limited, else "Daily limit". */
    fun limitName(apps: List<AppId>): String =
        if (apps.isNotEmpty() && apps.all { it.category == AppCategory.Social }) {
            s(R.string.rules_limit_social_name)
        } else {
            s(R.string.rules_limit_default_name)
        }

    /** "Before X opens". */
    fun habitName(apps: List<AppId>): String =
        apps.firstOrNull()?.let { s(R.string.rules_habit_before, it.displayName) } ?: s(R.string.rules_habit_default_name)
}
