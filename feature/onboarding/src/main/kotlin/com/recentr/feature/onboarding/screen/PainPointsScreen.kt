package com.recentr.feature.onboarding.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.recentr.core.designsystem.component.OptionRow
import com.recentr.core.designsystem.component.OptionTrailing
import com.recentr.core.designsystem.theme.RecentrTheme
import com.recentr.feature.onboarding.PainPoint
import com.recentr.feature.onboarding.R
import com.recentr.feature.onboarding.component.ContinueButton
import com.recentr.feature.onboarding.component.HeaderProgress
import com.recentr.feature.onboarding.component.OnboardingHeading
import com.recentr.feature.onboarding.component.OnboardingPage

/** 10 · Onboarding — What's wrong (Figma 2023:1002). Multi-select; Continue needs one answer. */
@Composable
internal fun PainPointsScreen(
    progress: HeaderProgress,
    selected: Set<PainPoint>,
    onToggle: (PainPoint) -> Unit,
    onBack: () -> Unit,
    onContinue: () -> Unit,
) {
    OnboardingPage(
        progress = progress,
        onBack = onBack,
        bottomBar = { ContinueButton(enabled = selected.isNotEmpty(), onClick = onContinue) },
    ) {
        Spacer(Modifier.height(20.dp))
        OnboardingHeading(
            title = stringResource(R.string.onb_pain_title),
            subtitle = stringResource(R.string.onb_pain_subtitle),
        )
        Spacer(Modifier.height(25.dp))
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = RecentrTheme.spacing.screenGutter),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            PainPoint.entries.forEach { point ->
                OptionRow(
                    label = stringResource(point.label),
                    selected = point in selected,
                    onClick = { onToggle(point) },
                    icon = point.icon,
                    trailing = OptionTrailing.Checkbox,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
        Spacer(Modifier.height(16.dp))
    }
}

@Preview(widthDp = 402, heightDp = 874)
@Composable
private fun PainPointsScreenPreview() {
    RecentrTheme {
        PainPointsScreen(
            HeaderProgress(0.65f),
            selected = setOf(PainPoint.Sleep, PainPoint.Scroll),
            onToggle = {},
            onBack = {},
            onContinue = {},
        )
    }
}
