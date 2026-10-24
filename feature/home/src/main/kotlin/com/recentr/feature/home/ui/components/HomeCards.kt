package com.recentr.feature.home.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.produceState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.recentr.core.designsystem.component.MiniTile
import com.recentr.core.designsystem.component.TodayCard
import com.recentr.core.designsystem.component.TodayStat
import com.recentr.core.designsystem.illustration.Sticker
import com.recentr.core.designsystem.theme.RecentrElevation
import com.recentr.core.designsystem.theme.RecentrShapes
import com.recentr.core.designsystem.theme.RecentrTheme
import com.recentr.core.designsystem.theme.recentrShadow
import com.recentr.core.model.RuleType
import com.recentr.core.model.ScreenTimeSummary
import com.recentr.feature.home.ActiveSessionUi
import kotlinx.coroutines.delay
import com.recentr.feature.home.NextRuleUi
import com.recentr.feature.home.R
import com.recentr.feature.home.ui.formatClock
import com.recentr.feature.home.ui.formatDuration
import com.recentr.feature.home.ui.sticker

/**
 * Figma "Today Card" for a single day (today / yesterday). On the Quality hero page (22) the
 * focus hours are painted `feedback/success`: [focusHighlight] blends them from sky (0) to green
 * (1) while the pager swipes.
 */
@Composable
internal fun HomeTodayCard(
    summary: ScreenTimeSummary,
    focusHighlight: Float,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    val colors = RecentrTheme.colors
    val focusColor = lerp(colors.brandSky, colors.feedbackSuccess, focusHighlight.coerceIn(0f, 1f))
    TodayCard(
        stats = listOf(
            TodayStat(
                label = stringResource(R.string.home_stat_decrease),
                value = stringResource(R.string.home_stat_decrease_value, formatDuration(summary.decreaseMinutes)),
                valueColor = colors.feedbackSuccess,
            ),
            TodayStat(stringResource(R.string.home_stat_pickups), summary.pickups.toString()),
            TodayStat(stringResource(R.string.home_stat_notifications), summary.notifications.toString()),
        ),
        modifier = modifier,
        onClick = onClick,
    ) {
        HourlyBarsChart(summary.hourlyMinutes) { hour ->
            if (hour in summary.focusHours) focusColor else colors.brandSky
        }
    }
}

/** Figma 27 "Week card" — Total / Best day / Pickups + the Monday → Sunday bars. */
@Composable
internal fun WeekCard(
    summary: ScreenTimeSummary,
    todayIndex: Int,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    val colors = RecentrTheme.colors
    // Days still to come (this week) or not reached yet (this month) are no best day.
    val measured = summary.dailyMinutes.withIndex().filter { it.index in summary.measuredDays }
    val bestIndex = measured.minByOrNull { it.value }?.index ?: 0
    val dayNames = stringArrayResource(R.array.home_day_short)
    Column(
        modifier = modifier
            .recentrShadow(RecentrElevation.float, RecentrShapes.card)
            .clip(RecentrShapes.card)
            .background(colors.bgSurface)
            .then(if (onClick != null) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier)
            .padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            CardStat(stringResource(R.string.home_stat_total), formatDuration(summary.totalMinutes))
            CardStat(stringResource(R.string.home_stat_best_day), dayNames[bestIndex], colors.feedbackSuccess)
            CardStat(stringResource(R.string.home_stat_pickups), summary.pickups.toString())
        }
        WeekBarsChart(
            dailyMinutes = summary.dailyMinutes,
            averageMinutes = measured.map { it.value }.average().takeIf { !it.isNaN() }?.toInt() ?: 0,
            bestIndex = bestIndex,
            todayIndex = todayIndex,
        )
    }
}

@Composable
private fun CardStat(label: String, value: String, valueColor: Color = RecentrTheme.colors.textPrimary) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(label, style = RecentrTheme.typography.captionRegular, color = RecentrTheme.colors.textTertiary, maxLines = 1)
        Text(value, style = RecentrTheme.typography.title2, color = valueColor, maxLines = 1)
    }
}

