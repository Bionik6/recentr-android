package com.recentr.app.main

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.recentr.app.R
import com.recentr.core.designsystem.component.RecentrTab
import com.recentr.core.designsystem.illustration.Backdrop
import com.recentr.core.designsystem.illustration.BackdropBox
import com.recentr.core.designsystem.illustration.Mascot
import com.recentr.core.designsystem.illustration.MascotMood
import com.recentr.core.designsystem.theme.RecentrTheme
import com.recentr.core.designsystem.util.SystemBarsAppearance

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
