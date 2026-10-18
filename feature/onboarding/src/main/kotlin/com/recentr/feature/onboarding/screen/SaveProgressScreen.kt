package com.recentr.feature.onboarding.screen

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.recentr.core.designsystem.component.RecentrButton
import com.recentr.core.designsystem.component.RecentrButtonStyle
import com.recentr.core.designsystem.illustration.Sticker
import com.recentr.core.designsystem.illustration.StickerImage
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
            RecentrButton(stringResource(R.string.onb_save_email), onContinueWithEmail, Modifier.fillMaxWidth())
            RecentrButton(
                text = stringResource(R.string.onb_save_phone),
                onClick = onContinueWithPhone,
                modifier = Modifier.fillMaxWidth(),
                style = RecentrButtonStyle.Secondary,
            )
            Text(
                text = stringResource(R.string.onb_save_terms),
                style = RecentrTheme.typography.captionRegular,
                color = colors.textTertiary,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .padding(top = 8.dp)
                    .width(300.dp),
            )
        },
    ) {
        Spacer(Modifier.height(67.dp))
        StickerImage(
            sticker = Sticker.Note,
            size = 144.dp,
            modifier = Modifier
                .offset(x = (-8).dp)
                .rotate(6f),
        )
        Spacer(Modifier.height(29.dp))
        OnboardingHeading(
            title = stringResource(R.string.onb_save_title),
            subtitle = stringResource(R.string.onb_save_subtitle),
            centered = true,
        )
    }
}

@Preview(widthDp = 402, heightDp = 874)
@Composable
private fun SaveProgressScreenPreview() {
    RecentrTheme { SaveProgressScreen(HeaderProgress(1f), onBack = {}, onContinueWithEmail = {}, onContinueWithPhone = {}) }
}
