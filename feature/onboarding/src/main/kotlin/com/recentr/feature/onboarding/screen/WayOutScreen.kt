package com.recentr.feature.onboarding.screen

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import com.recentr.core.designsystem.component.RecentrButton
import com.recentr.core.designsystem.icon.RecentrIcon
import com.recentr.core.designsystem.icon.RecentrIconImage
import com.recentr.core.designsystem.illustration.Backdrop
import com.recentr.core.designsystem.illustration.Mascot
import com.recentr.core.designsystem.illustration.MascotMood
import com.recentr.core.designsystem.motion.entrance
import com.recentr.core.designsystem.motion.floating
import com.recentr.core.designsystem.motion.rememberPingPong
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
        bottomBar = {
            RecentrButton(stringResource(R.string.onb_way_out_cta), onContinue, Modifier.fillMaxWidth().entrance(700))
        },
    ) {
        Spacer(Modifier.height(116.dp))
        Box(
            modifier = Modifier.entrance(100, from = DpOffset(0.dp, 120.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Sparkles()
            Mascot(MascotMood.Calm, width = 200.dp, modifier = Modifier.floating(amplitude = 6.dp, periodMillis = 4000))
        }
        Spacer(Modifier.height(80.dp))
        OnboardingHeading(
            title = stringResource(R.string.onb_way_out_title, spokenDuration(winBackMinutes)),
            subtitle = stringResource(R.string.onb_way_out_subtitle),
            centered = true,
            delayMillis = 400,
        )
    }
}

private class Sparkle(val x: Int, val y: Int, val size: Int, val delayMillis: Int)

private val SparkleStars = listOf(
    Sparkle(-110, -70, 18, 0),
    Sparkle(105, -95, 14, 400),
    Sparkle(120, 40, 20, 800),
    Sparkle(-120, 60, 12, 1200),
    Sparkle(-60, -130, 10, 600),
    Sparkle(70, 120, 12, 1000),
)

/** Little stars twinkling around the mascot, each on its own beat. */
@Composable
private fun Sparkles() {
    SparkleStars.forEach { star ->
        val twinkle by rememberPingPong(from = 0.3f, to = 1f, durationMillis = 1200, delayMillis = star.delayMillis)
        Box(
            modifier = Modifier
                .offset(x = star.x.dp, y = star.y.dp)
                .graphicsLayer {
                    val scale = 0.6f + 0.4f * twinkle
                    scaleX = scale
                    scaleY = scale
                    alpha = twinkle
                }
                .drawBehind {
                    drawCircle(
                        Brush.radialGradient(listOf(Color.White.copy(alpha = 0.7f), Color.Transparent)),
                        radius = size.minDimension,
                    )
                },
        ) {
            RecentrIconImage(RecentrIcon.Sparkle, tint = Color.White, size = (star.size * 1.2f).dp)
        }
    }
}

@Preview(widthDp = 402, heightDp = 874)
@Composable
private fun WayOutScreenPreview() {
    RecentrTheme { WayOutScreen(winBackMinutes = 100, onContinue = {}) }
}
