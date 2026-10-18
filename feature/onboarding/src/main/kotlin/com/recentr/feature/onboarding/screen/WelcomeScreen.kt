package com.recentr.feature.onboarding.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.recentr.core.designsystem.component.RecentrButton
import com.recentr.core.designsystem.component.RecentrButtonStyle
import com.recentr.core.designsystem.illustration.Backdrop
import com.recentr.core.designsystem.illustration.Mascot
import com.recentr.core.designsystem.illustration.MascotMood
import com.recentr.core.designsystem.theme.RecentrElevation
import com.recentr.core.designsystem.theme.RecentrShapes
import com.recentr.core.designsystem.theme.RecentrTheme
import com.recentr.core.designsystem.theme.recentrShadow
import com.recentr.feature.onboarding.R
import com.recentr.feature.onboarding.component.OnboardingPage

/** 05 · Onboarding — Welcome (Figma 2023:579). */
@Composable
internal fun WelcomeScreen(
    onGetStarted: () -> Unit,
    onHaveAccount: () -> Unit,
) {
    val colors = RecentrTheme.colors
    OnboardingPage(
        backdrop = Backdrop.Sky,
        bottomBar = {
            RecentrButton(stringResource(R.string.onb_welcome_get_started), onGetStarted, Modifier.fillMaxWidth())
            RecentrButton(
                text = stringResource(R.string.onb_welcome_have_account),
                onClick = onHaveAccount,
                modifier = Modifier.fillMaxWidth(),
                style = RecentrButtonStyle.Secondary,
            )
        },
    ) {
        Spacer(Modifier.height(14.dp))
        Text(
            text = stringResource(R.string.onb_wordmark),
            style = RecentrTheme.typography.brandWordmark,
            color = colors.textOnDark,
        )
        Spacer(Modifier.height(24.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            GlassStat(stringResource(R.string.onb_welcome_stat_people_value), stringResource(R.string.onb_welcome_stat_people_label))
            GlassStat(stringResource(R.string.onb_welcome_stat_time_value), stringResource(R.string.onb_welcome_stat_time_label))
        }
        Spacer(Modifier.height(45.dp))
        BeforeNowIllustration()
        Spacer(Modifier.height(58.dp))
        Column(
            modifier = Modifier.padding(horizontal = RecentrTheme.spacing.screenGutter),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = stringResource(R.string.onb_welcome_title),
                style = RecentrTheme.typography.title1,
                color = colors.textPrimary,
                textAlign = TextAlign.Center,
            )
            Text(
                text = stringResource(R.string.onb_welcome_subtitle),
                style = RecentrTheme.typography.bodyRegular,
                color = colors.textSecondary,
                textAlign = TextAlign.Center,
            )
        }
    }
}

/** Translucent "glass" stat pill over the sky (alpha/white-24 fill + border). */
@Composable
private fun GlassStat(value: String, label: String) {
    val colors = RecentrTheme.colors
    val shape = RoundedCornerShape(20.dp)
    Column(
        modifier = Modifier
            .clip(shape)
            .background(colors.alphaWhite24)
            .border(1.dp, colors.alphaWhite24, shape)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(value, style = RecentrTheme.typography.title2, color = colors.textOnDark, maxLines = 1)
        Text(label, style = RecentrTheme.typography.captionRegular, color = colors.textOnDarkMuted, maxLines = 1)
    }
}

private val BeforeBars = listOf(40, 52, 36, 58, 48, 62, 54)
private val NowBars = listOf(14, 22, 10, 18, 12, 20, 8)

/** "Before / Now" — two tilted usage cards and the calm mascot (354×260 group). */
@Composable
private fun BeforeNowIllustration() {
    val colors = RecentrTheme.colors
    Box(Modifier.size(width = 354.dp, height = 260.dp)) {
        UsageCard(
            label = stringResource(R.string.onb_welcome_before),
            value = stringResource(R.string.onb_welcome_before_value),
            accent = colors.feedbackDanger,
            bars = BeforeBars,
            modifier = Modifier.offset(x = 12.3.dp, y = 30.1.dp).rotate(-6f),
        )
        UsageCard(
            label = stringResource(R.string.onb_welcome_now),
            value = stringResource(R.string.onb_welcome_now_value),
            accent = colors.brandSky,
            bars = NowBars,
            modifier = Modifier.offset(x = 144.dp, y = 17.6.dp).rotate(5f),
        )
        Mascot(MascotMood.Calm, width = 99.2.dp, modifier = Modifier.offset(x = 232.dp, y = 150.dp))
    }
}

@Composable
private fun UsageCard(label: String, value: String, accent: Color, bars: List<Int>, modifier: Modifier = Modifier) {
    val colors = RecentrTheme.colors
    Column(
        modifier = modifier
            .width(180.dp)
            .recentrShadow(RecentrElevation.float, RecentrShapes.card)
            .clip(RecentrShapes.card)
            .background(colors.bgSurface)
            .padding(horizontal = 20.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(label, style = RecentrTheme.typography.headline, color = accent, maxLines = 1)
        Text(value, style = RecentrTheme.typography.title1, color = colors.textPrimary, maxLines = 1)
        Row(
            modifier = Modifier.padding(top = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(5.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            bars.forEach { height -> Bar(height.dp, accent) }
        }
    }
}

@Composable
private fun Bar(height: Dp, color: Color) {
    Box(
        Modifier
            .size(width = 10.dp, height = height)
            .clip(RoundedCornerShape(4.dp))
            .background(color),
    )
}

@Preview(widthDp = 402, heightDp = 874)
@Composable
private fun WelcomeScreenPreview() {
    RecentrTheme { WelcomeScreen(onGetStarted = {}, onHaveAccount = {}) }
}
