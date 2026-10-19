package com.recentr.feature.home.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.recentr.core.designsystem.component.RecentrChip
import com.recentr.core.designsystem.component.RecentrChipStyle
import com.recentr.core.designsystem.icon.RecentrIcon
import com.recentr.core.designsystem.theme.RecentrTheme
import com.recentr.core.model.ScreenTimeSummary
import com.recentr.core.model.UsagePeriod
import com.recentr.feature.home.R
import com.recentr.feature.home.ui.formatDuration
import kotlin.math.roundToInt

/** Pages of the Home hero pager (Figma: dot 1 = screen time · 24, dot 2 = quality · 22). */
internal enum class HeroPage { ScreenTime, Quality, Offline }

/**
 * Figma "Hero": swipeable overline + big number + chips, with the 3 pager dots. Tapping the
 * number opens the matching info sheet, tapping the goal chip opens the goal editor.
 */
@Composable
internal fun HomeHero(
    pagerState: PagerState,
    period: UsagePeriod,
    summary: ScreenTimeSummary?,
    syncing: Boolean,
    dailyGoalMinutes: Int,
    onScreenTimeInfo: () -> Unit,
    onOfflineInfo: () -> Unit,
    onEditGoal: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        HorizontalPager(state = pagerState, modifier = Modifier.fillMaxWidth()) { index ->
            val page = HeroPage.entries[index]
            HeroPageContent(
                overline = stringResource(page.overline(period)),
                syncing = syncing || summary == null,
                value = summary?.let { heroValue(page, it) },
                onValueClick = when (page) {
                    HeroPage.ScreenTime -> onScreenTimeInfo
                    HeroPage.Quality -> null
                    HeroPage.Offline -> onOfflineInfo
                },
            ) {
                if (summary != null) HeroChips(page, summary, dailyGoalMinutes, onEditGoal)
            }
        }
        Spacer(Modifier.height(6.dp))
        PagerDots(count = HeroPage.entries.size, current = pagerState.currentPage)
    }
}

@Composable
private fun HeroPageContent(
    overline: String,
    syncing: Boolean,
    value: String?,
    onValueClick: (() -> Unit)?,
    chips: @Composable () -> Unit,
) {
    val colors = RecentrTheme.colors
    val density = LocalDensity.current
    val numberStyle = RecentrTheme.typography.displayHero.copy(
        shadow = with(density) {
            Shadow(color = Color(0x400D3399), offset = androidx.compose.ui.geometry.Offset(0f, 6.dp.toPx()), blurRadius = 16.dp.toPx())
        },
    )
    Column(
        Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(overline, style = RecentrTheme.typography.bodyMedium, color = colors.textOnDarkMuted, maxLines = 1)
        if (syncing || value == null) {
            val alpha = skeletonPulse()
            Box(
                Modifier
                    .size(width = 200.dp, height = 64.dp)
                    .graphicsLayer { this.alpha = alpha() }
                    .clip(RoundedCornerShape(RecentrTheme.radius.lg))
                    .background(colors.alphaWhite24),
            )
            RecentrChip(
                label = stringResource(R.string.home_syncing),
                style = RecentrChipStyle.OnSky,
                icon = RecentrIcon.Refresh,
            )
        } else {
            AnimatedContent(
                targetState = value,
                transitionSpec = { fadeIn(tween(220)) togetherWith fadeOut(tween(160)) },
                label = "heroValue",
            ) { text ->
                Text(
                    text = text,
                    style = numberStyle,
                    color = colors.textOnDark,
                    maxLines = 1,
                    modifier = Modifier
                        .clip(RoundedCornerShape(RecentrTheme.radius.lg))
                        .then(
                            if (onValueClick != null) {
                                Modifier.clickable(
                                    onClickLabel = stringResource(R.string.home_hero_details),
                                    role = Role.Button,
                                    onClick = onValueClick,
                                )
                            } else {
                                Modifier
                            },
                        )
                        .padding(horizontal = 8.dp),
                )
            }
            Box(Modifier.height(30.dp), contentAlignment = Alignment.Center) { chips() }
        }
    }
}

