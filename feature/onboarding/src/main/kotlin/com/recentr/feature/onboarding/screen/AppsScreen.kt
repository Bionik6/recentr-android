package com.recentr.feature.onboarding.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.recentr.core.designsystem.component.MiniTile
import com.recentr.core.designsystem.component.RecentrButton
import com.recentr.core.designsystem.component.RecentrButtonStyle
import com.recentr.core.designsystem.format.clockTime
import com.recentr.core.designsystem.icon.RecentrIcon
import com.recentr.core.designsystem.illustration.Sticker
import com.recentr.core.designsystem.motion.entrance
import com.recentr.core.designsystem.theme.RecentrTheme
import com.recentr.core.model.AppId
import com.recentr.core.model.StarterRoutine
import com.recentr.core.model.UsageApp
import com.recentr.feature.onboarding.R
import com.recentr.feature.onboarding.component.ContinueButton
import com.recentr.feature.onboarding.component.HeaderProgress
import com.recentr.feature.onboarding.component.OnboardingHeading
import com.recentr.feature.onboarding.component.OnboardingPage
import com.recentr.feature.onboarding.title

/**
 * The apps the starter routines block (not in Figma; iOS has the same step, right after Screen
 * Time): the three routines, then "Choose apps" ([onChooseApps]) or the apps picked. Nothing is
 * picked for the user; Continue once they've picked one.
 */
@Composable
internal fun AppsScreen(
    progress: HeaderProgress,
    apps: List<UsageApp>,
    onChooseApps: () -> Unit,
    onBack: () -> Unit,
    onContinue: () -> Unit,
) {
    OnboardingPage(
        progress = progress,
        onBack = onBack,
        bottomBar = {
            RecentrButton(
                text = if (apps.isEmpty()) {
                    stringResource(R.string.onb_apps_choose)
                } else {
                    pluralStringResource(R.plurals.onb_apps_count, apps.size, apps.size)
                },
                onClick = onChooseApps,
                style = RecentrButtonStyle.Secondary,
                icon = if (apps.isEmpty()) RecentrIcon.Plus else RecentrIcon.Check,
                modifier = Modifier
                    .fillMaxWidth()
                    .entrance(500),
            )
            ContinueButton(enabled = apps.isNotEmpty(), onClick = onContinue, modifier = Modifier.entrance(600))
        },
    ) {
        Spacer(Modifier.height(20.dp))
        OnboardingHeading(
            title = stringResource(R.string.onb_apps_title),
            subtitle = stringResource(R.string.onb_apps_subtitle),
        )
        Spacer(Modifier.height(40.dp))
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = RecentrTheme.spacing.screenGutter),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            StarterRoutine.entries.forEachIndexed { index, routine ->
                MiniTile(
                    label = routine.scheduleLabel(),
                    value = stringResource(routine.title),
                    sticker = routine.stickerImage,
                    modifier = Modifier
                        .fillMaxWidth()
                        .entrance(350 + index * 120),
                )
            }
        }
        Spacer(Modifier.height(16.dp))
    }
}

/** "9:00 AM – 5:00 PM · Weekdays", on the clock the app shows. */
@Composable
@ReadOnlyComposable
private fun StarterRoutine.scheduleLabel(): String = stringResource(
    R.string.onb_apps_schedule,
    clockTime(schedule.startMinute),
    clockTime(schedule.endMinute),
    stringResource(
        when (this) {
            StarterRoutine.WorkingTime -> R.string.onb_days_weekdays
            StarterRoutine.NightPause -> R.string.onb_days_every_day
            StarterRoutine.WeekendZen -> R.string.onb_days_weekends
        },
    ),
)

private val StarterRoutine.stickerImage: Sticker
    get() = when (this) {
        StarterRoutine.WorkingTime -> Sticker.Bolt
        StarterRoutine.NightPause -> Sticker.Moon
        StarterRoutine.WeekendZen -> Sticker.Leaf
    }

@Preview(widthDp = 402, heightDp = 874)
@Composable
private fun AppsScreenPreview() {
    RecentrTheme {
        AppsScreen(
            HeaderProgress(0.95f),
            apps = listOf(AppId.Instagram, AppId.TikTok, AppId.X).map { UsageApp.of(it) },
            onChooseApps = {},
            onBack = {},
            onContinue = {},
        )
    }
}
