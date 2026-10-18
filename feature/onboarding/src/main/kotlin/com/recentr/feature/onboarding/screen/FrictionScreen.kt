package com.recentr.feature.onboarding.screen

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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.recentr.core.designsystem.component.RecentrButton
import com.recentr.core.designsystem.illustration.Mascot
import com.recentr.core.designsystem.illustration.MascotMood
import com.recentr.core.designsystem.illustration.Sticker
import com.recentr.core.designsystem.illustration.StickerImage
import com.recentr.core.designsystem.theme.CssLinearGradient
import com.recentr.core.designsystem.theme.RecentrElevation
import com.recentr.core.designsystem.theme.RecentrShapes
import com.recentr.core.designsystem.theme.RecentrTheme
import com.recentr.core.designsystem.theme.recentrShadow
import com.recentr.feature.onboarding.R
import com.recentr.feature.onboarding.component.HeaderProgress
import com.recentr.feature.onboarding.component.OnboardingPage

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
        bottomBar = { RecentrButton(stringResource(R.string.onb_continue), onContinue, Modifier.fillMaxWidth()) },
    ) {
        Spacer(Modifier.height(20.dp))
        Text(
            text = stringResource(R.string.onb_friction_title),
            style = RecentrTheme.typography.title1,
            color = RecentrTheme.colors.textPrimary,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = RecentrTheme.spacing.screenGutter),
        )
        Spacer(Modifier.height(64.dp))
        ShieldPreview()
    }
}

/** A tilted preview of the shield screen shown before a blocked app opens, with the mascot. */
@Composable
private fun ShieldPreview() {
    val colors = RecentrTheme.colors
    val shape = RoundedCornerShape(44.dp)
    Box(Modifier.size(width = 354.dp, height = 420.dp)) {
        Column(
            modifier = Modifier
                .offset(x = 26.9.dp, y = 27.dp)
                .rotate(3f)
                .size(width = 280.dp, height = 380.dp)
                .recentrShadow(RecentrElevation.float, shape)
                .clip(shape)
                .background(ShieldGradient)
                .padding(horizontal = 28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            StickerImage(Sticker.Lock, size = 108.dp)
            Text(
                text = stringResource(R.string.onb_friction_shield_message),
                style = RecentrTheme.typography.title2,
                color = colors.textOnDark,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
            Box(
                modifier = Modifier
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
        Mascot(MascotMood.Calm, width = 88.dp, modifier = Modifier.offset(x = 258.dp, y = 300.dp))
    }
}

@Preview(widthDp = 402, heightDp = 874)
@Composable
private fun FrictionScreenPreview() {
    RecentrTheme { FrictionScreen(HeaderProgress(0.1f), onBack = {}, onContinue = {}) }
}
