package com.recentr.feature.insights.ui

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import com.recentr.core.designsystem.illustration.Sticker
import com.recentr.core.model.Achievement
import com.recentr.core.model.AchievementProgress
import com.recentr.core.model.AppId
import com.recentr.core.model.CalmLevel
import com.recentr.feature.insights.R

/** What a badge's progress counts. */
internal enum class BadgeUnit { Once, Days, Nights, Mornings, Hours, Urges }

/** Copy and artwork of a badge: title, requirement, celebration and milestone line. */
internal data class BadgeCopy(
    val sticker: Sticker,
    @param:StringRes val title: Int,
    @param:StringRes val goal: Int,
    @param:StringRes val cheer: Int,
    @param:StringRes val milestone: Int,
    val unit: BadgeUnit,
)

internal val Achievement.copy: BadgeCopy
    get() = when (this) {
        Achievement.FirstLock -> BadgeCopy(
            Sticker.Lock, R.string.badge_first_lock, R.string.badge_first_lock_goal,
            R.string.badge_first_lock_cheer, R.string.badge_first_lock_milestone_any, BadgeUnit.Once,
        )
        Achievement.NoteToSelf -> BadgeCopy(
            Sticker.Note, R.string.badge_note_to_self, R.string.badge_note_to_self_goal,
            R.string.badge_note_to_self_cheer, R.string.badge_note_to_self_milestone, BadgeUnit.Once,
        )
        Achievement.FirstFocus -> BadgeCopy(
            Sticker.Bolt, R.string.badge_first_focus, R.string.badge_first_focus_goal,
            R.string.badge_first_focus_cheer, R.string.badge_first_focus_milestone, BadgeUnit.Once,
        )
        Achievement.FocusTenHours -> BadgeCopy(
            Sticker.Headphones, R.string.badge_focus_ten_hours, R.string.badge_focus_ten_hours_goal,
            R.string.badge_focus_ten_hours_cheer, R.string.badge_focus_ten_hours_milestone, BadgeUnit.Hours,
        )
        Achievement.SevenDayStreak -> BadgeCopy(
            Sticker.Flame, R.string.badge_seven_day_streak, R.string.badge_seven_day_streak_goal,
            R.string.badge_seven_day_streak_cheer, R.string.badge_seven_day_streak_milestone, BadgeUnit.Days,
        )
        Achievement.GoalCrusher -> BadgeCopy(
            Sticker.Target, R.string.badge_goal_crusher, R.string.badge_goal_crusher_goal,
            R.string.badge_goal_crusher_cheer, R.string.badge_goal_crusher_milestone, BadgeUnit.Days,
        )
        Achievement.TwoCalmWeeks -> BadgeCopy(
            Sticker.Medal, R.string.badge_two_calm_weeks, R.string.badge_two_calm_weeks_goal,
            R.string.badge_two_calm_weeks_cheer, R.string.badge_two_calm_weeks_milestone, BadgeUnit.Days,
        )
        Achievement.CalmMonth -> BadgeCopy(
            Sticker.Trophy, R.string.badge_calm_month, R.string.badge_calm_month_goal,
            R.string.badge_calm_month_cheer, R.string.badge_calm_month_milestone, BadgeUnit.Days,
        )
        Achievement.NightOwlRetired -> BadgeCopy(
            Sticker.Moon, R.string.badge_night_owl, R.string.badge_night_owl_goal,
            R.string.badge_night_owl_cheer, R.string.badge_night_owl_milestone, BadgeUnit.Nights,
        )
        Achievement.SlowMornings -> BadgeCopy(
            Sticker.Leaf, R.string.badge_slow_mornings, R.string.badge_slow_mornings_goal,
            R.string.badge_slow_mornings_cheer, R.string.badge_slow_mornings_milestone, BadgeUnit.Mornings,
        )
        Achievement.ScreenFreeSunday -> BadgeCopy(
            Sticker.Calendar, R.string.badge_screen_free_sunday, R.string.badge_screen_free_sunday_goal,
            R.string.badge_screen_free_sunday_cheer, R.string.badge_screen_free_sunday_milestone, BadgeUnit.Once,
        )
        Achievement.GentleDay -> BadgeCopy(
            Sticker.Heart, R.string.badge_gentle_day, R.string.badge_gentle_day_goal,
            R.string.badge_gentle_day_cheer, R.string.badge_gentle_day_milestone, BadgeUnit.Once,
        )
        Achievement.HandsOff -> BadgeCopy(
            Sticker.Shield, R.string.badge_hands_off, R.string.badge_hands_off_goal,
            R.string.badge_hands_off_cheer, R.string.badge_hands_off_milestone, BadgeUnit.Once,
        )
        Achievement.UrgesResisted -> BadgeCopy(
            Sticker.Star, R.string.badge_urges_resisted, R.string.badge_urges_resisted_goal,
            R.string.badge_urges_resisted_cheer, R.string.badge_urges_resisted_milestone, BadgeUnit.Urges,
        )
        Achievement.TenHoursWonBack -> BadgeCopy(
            Sticker.Timer, R.string.badge_ten_hours_won_back, R.string.badge_ten_hours_won_back_goal,
            R.string.badge_ten_hours_won_back_cheer, R.string.badge_ten_hours_won_back_milestone, BadgeUnit.Hours,
        )
        Achievement.FiftyHoursWonBack -> BadgeCopy(
            Sticker.Trophy, R.string.badge_fifty_hours_won_back, R.string.badge_fifty_hours_won_back_goal,
            R.string.badge_fifty_hours_won_back_cheer, R.string.badge_fifty_hours_won_back_milestone, BadgeUnit.Hours,
        )
    }

