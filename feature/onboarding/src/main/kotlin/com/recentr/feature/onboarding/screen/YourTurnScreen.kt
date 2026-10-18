package com.recentr.feature.onboarding.screen

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.recentr.core.designsystem.component.RecentrButton
import com.recentr.core.designsystem.illustration.Mascot
import com.recentr.core.designsystem.illustration.MascotMood
import com.recentr.core.designsystem.theme.RecentrTheme
import com.recentr.feature.onboarding.R
import com.recentr.feature.onboarding.component.HeaderProgress
import com.recentr.feature.onboarding.component.OnboardingHeading
import com.recentr.feature.onboarding.component.OnboardingPage

/** 08 · Onboarding — Your turn (Figma 2023:841). */
@Composable
internal fun YourTurnScreen(
    progress: HeaderProgress,
    onBack: () -> Unit,
    onContinue: () -> Unit,
) {
    OnboardingPage(
        progress = progress,
        onBack = onBack,
        bottomBar = { RecentrButton(stringResource(R.string.onb_your_turn_cta), onContinue, Modifier.fillMaxWidth()) },
    ) {
        Spacer(Modifier.height(110.dp))
        Mascot(MascotMood.Calm, width = 192.dp)
        Spacer(Modifier.height(40.dp))
        OnboardingHeading(
            overline = stringResource(R.string.onb_your_turn_overline),
            title = stringResource(R.string.onb_your_turn_title),
            centered = true,
            spacing = 6.dp,
        )
    }
}

@Preview(widthDp = 402, heightDp = 874)
@Composable
private fun YourTurnScreenPreview() {
    RecentrTheme { YourTurnScreen(HeaderProgress(0.35f), onBack = {}, onContinue = {}) }
}
