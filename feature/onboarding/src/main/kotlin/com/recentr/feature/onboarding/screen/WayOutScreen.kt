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
import com.recentr.core.designsystem.illustration.Backdrop
import com.recentr.core.designsystem.illustration.Mascot
import com.recentr.core.designsystem.illustration.MascotMood
import com.recentr.core.designsystem.theme.RecentrTheme
import com.recentr.feature.onboarding.R
import com.recentr.feature.onboarding.component.OnboardingHeading
import com.recentr.feature.onboarding.component.OnboardingPage
import com.recentr.feature.onboarding.component.spokenDuration

/**
 * 16 · Onboarding — A way out (Figma 2024:1199). [winBackMinutes] = average − goal
 * (6h − 4h20 → "2 hours" in the design).
 */
@Composable
internal fun WayOutScreen(winBackMinutes: Int, onContinue: () -> Unit) {
    OnboardingPage(
        backdrop = Backdrop.Sky,
        bottomBar = { RecentrButton(stringResource(R.string.onb_way_out_cta), onContinue, Modifier.fillMaxWidth()) },
    ) {
        Spacer(Modifier.height(116.dp))
        Mascot(MascotMood.Calm, width = 200.dp)
        Spacer(Modifier.height(80.dp))
        OnboardingHeading(
            title = stringResource(R.string.onb_way_out_title, spokenDuration(winBackMinutes)),
            subtitle = stringResource(R.string.onb_way_out_subtitle),
            centered = true,
        )
    }
}

@Preview(widthDp = 402, heightDp = 874)
@Composable
private fun WayOutScreenPreview() {
    RecentrTheme { WayOutScreen(winBackMinutes = 100, onContinue = {}) }
}
