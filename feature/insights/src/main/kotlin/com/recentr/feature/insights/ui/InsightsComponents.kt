package com.recentr.feature.insights.ui

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.recentr.core.designsystem.component.RecentrIconButton
import com.recentr.core.designsystem.component.RecentrIconButtonStyle
import com.recentr.core.designsystem.icon.RecentrIcon
import com.recentr.core.designsystem.icon.RecentrIconImage
import com.recentr.core.designsystem.illustration.Backdrop
import com.recentr.core.designsystem.illustration.BackdropBox
import com.recentr.core.designsystem.motion.rememberFill
import com.recentr.core.designsystem.theme.RecentrElevation
import com.recentr.core.designsystem.theme.RecentrShapes
import com.recentr.core.designsystem.theme.RecentrTheme
import com.recentr.core.designsystem.theme.recentrShadow
import com.recentr.core.designsystem.util.SystemBarsAppearance
import com.recentr.feature.insights.R

/** Figma "Card" of the Insights screens: white, 24dp corners, Elevation/Card. */
internal val InsightsCardShape = RoundedCornerShape(24.dp)

/**
 * Frame of the Insights & Achievements screens (Figma 99–110): [backdrop] (Aurora by default),
 * the [topBar] 12dp under the status bar, then the scrolling [content]. [bottomBar] (a CTA)
 * stays pinned 14dp above the navigation bar.
 */
@Composable
internal fun InsightsScaffold(
    topBar: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    backdrop: Backdrop? = Backdrop.Aurora,
    background: Color = RecentrTheme.colors.bgPage,
    lightStatusBarIcons: Boolean = false,
    scrollState: ScrollState = rememberScrollState(),
    bottomBar: (@Composable ColumnScope.() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    SystemBarsAppearance(lightIcons = lightStatusBarIcons)
    val navBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val body: @Composable () -> Unit = {
        Column(Modifier.fillMaxSize()) {
            Column(
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(scrollState)
                    .statusBarsPadding()
                    .padding(horizontal = RecentrTheme.spacing.screenGutter),
            ) {
                Spacer(Modifier.height(12.dp))
                topBar()
                content()
                Spacer(Modifier.height(if (bottomBar == null) navBottom + 32.dp else 24.dp))
            }
            if (bottomBar != null) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(start = 24.dp, end = 24.dp, bottom = 14.dp),
                    content = bottomBar,
                )
            }
        }
        // Frosted strip behind the status bar once content scrolls under it (like Home).
        if (!lightStatusBarIcons) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .graphicsLayer { alpha = if (scrollState.value > 0) 1f else 0f }
                    .background(RecentrTheme.colors.alphaWhite80)
                    .statusBarsPadding()
                    .height(6.dp),
            )
        }
    }
    if (backdrop != null) {
        BackdropBox(backdrop, modifier) { body() }
    } else {
        Box(modifier.fillMaxSize().background(background)) { body() }
    }
}

/**
 * Figma "Header" (40dp): a round [leading] button, the centred title (and subtitle), then
 * [trailing] — without one, the title centres in the remaining width, like the design.
 */
