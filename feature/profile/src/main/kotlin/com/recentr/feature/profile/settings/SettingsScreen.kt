package com.recentr.feature.profile.settings

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationManagerCompat
import androidx.core.net.toUri
import com.recentr.core.designsystem.component.RecentrAlertDialog
import com.recentr.core.designsystem.component.SettingRow
import com.recentr.core.designsystem.component.SettingsGroup
import com.recentr.core.designsystem.component.SettingsSectionLabel
import com.recentr.core.designsystem.icon.RecentrIcon
import com.recentr.core.designsystem.theme.RecentrTheme
import com.recentr.core.model.AppLanguage
import com.recentr.core.model.AppSettings
import com.recentr.feature.profile.R
import com.recentr.feature.profile.ui.SettingsPage
import com.recentr.feature.profile.ui.label
import java.util.Locale

/** Callbacks of the Settings screen. Developer actions are only passed in debug builds. */
internal class SettingsActions(
    val onBack: () -> Unit,
    val onOpenNotifications: () -> Unit,
    val onOpenAppearance: () -> Unit,
    val onOpenLanguage: () -> Unit,
    val onOpenHelp: () -> Unit,
    val onOpenPlus: () -> Unit,
    val onUpdate: ((AppSettings) -> AppSettings) -> Unit,
    val onLogOut: () -> Unit,
    val onDeleteAccount: () -> Unit,
    val onReplayOnboarding: (() -> Unit)? = null,
    val onOpenDesignSystem: (() -> Unit)? = null,
)

private enum class SettingsOverlay { Strict, Emergency, UnlockPass, LogOut, Delete }