/** Figma 29 "Skeleton card" shown while screen time syncs. */
@Composable
internal fun SkeletonCard(modifier: Modifier = Modifier) {
    val alpha = skeletonPulse()
    Column(
        modifier = modifier
            .recentrShadow(RecentrElevation.float, RecentrShapes.card)
            .clip(RecentrShapes.card)
            .background(RecentrTheme.colors.bgSurface)
            .padding(horizontal = 20.dp, vertical = 22.dp)
            .graphicsLayer { this.alpha = alpha() },
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        SkeletonBlock(Modifier.width(200.dp), 14.dp)
        SkeletonBlock(Modifier.fillMaxWidth(), 40.dp)
        SkeletonBlock(Modifier.fillMaxWidth(), 90.dp)
        SkeletonBlock(Modifier.width(160.dp), 10.dp)
    }
}

@Composable
private fun SkeletonBlock(modifier: Modifier, height: Dp) {
    Box(
        modifier
            .height(height)
            .clip(RoundedCornerShape(RecentrTheme.radius.xs))
            .background(RecentrTheme.colors.bgSunken),
    )
}

/** Gentle 1 → 0.55 alpha pulse for skeleton placeholders. */
@Composable
internal fun skeletonPulse(): () -> Float {
    val transition = rememberInfiniteTransition(label = "skeleton")
    val alpha by transition.animateFloat(
        initialValue = 1f,
        targetValue = 0.55f,
        animationSpec = infiniteRepeatable(tween(800), RepeatMode.Reverse),
        label = "skeletonAlpha",
    )
    return { alpha }
}

/**
 * Full-width next rule tile, or the running session's countdown ("Loading…" while syncing). A rule's
 * tile shows it on Rules ([onShowRule]); with none, the tile opens Rules ([onOpenRules]).
 */
@Composable
internal fun NextRuleTile(
    nextRule: NextRuleUi?,
    loading: Boolean,
    onOpenRules: () -> Unit,
    modifier: Modifier = Modifier,
    activeSession: ActiveSessionUi? = null,
    onShowRule: (ruleId: String) -> Unit = { onOpenRules() },
) {
    val dayNames = stringArrayResource(R.array.home_day_short)
    // Live countdown of the running session or routine (ticks every second while it runs).
    val remainingMillis by produceState(
        initialValue = activeSession?.let { it.endsAt - System.currentTimeMillis() } ?: 0L,
        activeSession,
    ) {
        val session = activeSession ?: return@produceState
        while (true) {
            value = session.endsAt - System.currentTimeMillis()
            if (value <= 0) break
            delay(1_000)
        }
    }
    val tileModifier = modifier.fillMaxWidth()
    when {
        loading -> MiniTile(
            label = stringResource(R.string.home_tile_placeholder),
            value = stringResource(R.string.home_tile_loading),
            sticker = nextRule?.type?.sticker ?: Sticker.Bolt,
            modifier = tileModifier,
        )
        activeSession != null && remainingMillis > 0 -> {
            val totalSeconds = (remainingMillis + 999) / 1000
            val left = if (totalSeconds >= 3600) {
                "%d:%02d:%02d".format(totalSeconds / 3600, totalSeconds % 3600 / 60, totalSeconds % 60)
            } else {
                "%d:%02d".format(totalSeconds / 60, totalSeconds % 60)
            }
            MiniTile(
                // Routines read "Blocking", like their chip on the Rules list.
                label = stringResource(if (activeSession.type == RuleType.Bedtime) R.string.home_tile_blocking else R.string.home_tile_ongoing),
                value = stringResource(R.string.home_tile_left, left),
                sticker = activeSession.type.sticker,
                onClick = { onShowRule(activeSession.ruleId) },
                modifier = tileModifier,
            )
        }
        nextRule != null -> {
            val time = formatClock(nextRule.startMinute)
            val whenText = if (nextRule.day == null) time else stringResource(R.string.home_tile_next_day, dayNames[nextRule.day.value - 1], time)
            MiniTile(
                label = stringResource(R.string.home_tile_next, whenText),
                value = nextRule.name,
                sticker = nextRule.type.sticker,
                onClick = { onShowRule(nextRule.ruleId) },
                modifier = tileModifier,
            )
        }
        else -> MiniTile(
            label = stringResource(R.string.home_tile_next_none),
            value = stringResource(R.string.home_tile_no_rule),
            sticker = Sticker.Bolt,
            onClick = onOpenRules,
            modifier = tileModifier,
        )
    }
}

@Preview
@Composable
private fun SkeletonCardPreview() {
    RecentrTheme {
        Box(Modifier.background(RecentrTheme.colors.bgPage).padding(24.dp)) { SkeletonCard(Modifier.fillMaxWidth()) }
    }
}
