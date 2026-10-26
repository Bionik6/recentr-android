package com.recentr.core.model

/**
 * A routine onboarding sets up (docs/RULES.md › Starter routines), in the order Rules lists them,
 * blocking the apps the user picks on its apps step. None is strict, and each lets an app through
 * after a 10-second wait, so its apps can be changed at any time. Their names are the onboarding's
 * strings.
 */
enum class StarterRoutine(val schedule: RuleSchedule, val sticker: RuleSticker) {
    /** Weekdays, 9 AM to 5 PM. */
    WorkingTime(RuleSchedule(9 * 60, 17 * 60, (1..5).toSet()), RuleSticker.Bolt),
    /** Every day, 11 PM to 7 AM. */
    NightPause(RuleSchedule(23 * 60, 7 * 60), RuleSticker.Moon),
    /** Weekends, 9 AM to noon. */
    WeekendZen(RuleSchedule(9 * 60, 12 * 60, setOf(6, 7)), RuleSticker.Leaf),
    ;

    /** The routine called [name], blocking [apps]. */
    fun rule(id: String, name: String, apps: List<UsageApp>): Rule = Rule(
        id = id,
        type = RuleType.Bedtime,
        name = name,
        apps = apps,
        schedule = schedule,
        strictMode = false,
        unlockMethod = UnlockMethod.Wait,
        waitSeconds = 10,
        sticker = sticker,
    )
}
