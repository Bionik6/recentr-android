package com.recentr.feature.onboarding.screen

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import com.recentr.core.designsystem.component.RecentrButton
import com.recentr.core.designsystem.illustration.Mascot
import com.recentr.core.designsystem.illustration.MascotMood
import com.recentr.core.designsystem.illustration.Sticker
import com.recentr.core.designsystem.illustration.StickerImage
import com.recentr.core.designsystem.motion.LocalReduceMotion
import com.recentr.core.designsystem.motion.entrance
import com.recentr.core.designsystem.motion.motionSpring
import com.recentr.core.designsystem.motion.rememberPingPong
import com.recentr.core.designsystem.theme.CssLinearGradient
import com.recentr.core.designsystem.theme.RecentrElevation
import com.recentr.core.designsystem.theme.RecentrShapes
import com.recentr.core.designsystem.theme.RecentrTheme
import com.recentr.core.designsystem.theme.recentrShadow
import com.recentr.feature.onboarding.R
import com.recentr.feature.onboarding.component.HeaderProgress
import com.recentr.feature.onboarding.component.OnboardingHeading
import com.recentr.feature.onboarding.component.OnboardingPage
import kotlinx.coroutines.delay

/** Shield gradient of this illustration (Figma "Shield" fill, 134.5° variant of Gradients/Shield). */
private val ShieldGradient = CssLinearGradient(
    134.49f,
    listOf(0f to Color(0xFF1F5CDB), 0.71429f to Color(0xFF0D1740)),
)

/** 06 · Onboarding — A little friction (Figma 2023:671). */
@Composable
internal fun FrictionScreen(
    progress: HeaderProgress,
    onBack: () -> Unit,
    onContinue: () -> Unit,
) {
    OnboardingPage(
        progress = progress,
        onBack = onBack,
        bottomBar = {
            RecentrButton(stringResource(R.string.onb_continue), onContinue, Modifier.fillMaxWidth().entrance(500))
        },
    ) {
        Spacer(Modifier.height(20.dp))
        OnboardingHeading(title = stringResource(R.string.onb_friction_title))
        Spacer(Modifier.height(64.dp))
        ShieldPreview()
    }
}

/**
 * A tilted preview of the shield screen shown before a blocked app opens, with the mascot. It
 * flips up from the bottom edge; the padlock wiggles now and then and the button breathes.
 */
@Composable
private fun ShieldPreview() {
    val colors = RecentrTheme.colors
    val shape = RoundedCornerShape(44.dp)
    val reduceMotion = LocalReduceMotion.current
    val flip = remember { Animatable(if (reduceMotion) 1f else 0f) }
    LaunchedEffect(Unit) {
        delay(200)
        flip.animateTo(1f, motionSpring(0.9f, 0.35f))
    }
    Box(Modifier.size(width = 354.dp, height = 420.dp)) {
        Column(
            modifier = Modifier
                .offset(x = 26.9.dp, y = 27.dp)
                .graphicsLayer {
                    val value = flip.value
                    alpha = value.coerceIn(0f, 1f)
                    rotationX = 75f * (1f - value)
                    transformOrigin = TransformOrigin(0.5f, 1f)
                    cameraDistance = 6f * density
                    compositingStrategy = CompositingStrategy.ModulateAlpha
                }
                .rotate(3f)
                .size(width = 280.dp, height = 380.dp)
                .recentrShadow(RecentrElevation.float, shape)
                .clip(shape)
                .background(ShieldGradient)
                .padding(horizontal = 28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            WigglingLock()
            Text(
                text = stringResource(R.string.onb_friction_shield_message),
                style = RecentrTheme.typography.title2,
                color = colors.textOnDark,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
            val breath by rememberPingPong(from = 1f, to = 1.04f, durationMillis = 1100)
            Box(
                modifier = Modifier
                    .graphicsLayer {
                        scaleX = breath
                        scaleY = breath
                    }
                    .fillMaxWidth()
                    .clip(RecentrShapes.full)
                    .background(colors.bgSurface)
                    .padding(vertical = 14.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(R.string.onb_friction_shield_primary),
                    style = RecentrTheme.typography.headline,
                    color = colors.textPrimary,
                )
            }
            Text(
                text = stringResource(R.string.onb_friction_shield_secondary),
                style = RecentrTheme.typography.captionMedium,
                color = colors.textOnDarkMuted,
            )
        }
        Mascot(
            mood = MascotMood.Calm,
            width = 88.dp,
            modifier = Modifier
                .offset(x = 258.dp, y = 300.dp)
                .entrance(750, from = DpOffset(60.dp, 0.dp)),
        )
    }
}

/** The padlock rests, then shakes from its top for a moment, on a loop (iOS keyframes). */
@Composable
private fun WigglingLock() {
    // Read while drawing, so the lock isn't recomposed every frame.
    val angle = if (LocalReduceMotion.current) {
        rememberUpdatedState(0f)
    } else {
        val transition = rememberInfiniteTransition(label = "lockWiggle")
        transition.animateFloat(
            initialValue = 0f,
            targetValue = 0f,
            animationSpec = infiniteRepeatable(
                keyframes {
                    durationMillis = 2260
                    0f at 1600
                    -9f at 1720 using FastOutSlowInEasing
                    8f at 1840 using FastOutSlowInEasing
                    -5f at 1960 using FastOutSlowInEasing
                    0f at 2260 using FastOutSlowInEasing
                },
            ),
            label = "lockAngle",
        )
    }
    StickerImage(
        sticker = Sticker.Lock,
        size = 108.dp,
        modifier = Modifier.graphicsLayer {
            rotationZ = angle.value
            transformOrigin = TransformOrigin(0.5f, 0f)
        },
    )
}

@Preview(widthDp = 402, heightDp = 874)
@Composable
private fun FrictionScreenPreview() {
    RecentrTheme { FrictionScreen(HeaderProgress(0.1f), onBack = {}, onContinue = {}) }
}
