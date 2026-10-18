package com.recentr.feature.onboarding.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.recentr.core.designsystem.component.OptionRow
import com.recentr.core.designsystem.component.OptionTrailing
import com.recentr.core.designsystem.motion.entrance
import com.recentr.core.designsystem.theme.RecentrTheme
import com.recentr.feature.onboarding.R
import com.recentr.feature.onboarding.UsageBucket
import com.recentr.feature.onboarding.component.ContinueButton
import com.recentr.feature.onboarding.component.HeaderProgress
import com.recentr.feature.onboarding.component.OnboardingHeading
import com.recentr.feature.onboarding.component.OnboardingPage

/** 11 · Onboarding — Time spent (Figma 2023:1084). Single choice; Continue needs an answer. */
@Composable
internal fun TimeSpentScreen(
    progress: HeaderProgress,
    selected: UsageBucket?,
    onSelect: (UsageBucket) -> Unit,
    onBack: () -> Unit,
    onContinue: () -> Unit,
) {
    OnboardingPage(
        progress = progress,
        onBack = onBack,
        bottomBar = { ContinueButton(enabled = selected != null, onClick = onContinue) },
    ) {
        Spacer(Modifier.height(20.dp))
        OnboardingHeading(
            title = stringResource(R.string.onb_usage_title),
            subtitle = stringResource(R.string.onb_usage_subtitle),
        )
        Spacer(Modifier.height(25.dp))
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = RecentrTheme.spacing.screenGutter)
                .selectableGroup(),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            UsageBucket.entries.forEachIndexed { index, bucket ->
                OptionRow(
                    label = stringResource(bucket.label),
                    selected = bucket == selected,
                    onClick = { onSelect(bucket) },
                    trailing = OptionTrailing.Radio,
                    modifier = Modifier
                        .fillMaxWidth()
                        .entrance(200 + index * 60),
                )
            }
        }
        Spacer(Modifier.height(16.dp))
    }
}

@Preview(widthDp = 402, heightDp = 874)
@Composable
private fun TimeSpentScreenPreview() {
    RecentrTheme {
        TimeSpentScreen(HeaderProgress(0.8f), selected = UsageBucket.From5To7, onSelect = {}, onBack = {}, onContinue = {})
    }
}
