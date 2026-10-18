package com.recentr.feature.onboarding.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.recentr.core.designsystem.theme.RecentrTheme

/**
 * Figma "Heading" frame of the onboarding screens: optional sky overline (Body/Medium), Title/1
 * and optional Body/Regular subtitle, inside the 24dp gutters.
 */
@Composable
internal fun OnboardingHeading(
    title: String,
    modifier: Modifier = Modifier,
    overline: String? = null,
    subtitle: String? = null,
    centered: Boolean = false,
    spacing: Dp = 8.dp,
) {
    val colors = RecentrTheme.colors
    val align = if (centered) TextAlign.Center else TextAlign.Start
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = RecentrTheme.spacing.screenGutter),
        verticalArrangement = Arrangement.spacedBy(spacing),
        horizontalAlignment = if (centered) Alignment.CenterHorizontally else Alignment.Start,
    ) {
        if (overline != null) {
            Text(overline, style = RecentrTheme.typography.bodyMedium, color = colors.brandSky, textAlign = align)
        }
        Text(
            text = title,
            style = RecentrTheme.typography.title1,
            color = colors.textPrimary,
            textAlign = align,
            modifier = Modifier
                .fillMaxWidth()
                .semantics { heading() },
        )
        if (subtitle != null) {
            Text(
                text = subtitle,
                style = RecentrTheme.typography.bodyRegular,
                color = colors.textSecondary,
                textAlign = align,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

/**
 * Lays a text out in the Figma line box [height] even when the font's natural height is larger:
 * Display/Hero (72sp, line-height 1.0) measures ~86dp on device, which would push everything
 * below it down by ~14dp. Glyphs stay centred and simply overflow the box, as in Figma.
 */
internal fun Modifier.figmaLineBox(height: Dp): Modifier = this
    .height(height)
    .wrapContentHeight(align = Alignment.CenterVertically, unbounded = true)
