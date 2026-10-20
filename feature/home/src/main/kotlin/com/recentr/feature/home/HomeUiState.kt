package com.recentr.feature.home

import androidx.compose.runtime.Immutable
import com.recentr.core.model.AvatarColor
import com.recentr.core.model.RuleType
import com.recentr.core.model.ScreenTimeSummary
import com.recentr.core.model.UsagePeriod
import java.time.DayOfWeek

/** Grouping of the "Most used" list (the "Apps ▾" chip). */
enum class UsageGrouping { Apps, Categories }

/** The upcoming routine shown in the "Next · 9:00 AM" tile. */
@Immutable
data class NextRuleUi(
    val ruleId: String,
    val name: String,
    val type: RuleType,
    /** Minutes from midnight. */
    val startMinute: Int,
    /** Day of the start when it is not today, null when it starts today. */
    val day: DayOfWeek?,
)

/**
 * What blocks apps right now, counting down: a running session, or a routine ([RuleType.Bedtime])
 * inside its window.
 */
@Immutable
data class ActiveSessionUi(
    val ruleId: String,
    val name: String,
    val type: RuleType,
    /** Epoch millis when the session or the routine's window ends. */
    val endsAt: Long,
)

/** Everything the Home tab renders. */
@Immutable
data class HomeUiState(
    val period: UsagePeriod = UsagePeriod.Today,
    /** Last loaded numbers for [period]; null until the first sync completes. */
    val summary: ScreenTimeSummary? = null,
    /** True while the repository aggregates screen time (Figma 29 · Syncing skeleton). */
    val isSyncing: Boolean = true,
    /** A pull to refresh is reading the numbers again (they stay on screen meanwhile). */
    val isRefreshing: Boolean = false,
    val dailyGoalMinutes: Int = 4 * 60,
    val nextRule: NextRuleUi? = null,
    /** Running session or routine, shown in place of the next rule in the tile. */
    val activeSession: ActiveSessionUi? = null,
    val grouping: UsageGrouping = UsageGrouping.Apps,
    /** Monday = 0 … Sunday = 6, used to highlight today in the week chart. */
    val todayIndex: Int = 6,
    /** Settings › Appearance › "Show mascot on Home". */
    val showMascot: Boolean = true,
    /** Header avatar: first letter of the name and its gradient (Profile › Edit profile). */
    val avatarInitial: String = "?",
    val avatarColor: AvatarColor = AvatarColor.Sunset,
) {
    /** Skeleton shown while syncing (no stale numbers on screen). */
    val showSkeleton: Boolean get() = isSyncing || summary == null
}
