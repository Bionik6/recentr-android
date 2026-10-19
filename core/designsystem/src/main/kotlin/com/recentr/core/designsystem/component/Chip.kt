package com.recentr.core.designsystem.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.recentr.core.designsystem.icon.RecentrIcon
import com.recentr.core.designsystem.icon.RecentrIconImage
import com.recentr.core.designsystem.motion.MotionSpecs
import com.recentr.core.designsystem.motion.pressScale
import com.recentr.core.designsystem.theme.RecentrShapes
import com.recentr.core.designsystem.theme.RecentrTheme

/** Figma "Chip" · Style. */
enum class RecentrChipStyle { Neutral, Selected, Success, OnSky, Outline }

/**
 * Small status / filter chip (Ongoing, All day, 5min…). [contentColor] overrides the style's label
 * and icon colour, like a Figma instance override.
 */
@Composable
fun RecentrChip(
    label: String,
    modifier: Modifier = Modifier,
    style: RecentrChipStyle = RecentrChipStyle.Neutral,
    icon: RecentrIcon? = null,
    onClick: (() -> Unit)? = null,
    contentColor: Color? = null,
) {
    val colors = RecentrTheme.colors
    val (background, content) = when (style) {
        RecentrChipStyle.Neutral -> colors.bgSunken to colors.textSecondary
        RecentrChipStyle.Selected -> colors.actionPrimary to colors.textOnAction
        RecentrChipStyle.Success -> colors.feedbackSuccess to colors.textOnDark
        RecentrChipStyle.OnSky -> colors.alphaWhite24 to colors.textOnDark
        RecentrChipStyle.Outline -> colors.bgSurface to colors.textSecondary
    }
    val border = if (style == RecentrChipStyle.Outline) 1.dp else 0.dp
    // Styles ease into each other (a preset becoming the selected one).
    val animatedBackground by animateColorAsState(background, MotionSpecs.smooth(0.25f), label = "chipBackground")
    val animatedContent by animateColorAsState(contentColor ?: content, MotionSpecs.smooth(0.25f), label = "chipContent")
    val interactionSource = remember { MutableInteractionSource() }
    Row(
        modifier = modifier
            .then(if (onClick != null) Modifier.pressScale(interactionSource, pressedScale = 0.96f) else Modifier)
            .clip(RecentrShapes.full)
            .background(animatedBackground)
            .then(
                if (style == RecentrChipStyle.Outline) {
                    Modifier.border(1.dp, colors.borderSubtle, RecentrShapes.full)
                } else {
                    Modifier
                },
            )
            .then(
                if (onClick != null) {
                    Modifier.clickable(
                        interactionSource = interactionSource,
                        indication = LocalIndication.current,
                        role = Role.Button,
                        onClick = onClick,
                    )
                } else {
                    Modifier
                },
            )
            // Figma includes the outline stroke in the auto-layout size (Outline chip = 32dp high).
            .padding(
                start = 10.dp + border,
                end = 12.dp + border,
                top = 6.dp + border,
                bottom = 6.dp + border,
            ),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) RecentrIconImage(icon, tint = animatedContent, size = 16.08.dp)
        Text(text = label, style = RecentrTheme.typography.captionMedium, color = animatedContent, maxLines = 1)
    }
}

/** Convenience to colour a chip label explicitly (e.g. tangerine streak counters). */
@Composable
fun ChipText(text: String, color: Color) {
    Text(text = text, style = RecentrTheme.typography.captionMedium, color = color, maxLines = 1)
}
