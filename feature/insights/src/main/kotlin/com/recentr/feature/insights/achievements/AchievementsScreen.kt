package com.recentr.feature.insights.achievements

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import com.recentr.core.data.AchievementsSnapshot
import com.recentr.core.data.StreakState
import com.recentr.core.designsystem.component.RecentrSegmented
import com.recentr.core.designsystem.icon.RecentrIcon
import com.recentr.core.designsystem.icon.RecentrIconImage
import com.recentr.core.designsystem.illustration.Sticker
import com.recentr.core.designsystem.illustration.StickerImage
import com.recentr.core.designsystem.theme.RecentrTheme
import com.recentr.core.designsystem.theme.recentrShadow
import com.recentr.core.model.Achievement
import com.recentr.core.model.AchievementProgress
import com.recentr.core.model.CalmLevel
import com.recentr.feature.insights.R
import com.recentr.feature.insights.ui.InsightsCard
import com.recentr.feature.insights.ui.InsightsScaffold
import com.recentr.feature.insights.ui.InsightsTopBar
import com.recentr.feature.insights.ui.LoadingBlock
import com.recentr.feature.insights.ui.ProgressTrack
import com.recentr.feature.insights.ui.copy
import com.recentr.feature.insights.ui.name
import com.recentr.feature.insights.ui.shortDate
import com.recentr.feature.insights.ui.shortProgress
import java.time.LocalDate

/** Filters of the badge grid (Figma "All · Earned · Next up"). */
internal enum class BadgeFilter { All, Earned, NextUp }

/** Glow of an earned badge disc (0 6 6 · sky 18%). */
private val EarnedGlow = Shadow(radius = 6.dp, color = Color(0x2E2E7AF5), offset = DpOffset(0.dp, 6.dp))

/**
 * 105 · Achievements — Overview: the calm level, the All / Earned / Next up filter and the badge
 * grid (earned discs on blue, locked ones greyed with their progress).
 */
@Composable
internal fun AchievementsScreen(
    snapshot: AchievementsSnapshot?,
    onBack: () -> Unit,
    onOpenBadge: (AchievementProgress) -> Unit,
    onOpenMilestones: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = RecentrTheme.colors
    var filterIndex by rememberSaveable { mutableIntStateOf(0) }
    val badges = snapshot?.badges.orEmpty()
    InsightsScaffold(
        modifier = modifier,
        topBar = {
            InsightsTopBar(
                title = stringResource(R.string.achievements_title),
                subtitle = snapshot?.let { stringResource(R.string.achievements_earned_count, badges.count { it.earned }, badges.size) },
                onBack = onBack,
            )
        },
    ) {
        Spacer(Modifier.height(20.dp))
        if (snapshot == null) {
            LoadingBlock(88.dp)
            Spacer(Modifier.height(16.dp))
            LoadingBlock(300.dp)
            return@InsightsScaffold
        }
        LevelCard(snapshot.level, onClick = onOpenMilestones)
        Spacer(Modifier.height(16.dp))
        RecentrSegmented(
            options = listOf(
                stringResource(R.string.achievements_all),
                stringResource(R.string.achievements_earned),
                stringResource(R.string.achievements_next_up),
            ),
            selectedIndex = filterIndex,
            onSelect = { filterIndex = it },
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(20.dp))
        val shown = filtered(badges, BadgeFilter.entries[filterIndex])
        if (shown.isEmpty()) {
            Text(
                stringResource(if (filterIndex == BadgeFilter.Earned.ordinal) R.string.achievements_none_earned else R.string.achievements_all_earned),
                style = RecentrTheme.typography.bodyRegular,
                color = colors.textSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(top = 24.dp),
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
            shown.chunked(3).forEach { row ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    row.forEach { BadgeCell(it, onClick = { onOpenBadge(it) }) }
                    repeat(3 - row.size) { Spacer(Modifier.width(104.dp)) }
                }
            }
        }
    }
}

/** Earned badges first (catalogue order), then the locked ones closest to done. */
private fun filtered(badges: List<AchievementProgress>, filter: BadgeFilter): List<AchievementProgress> {
    val earned = badges.filter { it.earned }
    val locked = badges.filter { !it.earned }.sortedByDescending { it.progress.toFloat() / it.achievement.target }
    return when (filter) {
        BadgeFilter.All -> earned + locked
        BadgeFilter.Earned -> earned.sortedByDescending { it.earnedOn }
        BadgeFilter.NextUp -> locked
    }
}

