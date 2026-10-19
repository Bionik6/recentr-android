package com.recentr.feature.insights.overview

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.recentr.core.data.DemoScreenTimeRepository
import com.recentr.core.designsystem.component.MiniTile
import com.recentr.core.designsystem.component.RecentrButton
import com.recentr.core.designsystem.component.RecentrButtonStyle
import com.recentr.core.designsystem.component.RecentrSegmented
import com.recentr.core.designsystem.illustration.Sticker
import com.recentr.core.designsystem.theme.RecentrTheme
import com.recentr.core.model.AppUsage
import com.recentr.core.model.ScreenTimeSummary
import com.recentr.core.model.UsagePeriod
import com.recentr.feature.insights.R
import com.recentr.feature.insights.ui.CardTitle
import com.recentr.feature.insights.ui.CategoryStack
import com.recentr.feature.insights.ui.ChangeChip
import com.recentr.feature.insights.ui.HourBarsChart
import com.recentr.feature.insights.ui.InsightsCard
import com.recentr.feature.insights.ui.InsightsScaffold
import com.recentr.feature.insights.ui.LoadingBlock
import com.recentr.feature.insights.ui.MonthCalendar
import com.recentr.feature.insights.ui.MoreMenuButton
import com.recentr.feature.insights.ui.ProgressTrack
import com.recentr.feature.insights.ui.StackSegment
import com.recentr.feature.insights.ui.UsageAppIcon
import com.recentr.feature.insights.ui.WeekGoalChart
import com.recentr.feature.insights.ui.duration
import com.recentr.feature.insights.ui.labelRes
import com.recentr.feature.insights.ui.monthName
import com.recentr.feature.insights.ui.percentChange
import com.recentr.feature.insights.ui.signedPercent

/** What the overview can do. */
internal class OverviewActions(
    val onSelectTab: (InsightsTab) -> Unit,
    val onOpenApp: (packageName: String, period: UsagePeriod) -> Unit,
    val onOpenDailyGoal: () -> Unit,
    val onOpenPickups: (UsagePeriod) -> Unit,
    val onOpenStreak: () -> Unit,
    val onOpenAchievements: () -> Unit,
    val onOpenReport: () -> Unit,
)

/**
 * 99 · Insights — Overview (Day / Week) and 102 · Month, as the Insights tab: segmented tabs, then
 * the daily average with the goal line, the category split and the top apps — or the month
 * calendar. Drawn under the floating tab bar ([contentPadding] bottom).
 */
@Composable
internal fun InsightsOverviewScreen(
    state: OverviewUiState,
    actions: OverviewActions,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    val month = state.tab == InsightsTab.Month
    InsightsScaffold(
        modifier = modifier,
        bottomInset = contentPadding.calculateBottomPadding(),
        topBar = { OverviewHeader(state, actions) },
        bottomBar = if (month) {
            {
                RecentrButton(
                    text = stringResource(R.string.insights_see_report, monthName(state.month)),
                    onClick = actions.onOpenReport,
                    style = RecentrButtonStyle.Brand,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        } else {
            null
        },
    ) {
        Spacer(Modifier.height(16.dp))
        RecentrSegmented(
            options = listOf(
                stringResource(R.string.insights_tab_day),
                stringResource(R.string.insights_tab_week),
                stringResource(R.string.insights_tab_month),
            ),
            selectedIndex = state.tab.ordinal,
            onSelect = { actions.onSelectTab(InsightsTab.entries[it]) },
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(14.dp))
        AnimatedContent(
            targetState = state.tab,
            transitionSpec = { fadeIn(tween(220)) togetherWith fadeOut(tween(120)) },
            label = "insightsTab",
        ) { tab ->
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (tab == InsightsTab.Month) {
                    MonthContent(state)
                } else {
                    val summary = state.summary
                    if (summary == null || summary.period != state.period) {
                        LoadingBlock(219.dp)
                        LoadingBlock(110.dp)
                        LoadingBlock(160.dp)
                    } else {
                        UsageCard(state, summary, onClick = actions.onOpenDailyGoal)
                        CategoryCard(summary)
                        TopAppsCard(summary.mostUsed.take(3), onOpenApp = { actions.onOpenApp(it, summary.period) })
                    }
                }
            }
        }
    }
}

/** "Daily average" (Week) or "Screen time today" (Day) with its chart and the change chip. */
/**
 * Tab header, like Rules: Display/Large "Insights" over the period shown (the month on the Month
 * tab), and the ⋯ menu.
 */
@Composable
private fun OverviewHeader(state: OverviewUiState, actions: OverviewActions) {
    val colors = RecentrTheme.colors
    val month = state.tab == InsightsTab.Month
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Top) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                stringResource(R.string.insights_title),
                style = RecentrTheme.typography.displayLarge,
                color = colors.textPrimary,
                maxLines = 1,
                modifier = Modifier.semantics { heading() },
            )
            Text(
                if (month) monthName(state.month) else stringResource(state.period.labelRes),
                style = RecentrTheme.typography.captionRegular,
                color = colors.textSecondary,
                maxLines = 1,
            )
        }
        MoreMenuButton(
            listOf(
                stringResource(R.string.insights_menu_pickups) to {
                    actions.onOpenPickups(if (month) UsagePeriod.ThisMonth else state.period)
                },
                stringResource(R.string.insights_menu_daily_goal) to actions.onOpenDailyGoal,
                stringResource(R.string.insights_menu_streak) to actions.onOpenStreak,
                stringResource(R.string.insights_menu_achievements) to actions.onOpenAchievements,
            ),
        )
    }
}

