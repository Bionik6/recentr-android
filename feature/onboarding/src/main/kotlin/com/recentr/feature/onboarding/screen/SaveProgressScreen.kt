package com.recentr.feature.onboarding.screen

import androidx.compose.animation.core.EaseInOut
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import com.recentr.core.designsystem.component.RecentrButton
import com.recentr.core.designsystem.component.RecentrButtonStyle
import com.recentr.core.designsystem.illustration.Sticker
import com.recentr.core.designsystem.illustration.StickerImage
import com.recentr.core.designsystem.motion.LocalReduceMotion
import com.recentr.core.designsystem.motion.entrance
import com.recentr.core.designsystem.motion.floating
import com.recentr.core.designsystem.theme.RecentrTheme
import com.recentr.feature.onboarding.R
import com.recentr.feature.onboarding.component.HeaderProgress
import com.recentr.feature.onboarding.component.OnboardingHeading
import com.recentr.feature.onboarding.component.OnboardingPage

/**
 * 20 · Onboarding — Save progress (Figma 2024:1470). There is no account backend yet: both
 * buttons simply continue to the last screen.
 */
@Composable
internal fun SaveProgressScreen(
    progress: HeaderProgress,
    onBack: () -> Unit,
    onContinueWithEmail: () -> Unit,
    onContinueWithPhone: () -> Unit,
) {
    val colors = RecentrTheme.colors
    OnboardingPage(
        progress = progress,
        onBack = onBack,
        bottomGap = 24.dp,
        bottomBar = {
            RecentrButton(stringResource(R.string.onb_save_email), onContinueWithEmail, Modifier.fillMaxWidth().entrance(500))
            RecentrButton(
                text = stringResource(R.string.onb_save_phone),
                onClick = onContinueWithPhone,
                modifier = Modifier
                    .fillMaxWidth()
                    .entrance(600, blur = false),
                style = RecentrButtonStyle.Secondary,
            )
            Text(
                text = stringResource(R.string.onb_save_terms),
                style = RecentrTheme.typography.captionRegular,
                color = colors.textTertiary,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .padding(top = 8.dp)
                    .width(300.dp)
                    .entrance(700),
            )
        },
    ) {
        Spacer(Modifier.height(67.dp))
        val sway = rememberNoteSway()
        StickerImage(
            sticker = Sticker.Note,
            size = 144.dp,
            modifier = Modifier
                .offset(x = (-8).dp)
                .entrance(100, from = DpOffset(0.dp, (-50).dp))
                .floating(amplitude = 4.dp, periodMillis = 4200)
                .graphicsLayer { rotationZ = sway.value },
        )
        Spacer(Modifier.height(29.dp))
        OnboardingHeading(
            title = stringResource(R.string.onb_save_title),
            subtitle = stringResource(R.string.onb_save_subtitle),
            centered = true,
            delayMillis = 300,
        )
    }
}

/**
 * The note's angle: it sways between 2° and 10° while it floats (iOS keyframes 6 → 10 → 2 → 6°),
 * 6° when motion is reduced. Read it while drawing, so the page isn't recomposed every frame.
 */
@Composable
private fun rememberNoteSway(): State<Float> {
    if (LocalReduceMotion.current) return rememberUpdatedState(6f)
    return rememberInfiniteTransition(label = "noteSway").animateFloat(
        initialValue = 6f,
        targetValue = 6f,
        animationSpec = infiniteRepeatable(
            keyframes {
                durationMillis = 4200
                // `using` eases the segment that starts at the keyframe.
                6f at 0 using EaseInOut
                10f at 1400 using EaseInOut
                2f at 3000 using EaseInOut
            },
        ),
        label = "noteAngle",
    )
}

@Preview(widthDp = 402, heightDp = 874)
@Composable
private fun SaveProgressScreenPreview() {
    RecentrTheme { SaveProgressScreen(HeaderProgress(1f), onBack = {}, onContinueWithEmail = {}, onContinueWithPhone = {}) }
}
