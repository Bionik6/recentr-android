package com.recentr.feature.home

import androidx.compose.runtime.Immutable
import com.recentr.core.model.RuleType
import com.recentr.core.model.ScreenTimeSummary
import com.recentr.core.model.UsagePeriod
import java.time.DayOfWeek

/** Grouping of the "Most used" list (the "Apps ▾" chip). */
enum class UsageGrouping { Apps, Categories }

/** The upcoming blocking rule shown in the "Next · 9:00 AM" tile. */
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

/** A session currently blocking apps (started with "Start blocking"). */
@Immutable
data class ActiveSessionUi(
    val ruleId: String,
    val name: String,
    val type: RuleType,
    /** Epoch millis when the session ends. */
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
    val dailyGoalMinutes: Int = 4 * 60,
    val nextRule: NextRuleUi? = null,
    /** Running session, shown in place of the next rule in the tile. */
    val activeSession: ActiveSessionUi? = null,
    /** Package names answered "Time well spent?" on the Most used list (local only). */
    val wellSpent: Set<String> = emptySet(),
    val grouping: UsageGrouping = UsageGrouping.Apps,
    /** Monday = 0 … Sunday = 6, used to highlight today in the week chart. */
    val todayIndex: Int = 6,
) {
    /** Skeleton shown while syncing (no stale numbers on screen). */
    val showSkeleton: Boolean get() = isSyncing || summary == null
}