/** Figma "Level": the star, "Level 4 · Calm keeper", the yellow points track. */
@Composable
private fun LevelCard(level: CalmLevel, onClick: () -> Unit) {
    val colors = RecentrTheme.colors
    val ceiling = level.ceiling
    InsightsCard(onClick = onClick, contentPadding = PaddingValues(start = 14.dp, end = 18.dp, top = 14.dp, bottom = 14.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
            StickerImage(Sticker.Star, size = 56.dp)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.achievements_level, level.level), style = RecentrTheme.typography.headline, color = colors.textPrimary, maxLines = 1)
                    Text(stringResource(R.string.achievements_level_name, level.name()), style = RecentrTheme.typography.bodyRegular, color = colors.textSecondary, maxLines = 1)
                }
                ProgressTrack(
                    fraction = if (ceiling == null) 1f else (level.points - level.floor).toFloat() / (ceiling - level.floor),
                    color = colors.accentNote,
                )
                Text(
                    if (ceiling == null) {
                        stringResource(R.string.achievements_points_max, level.points)
                    } else {
                        stringResource(R.string.achievements_points, level.points, ceiling)
                    },
                    style = RecentrTheme.typography.captionRegular,
                    color = colors.textSecondary,
                )
            }
        }
    }
}

/** Figma "Badge" (104 wide): the disc, the title and the earned date or the progress. */
@Composable
private fun BadgeCell(badge: AchievementProgress, onClick: () -> Unit) {
    val colors = RecentrTheme.colors
    val title = stringResource(badge.achievement.copy.title)
    val caption = badge.earnedOn?.let { shortDate(it) } ?: badge.shortProgress()
    val description = if (badge.earned) {
        stringResource(R.string.achievements_earned_cd, title, caption)
    } else {
        stringResource(R.string.achievements_locked_cd, title, caption)
    }
    Column(
        Modifier
            .width(104.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .clearAndSetSemantics {
                contentDescription = description
                role = Role.Button
            }
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        BadgeDisc(badge.achievement, earned = badge.earned)
        Text(
            title,
            style = RecentrTheme.typography.captionMedium,
            color = if (badge.earned) colors.textPrimary else colors.textSecondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        Text(caption, style = RecentrTheme.typography.labelSmall, color = colors.textTertiary, maxLines = 1)
    }
}

/**
 * Figma "Badge · Disc": earned = blue note disc with a 4dp white ring and a sky glow; locked =
 * sunken disc, the sticker at 28% and a white lock pill.
 */
@Composable
internal fun BadgeDisc(achievement: Achievement, earned: Boolean, size: Dp = 84.dp, sticker: Dp = 60.dp) {
    val colors = RecentrTheme.colors
    Box(Modifier.size(size)) {
        Box(
            Modifier
                .size(size)
                .then(if (earned) Modifier.recentrShadow(EarnedGlow, CircleShape) else Modifier)
                .clip(CircleShape)
                .background(if (earned) colors.accentNoteBlue else colors.bgSunken)
                .then(if (earned) Modifier.border(4.dp, colors.bgSurface, CircleShape) else Modifier),
            contentAlignment = Alignment.Center,
        ) {
            StickerImage(achievement.copy.sticker, size = sticker, modifier = Modifier.graphicsLayer { alpha = if (earned) 1f else 0.28f })
        }
        if (!earned) {
            Box(
                Modifier
                    .offset(x = size * (58f / 84f), y = size * (58f / 84f))
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(colors.bgSurface),
                contentAlignment = Alignment.Center,
            ) {
                RecentrIconImage(RecentrIcon.Lock, tint = colors.textSecondary, size = 16.dp)
            }
        }
    }
}

@Preview(widthDp = 402, heightDp = 874)
@Composable
private fun AchievementsPreview() {
    val today = LocalDate.now()
    val badges = Achievement.entries.mapIndexed { index, achievement ->
        AchievementProgress(achievement, if (index < 6) achievement.target else index % achievement.target, if (index < 6) today.minusDays(index * 2L) else null)
    }
    RecentrTheme {
        AchievementsScreen(
            snapshot = AchievementsSnapshot(
                badges = badges,
                level = CalmLevel.of(640),
                streak = StreakState(12, emptyMap(), 2, null),
                bestStreak = 12,
                uncelebrated = emptyList(),
                days = emptyList(),
                goalMinutes = 150,
                startedOn = today.minusDays(23),
                firstLockApps = emptyList(),
            ),
            onBack = {},
            onOpenBadge = {},
            onOpenMilestones = {},
        )
    }
}
