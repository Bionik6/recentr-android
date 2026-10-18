package com.recentr.feature.profile.settings

import android.content.ActivityNotFoundException
import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerDefaults
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.recentr.core.designsystem.component.RecentrButton
import com.recentr.core.designsystem.component.RecentrSheet
import com.recentr.core.designsystem.component.RecentrTextButton
import com.recentr.core.designsystem.component.SettingRow
import com.recentr.core.designsystem.component.SettingToggleRow
import com.recentr.core.designsystem.component.SettingsGroup
import com.recentr.core.designsystem.component.SettingsSectionLabel
import com.recentr.core.designsystem.format.LocalUse24HourClock
import com.recentr.core.designsystem.icon.RecentrIcon
import com.recentr.core.designsystem.theme.RecentrElevation
import com.recentr.core.designsystem.theme.RecentrTheme
import com.recentr.core.designsystem.theme.recentrShadow
import com.recentr.core.model.AppSettings
import com.recentr.feature.profile.R
import com.recentr.feature.profile.ui.SettingsPage
import com.recentr.feature.profile.ui.SheetContent
import com.recentr.feature.profile.ui.formatShortClock

/** 69 · Settings — Notifications: reminders, celebrations and quiet hours. */
@Composable
internal fun NotificationsScreen(
    settings: AppSettings,
    onBack: () -> Unit,
    onUpdate: ((AppSettings) -> AppSettings) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var systemEnabled by rememberSaveable { mutableStateOf(true) }
    LifecycleResumeEffect(Unit) {
        systemEnabled = NotificationManagerCompat.from(context).areNotificationsEnabled()
        onPauseOrDispose { }
    }
    var quietHours by rememberSaveable { mutableStateOf(false) }
    SettingsPage(
        title = stringResource(R.string.notifications_title),
        subtitle = stringResource(R.string.notifications_subtitle),
        onBack = onBack,
        modifier = modifier,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (!systemEnabled) {
                SystemOffBanner(
                    onTurnOn = {
                        val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                            .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                        try {
                            context.startActivity(intent)
                        } catch (_: ActivityNotFoundException) {
                        }
                    },
                )
                Spacer(Modifier.height(8.dp))
            }
            SettingsSectionLabel(stringResource(R.string.notifications_reminders))
            SettingsGroup {
                Toggle(R.string.notifications_routine, settings.notifyBeforeRoutine, RecentrIcon.Moon) { v -> onUpdate { it.copy(notifyBeforeRoutine = v) } }
                Toggle(R.string.notifications_limit, settings.notifyLimitAlmostReached, RecentrIcon.Clock) { v -> onUpdate { it.copy(notifyLimitAlmostReached = v) } }
                Toggle(R.string.notifications_goal, settings.notifyDailyGoal, RecentrIcon.Chart) { v -> onUpdate { it.copy(notifyDailyGoal = v) } }
            }
            Spacer(Modifier.height(8.dp))
            SettingsSectionLabel(stringResource(R.string.notifications_celebrations))
            SettingsGroup {
                Toggle(R.string.notifications_streaks, settings.notifyStreaks, RecentrIcon.Flame) { v -> onUpdate { it.copy(notifyStreaks = v) } }
                Toggle(R.string.notifications_recap, settings.notifyWeeklyRecap, RecentrIcon.Sparkle) { v -> onUpdate { it.copy(notifyWeeklyRecap = v) } }
            }
            Spacer(Modifier.height(8.dp))
            SettingsSectionLabel(stringResource(R.string.notifications_quiet_hours))
            SettingsGroup {
                SettingRow(
                    label = stringResource(R.string.notifications_dont_notify),
                    value = stringResource(
                        R.string.notifications_range,
                        formatShortClock(settings.quietHoursStartMinute),
                        formatShortClock(settings.quietHoursEndMinute),
                    ),
                    icon = RecentrIcon.Bell,
                    onClick = { quietHours = true },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
    if (quietHours) {
        QuietHoursSheet(
            startMinute = settings.quietHoursStartMinute,
            endMinute = settings.quietHoursEndMinute,
            onSave = { start, end -> onUpdate { it.copy(quietHoursStartMinute = start, quietHoursEndMinute = end) } },
            onDismissRequest = { quietHours = false },
        )
    }
}

@Composable
private fun Toggle(label: Int, checked: Boolean, icon: RecentrIcon, onChange: (Boolean) -> Unit) {
    SettingToggleRow(
        label = stringResource(label),
        checked = checked,
        onCheckedChange = onChange,
        icon = icon,
        modifier = Modifier.fillMaxWidth(),
    )
}

/** Shown when notifications are blocked for the app at system level. */
@Composable
private fun SystemOffBanner(onTurnOn: () -> Unit) {
    val colors = RecentrTheme.colors
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(colors.alphaTangerine14)
            .padding(start = 16.dp, end = 4.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            stringResource(R.string.notifications_system_off),
            style = RecentrTheme.typography.captionRegular,
            color = colors.textPrimary,
            modifier = Modifier.weight(1f),
        )
        RecentrTextButton(text = stringResource(R.string.notifications_turn_on), onClick = onTurnOn, color = colors.accentTangerine)
    }
}

/** Quiet hours: start and end times, each edited with the Material time picker. */
@Composable
private fun QuietHoursSheet(
    startMinute: Int,
    endMinute: Int,
    onSave: (Int, Int) -> Unit,
    onDismissRequest: () -> Unit,
) {
    var start by rememberSaveable { mutableIntStateOf(startMinute) }
    var end by rememberSaveable { mutableIntStateOf(endMinute) }
    var editing by rememberSaveable { mutableStateOf<Boolean?>(null) } // true = start, false = end
    RecentrSheet(onDismissRequest = onDismissRequest) { controller ->
        SheetContent(topGap = 22.dp, horizontalAlignment = Alignment.Start) {
            Text(stringResource(R.string.quiet_title), style = RecentrTheme.typography.title2, color = RecentrTheme.colors.textPrimary)
            Spacer(Modifier.height(4.dp))
            Text(stringResource(R.string.quiet_subtitle), style = RecentrTheme.typography.bodyRegular, color = RecentrTheme.colors.textSecondary)
            Spacer(Modifier.height(18.dp))
            SettingsGroup {
                SettingRow(
                    label = stringResource(R.string.quiet_from),
                    value = formatShortClock(start),
                    icon = RecentrIcon.Moon,
                    onClick = { editing = true },
                    modifier = Modifier.fillMaxWidth(),
                )
                SettingRow(
                    label = stringResource(R.string.quiet_to),
                    value = formatShortClock(end),
                    icon = RecentrIcon.Sun,
                    onClick = { editing = false },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            Spacer(Modifier.height(22.dp))
            RecentrButton(
                text = stringResource(R.string.quiet_save),
                onClick = {
                    onSave(start, end)
                    controller.close()
                },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
    editing?.let { isStart ->
        TimeDialog(
            title = stringResource(if (isStart) R.string.quiet_from else R.string.quiet_to),
            minuteOfDay = if (isStart) start else end,
            onConfirm = { minute ->
                if (isStart) start = minute else end = minute
                editing = null
            },
            onDismiss = { editing = null },
        )
    }
}

/** Material time picker in the design-system dialog chrome. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimeDialog(title: String, minuteOfDay: Int, onConfirm: (Int) -> Unit, onDismiss: () -> Unit) {
    val colors = RecentrTheme.colors
    val state = rememberTimePickerState(
        initialHour = minuteOfDay / 60,
        initialMinute = minuteOfDay % 60,
        is24Hour = LocalUse24HourClock.current,
    )
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Column(
            Modifier
                .padding(horizontal = 24.dp)
                .recentrShadow(RecentrElevation.float, RoundedCornerShape(24.dp))
                .clip(RoundedCornerShape(24.dp))
                .background(colors.bgSurface),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(title, style = RecentrTheme.typography.headline, color = colors.textPrimary, modifier = Modifier.padding(top = 22.dp, bottom = 16.dp))
            TimePicker(
                state = state,
                colors = TimePickerDefaults.colors(
                    clockDialColor = colors.bgSunken,
                    clockDialSelectedContentColor = colors.textOnDark,
                    clockDialUnselectedContentColor = colors.textPrimary,
                    selectorColor = colors.brandSky,
                    containerColor = colors.bgSurface,
                    periodSelectorBorderColor = colors.borderSubtle,
                    periodSelectorSelectedContainerColor = colors.alphaSky12,
                    periodSelectorUnselectedContainerColor = colors.bgSurface,
                    periodSelectorSelectedContentColor = colors.brandSky,
                    periodSelectorUnselectedContentColor = colors.textSecondary,
                    timeSelectorSelectedContainerColor = colors.alphaSky12,
                    timeSelectorUnselectedContainerColor = colors.bgSunken,
                    timeSelectorSelectedContentColor = colors.brandSky,
                    timeSelectorUnselectedContentColor = colors.textPrimary,
                ),
                modifier = Modifier.padding(horizontal = 16.dp),
            )
            Box(Modifier.fillMaxWidth().height(1.dp).background(colors.borderSubtle))
            Row(Modifier.fillMaxWidth().height(50.dp)) {
                DialogButton(stringResource(R.string.time_dialog_cancel), bold = false, onClick = onDismiss, modifier = Modifier.weight(1f))
                Box(Modifier.width(1.dp).fillMaxHeight().background(colors.borderSubtle))
                DialogButton(stringResource(R.string.time_dialog_ok), bold = true, onClick = { onConfirm(state.hour * 60 + state.minute) }, modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun DialogButton(text: String, bold: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxHeight().clickable(role = Role.Button, onClick = onClick), contentAlignment = Alignment.Center) {
        Text(
            text,
            style = if (bold) RecentrTheme.typography.headline else RecentrTheme.typography.bodyRegular,
            color = RecentrTheme.colors.brandSky,
        )
    }
}
