package com.recentr.feature.rules.allowed

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.recentr.core.designsystem.component.RecentrIconButton
import com.recentr.core.designsystem.component.RecentrIconButtonStyle
import com.recentr.core.designsystem.component.RecentrTextButton
import com.recentr.core.designsystem.component.SettingsGroup
import com.recentr.core.designsystem.component.SettingsSectionLabel
import com.recentr.core.designsystem.component.SurfaceCard
import com.recentr.core.designsystem.icon.RecentrIcon
import com.recentr.core.designsystem.icon.RecentrIconImage
import com.recentr.core.designsystem.illustration.Backdrop
import com.recentr.core.designsystem.illustration.BackdropBox
import com.recentr.core.designsystem.illustration.InstalledAppIcon
import com.recentr.core.designsystem.illustration.Sticker
import com.recentr.core.designsystem.illustration.StickerImage
import com.recentr.core.designsystem.theme.RecentrTheme
import com.recentr.core.designsystem.util.SystemBarsAppearance
import com.recentr.core.model.AllowedChange
import com.recentr.core.model.AlwaysAllowed
import com.recentr.core.model.AppId
import com.recentr.core.model.UsageApp
import com.recentr.feature.rules.R
import com.recentr.feature.rules.sheets.ChooseAppsSheet
import com.recentr.feature.rules.ui.components.Gap
import com.recentr.feature.rules.ui.wallClock
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Settings › Always allowed, with its view model. */
@Composable
internal fun AlwaysAllowedDestination(onBack: () -> Unit) {
    val viewModel: AlwaysAllowedViewModel = viewModel(factory = AlwaysAllowedViewModel.Factory)
    val state by viewModel.state.collectAsStateWithLifecycle()
    AlwaysAllowedScreen(
        state = state,
        onBack = onBack,
        onAllow = viewModel::allow,
        onRemove = viewModel::remove,
        onDropChanges = viewModel::dropChanges,
    )
}

/**
 * Settings › Always allowed (docs/RULES.md › "Always allowed"): what it does, when the apps
 * waiting to join will ("Your changes apply at midnight", with "Drop changes"), the apps allowed
 * then the ones waiting, each with its remove button, and "Add app", which opens the picker on all
 * of them.
 */
@Composable
internal fun AlwaysAllowedScreen(
    state: AlwaysAllowedUiState,
    onBack: () -> Unit,
    onAllow: (List<UsageApp>) -> Unit,
    onRemove: (UsageApp) -> Unit,
    onDropChanges: () -> Unit,
    modifier: Modifier = Modifier,
) {
    SystemBarsAppearance(lightIcons = false)
    val colors = RecentrTheme.colors
    var picking by rememberSaveable { mutableStateOf(false) }
    BackdropBox(Backdrop.Aurora, modifier) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = RecentrTheme.spacing.screenGutter),
        ) {
            Gap(8.dp)
            RecentrIconButton(RecentrIcon.ChevronLeft, contentDescription = stringResource(R.string.rules_back), onClick = onBack)
            Gap(16.dp)
            Text(
                stringResource(R.string.rules_allowed_title),
                style = RecentrTheme.typography.displayLarge,
                color = colors.textPrimary,
                modifier = Modifier.semantics { heading() },
            )
            Gap(19.dp)
            Explainer()
            state.joinsAt?.let { at ->
                Gap(12.dp)
                JoinNotice(JoinTime.of(at, state.now), onDropChanges)
            }
            Gap(22.dp)
            SettingsSectionLabel(
                stringResource(R.string.rules_allowed_section) + stringResource(R.string.rules_apps_section_count, state.apps.size + state.waiting.size),
                modifier = Modifier.padding(start = 4.dp, bottom = 8.dp),
            )
            SettingsGroup {
                state.apps.forEach { app -> key(app.packageName) { AllowedAppRow(app, waiting = false, onRemove = { onRemove(app) }) } }
                state.waiting.forEach { app -> key(app.packageName) { AllowedAppRow(app, waiting = true, onRemove = { onRemove(app) }) } }
                AddAppRow(empty = state.apps.isEmpty() && state.waiting.isEmpty(), onClick = { picking = true })
            }
            Gap(32.dp)
        }
    }
    if (picking) {
        ChooseAppsSheet(
            initial = state.latestWish,
            onDone = onAllow,
            onDismiss = { picking = false },
            title = stringResource(R.string.rules_allowed_title),
            doneText = { picked ->
                if (picked == 0) stringResource(R.string.rules_allowed_done_none) else pluralStringResource(R.plurals.rules_allowed_done, picked, picked)
            },
            allowsNone = true,
        )
    }
}

