package com.recentr.app

/**
 * Where tapping a notification opens Recentr, carried as its intent's data, as on iOS: a rule's
 * reminder opens the rule's page, "recentr://rules/<id>". MainActivity reads it from the intent
 * that starts it, or from the new one when it's already open (singleTask), and RecentrNavHost
 * opens the page over the Rules tab.
 */
internal object NotificationLink {

    private const val RULES = "recentr://rules/"

    /** A rule's page. */
    fun rule(ruleId: String): String = RULES + ruleId

    /** The rule whose page [link] opens, if it's one of [rule]. */
    fun ruleId(link: String?): String? =
        link?.takeIf { it.startsWith(RULES) }?.removePrefix(RULES)?.takeIf { it.isNotEmpty() && '/' !in it }

    /**
     * The rule to open as MainActivity is created: only on the launch the tap made ([freshLaunch]).
     * A task brought back after its process died, or from Recents, is recreated with its first
     * intent, and would open the page again each time (as with [QaShortcuts]).
     */
    fun ruleToOpen(freshLaunch: Boolean, link: String?): String? = if (freshLaunch) ruleId(link) else null

    /**
     * Whether a tapped rule's link opens its page: not over the onboarding, and not again when that
     * page is the one showing ([ruleShown]). Another screen open over the tabs closes for it.
     */
    fun opensRulePage(onOnboarding: Boolean, ruleShown: String?, ruleId: String): Boolean =
        !onOnboarding && ruleShown != ruleId
}