/** Grid caption of a locked badge: "5 / 7 days", "31 / 50 hours", "0 / 1". */
@Composable
@ReadOnlyComposable
internal fun AchievementProgress.shortProgress(): String {
    val target = achievement.target
    return when (achievement.copy.unit) {
        BadgeUnit.Days -> stringResource(R.string.achievements_progress_days, progress, target)
        BadgeUnit.Nights -> stringResource(R.string.achievements_progress_nights, progress, target)
        BadgeUnit.Mornings -> stringResource(R.string.achievements_progress_mornings, progress, target)
        BadgeUnit.Hours -> stringResource(R.string.achievements_progress_hours, progress, target)
        BadgeUnit.Once, BadgeUnit.Urges -> stringResource(R.string.achievements_progress_count, progress, target)
    }
}

/** Progress line of the badge sheet: "5 of 7 days", "38 of 100 urges", "Not earned yet". */
@Composable
@ReadOnlyComposable
internal fun AchievementProgress.longProgress(): String {
    val target = achievement.target
    return when (achievement.copy.unit) {
        BadgeUnit.Once -> stringResource(R.string.achievements_not_yet)
        BadgeUnit.Days -> stringResource(R.string.achievements_of_days, progress, target)
        BadgeUnit.Nights -> stringResource(R.string.achievements_of_nights, progress, target)
        BadgeUnit.Mornings -> stringResource(R.string.achievements_of_mornings, progress, target)
        BadgeUnit.Hours -> stringResource(R.string.achievements_of_hours, progress, target)
        BadgeUnit.Urges -> stringResource(R.string.achievements_of_urges, progress, target)
    }
}

/** Milestone line: the first lock names its apps ("You blocked Instagram & X for the first time"). */
@Composable
@ReadOnlyComposable
internal fun Achievement.milestoneText(firstLockApps: List<AppId>): String =
    if (this == Achievement.FirstLock && firstLockApps.isNotEmpty()) {
        stringResource(R.string.badge_first_lock_milestone, appList(firstLockApps))
    } else {
        stringResource(copy.milestone)
    }

/** "Instagram", "Instagram & X", "Instagram, X & TikTok". */
internal fun appList(apps: List<AppId>): String {
    val names = apps.map { it.displayName }
    return if (names.size <= 1) names.joinToString() else names.dropLast(1).joinToString(", ") + " & " + names.last()
}

@Composable
@ReadOnlyComposable
internal fun CalmLevel.name(): String {
    val names = stringArrayResource(R.array.achievements_level_names)
    return names[(level - 1).coerceIn(0, names.lastIndex)]
}
