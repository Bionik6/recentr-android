package com.recentr.feature.onboarding.screen

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.recentr.core.designsystem.component.RecentrAlertDialog
import com.recentr.core.designsystem.component.RecentrButton
import com.recentr.core.designsystem.illustration.Mascot
import com.recentr.core.designsystem.illustration.MascotMood
import com.recentr.core.designsystem.motion.entrance
import com.recentr.core.designsystem.motion.floating
import com.recentr.core.designsystem.theme.RecentrTheme
import com.recentr.feature.onboarding.R
import com.recentr.feature.onboarding.component.HeaderProgress
import com.recentr.feature.onboarding.component.OnboardingHeading
import com.recentr.feature.onboarding.component.OnboardingPage
import com.recentr.feature.onboarding.permission.hasUsageAccess
import com.recentr.feature.onboarding.permission.openUsageAccessSettings

/**
 * 18 · Onboarding — Screen Time access (Figma 2024:1320).
 *
 * Android has no Screen Time prompt: "Connect" shows the design's alert as an in-app rationale
 * ([RecentrAlertDialog]); its Continue opens Settings › Usage access and the flow moves on when
 * the user comes back (whatever they chose). "Don't allow" moves on directly. If access is already
 * granted, Connect skips the alert.
 *
 * @param onContinue called with whether usage access is granted.
 */
@Composable
internal fun ScreenTimeRoute(
    progress: HeaderProgress,
    onBack: () -> Unit,
    onContinue: (granted: Boolean) -> Unit,
) {
    val context = LocalContext.current
    var showAlert by rememberSaveable { mutableStateOf(false) }
    var awaitingSettings by rememberSaveable { mutableStateOf(false) }

    // Registered once: fires when the user returns from Settings (also after a process restart).
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        if (awaitingSettings) {
            awaitingSettings = false
            onContinue(context.hasUsageAccess())
        }
    }

    ScreenTimeScreen(
        progress = progress,
        showAlert = showAlert,
        onBack = onBack,
        onConnect = {
            if (context.hasUsageAccess()) onContinue(true) else showAlert = true
        },
        onAllow = {
            showAlert = false
            if (context.openUsageAccessSettings()) awaitingSettings = true else onContinue(false)
        },
        onDeny = {
            showAlert = false
            onContinue(false)
        },
        onDismissAlert = { showAlert = false },
    )
}

@Composable
internal fun ScreenTimeScreen(
    progress: HeaderProgress,
    showAlert: Boolean,
    onBack: () -> Unit,
    onConnect: () -> Unit,
    onAllow: () -> Unit,
    onDeny: () -> Unit,
    onDismissAlert: () -> Unit,
) {
    OnboardingPage(
        progress = progress,
        onBack = onBack,
        bottomBar = {
            RecentrButton(stringResource(R.string.onb_screen_time_cta), onConnect, Modifier.fillMaxWidth().entrance(500))
        },
    ) {
        Spacer(Modifier.height(60.dp))
        Mascot(
            mood = MascotMood.Calm,
            width = 144.dp,
            modifier = Modifier
                .entrance(100, from = DpOffset(0.dp, (-40).dp))
                .floating(amplitude = 4.dp, periodMillis = 3800),
        )
        Spacer(Modifier.height(50.dp))
        OnboardingHeading(
            title = stringResource(R.string.onb_screen_time_title),
            subtitle = stringResource(R.string.onb_screen_time_subtitle),
            centered = true,
            delayMillis = 300,
        )
    }
    if (showAlert) {
        RecentrAlertDialog(
            title = stringResource(R.string.onb_screen_time_alert_title),
            body = stringResource(R.string.onb_screen_time_alert_body),
            primaryText = stringResource(R.string.onb_screen_time_alert_allow),
            onPrimary = onAllow,
            onDismissRequest = onDismissAlert,
            secondaryText = stringResource(R.string.onb_screen_time_alert_deny),
            onSecondary = onDeny,
        )
    }
}

@Preview(widthDp = 402, heightDp = 874)
@Composable
private fun ScreenTimeScreenPreview() {
    RecentrTheme {
        ScreenTimeScreen(
            HeaderProgress(0.9f),
            showAlert = false,
            onBack = {},
            onConnect = {},
            onAllow = {},
            onDeny = {},
            onDismissAlert = {},
        )
    }
}
