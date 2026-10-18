package com.recentr.feature.onboarding.screen

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseOut
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import com.recentr.core.designsystem.component.MiniTile
import com.recentr.core.designsystem.component.RecentrButton
import com.recentr.core.designsystem.illustration.Mascot
import com.recentr.core.designsystem.illustration.MascotMood
import com.recentr.core.designsystem.illustration.Sticker
import com.recentr.core.designsystem.motion.entrance
import com.recentr.core.designsystem.theme.RecentrTheme
import com.recentr.feature.onboarding.R
import com.recentr.feature.onboarding.component.OnboardingPage
import com.recentr.feature.onboarding.component.figmaLineBox
import com.recentr.feature.onboarding.component.spokenDuration
import kotlin.math.roundToInt

/**
 * 15 · Onboarding — Years on phone (Figma 2024:1112). Numbers come from the reported usage
 * (6h a day → 16 years, 91 days a year in the design); the year count rolls up on entry.
 */
@Composable
internal fun YearsScreen(
    years: Int,
    averageMinutes: Int,
    daysPerYear: Int,
    onContinue: () -> Unit,
) {
    val colors = RecentrTheme.colors
    val counter = remember { Animatable(0f) }
    // The years tick up from 0 (`.easeOut(duration: 1.4).delay(0.3)`).
    LaunchedEffect(years) { counter.animateTo(years.toFloat(), tween(1400, delayMillis = 300, easing = EaseOut)) }
    val shown = counter.value.roundToInt()
    val yearsText = pluralStringResource(R.plurals.onb_years_number, years, years)
    OnboardingPage(
        bottomBar = {
            RecentrButton(stringResource(R.string.onb_continue), onContinue, Modifier.fillMaxWidth().entrance(600))
        },
    ) {
        Spacer(Modifier.height(116.dp))
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                stringResource(R.string.onb_years_lead),
                style = RecentrTheme.typography.title2,
                color = colors.textSecondary,
                modifier = Modifier.entrance(50),
            )
            Text(
                text = pluralStringResource(R.plurals.onb_years_number, shown, shown),
                style = RecentrTheme.typography.displayHero.copy(fontFeatureSettings = "tnum"),
                color = colors.brandSky,
                maxLines = 1,
                modifier = Modifier
                    .figmaLineBox(72.dp)
                    .entrance(150)
                    .semantics { contentDescription = yearsText },
            )
            Text(
                stringResource(R.string.onb_years_tail),
                style = RecentrTheme.typography.title2,
                color = colors.textPrimary,
                modifier = Modifier.entrance(250),
            )
        }
        Spacer(Modifier.height(126.dp))
        Box(Modifier.size(width = 354.dp, height = 250.dp)) {
            MiniTile(
                label = stringResource(R.string.onb_years_every_day),
                value = spokenDuration(averageMinutes),
                sticker = Sticker.Timer,
                modifier = Modifier
                    .offset(x = 12.6.dp, y = 12.4.dp)
                    .entrance(900, from = DpOffset((-140).dp, 0.dp), blur = false)
                    .rotate(-5f)
                    .width(171.dp),
            )
            MiniTile(
                label = stringResource(R.string.onb_years_every_year),
                value = pluralStringResource(R.plurals.onb_days, daysPerYear, daysPerYear),
                sticker = Sticker.Moon,
                modifier = Modifier
                    .offset(x = 169.4.dp, y = 75.9.dp)
                    .entrance(1100, from = DpOffset(140.dp, 0.dp), blur = false)
                    .rotate(4f)
                    .width(171.dp),
            )
            Mascot(
                mood = MascotMood.Dozing,
                width = 96.dp,
                modifier = Modifier
                    .offset(x = 126.dp, y = 130.dp)
                    .entrance(1300, from = DpOffset(0.dp, 30.dp)),
            )
        }
    }
}

@Preview(widthDp = 402, heightDp = 874)
@Composable
private fun YearsScreenPreview() {
    RecentrTheme { YearsScreen(years = 16, averageMinutes = 360, daysPerYear = 91, onContinue = {}) }
}
