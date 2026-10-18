package com.recentr.feature.profile.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.recentr.core.designsystem.component.RecentrIconButton
import com.recentr.core.designsystem.icon.RecentrIcon
import com.recentr.core.designsystem.illustration.Backdrop
import com.recentr.core.designsystem.illustration.BackdropBox
import com.recentr.core.designsystem.theme.RecentrTheme
import com.recentr.core.designsystem.util.SystemBarsAppearance
import com.recentr.feature.profile.R

/**
 * Frame of the settings sub-screens (Figma 65–72): aurora backdrop, back button 8dp under the
 * status bar, Display/Large title 16dp lower, optional subtitle, then the scrolling [content]
 * [bodyGap] below. [bottomBar] (a CTA) stays pinned 14dp above the navigation bar; [fade] draws
 * the 120dp page-grey fade of the long lists.
 */
@Composable
internal fun SettingsPage(
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    bodyGap: Dp = 23.dp,
    fade: Boolean = false,
    bottomBar: (@Composable ColumnScope.() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    SystemBarsAppearance(lightIcons = false)
    val navBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val scrollState = rememberScrollState()
    BackdropBox(Backdrop.Aurora, modifier) {
        Column(Modifier.fillMaxSize()) {
            Column(
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(scrollState)
                    .statusBarsPadding()
                    .padding(horizontal = RecentrTheme.spacing.screenGutter),
            ) {
                Spacer(Modifier.height(8.dp))
                RecentrIconButton(
                    icon = RecentrIcon.ChevronLeft,
                    contentDescription = stringResource(R.string.back),
                    onClick = onBack,
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    title,
                    style = RecentrTheme.typography.displayLarge,
                    color = RecentrTheme.colors.textPrimary,
                    modifier = Modifier.semantics { heading() },
                )
                if (subtitle != null) {
                    Spacer(Modifier.height(5.dp))
                    Text(subtitle, style = RecentrTheme.typography.bodyRegular, color = RecentrTheme.colors.textSecondary)
                }
                Spacer(Modifier.height(bodyGap))
                content()
                Spacer(Modifier.height(if (bottomBar == null) navBottom + 32.dp else 24.dp))
            }
            if (bottomBar != null) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .windowInsetsPadding(WindowInsets.navigationBars.union(WindowInsets.ime))
                        .padding(start = 24.dp, end = 24.dp, bottom = 14.dp),
                    content = bottomBar,
                )
            }
        }
        // Frosted strip behind the status bar once content scrolls under it (like Home).
        Box(
            Modifier
                .fillMaxWidth()
                .graphicsLayer { alpha = if (scrollState.value > 0) 1f else 0f }
                .background(RecentrTheme.colors.alphaWhite80)
                .statusBarsPadding()
                .height(6.dp),
        )
        if (fade) {
            Box(
                Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(navBottom + 86.dp)
                    .background(
                        Brush.verticalGradient(
                            0f to RecentrTheme.colors.bgPage.copy(alpha = 0f),
                            1f to RecentrTheme.colors.bgPage,
                        ),
                    ),
            )
        }
    }
}

/**
 * Body of the profile sheets (Figma 62 / 71 / 72): the drag handle of the design-system sheet
 * already takes 21dp, so [topGap] is the Figma gap under the grabber minus 6dp.
 */
@Composable
internal fun SheetContent(
    topGap: Dp,
    modifier: Modifier = Modifier,
    horizontalAlignment: Alignment.Horizontal = Alignment.CenterHorizontally,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(start = 24.dp, end = 24.dp, top = (topGap - 6.dp).coerceAtLeast(0.dp), bottom = 10.dp),
        horizontalAlignment = horizontalAlignment,
        content = content,
    )
}

/** Centred Title/1 + Body/Regular block of the confirmation sheets ("Log out of Recentr?"). */
@Composable
internal fun SheetHeading(title: String, body: String, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(title, style = RecentrTheme.typography.title1, color = RecentrTheme.colors.textPrimary, textAlign = TextAlign.Center)
        Text(body, style = RecentrTheme.typography.bodyRegular, color = RecentrTheme.colors.textSecondary, textAlign = TextAlign.Center)
    }
}