/** The shield, and what Always allowed does. */
@Composable
private fun Explainer() {
    SurfaceCard(Modifier.fillMaxWidth()) {
        Row(
            Modifier.padding(horizontal = 20.dp, vertical = 18.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            StickerImage(Sticker.Shield, size = 64.dp)
            Text(
                stringResource(R.string.rules_allowed_explainer),
                style = RecentrTheme.typography.bodyMedium,
                color = RecentrTheme.colors.textPrimary,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

/** "Your changes apply at midnight" and "Drop changes", like a rule's kept change. */
@Composable
private fun JoinNotice(time: JoinTime, onDrop: () -> Unit) {
    val colors = RecentrTheme.colors
    val text = when (time) {
        JoinTime.Midnight -> stringResource(R.string.rules_pending_changes_midnight)
        is JoinTime.Today -> stringResource(R.string.rules_pending_changes_at, wallClock(time.at))
        is JoinTime.Tomorrow -> stringResource(R.string.rules_allowed_changes_tomorrow, wallClock(time.at))
        is JoinTime.Later -> stringResource(R.string.rules_allowed_changes_on, "${weekday(time.at)} ${wallClock(time.at)}")
    }
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(colors.alphaSky12)
            .padding(start = 14.dp, end = 4.dp, top = 6.dp, bottom = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(32.dp).clip(RoundedCornerShape(10.dp)).background(colors.bgSurface), contentAlignment = Alignment.Center) {
            RecentrIconImage(RecentrIcon.Clock, tint = colors.brandSky, size = 18.dp)
        }
        Text(text, style = RecentrTheme.typography.bodyMedium, color = colors.textPrimary, modifier = Modifier.weight(1f))
        RecentrTextButton(text = stringResource(R.string.rules_pending_drop), onClick = onDrop, color = colors.brandSky)
    }
}

/** An app allowed, or [waiting] to join: its icon and name, and its remove button. */
@Composable
private fun AllowedAppRow(app: UsageApp, waiting: Boolean, onRemove: () -> Unit) {
    val colors = RecentrTheme.colors
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(colors.bgSurface)
            .padding(start = 16.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        InstalledAppIcon(app.packageName, size = 32.dp)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(app.label, style = RecentrTheme.typography.bodyMedium, color = colors.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (waiting) {
                Text(stringResource(R.string.rules_allowed_waiting), style = RecentrTheme.typography.captionRegular, color = colors.textSecondary)
            }
        }
        RecentrIconButton(
            RecentrIcon.X,
            contentDescription = stringResource(R.string.rules_allowed_remove, app.label),
            onClick = onRemove,
            style = RecentrIconButtonStyle.Sunken,
            size = 32.dp,
            iconSize = 16.dp,
            tint = colors.textSecondary,
        )
    }
}

/** "Add app": opens the picker. With nothing allowed yet, a few ideas under it. */
@Composable
private fun AddAppRow(empty: Boolean, onClick: () -> Unit) {
    val colors = RecentrTheme.colors
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(colors.bgSurface)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(start = 16.dp, end = 14.dp, top = 14.dp, bottom = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(32.dp).clip(RoundedCornerShape(10.dp)).background(colors.bgSunken), contentAlignment = Alignment.Center) {
            RecentrIconImage(RecentrIcon.Plus, tint = colors.textPrimary, size = 19.2.dp)
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(stringResource(R.string.rules_allowed_add), style = RecentrTheme.typography.bodyMedium, color = colors.textPrimary)
            if (empty) {
                Text(stringResource(R.string.rules_allowed_none), style = RecentrTheme.typography.captionRegular, color = colors.textSecondary)
            }
        }
    }
}

/** "Wed", in the device's language. */
private fun weekday(epochMillis: Long): String =
    DateTimeFormatter.ofPattern("EEE", Locale.getDefault()).format(Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()))

@Preview(widthDp = 402, heightDp = 874)
@Composable
private fun AlwaysAllowedPreview() {
    val maps = UsageApp.of(AppId.Gmail)
    val spotify = UsageApp.of(AppId.Spotify)
    val state = AlwaysAllowedUiState(
        allowed = AlwaysAllowed(listOf(maps), AllowedChange(2 * 3_600_000L, listOf(maps, spotify))),
        now = 0L,
    )
    RecentrTheme {
        AlwaysAllowedScreen(state = state, onBack = {}, onAllow = {}, onRemove = {}, onDropChanges = {})
    }
}
