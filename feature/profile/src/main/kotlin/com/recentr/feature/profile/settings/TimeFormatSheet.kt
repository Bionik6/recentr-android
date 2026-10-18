package com.recentr.feature.profile.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.recentr.core.designsystem.component.OptionRow
import com.recentr.core.designsystem.component.OptionTrailing
import com.recentr.core.designsystem.component.RecentrSheet
import com.recentr.core.designsystem.format.clockTimeOf
import com.recentr.core.model.TimeFormat
import com.recentr.feature.profile.R
import com.recentr.feature.profile.ui.SheetContent
import com.recentr.feature.profile.ui.SheetHeading
import com.recentr.feature.profile.ui.label

/** Settings › Time format: "AM/PM · 9:00 PM" or "24H · 21:00" radio rows. */
@Composable
internal fun TimeFormatSheet(
    selected: TimeFormat,
    onSelect: (TimeFormat) -> Unit,
    onDismissRequest: () -> Unit,
) {
    RecentrSheet(onDismissRequest = onDismissRequest) { controller ->
        SheetContent(topGap = 16.dp) {
            SheetHeading(stringResource(R.string.settings_time_format), stringResource(R.string.time_format_body))
            Spacer(Modifier.height(22.dp))
            Column(Modifier.selectableGroup(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                TimeFormat.entries.forEach { format ->
                    val sample = clockTimeOf(21 * 60, use24Hour = format == TimeFormat.TwentyFourHour)
                    OptionRow(
                        label = stringResource(R.string.time_format_option, stringResource(format.label), sample),
                        selected = format == selected,
                        onClick = { controller.close { onSelect(format) } },
                        trailing = OptionTrailing.Radio,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}
