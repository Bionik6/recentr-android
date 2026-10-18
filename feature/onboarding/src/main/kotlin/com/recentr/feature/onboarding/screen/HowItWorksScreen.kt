package com.recentr.feature.onboarding.screen

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import com.recentr.core.designsystem.component.RecentrButton
import com.recentr.core.designsystem.illustration.Sticker
import com.recentr.core.designsystem.illustration.StickerImage
import com.recentr.core.designsystem.motion.entrance
import com.recentr.core.designsystem.motion.rememberPingPong
import com.recentr.core.designsystem.theme.RecentrElevation
import com.recentr.core.designsystem.theme.RecentrShapes
import com.recentr.core.designsystem.theme.RecentrTheme
import com.recentr.core.designsystem.theme.recentrShadow
import com.recentr.feature.onboarding.R
import com.recentr.feature.onboarding.component.HeaderProgress
import com.recentr.feature.onboarding.component.OnboardingHeading
import com.recentr.feature.onboarding.component.OnboardingPage

private data class HowStep(val sticker: Sticker, @param:StringRes val title: Int, @param:StringRes val body: Int)

private val HowSteps = listOf(
    HowStep(Sticker.Lock, R.string.onb_how_step1_title, R.string.onb_how_step1_body),
    HowStep(Sticker.Timer, R.string.onb_how_step2_title, R.string.onb_how_step2_body),
    HowStep(Sticker.Flame, R.string.onb_how_step3_title, R.string.onb_how_step3_body),
)

/** 17 · Onboarding — How it works (Figma 2024:1254). */
@Composable
internal fun HowItWorksScreen(
    progress: HeaderProgress,
    onBack: () -> Unit,
    onContinue: () -> Unit,
) {
    OnboardingPage(
        progress = progress,
        onBack = onBack,
        bottomBar = {
            RecentrButton(stringResource(R.string.onb_continue), onContinue, Modifier.fillMaxWidth().entrance(600))
        },
    ) {
        Spacer(Modifier.height(20.dp))
        OnboardingHeading(title = stringResource(R.string.onb_how_title))
        Spacer(Modifier.height(37.dp))
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = RecentrTheme.spacing.screenGutter),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            HowSteps.forEachIndexed { index, step ->
                StepCard(index + 1, step, Modifier.entrance(150 + index * 150, from = DpOffset(0.dp, 40.dp), blur = false))
            }
        }
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun StepCard(number: Int, step: HowStep, modifier: Modifier = Modifier) {
    val colors = RecentrTheme.colors
    val index = number - 1
    // Each sticker pops in after its card, then bobs gently, one after the other.
    val bob by rememberPingPong(from = 0f, to = -3f, durationMillis = 1600, delayMillis = index * 300)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 108.dp)
            .recentrShadow(RecentrElevation.card, RecentrShapes.xl)
            .clip(RecentrShapes.xl)
            .background(colors.bgSurface)
            .semantics(mergeDescendants = true) {}
            .padding(start = 16.dp, end = 18.dp, top = 16.dp, bottom = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        StickerImage(
            sticker = step.sticker,
            size = 60.dp,
            modifier = Modifier
                .entrance(300 + index * 150, from = DpOffset.Zero)
                .graphicsLayer { translationY = bob.dp.toPx() },
        )
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(stringResource(R.string.onb_how_step, number), style = RecentrTheme.typography.labelSmall, color = colors.brandSky)
            Text(stringResource(step.title), style = RecentrTheme.typography.headline, color = colors.textPrimary)
            Text(stringResource(step.body), style = RecentrTheme.typography.captionRegular, color = colors.textSecondary)
        }
    }
}

@Preview(widthDp = 402, heightDp = 874)
@Composable
private fun HowItWorksScreenPreview() {
    RecentrTheme { HowItWorksScreen(HeaderProgress(0.9f), onBack = {}, onContinue = {}) }
}
