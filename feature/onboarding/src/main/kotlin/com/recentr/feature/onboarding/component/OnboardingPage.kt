package com.recentr.feature.onboarding.component

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import com.recentr.core.designsystem.component.OnboardingProgress
import com.recentr.core.designsystem.component.RecentrIconButton
import com.recentr.core.designsystem.icon.RecentrIcon
import com.recentr.core.designsystem.illustration.Backdrop
import com.recentr.core.designsystem.illustration.BackdropBox
import com.recentr.core.designsystem.theme.RecentrTheme
import com.recentr.core.designsystem.R as DsR

/** Progress shown by the header of a step plus the value it animates from. */
data class HeaderProgress(val value: Float, val from: Float = value)

/**
 * Common frame of every onboarding screen (402×874 Figma artboards):
 * backdrop → optional back · progress header (56dp, right under the status bar) → scrollable
 * content in Figma order → bottom actions pinned above the navigation bar / keyboard with the
 * design's 14dp gap (the iOS home indicator area becomes the Android navigation bar inset).
 */
@Composable
internal fun OnboardingPage(
    modifier: Modifier = Modifier,
    backdrop: Backdrop = Backdrop.Aurora,
    progress: HeaderProgress? = null,
    onBack: (() -> Unit)? = null,
    bottomGap: Dp = 14.dp,
    bottomBar: @Composable ColumnScope.() -> Unit = {},
    content: @Composable ColumnScope.() -> Unit,
) {
    OnboardingSystemBars(lightIcons = backdrop == Backdrop.Sky)
    BackdropBox(backdrop, modifier) {
        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            if (progress != null) OnboardingTopBar(progress, onBack)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                content = content,
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.navigationBars.union(WindowInsets.ime))
                    .padding(start = 24.dp, end = 24.dp, bottom = bottomGap),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                content = bottomBar,
            )
        }
    }
}

/**
 * Figma "Onboarding Header" with `showSkip = false` (every instance of the flow): back button on
 * the left, progress bar pushed to the right gutter. The design-system [com.recentr.core
 * .designsystem.component.OnboardingHeader] keeps a 40dp skip slot, which centres the bar instead.
 */
@Composable
internal fun OnboardingTopBar(progress: HeaderProgress, onBack: (() -> Unit)?, modifier: Modifier = Modifier) {
    val animated = remember { Animatable(progress.from) }
    LaunchedEffect(progress.value) {
        animated.animateTo(progress.value, tween(durationMillis = 500, easing = FastOutSlowInEasing))
    }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .padding(horizontal = RecentrTheme.spacing.screenGutter),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (onBack != null) {
            RecentrIconButton(
                icon = RecentrIcon.ChevronLeft,
                contentDescription = stringResource(DsR.string.ds_back),
                onClick = onBack,
            )
        }
        Spacer(Modifier.weight(1f))
        OnboardingProgress(animated.value)
    }
}

/**
 * Sets the system bar icon tone for the screen. Unlike the design-system `SystemBarsAppearance`
 * it does not restore the previous tone on dispose: in a navigation flow the outgoing screen is
 * disposed *after* the incoming one applied its tone, so restoring would flip it back.
 */
@Composable
internal fun OnboardingSystemBars(lightIcons: Boolean) {
    val view = LocalView.current
    if (view.isInEditMode) return
    DisposableEffect(lightIcons) {
        val window = view.context.findActivity()?.window
        if (window != null) {
            val controller = WindowCompat.getInsetsController(window, view)
            controller.isAppearanceLightStatusBars = !lightIcons
            controller.isAppearanceLightNavigationBars = !lightIcons
        }
        onDispose { }
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
