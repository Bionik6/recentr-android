package com.recentr.feature.rules.sheets

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.recentr.core.data.MIN_WAIT_SECONDS
import com.recentr.core.data.UNLOCK_METHODS
import com.recentr.core.designsystem.component.OptionRow
import com.recentr.core.designsystem.component.OptionTrailing
import com.recentr.core.designsystem.component.RecentrButton
import com.recentr.core.designsystem.component.SettingRow
import com.recentr.core.designsystem.icon.RecentrIcon
import com.recentr.core.designsystem.theme.RecentrTheme
import com.recentr.core.model.UnlockMethod
import com.recentr.feature.rules.R
import com.recentr.feature.rules.ui.components.Gap
import com.recentr.feature.rules.ui.components.HeroValueText
import com.recentr.feature.rules.ui.components.PresetChips
import com.recentr.feature.rules.ui.components.RulesSheet
import com.recentr.feature.rules.ui.components.SettingsGroup
import com.recentr.feature.rules.ui.components.SheetBody
import com.recentr.feature.rules.ui.durationLabel
import com.recentr.feature.rules.ui.unlocksPerDayLabel

/**
 * The ways out of "How do you unlock?", lightest first. No "No unlocks": that's Strict mode, so a
 * rule without it can always be unlocked, waiting 10 seconds by default.
 */
internal val UnlockMethodOptions = UNLOCK_METHODS

/** Figma 41 "How do you unlock?" — friction options as radio buttons, Continue. */
@Composable
internal fun UnlockMethodSheet(
    initial: UnlockMethod,
    onContinue: (UnlockMethod) -> Unit,
    onDismiss: () -> Unit,
) {
    var method by rememberSaveable { mutableStateOf(initial.takeIf { it in UnlockMethodOptions } ?: UnlockMethod.Wait) }
    val options = UnlockMethodOptions.map { option ->
        when (option) {
            UnlockMethod.Retype -> Triple(option, R.string.rules_unlock_option_retype, RecentrIcon.Pencil)
            UnlockMethod.ScanCode -> Triple(option, R.string.rules_unlock_option_scan, RecentrIcon.Qr)
            else -> Triple(option, R.string.rules_unlock_option_wait, RecentrIcon.Hourglass)
        }
    }
    RulesSheet(onDismissRequest = onDismiss) { closer ->
        SheetBody(
            figmaHeight = 574.dp,
            topGap = 18.dp,
            horizontalAlignment = Alignment.Start,
            bottomBar = {
                RecentrButton(
                    text = stringResource(R.string.rules_continue),
                    onClick = { closer.close { onContinue(method) } },
                    modifier = Modifier.fillMaxWidth(),
                )
            },
        ) {
            Text(stringResource(R.string.rules_unlock_title), style = RecentrTheme.typography.title1, color = RecentrTheme.colors.textPrimary)
            Gap(4.dp)
            Text(
                stringResource(R.string.rules_unlock_subtitle),
                style = RecentrTheme.typography.bodyRegular,
                color = RecentrTheme.colors.textSecondary,
            )
            Gap(16.dp)
            Column(Modifier.selectableGroup(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                options.forEach { (value, label, icon) ->
                    OptionRow(
                        label = stringResource(label),
                        selected = method == value,
                        onClick = { method = value },
                        icon = icon,
                        trailing = OptionTrailing.Radio,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}

/** Wait presets: [MIN_WAIT_SECONDS] at least. */
internal val WaitPresets = listOf(MIN_WAIT_SECONDS, 10, 30, 60)
internal val UnlocksPerDayOptions = listOf(1, 3, 5, 10, null)
internal val UnlockLengthOptions = listOf(1, 5, 10, 15, 30)

/** Figma 42 "Wait before unlocking · 5 seconds" with daily unlock settings. */
@Composable
internal fun WaitSettingsSheet(
    initialSeconds: Int,
    initialPerDay: Int?,
    initialLength: Int,
    onSave: (seconds: Int, perDay: Int?, lengthMinutes: Int) -> Unit,
    onDismiss: () -> Unit,
) {
    var seconds by rememberSaveable { mutableIntStateOf(initialSeconds.coerceAtLeast(MIN_WAIT_SECONDS)) }
    var perDay by rememberSaveable { mutableStateOf(initialPerDay) }
    var length by rememberSaveable { mutableIntStateOf(initialLength) }
    var picker by rememberSaveable { mutableStateOf(WaitPicker.None) }
    RulesSheet(onDismissRequest = onDismiss) { closer ->
        SheetBody(
            figmaHeight = 544.dp,
            topGap = 18.dp,
            bottomBar = {
                RecentrButton(
                    text = stringResource(R.string.rules_save),
                    onClick = { closer.close { onSave(seconds, perDay, length) } },
                    modifier = Modifier.fillMaxWidth(),
                )
            },
        ) {
            Text(stringResource(R.string.rules_wait_title), style = RecentrTheme.typography.title2, color = RecentrTheme.colors.textSecondary)
            Gap(2.dp)
            HeroValueText(
                if (seconds >= 60) stringResource(R.string.rules_wait_one_minute) else pluralStringResource(R.plurals.rules_wait_seconds, seconds, seconds),
                Modifier.fillMaxWidth(),
            )
            Gap(12.dp)
            PresetChips(
                labels = WaitPresets.map {
                    if (it >= 60) stringResource(R.string.rules_one_min_chip) else stringResource(R.string.rules_seconds_chip, it)
                },
                selectedIndex = WaitPresets.indexOf(seconds),
                onSelect = { seconds = WaitPresets[it] },
            )
            Gap(18.dp)
            SettingsGroup {
                SettingRow(
                    label = stringResource(R.string.rules_row_unlocks_per_day),
                    value = unlocksPerDayLabel(perDay),
                    onClick = { picker = WaitPicker.PerDay },
                    icon = RecentrIcon.Refresh,
                    modifier = Modifier.fillMaxWidth(),
                )
                SettingRow(
                    label = stringResource(R.string.rules_row_each_unlock),
                    value = durationLabel(length),
                    onClick = { picker = WaitPicker.Length },
                    icon = RecentrIcon.Clock,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
    when (picker) {
        WaitPicker.None -> Unit
        WaitPicker.PerDay -> OptionPickerSheet(
            title = stringResource(R.string.rules_unlocks_per_day_title),
            options = UnlocksPerDayOptions.map { unlocksPerDayLabel(it) },
            selectedIndex = UnlocksPerDayOptions.indexOf(perDay),
            onSelect = { perDay = UnlocksPerDayOptions[it] },
            onDismiss = { picker = WaitPicker.None },
        )
        WaitPicker.Length -> OptionPickerSheet(
            title = stringResource(R.string.rules_each_unlock_title),
            options = UnlockLengthOptions.map { durationLabel(it) },
            selectedIndex = UnlockLengthOptions.indexOf(length),
            onSelect = { length = UnlockLengthOptions[it] },
            onDismiss = { picker = WaitPicker.None },
        )
    }
}

private enum class WaitPicker { None, PerDay, Length }
