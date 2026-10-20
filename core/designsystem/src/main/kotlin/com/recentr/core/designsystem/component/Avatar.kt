package com.recentr.core.designsystem.component

import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.recentr.core.designsystem.motion.pressScale
import com.recentr.core.designsystem.theme.CssLinearGradient
import com.recentr.core.designsystem.theme.RecentrElevation
import com.recentr.core.designsystem.theme.RecentrTheme
import com.recentr.core.designsystem.theme.recentrShadow

private fun avatarGradient(from: Long, to: Long): Brush =
    CssLinearGradient(143.13f, listOf(0f to Color(from), 0.71429f to Color(to)))

/** Figma "Avatar" · Color, plus the warm Sunset gradient of Profile 61/62. */
enum class AvatarStyle(val brush: Brush) {
    Sunset(avatarGradient(0xFFFFB27A, 0xFFF04F7A)),
    Sky(avatarGradient(0xFFA8D4FF, 0xFF2F7BF6)),
    Mint(avatarGradient(0xFFA6EBC4, 0xFF1FAF5A)),
    Pink(avatarGradient(0xFFFFB3C2, 0xFFF04F7A)),
    Tangerine(avatarGradient(0xFFFFC48A, 0xFFFF7A1A)),
    Night(avatarGradient(0xFF7B87B8, 0xFF1B2340)),
}

/**
 * Figma "Avatar" — gradient disc with a white ring and the user's initial. Size L (68dp, 4dp
 * ring, Title/1 initial) is the default; Profile › Edit uses 96dp with the same ring and text,
 * the Home header a 40dp button over the sky.
 */
@Composable
fun RecentrAvatar(
    initial: String,
    style: AvatarStyle,
    modifier: Modifier = Modifier,
    size: Dp = 68.dp,
    ringWidth: Dp = 4.dp,
    ringColor: Color = RecentrTheme.colors.bgSurface,
    textStyle: TextStyle = RecentrTheme.typography.title1,
    onClick: (() -> Unit)? = null,
    onClickLabel: String? = null,
) {
    val interactionSource = remember { MutableInteractionSource() }
    Box(
        modifier = modifier
            .then(if (onClick != null) Modifier.pressScale(interactionSource, pressedScale = 0.92f) else Modifier)
            .size(size)
            .recentrShadow(RecentrElevation.card, CircleShape)
            .clip(CircleShape)
            .background(style.brush)
            .then(
                if (onClick != null) {
                    Modifier.clickable(
                        interactionSource = interactionSource,
                        indication = LocalIndication.current,
                        onClickLabel = onClickLabel,
                        role = Role.Button,
                        onClick = onClick,
                    )
                } else {
                    Modifier
                },
            )
            .border(ringWidth, ringColor, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Text(initial, style = textStyle, color = RecentrTheme.colors.textOnDark, maxLines = 1)
    }
}
