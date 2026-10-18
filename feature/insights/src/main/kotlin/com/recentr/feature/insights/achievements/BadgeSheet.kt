package com.recentr.feature.insights.achievements

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.recentr.core.designsystem.component.RecentrButton
import com.recentr.core.designsystem.component.RecentrSheet
import com.recentr.core.designsystem.icon.RecentrIcon
import com.recentr.core.designsystem.icon.RecentrIconImage
import com.recentr.core.designsystem.illustration.StickerImage
import com.recentr.core.designsystem.theme.RecentrTheme
import com.recentr.core.model.Achievement
import com.recentr.core.model.AchievementProgress
import com.recentr.feature.insights.R
import com.recentr.feature.insights.ui.BadgeUnit
import com.recentr.feature.insights.ui.IconChip
import com.recentr.feature.insights.ui.ProgressTrack
import com.recentr.feature.insights.ui.copy
import com.recentr.feature.insights.ui.longProgress

/** Targets up to this many steps are drawn as dots ("5 of 7 days"), longer ones as a bar. */
private const val MAX_DOTS = 10

/**
 * 107 · Achievements — Badge detail: a locked badge, what it takes, the calm points it is worth
 * and the progress so far (one dot per day, or a bar for hours and urges).
 */
@Composable
internal fun BadgeSheet(badge: AchievementProgress, onDismissRequest: () -> Unit) {
    RecentrSheet(onDismissRequest = onDismissRequest) { controller ->
        BadgeSheetContent(badge, onGotIt = { controller.close() })
    }
}

@Composable
private fun BadgeSheetContent(badge: AchievementProgress, onGotIt: () -> Unit) {
    val colors = RecentrTheme.colors
    val copy = badge.achievement.copy
    Column(
        Modifier.fillMaxWidth().padding(start = 24.dp, end = 24.dp, top = 1.dp, bottom = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(Modifier.size(120.dp).clip(CircleShape).background(colors.bgSunken), contentAlignment = Alignment.Center) {
            StickerImage(copy.sticker, size = 84.dp)
        }
        Spacer(Modifier.height(14.dp))
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(stringResource(copy.title), style = RecentrTheme.typography.title2, color = colors.textPrimary, textAlign = TextAlign.Center)
            Text(stringResource(copy.goal), style = RecentrTheme.typography.bodyRegular, color = colors.textSecondary, textAlign = TextAlign.Center)
        }
        Spacer(Modifier.height(18.dp))
        Column(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(colors.bgSunken).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(badge.longProgress(), style = RecentrTheme.typography.headline, color = colors.textPrimary, modifier = Modifier.weight(1f), maxLines = 1)
                IconChip(
                    label = stringResource(R.string.achievements_points_reward, badge.achievement.points),
                    icon = RecentrIcon.StarFill,
                    iconTint = colors.accentTangerine,
                )
            }
            val target = badge.achievement.target
            when {
                copy.unit == BadgeUnit.Once -> Unit
                target <= MAX_DOTS -> ProgressDots(done = badge.progress, total = target)
                else -> ProgressTrack(badge.progress.toFloat() / target, trackColor = colors.bgSurface)
            }
        }
        Spacer(Modifier.height(18.dp))
        RecentrButton(text = stringResource(R.string.achievements_got_it), onClick = onGotIt, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(6.dp))
    }
}

/** Figma "Days": green check discs for the steps done, dashed rings for the rest. */
@Composable
private fun ProgressDots(done: Int, total: Int) {
    val colors = RecentrTheme.colors
    Row(
        Modifier.fillMaxWidth().clearAndSetSemantics { contentDescription = "$done / $total" },
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        repeat(total) { index ->
            if (index < done) {
                Box(Modifier.size(36.dp).clip(CircleShape).background(colors.feedbackSuccess), contentAlignment = Alignment.Center) {
                    RecentrIconImage(RecentrIcon.Check, tint = colors.textOnDark, size = 18.dp)
                }
            } else {
                Box(
                    Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(colors.bgSurface)
                        .drawBehind {
                            val stroke = 1.5.dp.toPx()
                            drawCircle(
                                color = colors.borderDashed,
                                radius = (size.minDimension - stroke) / 2f,
                                center = Offset(size.width / 2f, size.height / 2f),
                                style = Stroke(width = stroke, pathEffect = PathEffect.dashPathEffect(floatArrayOf(3.dp.toPx(), 3.dp.toPx()))),
                            )
                        },
                )
            }
        }
    }
}

@Preview(widthDp = 402)
@Composable
private fun BadgeSheetPreview() {
    RecentrTheme {
        Box(Modifier.background(RecentrTheme.colors.bgSurface)) {
            BadgeSheetContent(AchievementProgress(Achievement.GoalCrusher, 5, null), onGotIt = {})
        }
    }
}
