package com.recentr.feature.home.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.recentr.core.designsystem.icon.RecentrIcon
import com.recentr.core.designsystem.icon.RecentrIconImage
import com.recentr.core.designsystem.illustration.InstalledAppIcon
import com.recentr.core.designsystem.illustration.Sticker
import com.recentr.core.designsystem.illustration.StickerImage
import com.recentr.core.designsystem.theme.RecentrElevation
import com.recentr.core.designsystem.theme.RecentrShapes
import com.recentr.core.designsystem.theme.RecentrTheme
import com.recentr.core.designsystem.theme.recentrShadow
import com.recentr.core.model.AppCategory
import com.recentr.core.model.UsageApp
import com.recentr.feature.home.R

/** The most app icons in the card's row, before "+4". */
private const val ICON_LIMIT = 3

/**
 * Always allowed (Figma 22 · Home, "Always Allowed Card"): the [apps] let through, or none yet,
 * and Set up, which opens Settings › Always allowed ([onSetUp]). The × ([onClose]) asks to close
 * the card: Home first says where to find it again.
 */
@Composable
internal fun AlwaysAllowedCard(apps: List<UsageApp>, onSetUp: () -> Unit, onClose: () -> Unit, modifier: Modifier = Modifier) {
    val colors = RecentrTheme.colors
    Column(
        modifier = modifier
            .fillMaxWidth()
            .recentrShadow(RecentrElevation.card, RecentrShapes.card)
            .clip(RecentrShapes.card)
            .background(colors.bgSurface)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            StickerImage(Sticker.Shield, size = 36.dp)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
                Text(stringResource(R.string.home_always_allowed_title), style = RecentrTheme.typography.headline, color = colors.textPrimary)
                Text(stringResource(R.string.home_always_allowed_subtitle), style = RecentrTheme.typography.captionRegular, color = colors.textSecondary)
            }
            val close = stringResource(R.string.home_always_allowed_close)
            Box(
                Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .clickable(role = Role.Button, onClickLabel = close, onClick = onClose)
                    .clearAndSetSemantics { contentDescription = close },
                contentAlignment = Alignment.Center,
            ) {
                RecentrIconImage(RecentrIcon.X, tint = colors.textTertiary, size = 18.dp)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            if (apps.isEmpty()) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(stringResource(R.string.home_always_allowed_empty_title), style = RecentrTheme.typography.headline, color = colors.textPrimary)
                    Text(stringResource(R.string.home_always_allowed_empty_body), style = RecentrTheme.typography.captionRegular, color = colors.textPrimary.copy(alpha = 0.8f))
                }
            } else {
                AppIcons(apps, Modifier.weight(1f))
            }
            SetUpButton(onSetUp)
        }
    }
}

/** Up to [ICON_LIMIT] icons, overlapping, each in a white ring, then "+4" for the rest. */
@Composable
private fun AppIcons(apps: List<UsageApp>, modifier: Modifier = Modifier) {
    val colors = RecentrTheme.colors
    val description = pluralStringResource(R.plurals.home_always_allowed_apps, apps.size, apps.size)
    Row(modifier.clearAndSetSemantics { contentDescription = description }, horizontalArrangement = Arrangement.spacedBy((-6).dp)) {
        apps.take(ICON_LIMIT).forEach { app ->
            key(app.packageName) {
                Box(Modifier.clip(RoundedCornerShape(11.dp)).background(colors.bgSurface).padding(2.dp)) {
                    InstalledAppIcon(app.packageName, size = 32.dp)
                }
            }
        }
        if (apps.size > ICON_LIMIT) {
            Box(
                Modifier.size(36.dp).border(2.dp, colors.bgSurface, CircleShape).padding(2.dp).clip(CircleShape).background(colors.bgSunken),
                contentAlignment = Alignment.Center,
            ) {
                Text("+${apps.size - ICON_LIMIT}", style = RecentrTheme.typography.captionMedium, color = colors.textSecondary)
            }
        }
    }
}

/** Figma's "Set Up CTA": a small dark capsule. */
@Composable
private fun SetUpButton(onClick: () -> Unit) {
    val colors = RecentrTheme.colors
    Box(
        Modifier
            .height(34.dp)
            .clip(CircleShape)
            .background(colors.actionPrimary)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(stringResource(R.string.home_always_allowed_set_up), style = RecentrTheme.typography.captionMedium, color = colors.textOnAction)
    }
}

@Preview(widthDp = 402)
@Composable
private fun AlwaysAllowedCardPreview() {
    val apps = listOf("com.google.android.gm" to "Gmail", "com.spotify.music" to "Spotify", "com.instagram.android" to "Instagram", "com.ubercab" to "Uber", "com.strava" to "Strava")
        .map { (packageName, label) -> UsageApp(packageName, label, AppCategory.Other) }
    RecentrTheme {
        Column(Modifier.background(RecentrTheme.colors.bgPage).padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            AlwaysAllowedCard(apps = emptyList(), onSetUp = {}, onClose = {})
            AlwaysAllowedCard(apps = apps, onSetUp = {}, onClose = {})
        }
    }
}