@Composable
private fun UsageCard(state: OverviewUiState, summary: ScreenTimeSummary, onClick: () -> Unit) {
    val colors = RecentrTheme.colors
    val week = summary.isMultiDay
    val change = percentChange(summary.totalMinutes, summary.previousTotalMinutes)
    InsightsCard(onClick = onClick) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Bottom) {
            Column(Modifier.weight(1f)) {
                Text(
                    stringResource(
                        when {
                            week -> R.string.insights_daily_average
                            summary.period == UsagePeriod.Yesterday -> R.string.insights_screen_time_yesterday
                            else -> R.string.insights_screen_time_today
                        },
                    ),
                    style = RecentrTheme.typography.captionMedium,
                    color = colors.textSecondary,
                    maxLines = 1,
                )
                Text(
                    duration(if (week) summary.dailyAverageMinutes else summary.totalMinutes),
                    style = RecentrTheme.typography.displayLarge,
                    color = colors.textPrimary,
                    maxLines = 1,
                )
            }
            if (change != null) {
                ChangeChip(
                    label = stringResource(
                        when {
                            week -> R.string.insights_vs_last_week
                            summary.period == UsagePeriod.Yesterday -> R.string.insights_vs_day_before
                            else -> R.string.insights_vs_yesterday
                        },
                        signedPercent(change),
                    ),
                    good = change <= 0,
                    modifier = Modifier.padding(bottom = 4.dp),
                )
            }
        }
        if (week) {
            WeekGoalChart(
                minutes = summary.dailyMinutes.mapIndexed { index, value -> value.takeIf { index in summary.measuredDays } },
                goalMinutes = state.goalMinutes,
                dayLabels = stringArrayResource(R.array.insights_weekday_initials).toList(),
                goalLabel = stringResource(R.string.insights_goal_line, duration(state.goalMinutes)),
            )
        } else {
            HourBarsChart(summary.hourlyMinutes)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CategoryCard(summary: ScreenTimeSummary) {
    val colors = RecentrTheme.colors
    val shares = categoryShares(summary)
    val palette = listOf(colors.brandSky, colors.accentTangerine, colors.feedbackSuccess)
    fun colorOf(index: Int, share: CategoryShare): Color = if (share.category == null) colors.borderDashed else palette[index]
    InsightsCard {
        CardTitle(stringResource(R.string.insights_by_category))
        CategoryStack(shares.mapIndexed { index, share -> StackSegment(colorOf(index, share), share.minutes.toFloat()) })
        val named = shares.withIndex().filter { it.value.category != null }
        if (named.isEmpty()) {
            Text(stringResource(R.string.insights_no_usage), style = RecentrTheme.typography.captionRegular, color = colors.textSecondary)
        } else {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                named.forEach { (index, share) ->
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(8.dp).clip(RoundedCornerShape(4.dp)).background(colorOf(index, share)))
                        Text(stringResource(share.category!!.labelRes), style = RecentrTheme.typography.captionRegular, color = colors.textSecondary, maxLines = 1)
                        Text(stringResource(R.string.insights_percent, share.percent), style = RecentrTheme.typography.captionMedium, color = colors.textPrimary, maxLines = 1)
                    }
                }
            }
        }
    }
}

