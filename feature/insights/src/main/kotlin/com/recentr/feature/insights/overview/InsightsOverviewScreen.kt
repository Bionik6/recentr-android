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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.recentr.core.data.DemoScreenTimeRepository
import com.recentr.core.designsystem.component.MiniTile
import com.recentr.core.designsystem.component.RecentrButton
import com.recentr.core.designsystem.component.RecentrButtonStyle
import com.recentr.core.designsystem.component.RecentrIconButton
import com.recentr.core.designsystem.component.RecentrSegmented
import com.recentr.core.designsystem.component.RecentrTextButton
import com.recentr.core.designsystem.component.UsageAccessCard
import com.recentr.core.designsystem.icon.RecentrIcon
import com.recentr.core.designsystem.illustration.Sticker
import com.recentr.core.designsystem.theme.RecentrTheme
import com.recentr.core.model.AppUsage
import com.recentr.core.model.RangeUnit
import com.recentr.core.model.ScreenTimeSummary
import com.recentr.core.model.UsagePeriod
import com.recentr.core.model.UsageRange
import com.recentr.feature.insights.R
import com.recentr.feature.insights.ui.CardTitle
import com.recentr.feature.insights.ui.CategoryStack
import com.recentr.feature.insights.ui.ChangeChip
import com.recentr.feature.insights.ui.HourBarsChart
import com.recentr.feature.insights.ui.InsightsCard
import com.recentr.feature.insights.ui.InsightsScaffold
import com.recentr.feature.insights.ui.LoadingBlock
import com.recentr.feature.insights.ui.MonthCalendar
import com.recentr.feature.insights.ui.RangeName
import com.recentr.feature.insights.ui.MoreMenuButton
import com.recentr.feature.insights.ui.ProgressTrack
import com.recentr.feature.insights.ui.StackSegment
import com.recentr.feature.insights.ui.UsageAppIcon
import com.recentr.feature.insights.ui.WeekGoalChart
import com.recentr.feature.insights.ui.duration
import com.recentr.feature.insights.ui.labelRes
import com.recentr.feature.insights.ui.monthName
import com.recentr.feature.insights.ui.name
import com.recentr.feature.insights.ui.rangeLabel
import com.recentr.feature.insights.ui.percentChange
import com.recentr.feature.insights.ui.signedPercent

/** What the overview can do. */
internal class OverviewActions(
    val onSelectTab: (RangeUnit) -> Unit,
    val onPrevious: () -> Unit,
    val onNext: () -> Unit,
    val onOpenApp: (packageName: String, range: UsageRange) -> Unit,
    /** "See more" under "Top apps". */
    val onSeeMoreApps: () -> Unit,
    val onOpenDailyGoal: () -> Unit,
    val onOpenPickups: (UsageRange) -> Unit,
    val onOpenStreak: () -> Unit,
    val onOpenAchievements: () -> Unit,
    val onOpenReport: () -> Unit,
    /** Settings › Usage access, without which there are no numbers. */
    val onConnectScreenTime: () -> Unit = {},
)

/**
 * 99 · Insights — Overview (Day / Week) and 102 · Month, as the Insights tab: segmented tabs and
 * the date selector (any day, week or month the device has data for), then the daily average with
 * the goal line, the category split and the top apps — or the month calendar. Drawn under the
 * floating tab bar ([contentPadding] bottom). Without usage access, only the card asking for it.
 */
@Composable
internal fun InsightsOverviewScreen(
    state: OverviewUiState,
    actions: OverviewActions,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    // The monthly report covers the month under way.
    val reportMonth = state.tab == RangeUnit.Month && state.today in state.range && !state.needsUsageAccess
    InsightsScaffold(
        modifier = modifier,
        bottomInset = contentPadding.calculateBottomPadding(),
        topBar = { OverviewHeader(state, actions) },
        bottomBar = if (reportMonth) {
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
        if (state.needsUsageAccess) {
            // No day, week or month to look at: their empty numbers would read as no screen time.
            UsageAccessCard(onConnect = actions.onConnectScreenTime)
        } else {
            RecentrSegmented(
                options = listOf(
                    stringResource(R.string.insights_tab_day),
                    stringResource(R.string.insights_tab_week),
                    stringResource(R.string.insights_tab_month),
                ),
                selectedIndex = state.tab.ordinal,
                onSelect = { actions.onSelectTab(RangeUnit.entries[it]) },
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(10.dp))
            DateSelector(state, actions)
            Spacer(Modifier.height(10.dp))
            AnimatedContent(
                targetState = state.tab,
                transitionSpec = { fadeIn(tween(220)) togetherWith fadeOut(tween(120)) },
                label = "insightsTab",
            ) { tab ->
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (tab == RangeUnit.Month) {
                        MonthContent(state)
                    } else {
                        val summary = state.summary
                        if (summary == null || summary.range != state.range) {
                            LoadingBlock(219.dp)
                            LoadingBlock(110.dp)
                            LoadingBlock(260.dp)
                        } else {
                            UsageCard(state, summary, onClick = actions.onOpenDailyGoal)
                            CategoryCard(summary)
                            TopAppsCard(
                                apps = summary.mostUsed,
                                shown = state.topAppsShown,
                                onOpenApp = { actions.onOpenApp(it, state.range) },
                                onSeeMore = actions.onSeeMoreApps,
                            )
                        }
                    }
                }
            }
        }
    }
}

