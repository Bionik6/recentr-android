package com.recentr.feature.profile.settings

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.text.format.DateFormat
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
import com.recentr.core.data.BlockingAccess
import com.recentr.core.data.EmergencyUnlocks
import com.recentr.core.designsystem.component.BlockingAccessSheet
import com.recentr.core.designsystem.component.RecentrAlertDialog
import com.recentr.core.designsystem.component.SettingRow
import com.recentr.core.designsystem.component.SettingToggleRow
import com.recentr.core.designsystem.component.SettingsGroup
import com.recentr.core.designsystem.component.SettingsSectionLabel
import com.recentr.core.designsystem.format.LocalUse24HourClock
import com.recentr.core.designsystem.icon.RecentrIcon
import com.recentr.core.designsystem.theme.RecentrTheme
import com.recentr.core.model.AppLanguage
import com.recentr.core.model.AppSettings
import com.recentr.core.model.Rule
import com.recentr.core.model.TimeFormat
import com.recentr.feature.profile.R
import com.recentr.feature.profile.ui.SettingsPage
import com.recentr.feature.profile.ui.label
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
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
    /** Opens a rule's unlock pass (53). */
    val onOpenUnlockPass: (ruleId: String) -> Unit = {},
    val onReplayOnboarding: (() -> Unit)? = null,
    val onOpenDesignSystem: (() -> Unit)? = null,
)

private enum class SettingsOverlay { TimeFormat, Blocking, Emergency, UnlockPass, LogOut, Delete }

/** 66 · Settings (+ 71 / 72 sheets): General, Protection, Support and Account groups. */
@Composable
internal fun SettingsScreen(
    state: SettingsUiState,
    actions: SettingsActions,
    modifier: Modifier = Modifier,
    /** Whether app blocking (the accessibility service) is on. */
    blockingOn: Boolean = true,
    /** The rules unlocked by scanning a printed pass. */
    passRules: List<Rule> = emptyList(),
) {
    val context = LocalContext.current
    val settings = state.settings
    var overlay by rememberSaveable { mutableStateOf<SettingsOverlay?>(null) }
    val notificationsOn = settings.anyNotification && NotificationManagerCompat.from(context).areNotificationsEnabled()
    val timeFormat = settings.timeFormat ?: if (LocalUse24HourClock.current) TimeFormat.TwentyFourHour else TimeFormat.AmPm
    val now = System.currentTimeMillis()
    val zone = ZoneId.systemDefault()
    val emergencyLeft = EmergencyUnlocks.left(settings, now, zone)

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
                ValueRow(R.string.settings_time_format, stringResource(timeFormat.label), RecentrIcon.Clock) {
                    overlay = SettingsOverlay.TimeFormat
                }
            }
            Spacer(Modifier.height(6.dp))
            Section(R.string.settings_protection) {
                ValueRow(R.string.settings_app_blocking, stringResource(if (blockingOn) R.string.settings_on else R.string.settings_off), RecentrIcon.Lock) {
                    // Off: explain it first. On: straight to Settings, where it can be turned off.
                    if (blockingOn) BlockingAccess.openSettings(context) else overlay = SettingsOverlay.Blocking
                }
                SettingToggleRow(
                    label = stringResource(R.string.settings_strict_mode),
                    checked = settings.strictByDefault,
                    onCheckedChange = { on -> actions.onUpdate { it.copy(strictByDefault = on) } },
                    icon = RecentrIcon.Shield,
                    iconTint = RecentrTheme.colors.accentTangerine,
                    iconBackground = RecentrTheme.colors.alphaTangerine14,
                    caption = stringResource(R.string.settings_strict_caption),
                    modifier = Modifier.fillMaxWidth(),
                )
                // Off by default: a lock or a limit that's on keeps what makes it easier for midnight.
                SettingToggleRow(
                    label = stringResource(R.string.settings_changes_anytime),
                    checked = settings.changesLocksAnytime,
                    onCheckedChange = { on -> actions.onUpdate { it.copy(changesLocksAnytime = on) } },
                    icon = RecentrIcon.Pencil,
                    caption = stringResource(R.string.settings_changes_anytime_caption),
                    modifier = Modifier.fillMaxWidth(),
                )
                ValueRow(R.string.settings_emergency_unlocks, stringResource(R.string.settings_unlocks_left, emergencyLeft), RecentrIcon.Lock) {
                    overlay = SettingsOverlay.Emergency
                }
                ValueRow(
                    R.string.settings_unlock_pass,
                    when (passRules.size) {
                        0 -> stringResource(R.string.settings_pass_none)
                        1 -> passRules.first().name
                        else -> stringResource(R.string.settings_pass_rules, passRules.size)
                    },
                    RecentrIcon.Qr,
                ) {
                    passRules.singleOrNull()?.let { actions.onOpenUnlockPass(it.id) } ?: run { overlay = SettingsOverlay.UnlockPass }
                }
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
        SettingsOverlay.TimeFormat -> TimeFormatSheet(
            selected = timeFormat,
            onSelect = { format -> actions.onUpdate { it.copy(timeFormat = format) } },
            onDismissRequest = { overlay = null },
        )
        SettingsOverlay.Blocking -> BlockingAccessSheet(
            onOpenSettings = { BlockingAccess.openSettings(context) },
            onDismissRequest = { overlay = null },
        )
        SettingsOverlay.Emergency -> RecentrAlertDialog(
            title = stringResource(R.string.emergency_title),
            body = stringResource(R.string.emergency_body, emergencyLeft, EmergencyUnlocks.PER_MONTH, monthDay(EmergencyUnlocks.resetAt(now, zone))),
            primaryText = stringResource(R.string.dialog_ok),
            onPrimary = { overlay = null },
            onDismissRequest = { overlay = null },
        )
        SettingsOverlay.UnlockPass -> UnlockPassSheet(
            rules = passRules,
            onOpen = actions.onOpenUnlockPass,
            onDismissRequest = { overlay = null },
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

/** "November 1", in the device's language. */
private fun monthDay(epochMillis: Long): String {
    val locale = Locale.getDefault()
    val pattern = DateFormat.getBestDateTimePattern(locale, "MMMMd")
    return DateTimeFormatter.ofPattern(pattern, locale).format(Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()))
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
