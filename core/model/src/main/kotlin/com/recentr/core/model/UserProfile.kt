package com.recentr.core.model

/** What the user told us during onboarding. Owned by the onboarding flow. */
data class UserProfile(
    val name: String = "",
    val onboardingCompleted: Boolean = false,
    /** Daily screen-time goal, in minutes (4h20 by default, as in the design). */
    val dailyGoalMinutes: Int = 4 * 60 + 20,
    /** Self-reported current daily usage bucket id (onboarding question), if answered. */
    val reportedUsageBucket: String? = null,
    /** Ids of the "what's wrong with your phone time?" answers. */
    val painPoints: Set<String> = emptySet(),
    val notificationsEnabled: Boolean = false,
)
