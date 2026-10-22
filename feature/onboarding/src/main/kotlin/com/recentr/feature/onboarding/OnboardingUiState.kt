package com.recentr.feature.onboarding

import androidx.annotation.StringRes
import androidx.compose.runtime.Immutable
import com.recentr.core.designsystem.icon.RecentrIcon
import com.recentr.core.designsystem.illustration.Sticker
import kotlin.math.roundToInt

/** Answers of "So, what's wrong with your phone time?" — [id] is what gets persisted. */
enum class PainPoint(val id: String, @param:StringRes val label: Int, val icon: RecentrIcon) {
    Sleep("sleep", R.string.onb_pain_sleep, RecentrIcon.Moon),
    Focus("focus", R.string.onb_pain_focus, RecentrIcon.Zap),
    Scroll("scroll", R.string.onb_pain_scroll, RecentrIcon.Refresh),
    Notifications("notifications", R.string.onb_pain_notifications, RecentrIcon.Bell),
    Hours("hours", R.string.onb_pain_hours, RecentrIcon.Clock),

    /** Exclusive answer: selecting it clears the others (and vice versa). */
    Fine("fine", R.string.onb_pain_fine, RecentrIcon.Sparkle),
    ;

    companion object {
        fun fromId(id: String): PainPoint? = entries.firstOrNull { it.id == id }
    }
}

/**
 * Answers of "How much time do you spend on your phone?" with the daily average used for the
 * numbers shown later (5 – 7 hours → 6h, the Figma example).
 */
enum class UsageBucket(val id: String, @param:StringRes val label: Int, val averageMinutes: Int) {
    Over7("over_7h", R.string.onb_usage_over_7, 8 * 60),
    From5To7("5_7h", R.string.onb_usage_5_7, 6 * 60),
    From3To5("3_5h", R.string.onb_usage_3_5, 4 * 60),
    From1To3("1_3h", R.string.onb_usage_1_3, 2 * 60),
    Under1("under_1h", R.string.onb_usage_under_1, 45),
    ;

    /** Suggested goal: ~72% of the average, rounded to the ruler's step (6h → 4h20). */
    val suggestedGoalMinutes: Int
        get() = ((averageMinutes * 0.72f / GOAL_STEP_MINUTES).roundToInt() * GOAL_STEP_MINUTES)
            .coerceIn(MIN_GOAL_MINUTES, MAX_GOAL_MINUTES)

    companion object {
        fun fromId(id: String?): UsageBucket? = entries.firstOrNull { it.id == id }
    }
}

const val GOAL_STEP_MINUTES = 10
const val MIN_GOAL_MINUTES = 30
const val MAX_GOAL_MINUTES = 12 * 60

/** How ambitious the goal is compared with the current average ("Bold target!" chip). */
enum class GoalTone(@param:StringRes val label: Int, val icon: RecentrIcon) {
    Bold(R.string.onb_goal_tone_bold, RecentrIcon.Flame),
    Balanced(R.string.onb_goal_tone_balanced, RecentrIcon.Leaf),
    Easy(R.string.onb_goal_tone_easy, RecentrIcon.Sun),
}

/** The first rule suggested on "You're all set" (Figma: Bedtime). */
enum class FirstRule(@param:StringRes val label: Int, val sticker: Sticker) {
    Bedtime(R.string.onb_rule_bedtime, Sticker.Moon),
    Focus(R.string.onb_rule_focus, Sticker.Bolt),
    Social(R.string.onb_rule_social, Sticker.Timer),
    Quiet(R.string.onb_rule_quiet, Sticker.Moon),
    DailyLimit(R.string.onb_rule_daily_limit, Sticker.Timer),
}

/** Everything the onboarding screens need, derived from the user's answers. */
@Immutable
data class OnboardingUiState(
    val name: String = "",
    val painPoints: Set<PainPoint> = emptySet(),
    val usageBucket: UsageBucket? = null,
    val dailyGoalMinutes: Int = UsageBucket.From5To7.suggestedGoalMinutes,
    /** True once the user moved the ruler: the goal no longer follows the usage answer. */
    val goalEdited: Boolean = false,
    val notificationsEnabled: Boolean = false,
) {
    val isNameValid: Boolean get() = name.isNotBlank()
    val trimmedName: String get() = name.trim()

    /** Daily average used for the projections (falls back to the Figma 6h when unanswered). */
    val averageMinutes: Int get() = (usageBucket ?: UsageBucket.From5To7).averageMinutes


    val goalTone: GoalTone
        get() {
            val ratio = dailyGoalMinutes.toFloat() / averageMinutes
            return when {
                ratio <= 0.75f -> GoalTone.Bold
                ratio < 1f -> GoalTone.Balanced
                else -> GoalTone.Easy
            }
        }

    /** Compare bars of the goal card share one scale (Figma: 6h fills ~79% of its track). */
    private val compareScaleMinutes: Float get() = maxOf(averageMinutes, dailyGoalMinutes) / 0.79f
    val goalBarFraction: Float get() = dailyGoalMinutes / compareScaleMinutes
    val averageBarFraction: Float get() = averageMinutes / compareScaleMinutes

    /** Share of the remaining ~64 waking-life years spent on the phone (6h → 16 years). */
    val yearsOnPhone: Int get() = (averageMinutes / MINUTES_PER_DAY * REMAINING_YEARS).roundToInt().coerceAtLeast(1)

    /** Days per year spent on the phone (6h → 91 days). */
    val daysPerYear: Int get() = (averageMinutes * 365 / MINUTES_PER_DAY).toInt()

    /** Daily time won back by sticking to the goal (6h − 4h20 = 1h40 → "2 hours"). */
    val winBackMinutes: Int get() = (averageMinutes - dailyGoalMinutes).coerceAtLeast(15)

    val firstRule: FirstRule
        get() = when {
            PainPoint.Sleep in painPoints -> FirstRule.Bedtime
            PainPoint.Focus in painPoints -> FirstRule.Focus
            PainPoint.Scroll in painPoints -> FirstRule.Social
            PainPoint.Notifications in painPoints -> FirstRule.Quiet
            PainPoint.Hours in painPoints -> FirstRule.DailyLimit
            else -> FirstRule.Bedtime
        }

    @get:StringRes
    val reason: Int
        get() = when {
            PainPoint.Scroll in painPoints -> R.string.onb_reason_scroll
            PainPoint.Sleep in painPoints -> R.string.onb_reason_sleep
            PainPoint.Focus in painPoints -> R.string.onb_reason_focus
            PainPoint.Notifications in painPoints -> R.string.onb_reason_notifications
            PainPoint.Hours in painPoints -> R.string.onb_reason_hours
            else -> R.string.onb_reason_fine
        }

    private companion object {
        const val MINUTES_PER_DAY = 24f * 60f
        const val REMAINING_YEARS = 64f
    }
}
