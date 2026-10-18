package com.recentr.core.model

import java.time.LocalDate

/**
 * Badges of the Achievements flow (Figma 105–110). [points] are the calm points the badge is
 * worth, [target] the progress it needs (days, nights, hours, urges… — see the feature strings).
 */
enum class Achievement(val points: Int, val target: Int) {
    /** A rule blocks apps. */
    FirstLock(50, 1),
    /** A rule carries a "why" note. */
    NoteToSelf(50, 1),
    /** A focus session is logged. */
    FirstFocus(50, 1),
    /** Hours of focus sessions. */
    FocusTenHours(150, 10),
    /** Days in a row under the goal (streak savers included). */
    SevenDayStreak(100, 7),
    /** Days in a row under the goal, without a streak saver. */
    GoalCrusher(150, 7),
    TwoCalmWeeks(200, 14),
    CalmMonth(400, 30),
    /** Nights in a row without screen time after 11 PM. */
    NightOwlRetired(150, 7),
    /** Mornings in a row without screen time between 5 and 8 AM. */
    SlowMornings(100, 5),
    /** A Sunday under 30 minutes. */
    ScreenFreeSunday(150, 1),
    /** A day under an hour. */
    GentleDay(100, 1),
    /** A day with fewer than 20 pickups. */
    HandsOff(100, 1),
    /** Urges resisted during focus sessions. */
    UrgesResisted(200, 100),
    /** Hours won back versus the habit reported in the onboarding. */
    TenHoursWonBack(100, 10),
    FiftyHoursWonBack(250, 50),
}

/** Where a badge stands: [progress] towards [Achievement.target], [earnedOn] once earned. */
data class AchievementProgress(
    val achievement: Achievement,
    val progress: Int,
    val earnedOn: LocalDate?,
) {
    val earned: Boolean get() = earnedOn != null
}

/**
 * Level from the calm points of the earned badges: level [level] spans [floor] until [ceiling]
 * (null at the top level).
 */
data class CalmLevel(
    val level: Int,
    val points: Int,
    val floor: Int,
    val ceiling: Int?,
) {
    companion object {
        /** Points needed to reach each level, level 1 first. */
        val THRESHOLDS = listOf(0, 100, 300, 600, 1_000, 1_500, 2_100)

        fun of(points: Int): CalmLevel {
            val index = THRESHOLDS.indexOfLast { points >= it }.coerceAtLeast(0)
            return CalmLevel(
                level = index + 1,
                points = points,
                floor = THRESHOLDS[index],
                ceiling = THRESHOLDS.getOrNull(index + 1),
            )
        }
    }
}