/** Tab header, like Rules: Display/Large "Insights" and the ⋯ menu (the selector says which period). */
@Composable
private fun OverviewHeader(state: OverviewUiState, actions: OverviewActions) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(
            stringResource(R.string.insights_title),
            style = RecentrTheme.typography.displayLarge,
            color = RecentrTheme.colors.textPrimary,
            maxLines = 1,
            modifier = Modifier.weight(1f).semantics { heading() },
        )
        MoreMenuButton(
            listOf(
                stringResource(R.string.insights_menu_pickups) to { actions.onOpenPickups(state.range) },
                stringResource(R.string.insights_menu_daily_goal) to actions.onOpenDailyGoal,
                stringResource(R.string.insights_menu_streak) to actions.onOpenStreak,
                stringResource(R.string.insights_menu_achievements) to actions.onOpenAchievements,
            ),
        )
    }
}

/**
 * "‹ Sun, Oct 18 ›": the day, week or month shown, the one before (down to the first day with
 * data) and the one after (up to today's).
 */
@Composable
private fun DateSelector(state: OverviewUiState, actions: OverviewActions) {
    val label = rangeLabel(state.range, state.today)
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        SelectorArrow(RecentrIcon.ChevronLeft, stringResource(R.string.insights_previous), state.canGoBack, actions.onPrevious)
        AnimatedContent(
            targetState = label,
            transitionSpec = { fadeIn(tween(180)) togetherWith fadeOut(tween(120)) },
            label = "rangeLabel",
            modifier = Modifier.weight(1f),
        ) { text ->
            Text(
                text,
                style = RecentrTheme.typography.headline,
                color = RecentrTheme.colors.textPrimary,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        SelectorArrow(RecentrIcon.ChevronRight, stringResource(R.string.insights_next), state.canGoForward, actions.onNext)
    }
}

/** An arrow of the date selector, faded while there is nothing that way. */
@Composable
private fun SelectorArrow(icon: RecentrIcon, description: String, enabled: Boolean, onClick: () -> Unit) {
    RecentrIconButton(
        icon = icon,
        contentDescription = description,
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.graphicsLayer { alpha = if (enabled) 1f else 0.35f },
    )
}

/** "Daily average" (Week) or "Screen time today" (Day) with its chart and the change chip. */
@Composable
private fun UsageCard(state: OverviewUiState, summary: ScreenTimeSummary, onClick: () -> Unit) {
    val colors = RecentrTheme.colors
    val week = summary.isMultiDay
    val change = percentChange(summary.totalMinutes, summary.previousTotalMinutes)
    val name = state.range.name(state.today)
    InsightsCard(onClick = onClick) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Bottom) {
            Column(Modifier.weight(1f)) {
                Text(
                    stringResource(
                        when (name) {
                            RangeName.Today -> R.string.insights_screen_time_today
                            RangeName.Yesterday -> R.string.insights_screen_time_yesterday
                            is RangeName.Day -> R.string.insights_screen_time
                            else -> R.string.insights_daily_average
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
                        when (name) {
                            RangeName.Today -> R.string.insights_vs_yesterday
                            RangeName.ThisWeek -> R.string.insights_vs_last_week
                            is RangeName.Day, RangeName.Yesterday -> R.string.insights_vs_day_before
                            else -> R.string.insights_vs_week_before
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

/** The [shown] most used of [apps] (most used first), and "See more" while others are left. */
@Composable
private fun TopAppsCard(apps: List<AppUsage>, shown: Int, onOpenApp: (String) -> Unit, onSeeMore: () -> Unit) {
    val colors = RecentrTheme.colors
    val top = apps.maxOfOrNull { it.minutes }?.coerceAtLeast(1) ?: 1
    InsightsCard(spacing = 10.dp, contentPadding = PaddingValues(horizontal = 18.dp, vertical = 16.dp)) {
        CardTitle(stringResource(R.string.insights_top_apps))
        if (apps.isEmpty()) {
            Text(stringResource(R.string.insights_no_apps), style = RecentrTheme.typography.captionRegular, color = colors.textSecondary)
        }
        apps.take(shown).forEach { usage ->
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
        if (apps.size > shown) {
            RecentrTextButton(
                text = stringResource(R.string.insights_see_more),
                onClick = onSeeMore,
                modifier = Modifier.fillMaxWidth(),
                color = colors.brandSky,
            )
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

private val NoActions = OverviewActions({}, {}, {}, { _, _ -> }, {}, {}, {}, {}, {}, {})

@Preview(widthDp = 402, heightDp = 874)
@Composable
private fun OverviewWeekPreview() {
    RecentrTheme {
        InsightsOverviewScreen(
            state = OverviewUiState(
                range = UsageRange.of(RangeUnit.Week, java.time.LocalDate.now()),
                summary = DemoScreenTimeRepository().snapshot(UsagePeriod.ThisWeek).copy(range = UsageRange.of(RangeUnit.Week, java.time.LocalDate.now())),
                firstDay = java.time.LocalDate.now().minusDays(40),
                loading = false,
                goalMinutes = 90,
            ),
            actions = NoActions,
            contentPadding = PaddingValues(bottom = 96.dp),
        )
    }
}

@Preview(widthDp = 402, heightDp = 874)
@Composable
private fun OverviewNoUsageAccessPreview() {
    RecentrTheme {
        InsightsOverviewScreen(
            state = OverviewUiState(loading = false, usageAccess = false),
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
                range = UsageRange.of(RangeUnit.Month, today),
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
