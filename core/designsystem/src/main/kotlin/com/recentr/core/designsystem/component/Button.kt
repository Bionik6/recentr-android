package com.recentr.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.recentr.core.designsystem.icon.RecentrIcon
import com.recentr.core.designsystem.icon.RecentrIconImage
import com.recentr.core.designsystem.theme.RecentrElevation
import com.recentr.core.designsystem.theme.RecentrGradients
import com.recentr.core.designsystem.theme.RecentrShapes
import com.recentr.core.designsystem.theme.RecentrTheme
import com.recentr.core.designsystem.theme.recentrShadow

/** Figma "Button" · Style. */
enum class RecentrButtonStyle {
    /** Black pill — default CTA. */
    Primary,
    /** Sky gradient with glow — "Start blocking". */
    Brand,
    /** White card pill. */
    Secondary,
    /** Sunken grey — "Nevermind". */
    Soft,
}

/**
 * Pill button, 54dp high (`size/button`). Pass `Modifier.fillMaxWidth()` for the full-width
 * 354pt CTAs of the design.
 */
@Composable
fun RecentrButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: RecentrButtonStyle = RecentrButtonStyle.Primary,
    icon: RecentrIcon? = null,
    enabled: Boolean = true,
) {
    val colors = RecentrTheme.colors
    val background: Brush
    val contentColor: Color
    when (style) {
        RecentrButtonStyle.Primary -> {
            background = SolidColor(colors.actionPrimary); contentColor = colors.textOnAction
        }
        RecentrButtonStyle.Brand -> {
            background = RecentrGradients.brandCta; contentColor = colors.textOnDark
        }
        RecentrButtonStyle.Secondary -> {
            background = SolidColor(colors.bgSurface); contentColor = colors.textPrimary
        }
        RecentrButtonStyle.Soft -> {
            background = SolidColor(colors.bgSunken); contentColor = colors.textPrimary
        }
    }
    val shadowModifier = when (style) {
        RecentrButtonStyle.Brand -> Modifier.recentrShadow(RecentrElevation.glowButton, RecentrShapes.full)
        RecentrButtonStyle.Secondary -> Modifier.recentrShadow(RecentrElevation.control, RecentrShapes.full)
        else -> Modifier
    }
    Box(
        modifier = modifier
            .alpha(if (enabled) 1f else 0.4f)
            .then(if (enabled) shadowModifier else Modifier)
            .height(RecentrTheme.sizes.button)
            .defaultMinSize(minWidth = 120.dp)
            .clip(RecentrShapes.full)
            .background(background)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = 24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (icon != null) {
                RecentrIconImage(icon, tint = contentColor, modifier = Modifier.size(24.dp))
            }
            Text(
                text = text,
                style = RecentrTheme.typography.headline,
                color = contentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/** Plain text button used for secondary actions under a CTA ("Not now", "I already have an account"). */
@Composable
fun RecentrTextButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    color: Color = RecentrTheme.colors.textPrimary,
) {
    Box(
        modifier = modifier
            .height(RecentrTheme.sizes.control)
            .clip(RecentrShapes.full)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = text, style = RecentrTheme.typography.bodyMedium, color = color)
    }
}

@Preview
@Composable
private fun RecentrButtonPreview() {
    RecentrTheme {
        androidx.compose.foundation.layout.Column(
            Modifier.background(RecentrTheme.colors.bgPage).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            RecentrButtonStyle.entries.forEach {
                RecentrButton("Continue", onClick = {}, style = it)
            }
            RecentrButton("Start blocking", onClick = {}, style = RecentrButtonStyle.Brand, icon = RecentrIcon.Plus)
        }
    }
}
