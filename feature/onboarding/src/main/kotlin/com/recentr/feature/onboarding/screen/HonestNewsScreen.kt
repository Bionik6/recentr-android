package com.recentr.feature.onboarding.screen

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import com.recentr.core.designsystem.component.RecentrButton
import com.recentr.core.designsystem.illustration.Mascot
import com.recentr.core.designsystem.illustration.MascotMood
import com.recentr.core.designsystem.motion.entrance
import com.recentr.core.designsystem.theme.RecentrTheme
import com.recentr.feature.onboarding.R
import com.recentr.feature.onboarding.component.OnboardingHeading
import com.recentr.feature.onboarding.component.OnboardingPage

/** 14 · Onboarding — Honest news (Figma 2024:1058). No header in the design; system back works. */
@Composable
internal fun HonestNewsScreen(onContinue: () -> Unit) {
    OnboardingPage(
        bottomBar = {
            RecentrButton(stringResource(R.string.onb_news_cta), onContinue, Modifier.fillMaxWidth().entrance(600))
        },
    ) {
        Spacer(Modifier.height(136.dp))
        Mascot(MascotMood.Impatient, width = 200.dp, modifier = Modifier.entrance(100, from = DpOffset(0.dp, 40.dp)))
        Spacer(Modifier.height(30.dp))
        OnboardingHeading(
            overline = stringResource(R.string.onb_news_overline),
            title = stringResource(R.string.onb_news_title),
            centered = true,
            delayMillis = 350,
        )
    }
}

@Preview(widthDp = 402, heightDp = 874)
@Composable
private fun HonestNewsScreenPreview() {
    RecentrTheme { HonestNewsScreen(onContinue = {}) }
}