@Composable
private fun TopAppsCard(apps: List<AppUsage>, onOpenApp: (String) -> Unit) {
    val colors = RecentrTheme.colors
    val top = apps.maxOfOrNull { it.minutes }?.coerceAtLeast(1) ?: 1
    InsightsCard(spacing = 10.dp, contentPadding = PaddingValues(horizontal = 18.dp, vertical = 16.dp)) {
        CardTitle(stringResource(R.string.insights_top_apps))
        if (apps.isEmpty()) {
            Text(stringResource(R.string.insights_no_apps), style = RecentrTheme.typography.captionRegular, color = colors.textSecondary)
        }
        apps.forEach { usage ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable(role = Role.Button) { onOpenApp(usage.app.packageName) },
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                UsageAppIcon(usage.app, size = 32.dp)
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(usage.app.label, style = RecentrTheme.typography.bodyMedium, color = colors.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    // The top app fills 88% of the track, like the design.
                    ProgressTrack(usage.minutes * 0.88f / top, height = 6.dp)
                }
                Text(duration(usage.minutes), style = RecentrTheme.typography.headline, color = colors.textPrimary, maxLines = 1)
            }
        }
    }
}

@Composable
private fun MonthContent(state: OverviewUiState) {
    val days = state.monthDays
    if (days == null) {
        LoadingBlock(340.dp)
        return
    }
    InsightsCard(spacing = 0.dp, contentPadding = PaddingValues(horizontal = 14.dp, vertical = 16.dp)) {
        MonthCalendar(
            month = state.month,
            days = days.associateBy { it.date },
            goalMinutes = state.goalMinutes,
            today = state.today,
        )
    }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        MiniTile(
            label = stringResource(R.string.insights_best_day),
            value = state.bestDay?.let { duration(it.minutes) } ?: stringResource(R.string.insights_no_value),
            sticker = Sticker.Star,
            modifier = Modifier.weight(1f),
        )
        MiniTile(
            label = stringResource(R.string.insights_toughest_day),
            value = state.toughestDay?.let { duration(it.minutes) } ?: stringResource(R.string.insights_no_value),
            sticker = Sticker.Flame,
            modifier = Modifier.weight(1f),
        )
    }
}

private val NoActions = OverviewActions({}, { _, _ -> }, {}, {}, {}, {}, {})

@Preview(widthDp = 402, heightDp = 874)
@Composable
private fun OverviewWeekPreview() {
    RecentrTheme {
        InsightsOverviewScreen(
            state = OverviewUiState(summary = DemoScreenTimeRepository().snapshot(UsagePeriod.ThisWeek), loading = false, goalMinutes = 90),
            actions = NoActions,
            contentPadding = PaddingValues(bottom = 96.dp),
        )
    }
}

@Preview(widthDp = 402, heightDp = 874)
@Composable
private fun OverviewMonthPreview() {
    val today = java.time.LocalDate.now()
    val month = java.time.YearMonth.from(today)
    RecentrTheme {
        InsightsOverviewScreen(
            state = OverviewUiState(
                tab = InsightsTab.Month,
                month = month,
                monthDays = DemoScreenTimeRepository().dailySnapshot(month.atDay(1), month.atEndOfMonth(), today),
                goalMinutes = 150,
                loading = false,
                today = today,
            ),
            actions = NoActions,
            contentPadding = PaddingValues(bottom = 96.dp),
        )
    }
}