@Composable
private fun HeroChips(page: HeroPage, summary: ScreenTimeSummary, goalMinutes: Int, onEditGoal: () -> Unit) {
    when (page) {
        HeroPage.ScreenTime -> {
            val previous = summary.previousTotalMinutes
            if (summary.isMultiDay && previous != null && previous > 0) {
                val change = ((previous - summary.totalMinutes) * 100f / previous).roundToInt()
                val week = summary.period == UsagePeriod.ThisWeek
                val label = if (change >= 0) {
                    stringResource(if (week) R.string.home_compare_less_week else R.string.home_compare_less_month, change)
                } else {
                    stringResource(if (week) R.string.home_compare_more_week else R.string.home_compare_more_month, -change)
                }
                RecentrChip(
                    label = label,
                    style = if (change >= 0) RecentrChipStyle.Success else RecentrChipStyle.OnSky,
                    icon = if (change >= 0) RecentrIcon.Check else RecentrIcon.Chart,
                    onClick = onEditGoal,
                )
            } else {
                val under = summary.dailyAverageMinutes <= goalMinutes
                val goal = formatDuration(goalMinutes)
                RecentrChip(
                    label = stringResource(if (under) R.string.home_goal_under else R.string.home_goal_over, goal),
                    style = if (under) RecentrChipStyle.Success else RecentrChipStyle.OnSky,
                    icon = if (under) RecentrIcon.Check else RecentrIcon.Hourglass,
                    onClick = onEditGoal,
                )
            }
        }
        HeroPage.Quality -> Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            HeroInfoChip(stringResource(R.string.home_split_focus, summary.focusPercent))
            HeroInfoChip(stringResource(R.string.home_split_social, summary.socialPercent))
            HeroInfoChip(stringResource(R.string.home_split_fun, summary.funPercent))
        }
        HeroPage.Offline -> HeroInfoChip(stringResource(R.string.home_offline_chip), icon = RecentrIcon.Moon)
    }
}

/**
 * Quality split and phone-free chips: white outline chips with primary text over the light sky's
 * clouds (Figma 22), translucent glass on the dark sky.
 */
@Composable
private fun HeroInfoChip(label: String, icon: RecentrIcon? = null) {
    if (RecentrTheme.isDark) {
        RecentrChip(label, style = RecentrChipStyle.OnSky, icon = icon)
    } else {
        // The outline makes it 32dp high: it overflows the 30dp chip slot evenly, as in Figma.
        RecentrChip(
            label = label,
            style = RecentrChipStyle.Outline,
            icon = icon,
            contentColor = RecentrTheme.colors.textPrimary,
            modifier = Modifier.wrapContentHeight(unbounded = true),
        )
    }
}

@Composable
private fun heroValue(page: HeroPage, summary: ScreenTimeSummary): String = when (page) {
    HeroPage.ScreenTime -> formatDuration(summary.dailyAverageMinutes)
    HeroPage.Quality -> when {
        // No split without usage data (no usage access).
        summary.focusPercent + summary.socialPercent + summary.funPercent == 0 -> "–"
        summary.focusPercent >= 60 -> stringResource(R.string.home_quality_focused)
        summary.focusPercent >= 40 -> stringResource(R.string.home_quality_balanced)
        else -> stringResource(R.string.home_quality_distracted)
    }
    HeroPage.Offline -> formatDuration(summary.longestOfflineMinutes)
}

private fun HeroPage.overline(period: UsagePeriod): Int = when (this) {
    HeroPage.ScreenTime -> when (period) {
        UsagePeriod.Today -> R.string.home_hero_screen_time_today
        UsagePeriod.Yesterday -> R.string.home_hero_screen_time_yesterday
        UsagePeriod.ThisWeek -> R.string.home_hero_average_week
        UsagePeriod.ThisMonth -> R.string.home_hero_average_month
    }
    HeroPage.Quality -> when (period) {
        UsagePeriod.Today -> R.string.home_hero_quality_today
        UsagePeriod.Yesterday -> R.string.home_hero_quality_yesterday
        UsagePeriod.ThisWeek -> R.string.home_hero_quality_week
        UsagePeriod.ThisMonth -> R.string.home_hero_quality_month
    }
    HeroPage.Offline -> when (period) {
        UsagePeriod.Today -> R.string.home_hero_offline_today
        UsagePeriod.Yesterday -> R.string.home_hero_offline_yesterday
        UsagePeriod.ThisWeek -> R.string.home_hero_offline_week
        UsagePeriod.ThisMonth -> R.string.home_hero_offline_month
    }
}

/** 3 × 6dp dots, 6dp apart: current = white, others = alpha/white-24. */
@Composable
private fun PagerDots(count: Int, current: Int) {
    val colors = RecentrTheme.colors
    val description = stringResource(R.string.home_hero_page, current + 1, count)
    Row(
        modifier = Modifier.semantics { contentDescription = description },
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        repeat(count) { index ->
            val color by animateColorAsState(
                if (index == current) colors.textOnDark else colors.alphaWhite24,
                label = "dot$index",
            )
            Box(Modifier.width(6.dp).height(6.dp).clip(CircleShape).background(color))
        }
    }
}
