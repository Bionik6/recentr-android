package com.recentr.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.recentr.core.designsystem.R
import com.recentr.core.designsystem.icon.RecentrIcon
import com.recentr.core.designsystem.icon.RecentrIconImage
import com.recentr.core.designsystem.illustration.Sticker
import com.recentr.core.designsystem.illustration.StickerImage
import com.recentr.core.designsystem.theme.RecentrTheme

/**
 * Explains app blocking before sending the user to Settings › Accessibility (the disclosure the
 * Accessibility service needs): what it sees, what it doesn't, then "Open Settings".
 */
@Composable
fun BlockingAccessSheet(onOpenSettings: () -> Unit, onDismissRequest: () -> Unit) {
    val colors = RecentrTheme.colors
    RecentrSheet(onDismissRequest = onDismissRequest) { controller ->
        Column(
            Modifier.fillMaxWidth().padding(start = 24.dp, end = 24.dp, top = 16.dp, bottom = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            StickerImage(Sticker.Lock, size = 72.dp)
            Spacer(Modifier.height(12.dp))
            Text(
                stringResource(R.string.ds_blocking_title),
                style = RecentrTheme.typography.title2,
                color = colors.textPrimary,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                stringResource(R.string.ds_blocking_body),
                style = RecentrTheme.typography.bodyRegular,
                color = colors.textSecondary,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(14.dp))
            Text(
                stringResource(R.string.ds_blocking_steps),
                style = RecentrTheme.typography.captionMedium,
                color = colors.textTertiary,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(colors.bgSunken)
                    .padding(horizontal = 14.dp, vertical = 12.dp),
            )
            Spacer(Modifier.height(20.dp))
            RecentrButton(
                text = stringResource(R.string.ds_blocking_open_settings),
                onClick = { controller.close(onOpenSettings) },
                style = RecentrButtonStyle.Brand,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(10.dp))
            RecentrButton(
                text = stringResource(R.string.ds_blocking_not_now),
                onClick = { controller.close() },
                style = RecentrButtonStyle.Soft,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

/** "Blocking is off" card: rules and focus sessions can't put apps on hold until it's turned on. */
@Composable
fun BlockingOffBanner(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = RecentrTheme.colors
    val shape = RoundedCornerShape(20.dp)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(colors.alphaTangerine14)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(36.dp).clip(RoundedCornerShape(12.dp)).background(colors.bgSurface), contentAlignment = Alignment.Center) {
            RecentrIconImage(RecentrIcon.Shield, tint = colors.accentTangerine, size = 20.dp)
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(stringResource(R.string.ds_blocking_off_title), style = RecentrTheme.typography.headline, color = colors.textPrimary)
            Text(stringResource(R.string.ds_blocking_off_body), style = RecentrTheme.typography.captionRegular, color = colors.textSecondary)
        }
        RecentrIconImage(RecentrIcon.ChevronRight, tint = colors.textTertiary, size = 18.dp)
    }
}

@Preview
@Composable
private fun BlockingOffBannerPreview() {
    RecentrTheme {
        BlockingOffBanner(onClick = {}, modifier = Modifier.padding(16.dp))
    }
}
