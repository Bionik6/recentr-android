package com.recentr.feature.rules.sheets

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.recentr.core.designsystem.component.RecentrButton
import com.recentr.core.designsystem.theme.RecentrTheme
import com.recentr.feature.rules.R
import com.recentr.feature.rules.ui.components.Gap
import com.recentr.feature.rules.ui.components.PresetChips
import com.recentr.feature.rules.ui.components.RulesSheet
import com.recentr.feature.rules.ui.components.SheetBody
import com.recentr.feature.rules.ui.components.ValueStepper
import com.recentr.feature.rules.ui.durationLabel
import com.recentr.feature.rules.ui.wallClock

private val Presets = listOf(15, 30, 45, 60, 120)
private const val STEP = 5
private const val MIN = 5
private const val MAX = 12 * 60

/** Figma 37 "Active for 45 min": stepper, presets, "Ends at 3:45 PM", Done. */
@Composable
internal fun DurationSheet(
    initialMinutes: Int,
    now: Long,
    onDone: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    var minutes by rememberSaveable { mutableIntStateOf(initialMinutes) }
    var custom by rememberSaveable { mutableStateOf(false) }
    RulesSheet(onDismissRequest = onDismiss) { closer ->
        SheetBody(
            figmaHeight = 444.dp,
            topGap = 22.dp,
            bottomBar = {
                RecentrButton(
                    text = stringResource(R.string.rules_done),
                    onClick = { closer.close { onDone(minutes) } },
                    modifier = Modifier.fillMaxWidth(),
                )
            },
        ) {
            Text(stringResource(R.string.rules_active_for), style = RecentrTheme.typography.title2, color = RecentrTheme.colors.textSecondary)
            Gap(4.dp)
            ValueStepper(
                value = durationLabel(minutes),
                onDecrease = { minutes = (minutes - STEP).coerceAtLeast(MIN) },
                onIncrease = { minutes = (minutes + STEP).coerceAtMost(MAX) },
                decreaseEnabled = minutes > MIN,
                increaseEnabled = minutes < MAX,
            )
            Gap(16.dp)
            val labels = Presets.map {
                if (it < 60) stringResource(R.string.rules_compact_minutes, it) else stringResource(R.string.rules_compact_hours, it / 60)
            } + stringResource(R.string.rules_preset_custom)
            PresetChips(
                labels = labels,
                selectedIndex = Presets.indexOf(minutes).takeIf { it >= 0 } ?: Presets.size,
                onSelect = { index ->
                    if (index < Presets.size) minutes = Presets[index] else custom = true
                },
            )
            Gap(14.dp)
            Text(
                stringResource(R.string.rules_ends_at, wallClock(now + minutes * 60_000L)),
                style = RecentrTheme.typography.bodyMedium,
                color = RecentrTheme.colors.textTertiary,
            )
        }
    }
    if (custom) {
        DurationInputDialog(
            initialMinutes = minutes,
            onConfirm = {
                minutes = it.coerceIn(MIN, MAX)
                custom = false
            },
            onDismiss = { custom = false },
        )
    }
}
