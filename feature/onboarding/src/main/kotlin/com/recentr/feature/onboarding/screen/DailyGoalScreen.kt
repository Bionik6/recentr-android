package com.recentr.feature.onboarding.screen

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import com.recentr.core.designsystem.component.DurationPicker
import com.recentr.core.designsystem.component.RecentrButton
import com.recentr.core.designsystem.component.RecentrChip
import com.recentr.core.designsystem.component.RecentrChipStyle
import com.recentr.core.designsystem.icon.RecentrIcon
import com.recentr.core.designsystem.icon.RecentrIconImage
import com.recentr.core.designsystem.motion.LocalReduceMotion
import com.recentr.core.designsystem.motion.MotionSpecs
import com.recentr.core.designsystem.motion.entrance
import com.recentr.core.designsystem.motion.motionSpring
import com.recentr.core.designsystem.theme.RecentrElevation
import com.recentr.core.designsystem.theme.RecentrShapes
import com.recentr.core.designsystem.theme.RecentrTheme
import com.recentr.core.designsystem.theme.recentrShadow
import com.recentr.feature.onboarding.GOAL_STEP_MINUTES
import com.recentr.feature.onboarding.MAX_GOAL_MINUTES
import com.recentr.feature.onboarding.MIN_GOAL_MINUTES
import com.recentr.feature.onboarding.OnboardingUiState
import com.recentr.feature.onboarding.R
import com.recentr.feature.onboarding.UsageBucket
import com.recentr.feature.onboarding.component.HeaderProgress
import com.recentr.feature.onboarding.component.OnboardingHeading
import com.recentr.feature.onboarding.component.OnboardingPage
import com.recentr.feature.onboarding.component.compactDuration
import kotlinx.coroutines.delay

/** 12 · Onboarding — Daily goal (Figma 2023:1150). A ruler in 10 minutes, 30m … 12h. */
@Composable
internal fun DailyGoalScreen(
    progress: HeaderProgress,
    state: OnboardingUiState,
    onGoalChange: (Int) -> Unit,
    onBack: () -> Unit,
    onContinue: () -> Unit,
) {
    val colors = RecentrTheme.colors
    OnboardingPage(
        progress = progress,
        onBack = onBack,
        bottomBar = {
            RecentrButton(stringResource(R.string.onb_continue), onContinue, Modifier.fillMaxWidth().entrance(500))
        },
    ) {
        Spacer(Modifier.height(20.dp))
        OnboardingHeading(
            title = stringResource(R.string.onb_goal_title),
            subtitle = stringResource(R.string.onb_goal_subtitle, compactDuration(state.averageMinutes)),
        )
        Spacer(Modifier.height(58.dp))
        GoalCard(state, onGoalChange, Modifier.entrance(250, from = DpOffset(0.dp, 40.dp), blur = false))
        Spacer(Modifier.height(20.dp))
        Row(
            modifier = Modifier.entrance(450),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RecentrIconImage(RecentrIcon.Shield, tint = colors.textTertiary, size = 18.dp)
            Text(stringResource(R.string.onb_goal_note), style = RecentrTheme.typography.captionRegular, color = colors.textTertiary)
        }
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun GoalCard(
    state: OnboardingUiState,
    onGoalChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = RecentrTheme.colors
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = RecentrTheme.spacing.screenGutter)
            .recentrShadow(RecentrElevation.float, RecentrShapes.card)
            .clip(RecentrShapes.card)
            .background(colors.bgSurface)
            .padding(horizontal = 20.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // The mood chip pops when the goal crosses into another mood.
        AnimatedContent(
            targetState = state.goalTone,
            transitionSpec = {
                (scaleIn(MotionSpecs.snappy(), initialScale = 0.6f) + fadeIn(MotionSpecs.smooth(0.25f))) togetherWith
                    (scaleOut(MotionSpecs.snappy(), targetScale = 0.6f) + fadeOut(MotionSpecs.smooth(0.2f)))
            },
            label = "goalTone",
        ) { tone ->
            RecentrChip(label = stringResource(tone.label), style = RecentrChipStyle.Selected, icon = tone.icon)
        }
        DurationPicker(
            value = state.dailyGoalMinutes,
            onValueChange = onGoalChange,
            range = MIN_GOAL_MINUTES..MAX_GOAL_MINUTES,
            contentDescription = stringResource(R.string.onb_goal_your_goal),
            step = GOAL_STEP_MINUTES,
            labelEvery = 60,
            format = { compactDuration(it) },
        )
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            CompareRow(
                label = stringResource(R.string.onb_goal_your_goal),
                fraction = state.goalBarFraction,
                value = compactDuration(state.dailyGoalMinutes),
                fill = colors.brandSky,
            )
            CompareRow(
                label = stringResource(R.string.onb_goal_today),
                fraction = state.averageBarFraction,
                value = compactDuration(state.averageMinutes),
                fill = colors.textTertiary,
            )
        }
    }
}

/** A comparison bar: fills in on entry (`.spring(duration: 0.9, bounce: 0.2).delay(0.5)`), then follows the goal. */
@Composable
private fun CompareRow(label: String, fraction: Float, value: String, fill: Color) {
    val colors = RecentrTheme.colors
    val reduceMotion = LocalReduceMotion.current
    val target = fraction.coerceIn(0f, 1f)
    val bar = remember { Animatable(if (reduceMotion) target else 0f) }
    val hasFilled = remember { booleanArrayOf(reduceMotion) }
    LaunchedEffect(target) {
        if (hasFilled[0]) {
            bar.animateTo(target, MotionSpecs.snappy())
        } else {
            delay(500)
            hasFilled[0] = true
            bar.animateTo(target, motionSpring(0.9f, 0.2f))
        }
    }
    val animated = bar.value.coerceAtLeast(0f)
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = RecentrTheme.typography.captionMedium, color = colors.textSecondary, maxLines = 1, modifier = Modifier.width(64.dp))
        Box(
            Modifier
                .weight(1f)
                .height(12.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(colors.bgSunken),
        ) {
            Box(
                Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(animated)
                    .clip(RoundedCornerShape(6.dp))
                    .background(fill),
            )
        }
        Text(value, style = RecentrTheme.typography.captionMedium, color = colors.textPrimary, maxLines = 1)
    }
}

@Preview(widthDp = 402, heightDp = 874)
@Composable
private fun DailyGoalScreenPreview() {
    RecentrTheme {
        DailyGoalScreen(
            HeaderProgress(0.9f),
            OnboardingUiState(usageBucket = UsageBucket.From5To7),
            onGoalChange = {},
            onBack = {},
            onContinue = {},
        )
    }
}
