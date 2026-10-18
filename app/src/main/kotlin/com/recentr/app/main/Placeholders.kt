package com.recentr.app.main

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.recentr.app.R
import com.recentr.core.data.RecentrData
import com.recentr.core.designsystem.component.RecentrButton
import com.recentr.core.designsystem.component.RecentrButtonStyle
import com.recentr.core.designsystem.component.RecentrTab
import com.recentr.core.designsystem.illustration.Backdrop
import com.recentr.core.designsystem.illustration.BackdropBox
import com.recentr.core.designsystem.illustration.Mascot
import com.recentr.core.designsystem.illustration.MascotMood
import com.recentr.core.designsystem.theme.RecentrTheme
import com.recentr.core.designsystem.util.SystemBarsAppearance
import kotlinx.coroutines.launch

/** Squads is not part of this delivery: dozing mascot placeholder. */
@Composable
fun ComingSoonScreen(tab: RecentrTab, contentPadding: PaddingValues) {
    SystemBarsAppearance(lightIcons = false)
    BackdropBox(Backdrop.Aurora) {
        Column(
            Modifier.fillMaxSize().statusBarsPadding().padding(contentPadding).padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Mascot(MascotMood.Dozing, width = 128.dp)
            Spacer(Modifier.height(16.dp))
            Text(stringResource(tab.label), style = RecentrTheme.typography.title1, color = RecentrTheme.colors.textPrimary)
            Spacer(Modifier.height(4.dp))
            Text(
                stringResource(R.string.coming_soon_body),
                style = RecentrTheme.typography.bodyRegular,
                color = RecentrTheme.colors.textSecondary,
                textAlign = TextAlign.Center,
            )
        }
    }
}

/** Profile is not part of this delivery: exposes QA shortcuts (replay onboarding, DS catalogue). */
@Composable
fun ProfilePlaceholderScreen(
    contentPadding: PaddingValues,
    onReplayOnboarding: () -> Unit,
    onOpenDesignSystem: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    SystemBarsAppearance(lightIcons = false)
    BackdropBox(Backdrop.Aurora) {
        Column(
            Modifier.fillMaxSize().statusBarsPadding().padding(contentPadding).padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
        ) {
            Mascot(MascotMood.Calm, width = 128.dp)
            Text(stringResource(R.string.profile_title), style = RecentrTheme.typography.title1, color = RecentrTheme.colors.textPrimary)
            Text(
                stringResource(R.string.coming_soon_body),
                style = RecentrTheme.typography.bodyRegular,
                color = RecentrTheme.colors.textSecondary,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(8.dp))
            RecentrButton(
                text = stringResource(R.string.profile_replay_onboarding),
                onClick = {
                    scope.launch {
                        RecentrData.userPreferences.update { it.copy(onboardingCompleted = false) }
                        onReplayOnboarding()
                    }
                },
                style = RecentrButtonStyle.Primary,
                modifier = Modifier.fillMaxWidth(),
            )
            RecentrButton(
                text = stringResource(R.string.profile_design_system),
                onClick = onOpenDesignSystem,
                style = RecentrButtonStyle.Secondary,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
