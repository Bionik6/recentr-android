package com.recentr.feature.onboarding.screen

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import com.recentr.core.designsystem.component.RecentrButton
import com.recentr.core.designsystem.illustration.Mascot
import com.recentr.core.designsystem.illustration.MascotMood
import com.recentr.core.designsystem.motion.entrance
import com.recentr.core.designsystem.motion.floating
import com.recentr.core.designsystem.motion.rememberPingPong
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
        bottomBar = {
            RecentrButton(stringResource(R.string.onb_your_turn_cta), onContinue, Modifier.fillMaxWidth().entrance(600))
        },
    ) {
        Spacer(Modifier.height(110.dp))
        // The mascot drops in over a soft blue halo that breathes (iOS: blurred aurora circle).
        val halo by rememberPingPong(from = 0.7f, to = 1f, durationMillis = 2400)
        val auroraBlue = RecentrTheme.colors.auroraBlue
        Mascot(
            mood = MascotMood.Calm,
            width = 192.dp,
            modifier = Modifier
                .entrance(100, from = DpOffset(0.dp, (-160).dp))
                .drawBehind {
                    val radius = 150.dp.toPx() * halo
                    drawCircle(
                        brush = Brush.radialGradient(
                            0f to auroraBlue,
                            0.45f to auroraBlue,
                            1f to auroraBlue.copy(alpha = 0f),
                            center = center,
                            radius = radius,
                        ),
                        radius = radius,
                        alpha = halo,
                    )
                }
                .floating(amplitude = 5.dp, periodMillis = 3600),
        )
        Spacer(Modifier.height(40.dp))
        OnboardingHeading(
            overline = stringResource(R.string.onb_your_turn_overline),
            title = stringResource(R.string.onb_your_turn_title),
            centered = true,
            spacing = 6.dp,
            delayMillis = 350,
        )
    }
}

@Preview(widthDp = 402, heightDp = 874)
@Composable
private fun YourTurnScreenPreview() {
    RecentrTheme { YourTurnScreen(HeaderProgress(0.35f), onBack = {}, onContinue = {}) }
}
