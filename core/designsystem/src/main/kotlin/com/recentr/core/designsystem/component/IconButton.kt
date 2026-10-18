package com.recentr.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.recentr.core.designsystem.icon.RecentrIcon
import com.recentr.core.designsystem.icon.RecentrIconImage
import com.recentr.core.designsystem.theme.RecentrElevation
import com.recentr.core.designsystem.theme.RecentrShapes
import com.recentr.core.designsystem.theme.RecentrTheme
import com.recentr.core.designsystem.theme.recentrShadow

/** Figma "Icon Button" · Style. */
enum class RecentrIconButtonStyle {
    /** White disc with card shadow. */
    Surface,
    /** Sunken grey disc. */
    Sunken,
    /** Translucent glass over the Home sky. */
    OnSky,
}

/** 40dp round icon button. */
@Composable
fun RecentrIconButton(
    icon: RecentrIcon,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: RecentrIconButtonStyle = RecentrIconButtonStyle.Surface,
    size: Dp = RecentrTheme.sizes.iconButton,
    iconSize: Dp = 24.dp,
    tint: Color = if (style == RecentrIconButtonStyle.OnSky) RecentrTheme.colors.textOnDark else RecentrTheme.colors.textPrimary,
    enabled: Boolean = true,
) {
    val colors = RecentrTheme.colors
    val styled = when (style) {
        RecentrIconButtonStyle.Surface -> Modifier
            .recentrShadow(RecentrElevation.control, RecentrShapes.full)
            .clip(RecentrShapes.full)
            .background(colors.bgSurface)
        RecentrIconButtonStyle.Sunken -> Modifier
            .clip(RecentrShapes.full)
            .background(colors.bgSunken)
        RecentrIconButtonStyle.OnSky -> Modifier
            .clip(RecentrShapes.full)
            .background(colors.alphaWhite24)
            .border(1.dp, colors.alphaWhite24, RecentrShapes.full)
    }
    Box(
        modifier = modifier
            .size(size)
            .then(styled)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        RecentrIconImage(icon, contentDescription = contentDescription, tint = tint, size = iconSize)
    }
}
