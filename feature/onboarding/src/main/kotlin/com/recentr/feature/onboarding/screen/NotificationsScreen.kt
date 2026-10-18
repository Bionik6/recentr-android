package com.recentr.feature.onboarding.screen

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import com.recentr.core.designsystem.component.RecentrButton
import com.recentr.core.designsystem.component.RecentrButtonStyle
import com.recentr.core.designsystem.illustration.Sticker
import com.recentr.core.designsystem.illustration.StickerImage
import com.recentr.core.designsystem.motion.entrance
import com.recentr.core.designsystem.theme.RecentrElevation
import com.recentr.core.designsystem.theme.RecentrShapes
import com.recentr.core.designsystem.theme.RecentrTheme
import com.recentr.core.designsystem.theme.recentrShadow
import com.recentr.feature.onboarding.R
import com.recentr.feature.onboarding.component.HeaderProgress
import com.recentr.feature.onboarding.component.OnboardingHeading
import com.recentr.feature.onboarding.component.OnboardingPage
import com.recentr.feature.onboarding.component.compactDuration
import com.recentr.feature.onboarding.permission.canPostNotifications

/**
 * 19 · Onboarding — Notifications (Figma 2024:1392). "Turn on notifications" requests
 * `POST_NOTIFICATIONS` on API 33+ (earlier versions report the app setting); the result is stored
 * in the profile and the flow moves on either way. "Not now" skips.
 *
 * @param onResult called with whether notifications are enabled.
 */
@Composable
internal fun NotificationsRoute(
    progress: HeaderProgress,
    dailyGoalMinutes: Int,
    onBack: () -> Unit,
    onResult: (enabled: Boolean) -> Unit,
) {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        onResult(granted)
    }
    NotificationsScreen(
        progress = progress,
        dailyGoalMinutes = dailyGoalMinutes,
        onBack = onBack,
        onTurnOn = {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !context.canPostNotifications()) {
                launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                onResult(context.canPostNotifications())
            }
        },
        onNotNow = { onResult(false) },
    )
}

@Composable
internal fun NotificationsScreen(
    progress: HeaderProgress,
    dailyGoalMinutes: Int,
    onBack: () -> Unit,
    onTurnOn: () -> Unit,
    onNotNow: () -> Unit,
) {
    OnboardingPage(
        progress = progress,
        onBack = onBack,
        bottomBar = {
            RecentrButton(stringResource(R.string.onb_notif_turn_on), onTurnOn, Modifier.fillMaxWidth().entrance(500))
            RecentrButton(
                text = stringResource(R.string.onb_notif_not_now),
                onClick = onNotNow,
                modifier = Modifier
                    .fillMaxWidth()
                    .entrance(600, blur = false),
                style = RecentrButtonStyle.Secondary,
            )
        },
    ) {
        Spacer(Modifier.height(20.dp))
        OnboardingHeading(
            title = stringResource(R.string.onb_notif_title),
            subtitle = stringResource(R.string.onb_notif_subtitle),
        )
        Spacer(Modifier.height(57.dp))
        // Three tilted, overlapping notification banners (354×300 group), dropping in one by one.
        Box(Modifier.size(width = 354.dp, height = 316.dp)) {
            NotificationBanner(
                sticker = Sticker.Moon,
                title = stringResource(R.string.onb_notif_banner1_title),
                body = stringResource(R.string.onb_notif_banner1_body),
                modifier = Modifier.offset(x = 1.3.dp, y = 8.8.dp).dropIn(0).rotate(3f),
            )
            NotificationBanner(
                sticker = Sticker.Timer,
                title = stringResource(R.string.onb_notif_banner2_title),
                body = stringResource(R.string.onb_notif_banner2_body),
                modifier = Modifier.offset(x = 8.4.dp, y = 98.dp).dropIn(1).rotate(-2f),
            )
            NotificationBanner(
                sticker = Sticker.Flame,
                title = stringResource(R.string.onb_notif_banner3_title),
                body = stringResource(R.string.onb_notif_banner3_body, compactDuration(dailyGoalMinutes)),
                modifier = Modifier.offset(x = 4.2.dp, y = 212.4.dp).dropIn(2).rotate(1.5f),
            )
        }
    }
}

/** The [index]th banner drops in from above, 350ms after the previous one. */
private fun Modifier.dropIn(index: Int) = entrance(350 + index * 350, from = DpOffset(0.dp, (-70).dp), blur = false)

/** Frosted notification banner (alpha/white-80, radius 24, Elevation/Float). */
@Composable
private fun NotificationBanner(sticker: Sticker, title: String, body: String, modifier: Modifier = Modifier) {
    val colors = RecentrTheme.colors
    Row(
        modifier = modifier
            .width(340.dp)
            .recentrShadow(RecentrElevation.float, RecentrShapes.xl)
            .clip(RecentrShapes.xl)
            .background(colors.alphaWhite80)
            .semantics(mergeDescendants = true) {}
            .padding(start = 14.dp, end = 16.dp, top = 14.dp, bottom = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        StickerImage(sticker, size = 43.2.dp)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Row(Modifier.fillMaxWidth()) {
                Text(
                    text = stringResource(R.string.onb_app_name_caps),
                    style = RecentrTheme.typography.labelSmall,
                    color = colors.textTertiary,
                    modifier = Modifier.weight(1f),
                )
                Text(stringResource(R.string.onb_notif_now), style = RecentrTheme.typography.labelSmall, color = colors.textTertiary)
            }
            Text(title, style = RecentrTheme.typography.headline, color = colors.textPrimary, maxLines = 1)
            Text(body, style = RecentrTheme.typography.captionRegular, color = colors.textSecondary)
        }
    }
}

@Preview(widthDp = 402, heightDp = 874)
@Composable
private fun NotificationsScreenPreview() {
    RecentrTheme {
        NotificationsScreen(HeaderProgress(1f), dailyGoalMinutes = 260, onBack = {}, onTurnOn = {}, onNotNow = {})
    }
}