@Composable
internal fun InsightsTopBar(
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    backIcon: RecentrIcon = RecentrIcon.ChevronLeft,
    onSky: Boolean = false,
    trailing: (@Composable () -> Unit)? = null,
) {
    val colors = RecentrTheme.colors
    Row(
        modifier.fillMaxWidth().heightIn(min = 40.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RecentrIconButton(
            icon = backIcon,
            contentDescription = stringResource(if (backIcon == RecentrIcon.X) R.string.insights_close else R.string.insights_back),
            onClick = onBack,
            style = if (onSky) RecentrIconButtonStyle.OnSky else RecentrIconButtonStyle.Surface,
        )
        Column(
            Modifier.weight(1f).semantics { heading() },
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            if (title.isNotEmpty()) {
                Text(
                    title,
                    style = RecentrTheme.typography.headline,
                    color = if (onSky) colors.textOnDark else colors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (subtitle != null) {
                Text(
                    subtitle,
                    style = RecentrTheme.typography.captionRegular,
                    color = if (onSky) colors.textOnDarkMuted else colors.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        trailing?.invoke()
    }
}

/** The "⋯" header button and its design-system styled menu ([items] = label → action). */
@Composable
internal fun MoreMenuButton(items: List<Pair<String, () -> Unit>>, onSky: Boolean = false) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    Box {
        RecentrIconButton(
            icon = RecentrIcon.More,
            contentDescription = stringResource(R.string.insights_more),
            onClick = { expanded = true },
            style = if (onSky) RecentrIconButtonStyle.OnSky else RecentrIconButtonStyle.Surface,
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            shape = RoundedCornerShape(16.dp),
            containerColor = RecentrTheme.colors.bgSurface,
        ) {
            items.forEach { (label, action) ->
                DropdownMenuItem(
                    text = { Text(label, style = RecentrTheme.typography.bodyMedium) },
                    onClick = {
                        expanded = false
                        action()
                    },
                    colors = MenuDefaults.itemColors(textColor = RecentrTheme.colors.textPrimary),
                )
            }
        }
    }
}

/** White 24dp card (Figma "Daily average", "By category", "Top apps"…). */
@Composable
internal fun InsightsCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    spacing: Dp = 12.dp,
    contentPadding: PaddingValues = PaddingValues(horizontal = 18.dp, vertical = 16.dp),
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .recentrShadow(RecentrElevation.card, InsightsCardShape)
            .clip(InsightsCardShape)
            .background(RecentrTheme.colors.bgSurface)
            .then(if (onClick != null) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier)
            .padding(contentPadding),
        verticalArrangement = Arrangement.spacedBy(spacing),
        content = content,
    )
}

/** Headline title of a card ("By category", "Top apps"). */
@Composable
internal fun CardTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        style = RecentrTheme.typography.headline,
        color = RecentrTheme.colors.textPrimary,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier.semantics { heading() },
    )
}

/** Rounded progress track: [color] fill over [trackColor], springing in on first show (iOS `ProgressBar`). */
@Composable
internal fun ProgressTrack(
    fraction: Float,
    modifier: Modifier = Modifier,
    height: Dp = 8.dp,
    color: Color = RecentrTheme.colors.brandSky,
    trackColor: Color = RecentrTheme.colors.bgSunken,
) {
    val animated by rememberFill(fraction.coerceIn(0f, 1f))
    val shape = RoundedCornerShape(height / 2)
    Box(modifier.fillMaxWidth().height(height).clip(shape).background(trackColor)) {
        if (animated > 0f) {
            Box(Modifier.fillMaxWidth(animated.coerceAtMost(1f)).height(height).clip(shape).background(color))
        }
    }
}

/**
 * Figma "Chip" for a change: green when it is good news (less screen time, fewer pickups),
 * tangerine otherwise.
 */
@Composable
internal fun ChangeChip(label: String, good: Boolean, modifier: Modifier = Modifier) {
    val colors = RecentrTheme.colors
    Text(
        label,
        style = RecentrTheme.typography.captionMedium,
        color = colors.textOnDark,
        maxLines = 1,
        modifier = modifier
            .clip(RecentrShapes.full)
            .background(if (good) colors.feedbackSuccess else colors.accentTangerine)
            .padding(start = 10.dp, end = 12.dp, top = 6.dp, bottom = 6.dp),
    )
}

/** Small chip with a leading [icon] on a sunken pill (Figma "First at 7:02 AM"). */
@Composable
internal fun IconChip(
    label: String,
    icon: RecentrIcon,
    modifier: Modifier = Modifier,
    iconTint: Color = RecentrTheme.colors.textSecondary,
    background: Color = RecentrTheme.colors.bgSunken,
    contentColor: Color = RecentrTheme.colors.textSecondary,
) {
    Row(
        modifier
            .clip(RecentrShapes.full)
            .background(background)
            .padding(start = 10.dp, end = 12.dp, top = 6.dp, bottom = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RecentrIconImage(icon, tint = iconTint, size = 16.08.dp)
        Text(label, style = RecentrTheme.typography.captionMedium, color = contentColor, maxLines = 1)
    }
}

/** Skeleton block shown while the numbers load. */
@Composable
internal fun LoadingBlock(height: Dp, modifier: Modifier = Modifier) {
    Box(
        modifier
            .fillMaxWidth()
            .height(height)
            .recentrShadow(RecentrElevation.card, InsightsCardShape)
            .clip(InsightsCardShape)
            .background(RecentrTheme.colors.bgSurface),
    )
}
