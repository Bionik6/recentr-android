package com.recentr.feature.onboarding.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.recentr.core.designsystem.component.MiniTile
import com.recentr.core.designsystem.component.RecentrButton
import com.recentr.core.designsystem.component.RecentrButtonStyle
import com.recentr.core.designsystem.component.StickyNote
import com.recentr.core.designsystem.icon.RecentrIcon
import com.recentr.core.designsystem.illustration.Backdrop
import com.recentr.core.designsystem.illustration.Mascot
import com.recentr.core.designsystem.illustration.MascotMood
import com.recentr.core.designsystem.illustration.Sticker
import com.recentr.core.designsystem.theme.RecentrTheme
import com.recentr.feature.onboarding.OnboardingUiState
import com.recentr.feature.onboarding.PainPoint
import com.recentr.feature.onboarding.R
import com.recentr.feature.onboarding.UsageBucket
import com.recentr.feature.onboarding.component.OnboardingHeading
import com.recentr.feature.onboarding.component.OnboardingPage
import com.recentr.feature.onboarding.component.compactDuration

/**
 * 21 · Onboarding — All set (Figma 2024:1511). Name, goal, first rule and reason come from the
 * answers ("Mia", 4h20, Bedtime for "It hurts my sleep", the TikTok note for "I scroll…").
 */
@Composable
internal fun AllSetScreen(
    state: OnboardingUiState,
    onStart: () -> Unit,
) {
    OnboardingPage(
        backdrop = Backdrop.Sky,
        bottomBar = {
            RecentrButton(
                text = stringResource(R.string.onb_all_set_cta),
                onClick = onStart,
                modifier = Modifier.fillMaxWidth(),
                style = RecentrButtonStyle.Brand,
                icon = RecentrIcon.Plus,
            )
        },
    ) {
        Spacer(Modifier.height(56.dp))
        Mascot(MascotMood.Calm, width = 176.dp)
        Spacer(Modifier.height(20.dp))
        OnboardingHeading(
            title = if (state.isNameValid) {
                stringResource(R.string.onb_all_set_title, state.trimmedName)
            } else {
                stringResource(R.string.onb_all_set_title_no_name)
            },
            subtitle = stringResource(R.string.onb_all_set_subtitle),
            centered = true,
            spacing = 6.dp,
        )
        Spacer(Modifier.height(40.dp))
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = RecentrTheme.spacing.screenGutter),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MiniTile(
                    label = stringResource(R.string.onb_all_set_daily_goal),
                    value = compactDuration(state.dailyGoalMinutes),
                    sticker = Sticker.Timer,
                    modifier = Modifier.weight(1f),
                )
                val rule = state.firstRule
                MiniTile(
                    label = stringResource(R.string.onb_all_set_first_rule),
                    value = stringResource(rule.label),
                    sticker = rule.sticker,
                    modifier = Modifier.weight(1f),
                )
            }
            StickyNote(
                title = stringResource(R.string.onb_all_set_note_title),
                body = stringResource(state.reason),
                modifier = Modifier.fillMaxWidth(),
            )
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Preview(widthDp = 402, heightDp = 874)
@Composable
private fun AllSetScreenPreview() {
    RecentrTheme {
        AllSetScreen(
            state = OnboardingUiState(
                name = "Mia",
                painPoints = setOf(PainPoint.Sleep, PainPoint.Scroll),
                usageBucket = UsageBucket.From5To7,
            ),
            onStart = {},
        )
    }
}
