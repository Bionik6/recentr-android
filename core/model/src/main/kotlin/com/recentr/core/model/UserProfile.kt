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
    /** Contact email entered in "Edit profile" (no account backend yet). */
    val email: String = "",
    /** Avatar gradient (Profile › Edit profile cycles through them). */
    val avatarColor: AvatarColor = AvatarColor.Sunset,
    /** When the user first finished the onboarding ("Recentr-ing for 12 days"), or null. */
    val joinedAtMillis: Long? = null,
) {
    /** Daily average of the onboarding answer, in minutes (5 – 7 hours → 6h when unanswered). */
    val reportedDailyMinutes: Int
        get() = REPORTED_MINUTES[reportedUsageBucket] ?: 6 * 60

    /** First letter of the name, upper-cased, for the avatar ("M"). */
    val initial: String
        get() = name.trim().firstOrNull()?.uppercase() ?: "?"

    private companion object {
        /** Mirrors the onboarding `UsageBucket.averageMinutes`. */
        val REPORTED_MINUTES = mapOf(
            "over_7h" to 8 * 60,
            "5_7h" to 6 * 60,
            "3_5h" to 4 * 60,
            "1_3h" to 2 * 60,
            "under_1h" to 45,
        )
    }
}

/** Figma "Avatar" colours (+ the warm Sunset gradient of the Profile screen). */
enum class AvatarColor { Sunset, Sky, Mint, Pink, Tangerine, Night }
