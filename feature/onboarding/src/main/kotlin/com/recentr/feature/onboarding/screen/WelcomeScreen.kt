package com.recentr.feature.onboarding.screen

import androidx.compose.animation.core.Animatable
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import com.recentr.core.designsystem.component.RecentrButton
import com.recentr.core.designsystem.component.RecentrButtonStyle
import com.recentr.core.designsystem.illustration.Backdrop
import com.recentr.core.designsystem.illustration.Mascot
import com.recentr.core.designsystem.illustration.MascotMood
import com.recentr.core.designsystem.motion.LocalReduceMotion
import com.recentr.core.designsystem.motion.entrance
import com.recentr.core.designsystem.motion.floating
import com.recentr.core.designsystem.motion.motionSpring
import com.recentr.core.designsystem.theme.RecentrElevation
import com.recentr.core.designsystem.theme.RecentrShapes
import com.recentr.core.designsystem.theme.RecentrTheme
import com.recentr.core.designsystem.theme.recentrShadow
import com.recentr.feature.onboarding.R
import com.recentr.feature.onboarding.component.OnboardingHeading
import com.recentr.feature.onboarding.component.OnboardingPage
import kotlinx.coroutines.delay

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
            RecentrButton(
                text = stringResource(R.string.onb_welcome_get_started),
                onClick = onGetStarted,
                modifier = Modifier
                    .fillMaxWidth()
                    .entrance(750),
            )
            RecentrButton(
                text = stringResource(R.string.onb_welcome_have_account),
                onClick = onHaveAccount,
                modifier = Modifier
                    .fillMaxWidth()
                    .entrance(850, blur = false),
                style = RecentrButtonStyle.Secondary,
            )
        },
    ) {
        Spacer(Modifier.height(14.dp))
        Text(
            text = stringResource(R.string.onb_wordmark),
            style = RecentrTheme.typography.brandWordmark,
            color = colors.textOnDark,
            modifier = Modifier.entrance(from = DpOffset(0.dp, (-12).dp)),
        )
        Spacer(Modifier.height(24.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            GlassStat(
                value = stringResource(R.string.onb_welcome_stat_people_value),
                label = stringResource(R.string.onb_welcome_stat_people_label),
                modifier = Modifier.entrance(120, from = DpOffset((-16).dp, 0.dp)),
            )
            GlassStat(
                value = stringResource(R.string.onb_welcome_stat_time_value),
                label = stringResource(R.string.onb_welcome_stat_time_label),
                modifier = Modifier.entrance(200, from = DpOffset(16.dp, 0.dp)),
            )
        }
        Spacer(Modifier.height(45.dp))
        BeforeNowIllustration()
        Spacer(Modifier.height(58.dp))
        OnboardingHeading(
            title = stringResource(R.string.onb_welcome_title),
            subtitle = stringResource(R.string.onb_welcome_subtitle),
            centered = true,
            delayMillis = 550,
        )
    }
}

/** Translucent "glass" stat pill over the sky (alpha/white-24 fill + border). */
@Composable
private fun GlassStat(value: String, label: String, modifier: Modifier = Modifier) {
    val colors = RecentrTheme.colors
    val shape = RoundedCornerShape(20.dp)
    Column(
        modifier = modifier
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

/**
 * "Before / Now" — two tilted usage cards and the calm mascot (354×260 group). The cards fly in
 * from either side and keep bobbing out of step, their bars growing in; the mascot drops in last.
 */
@Composable
private fun BeforeNowIllustration() {
    val colors = RecentrTheme.colors
    Box(Modifier.size(width = 354.dp, height = 260.dp)) {
        UsageCard(
            label = stringResource(R.string.onb_welcome_before),
            value = stringResource(R.string.onb_welcome_before_value),
            accent = colors.feedbackDanger,
            bars = BeforeBars,
            barsDelayMillis = 450,
            modifier = Modifier
                .offset(x = 12.3.dp, y = 30.1.dp)
                .entrance(250, from = DpOffset((-80).dp, 30.dp), blur = false)
                .floating(amplitude = 4.dp, periodMillis = 5000)
                .rotate(-6f),
        )
        UsageCard(
            label = stringResource(R.string.onb_welcome_now),
            value = stringResource(R.string.onb_welcome_now_value),
            accent = colors.brandSky,
            bars = NowBars,
            barsDelayMillis = 650,
            modifier = Modifier
                .offset(x = 144.dp, y = 17.6.dp)
                .entrance(400, from = DpOffset(80.dp, 30.dp), blur = false)
                .floating(amplitude = 5.dp, periodMillis = 5000, phase = 0.4f)
                .rotate(5f),
        )
        Mascot(
            mood = MascotMood.Calm,
            width = 99.2.dp,
            modifier = Modifier
                .offset(x = 232.dp, y = 150.dp)
                .entrance(700, from = DpOffset(0.dp, (-60).dp)),
        )
    }
}

@Composable
private fun UsageCard(
    label: String,
    value: String,
    accent: Color,
    bars: List<Int>,
    barsDelayMillis: Int,
    modifier: Modifier = Modifier,
) {
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
            bars.forEachIndexed { index, height -> Bar(height.dp, accent, delayMillis = barsDelayMillis + index * 60) }
        }
    }
}

/** A bar growing up from its base, `.spring(duration: 0.7, bounce: 0.45)`. */
@Composable
private fun Bar(height: Dp, color: Color, delayMillis: Int) {
    val reduceMotion = LocalReduceMotion.current
    val growth = remember { Animatable(if (reduceMotion) 1f else 0.05f) }
    LaunchedEffect(Unit) {
        delay(delayMillis.toLong())
        growth.animateTo(1f, motionSpring(0.7f, 0.45f))
    }
    Box(
        Modifier
            .size(width = 10.dp, height = height)
            .graphicsLayer {
                scaleY = growth.value
                transformOrigin = TransformOrigin(0.5f, 1f)
            }
            .clip(RoundedCornerShape(4.dp))
            .background(color),
    )
}

@Preview(widthDp = 402, heightDp = 874)
@Composable
private fun WelcomeScreenPreview() {
    RecentrTheme { WelcomeScreen(onGetStarted = {}, onHaveAccount = {}) }
}
