package com.recentr.feature.insights.achievements

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.recentr.core.data.AchievementsSnapshot
import com.recentr.core.data.StreakState
import com.recentr.core.designsystem.illustration.StickerImage
import com.recentr.core.designsystem.theme.RecentrElevation
import com.recentr.core.designsystem.theme.RecentrTheme
import com.recentr.core.designsystem.theme.recentrShadow
import com.recentr.core.model.Achievement
import com.recentr.core.model.AchievementProgress
import com.recentr.core.model.AppId
import com.recentr.core.model.CalmLevel
import com.recentr.core.model.UsageApp
import com.recentr.feature.insights.R
import com.recentr.feature.insights.ui.InsightsCardShape
import com.recentr.feature.insights.ui.InsightsScaffold
import com.recentr.feature.insights.ui.InsightsTopBar
import com.recentr.feature.insights.ui.LoadingBlock
import com.recentr.feature.insights.ui.ProgressTrack
import com.recentr.feature.insights.ui.copy
import com.recentr.feature.insights.ui.longProgress
import com.recentr.feature.insights.ui.milestoneText
import com.recentr.feature.insights.ui.shortDate
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/** Gap between two milestones of the timeline. */
private val MilestoneGap = 12.dp

/**
 * 109 · Achievements — Milestones: the earned badges as a timeline since the user started, then
 * the next one (the locked badge closest to done) on a dashed sky card.
 */
@Composable
internal fun MilestonesScreen(
    snapshot: AchievementsSnapshot?,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    today: LocalDate = LocalDate.now(),
) {
    InsightsScaffold(
        modifier = modifier,
        topBar = {
            InsightsTopBar(
                title = stringResource(R.string.milestones_title),
                subtitle = snapshot?.let {
                    val days = (ChronoUnit.DAYS.between(it.startedOn, today) + 1).toInt().coerceAtLeast(1)
                    pluralStringResource(R.plurals.milestones_since, days, days)
                },
                onBack = onBack,
            )
        },
    ) {
        Spacer(Modifier.height(20.dp))
        if (snapshot == null) {
            LoadingBlock(300.dp)
            return@InsightsScaffold
        }
        val earned = snapshot.badges.filter { it.earned }.sortedBy { it.earnedOn }
        val next = snapshot.badges.filter { !it.earned }.maxByOrNull { it.progress.toFloat() / it.achievement.target }
        val entries = earned.map { it to false } + listOfNotNull(next?.let { it to true })
        Column(verticalArrangement = Arrangement.spacedBy(MilestoneGap)) {
            entries.forEachIndexed { index, (badge, isNext) ->
                MilestoneRow(badge, isNext, last = index == entries.lastIndex, firstLockApps = snapshot.firstLockApps)
            }
        }
    }
}

@Composable
private fun MilestoneRow(badge: AchievementProgress, isNext: Boolean, last: Boolean, firstLockApps: List<UsageApp>) {
    val colors = RecentrTheme.colors
    Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        // Node column: the timeline line runs from this node's centre to the next one.
        Box(
            Modifier
                .width(56.dp)
                .fillMaxHeight()
                .drawBehind {
                    if (!last) {
                        val center = 28.dp.toPx()
                        drawRoundRect(
                            color = colors.borderSubtle,
                            topLeft = Offset(26.dp.toPx(), center),
                            size = Size(3.dp.toPx(), size.height + MilestoneGap.toPx()),
                            cornerRadius = CornerRadius(2.dp.toPx()),
                        )
                    }
                },
        ) {
            Box(
                Modifier
                    .size(56.dp)
                    .recentrShadow(RecentrElevation.card, CircleShape)
                    .clip(CircleShape)
                    .background(if (isNext) colors.bgSunken else colors.bgSurface),
                contentAlignment = Alignment.Center,
            ) {
                StickerImage(badge.achievement.copy.sticker, size = 40.dp, modifier = Modifier.graphicsLayer { alpha = if (isNext) 0.5f else 1f })
            }
        }
        if (isNext) NextCard(badge, Modifier.weight(1f)) else EarnedCard(badge, firstLockApps, Modifier.weight(1f))
    }
}

@Composable
private fun EarnedCard(badge: AchievementProgress, firstLockApps: List<UsageApp>, modifier: Modifier) {
    val colors = RecentrTheme.colors
    Column(
        modifier
            .recentrShadow(RecentrElevation.card, InsightsCardShape)
            .clip(InsightsCardShape)
            .background(colors.bgSurface)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        badge.earnedOn?.let { Text(shortDate(it), style = RecentrTheme.typography.labelSmall, color = colors.textTertiary) }
        Text(stringResource(badge.achievement.copy.title), style = RecentrTheme.typography.headline, color = colors.textPrimary)
        Text(badge.achievement.milestoneText(firstLockApps), style = RecentrTheme.typography.captionRegular, color = colors.textSecondary)
    }
}

/** Figma "Milestone · Next": sky 12% fill, 1.5dp dashed sky border, progress on a white track. */
@Composable
private fun NextCard(badge: AchievementProgress, modifier: Modifier) {
    val colors = RecentrTheme.colors
    Column(
        modifier
            .clip(InsightsCardShape)
            .background(colors.alphaSky12)
            .drawBehind {
                val stroke = 1.5.dp.toPx()
                drawRoundRect(
                    color = colors.brandSky,
                    topLeft = Offset(stroke / 2, stroke / 2),
                    size = Size(size.width - stroke, size.height - stroke),
                    cornerRadius = CornerRadius(24.dp.toPx() - stroke / 2),
                    style = Stroke(width = stroke, pathEffect = PathEffect.dashPathEffect(floatArrayOf(5.dp.toPx(), 4.dp.toPx()))),
                )
            }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(stringResource(R.string.milestones_next), style = RecentrTheme.typography.labelSmall, color = colors.brandSky)
        Text(stringResource(badge.achievement.copy.title), style = RecentrTheme.typography.headline, color = colors.textPrimary)
        Text(badge.longProgress(), style = RecentrTheme.typography.captionRegular, color = colors.textSecondary)
        if (badge.achievement.target > 1) {
            ProgressTrack(badge.progress.toFloat() / badge.achievement.target, trackColor = colors.bgSurface)
        }
    }
}

@Preview(widthDp = 402, heightDp = 874)
@Composable
private fun MilestonesPreview() {
    val today = LocalDate.now()
    val earned = listOf(Achievement.FirstLock, Achievement.SevenDayStreak, Achievement.NightOwlRetired, Achievement.UrgesResisted)
    RecentrTheme {
        MilestonesScreen(
            snapshot = AchievementsSnapshot(
                badges = Achievement.entries.map { achievement ->
                    val index = earned.indexOf(achievement)
                    when {
                        index >= 0 -> AchievementProgress(achievement, achievement.target, today.minusDays(20L - index * 5))
                        achievement == Achievement.FiftyHoursWonBack -> AchievementProgress(achievement, 31, null)
                        else -> AchievementProgress(achievement, 0, null)
                    }
                },
                level = CalmLevel.of(640),
                streak = StreakState(12, emptyMap(), 2, null),
                bestStreak = 12,
                uncelebrated = emptyList(),
                days = emptyList(),
                goalMinutes = 150,
                startedOn = today.minusDays(22),
                firstLockApps = listOf(AppId.Instagram, AppId.X).map { UsageApp.of(it) },
            ),
            onBack = {},
            today = today,
        )
    }
}