/** 66 · Settings (+ 71 / 72 sheets): General, Protection, Support and Account groups. */
@Composable
internal fun SettingsScreen(
    state: SettingsUiState,
    actions: SettingsActions,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val settings = state.settings
    var overlay by rememberSaveable { mutableStateOf<SettingsOverlay?>(null) }
    val notificationsOn = settings.anyNotification && NotificationManagerCompat.from(context).areNotificationsEnabled()

    SettingsPage(
        title = stringResource(R.string.settings_title),
        onBack = actions.onBack,
        bodyGap = 19.dp,
        fade = true,
        modifier = modifier,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Section(R.string.settings_general) {
                ValueRow(R.string.settings_notifications, stringResource(if (notificationsOn) R.string.settings_on else R.string.settings_off), RecentrIcon.Bell, actions.onOpenNotifications)
                ValueRow(R.string.settings_appearance, stringResource(settings.theme.label), RecentrIcon.Sun, actions.onOpenAppearance)
                ValueRow(R.string.settings_app_icon, stringResource(settings.appIcon.label), RecentrIcon.Apps, actions.onOpenAppearance)
                ValueRow(R.string.settings_language, (settings.language ?: systemLanguage()).endonym, RecentrIcon.Sparkle, actions.onOpenLanguage)
            }
            Spacer(Modifier.height(6.dp))
            Section(R.string.settings_protection) {
                SettingRow(
                    label = stringResource(R.string.settings_strict_mode),
                    value = stringResource(settings.strictScope.label),
                    icon = RecentrIcon.Shield,
                    iconTint = RecentrTheme.colors.accentTangerine,
                    iconBackground = RecentrTheme.colors.alphaTangerine14,
                    onClick = { overlay = SettingsOverlay.Strict },
                    modifier = Modifier.fillMaxWidth(),
                )
                ValueRow(R.string.settings_emergency_unlocks, stringResource(R.string.settings_unlocks_left, settings.emergencyUnlocksLeft), RecentrIcon.Lock) {
                    overlay = SettingsOverlay.Emergency
                }
                ValueRow(
                    R.string.settings_unlock_pass,
                    stringResource(if (settings.unlockPassPrinted) R.string.settings_pass_printed else R.string.settings_pass_not_printed),
                    RecentrIcon.Qr,
                ) { overlay = SettingsOverlay.UnlockPass }
            }
            Spacer(Modifier.height(6.dp))
            Section(R.string.settings_support) {
                ValueRow(R.string.settings_help, null, RecentrIcon.Help, actions.onOpenHelp)
                ValueRow(R.string.settings_rate, null, RecentrIcon.StarFill) { openStoreListing(context) }
            }
            Spacer(Modifier.height(6.dp))
            Section(R.string.settings_account) {
                ValueRow(
                    R.string.settings_plus,
                    stringResource(settings.plusPlan?.label ?: R.string.settings_plus_free),
                    RecentrIcon.Flame,
                    actions.onOpenPlus,
                )
                ValueRow(R.string.settings_log_out, null, RecentrIcon.ArrowRight) { overlay = SettingsOverlay.LogOut }
                SettingRow(
                    label = stringResource(R.string.settings_delete_account),
                    value = null,
                    icon = RecentrIcon.X,
                    iconTint = RecentrTheme.colors.feedbackDanger,
                    labelColor = RecentrTheme.colors.feedbackDanger,
                    onClick = { overlay = SettingsOverlay.Delete },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            if (actions.onReplayOnboarding != null || actions.onOpenDesignSystem != null) {
                Spacer(Modifier.height(6.dp))
                Section(R.string.settings_developer) {
                    actions.onReplayOnboarding?.let { ValueRow(R.string.settings_replay_onboarding, null, RecentrIcon.Refresh, it) }
                    actions.onOpenDesignSystem?.let { ValueRow(R.string.settings_design_system, null, RecentrIcon.Apps, it) }
                }
            }
        }
    }

    when (overlay) {
        SettingsOverlay.Strict -> StrictModeSheet(
            selected = settings.strictScope,
            onSelect = { scope -> actions.onUpdate { it.copy(strictScope = scope) } },
            onDismissRequest = { overlay = null },
        )
        SettingsOverlay.Emergency -> RecentrAlertDialog(
            title = stringResource(R.string.emergency_title),
            body = stringResource(R.string.emergency_body, settings.emergencyUnlocksLeft),
            primaryText = stringResource(R.string.dialog_ok),
            onPrimary = { overlay = null },
            onDismissRequest = { overlay = null },
        )
        SettingsOverlay.UnlockPass -> RecentrAlertDialog(
            title = stringResource(R.string.unlock_pass_title),
            body = stringResource(R.string.unlock_pass_body),
            primaryText = stringResource(R.string.unlock_pass_printed),
            onPrimary = {
                overlay = null
                actions.onUpdate { it.copy(unlockPassPrinted = true) }
            },
            onDismissRequest = { overlay = null },
            secondaryText = stringResource(R.string.unlock_pass_not_printed),
            onSecondary = {
                overlay = null
                actions.onUpdate { it.copy(unlockPassPrinted = false) }
            },
        )
        SettingsOverlay.LogOut -> LogOutSheet(onLogOut = actions.onLogOut, onDismissRequest = { overlay = null })
        SettingsOverlay.Delete -> DeleteAccountSheet(onDelete = actions.onDeleteAccount, onDismissRequest = { overlay = null })
        null -> Unit
    }
}

@Composable
private fun Section(title: Int, rows: @Composable ColumnScope.() -> Unit) {
    SettingsSectionLabel(stringResource(title))
    SettingsGroup(content = rows)
}

@Composable
private fun ValueRow(label: Int, value: String?, icon: RecentrIcon, onClick: () -> Unit) {
    SettingRow(label = stringResource(label), value = value, icon = icon, onClick = onClick, modifier = Modifier.fillMaxWidth())
}

/** The language shown when the app follows the system: the system one if offered, else English. */
internal fun systemLanguage(): AppLanguage =
    AppLanguage.fromTag(Locale.getDefault().language) ?: AppLanguage.English

private fun openStoreListing(context: Context) {
    val market = Intent(Intent.ACTION_VIEW, "market://details?id=${context.packageName}".toUri())
    try {
        context.startActivity(market)
    } catch (_: ActivityNotFoundException) {
        val web = Intent(Intent.ACTION_VIEW, "https://play.google.com/store/apps/details?id=${context.packageName}".toUri())
        try {
            context.startActivity(web)
        } catch (_: ActivityNotFoundException) {
        }
    }
}
