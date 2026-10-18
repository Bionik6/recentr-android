package com.recentr.feature.rules.sheets

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TimeInput
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerColors
import androidx.compose.material3.TimePickerDefaults
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.recentr.core.designsystem.format.LocalUse24HourClock
import com.recentr.core.designsystem.theme.RecentrElevation
import com.recentr.core.designsystem.theme.RecentrTheme
import com.recentr.core.designsystem.theme.recentrShadow
import com.recentr.feature.rules.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun recentrTimePickerColors(): TimePickerColors {
    val colors = RecentrTheme.colors
    return TimePickerDefaults.colors(
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
    )
}

/** Dialog chrome shared by the time dialogs: same look as [com.recentr.core.designsystem.component.RecentrAlertDialog]. */
@Composable
private fun RulesDialog(
    title: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    content: @Composable () -> Unit,
) {
    val colors = RecentrTheme.colors
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Column(
            Modifier
                .padding(horizontal = 24.dp)
                .recentrShadow(RecentrElevation.float, RoundedCornerShape(24.dp))
                .clip(RoundedCornerShape(24.dp))
                .background(colors.bgSurface),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                title,
                style = RecentrTheme.typography.headline,
                color = colors.textPrimary,
                modifier = Modifier.padding(top = 22.dp, bottom = 16.dp),
            )
            Box(Modifier.padding(horizontal = 20.dp)) { content() }
            Box(Modifier.padding(top = 8.dp).fillMaxWidth().height(1.dp).background(colors.borderSubtle))
            Row(Modifier.fillMaxWidth().height(50.dp)) {
                Box(
                    Modifier.weight(1f).fillMaxHeight().clickable(role = Role.Button, onClick = onDismiss),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(stringResource(R.string.rules_cancel), style = RecentrTheme.typography.bodyRegular, color = colors.brandSky)
                }
                Box(Modifier.width(1.dp).fillMaxHeight().background(colors.borderSubtle))
                Box(
                    Modifier.weight(1f).fillMaxHeight().clickable(role = Role.Button, onClick = onConfirm),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(stringResource(R.string.rules_done), style = RecentrTheme.typography.headline, color = colors.brandSky)
                }
            }
        }
    }
}

/** Clock picker for a schedule boundary ("Starts at" / "Ends at"). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun TimeOfDayDialog(
    title: String,
    initialMinuteOfDay: Int,
    onConfirm: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    val state = rememberTimePickerState(
        initialHour = (initialMinuteOfDay / 60) % 24,
        initialMinute = initialMinuteOfDay % 60,
        is24Hour = LocalUse24HourClock.current,
    )
    RulesDialog(title = title, onConfirm = { onConfirm(state.hour * 60 + state.minute) }, onDismiss = onDismiss) {
        TimePicker(state = state, colors = recentrTimePickerColors())
    }
}

/** Hours / minutes entry for a custom session length. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun DurationInputDialog(
    initialMinutes: Int,
    onConfirm: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    val state = rememberTimePickerState(
        initialHour = (initialMinutes / 60).coerceAtMost(23),
        initialMinute = initialMinutes % 60,
        is24Hour = true,
    )
    RulesDialog(
        title = stringResource(R.string.rules_custom_length),
        onConfirm = { onConfirm(state.hour * 60 + state.minute) },
        onDismiss = onDismiss,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
            TimeInput(state = state, colors = recentrTimePickerColors())
        }
    }
}
