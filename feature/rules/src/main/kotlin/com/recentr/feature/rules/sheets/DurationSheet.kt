package com.recentr.feature.rules.sheets

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.recentr.core.designsystem.component.DurationPicker
import com.recentr.core.designsystem.component.RecentrButton
import com.recentr.core.designsystem.theme.RecentrTheme
import com.recentr.feature.rules.R
import com.recentr.feature.rules.model.SESSION_MAX_MINUTES
import com.recentr.feature.rules.model.SESSION_MIN_MINUTES
import com.recentr.feature.rules.ui.compactDuration
import com.recentr.feature.rules.ui.components.Gap
import com.recentr.feature.rules.ui.components.RulesSheet
import com.recentr.feature.rules.ui.components.SheetBody
import com.recentr.feature.rules.ui.durationLabel
import com.recentr.feature.rules.ui.wallClock

private val Presets = listOf(15, 30, 45, 60, 120)
private const val STEP = 5

/** Figma 37 "Active for 45 min": a ruler in five minutes, presets, "Ends at 3:45 PM", Done. */
@Composable
internal fun DurationSheet(
    initialMinutes: Int,
    now: Long,
    onDone: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    var minutes by rememberSaveable { mutableIntStateOf(initialMinutes) }
    RulesSheet(onDismissRequest = onDismiss) { closer ->
        // As tall as the ruler and its presets need, rather than the stepper's 444pt.
        SheetBody(
            figmaHeight = null,
            topGap = 22.dp,
            wrapContent = true,
            bottomBar = {
                RecentrButton(
                    text = stringResource(R.string.rules_done),
                    onClick = { closer.close { onDone(minutes) } },
                    modifier = Modifier.fillMaxWidth(),
                )
            },
        ) {
            Text(stringResource(R.string.rules_active_for), style = RecentrTheme.typography.title2, color = RecentrTheme.colors.textSecondary)
            Gap(8.dp)
            DurationPicker(
                value = minutes,
                onValueChange = { minutes = it },
                range = SESSION_MIN_MINUTES..SESSION_MAX_MINUTES,
                contentDescription = stringResource(R.string.rules_active_for),
                step = STEP,
                labelEvery = 30,
                presets = Presets,
                format = { durationLabel(it) },
                compactFormat = { compactDuration(it) },
            )
            Gap(14.dp)
            Text(
                stringResource(R.string.rules_ends_at, wallClock(now + minutes * 60_000L)),
                style = RecentrTheme.typography.bodyMedium,
                color = RecentrTheme.colors.textTertiary,
            )
        }
    }
}
