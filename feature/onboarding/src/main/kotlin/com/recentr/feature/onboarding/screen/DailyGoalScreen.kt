package com.recentr.feature.onboarding.screen

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.recentr.core.designsystem.component.RecentrButton
import com.recentr.core.designsystem.component.RecentrChip
import com.recentr.core.designsystem.component.RecentrChipStyle
import com.recentr.core.designsystem.component.RecentrIconButton
import com.recentr.core.designsystem.component.RecentrIconButtonStyle
import com.recentr.core.designsystem.icon.RecentrIcon
import com.recentr.core.designsystem.icon.RecentrIconImage
import com.recentr.core.designsystem.theme.RecentrElevation
import com.recentr.core.designsystem.theme.RecentrShapes
import com.recentr.core.designsystem.theme.RecentrTheme
import com.recentr.core.designsystem.theme.recentrShadow
import com.recentr.feature.onboarding.OnboardingUiState
import com.recentr.feature.onboarding.R
import com.recentr.feature.onboarding.UsageBucket
import com.recentr.feature.onboarding.component.HeaderProgress
import com.recentr.feature.onboarding.component.OnboardingHeading
import com.recentr.feature.onboarding.component.OnboardingPage
import com.recentr.feature.onboarding.component.compactDuration
import com.recentr.feature.onboarding.component.figmaLineBox

/** 12 · Onboarding — Daily goal (Figma 2023:1150). 10-minute stepper, 30m … 12h. */
@Composable
internal fun DailyGoalScreen(
    progress: HeaderProgress,
    state: OnboardingUiState,
    onDecrease: () -> Unit,
    onIncrease: () -> Unit,
    onBack: () -> Unit,
    onContinue: () -> Unit,
) {
    val colors = RecentrTheme.colors
    OnboardingPage(
        progress = progress,
        onBack = onBack,
        bottomBar = { RecentrButton(stringResource(R.string.onb_continue), onContinue, Modifier.fillMaxWidth()) },
    ) {
        Spacer(Modifier.height(20.dp))
        OnboardingHeading(
            title = stringResource(R.string.onb_goal_title),
            subtitle = stringResource(R.string.onb_goal_subtitle, compactDuration(state.averageMinutes)),
        )
        Spacer(Modifier.height(58.dp))
        GoalCard(state, onDecrease, onIncrease)
        Spacer(Modifier.height(20.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            RecentrIconImage(RecentrIcon.Shield, tint = colors.textTertiary, size = 18.dp)
            Text(stringResource(R.string.onb_goal_note), style = RecentrTheme.typography.captionRegular, color = colors.textTertiary)
        }
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun GoalCard(state: OnboardingUiState, onDecrease: () -> Unit, onIncrease: () -> Unit) {
    val colors = RecentrTheme.colors
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = RecentrTheme.spacing.screenGutter)
            .recentrShadow(RecentrElevation.float, RecentrShapes.card)
            .clip(RecentrShapes.card)
            .background(colors.bgSurface)
            .padding(horizontal = 20.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        val tone = state.goalTone
        RecentrChip(label = stringResource(tone.label), style = RecentrChipStyle.Selected, icon = tone.icon)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RecentrIconButton(
                icon = RecentrIcon.Minus,
                contentDescription = stringResource(R.string.onb_goal_decrease),
                onClick = onDecrease,
                style = RecentrIconButtonStyle.Sunken,
                size = 48.dp,
                enabled = state.canDecreaseGoal,
            )
            val goal = state.dailyGoalMinutes
            AnimatedContent(
                targetState = goal,
                transitionSpec = {
                    val up = targetState > initialState
                    (slideInVertically { if (up) it / 3 else -it / 3 } + fadeIn()) togetherWith
                        (slideOutVertically { if (up) -it / 3 else it / 3 } + fadeOut())
                },
                contentAlignment = Alignment.Center,
                label = "goalValue",
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
            ) { minutes ->
                Text(
                    text = compactDuration(minutes),
                    style = RecentrTheme.typography.displayHero,
                    color = colors.textPrimary,
                    maxLines = 1,
                    modifier = Modifier.figmaLineBox(72.dp),
                )
            }
            RecentrIconButton(
                icon = RecentrIcon.Plus,
                contentDescription = stringResource(R.string.onb_goal_increase),
                onClick = onIncrease,
                style = RecentrIconButtonStyle.Sunken,
                size = 48.dp,
                enabled = state.canIncreaseGoal,
            )
        }
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

@Composable
private fun CompareRow(label: String, fraction: Float, value: String, fill: Color) {
    val colors = RecentrTheme.colors
    val animated by animateFloatAsState(fraction.coerceIn(0f, 1f), label = "compareBar")
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
            onDecrease = {},
            onIncrease = {},
            onBack = {},
            onContinue = {},
        )
    }
}
