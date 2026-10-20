package com.recentr.feature.rules.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.recentr.core.designsystem.component.OptionRow
import com.recentr.core.designsystem.component.RecentrButton
import com.recentr.core.designsystem.component.RecentrButtonStyle
import com.recentr.core.designsystem.illustration.Mascot
import com.recentr.core.designsystem.illustration.MascotMood
import com.recentr.core.designsystem.theme.RecentrTheme
import com.recentr.feature.rules.R
import com.recentr.feature.rules.ui.components.Gap
import com.recentr.feature.rules.ui.components.RulesPillButton
import com.recentr.feature.rules.ui.components.RulesPillStyle
import com.recentr.feature.rules.ui.components.RulesSheet
import com.recentr.feature.rules.ui.components.SheetBody

/**
 * Figma 45 "Pause Work Hours?" — dozing mascot and pause length options. Only routines pause;
 * during a window ([later]) the pause starts when the window ends, and the sheet says so.
 */
@Composable
internal fun PauseRuleSheet(
    ruleName: String,
    later: Boolean,
    onPause: (PauseOption) -> Unit,
    onDismiss: () -> Unit,
) {
    var option by rememberSaveable { mutableStateOf(PauseOption.OneHour) }
    RulesSheet(onDismissRequest = onDismiss) { closer ->
        SheetBody(
            // Taller by the line saying when the pause starts.
            figmaHeight = if (later) 562.dp else 514.dp,
            topGap = 14.dp,
            bottomBar = {
                RecentrButton(
                    text = stringResource(R.string.rules_pause_rule),
                    onClick = { closer.close { onPause(option) } },
                    modifier = Modifier.fillMaxWidth(),
                )
            },
        ) {
            Mascot(MascotMood.Dozing, width = 80.dp)
            Gap(6.dp)
            Text(
                stringResource(R.string.rules_pause_title, ruleName),
                style = RecentrTheme.typography.title1,
                color = RecentrTheme.colors.textPrimary,
                textAlign = TextAlign.Center,
            )
            if (later) {
                Gap(6.dp)
                Text(
                    stringResource(R.string.rules_pause_later),
                    style = RecentrTheme.typography.bodyRegular,
                    color = RecentrTheme.colors.textSecondary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            Gap(16.dp)
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                PauseOption.entries.forEach { entry ->
                    OptionRow(
                        label = stringResource(entry.label),
                        selected = entry == option,
                        onClick = { option = entry },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}

/**
 * Figma 47 "Delete Work Hours?" — impatient mascot, red delete, "Keep it". While the rule is
 * engaged, the delete waits for its end: [later] says when ("It keeps blocking until midnight…").
 */
@Composable
internal fun DeleteRuleSheet(
    ruleName: String,
    streakDays: Int,
    later: String?,
    onDelete: () -> Unit,
    onDismiss: () -> Unit,
) {
    RulesSheet(onDismissRequest = onDismiss) { closer ->
        SheetBody(
            figmaHeight = 454.dp,
            topGap = 14.dp,
            bottomBar = {
                RulesPillButton(
                    text = stringResource(R.string.rules_delete_rule),
                    onClick = { closer.close(onDelete) },
                    style = RulesPillStyle.Danger,
                )
                Gap(10.dp)
                RecentrButton(
                    text = stringResource(R.string.rules_keep_it),
                    onClick = { closer.close() },
                    style = RecentrButtonStyle.Soft,
                    modifier = Modifier.fillMaxWidth(),
                )
            },
        ) {
            Mascot(MascotMood.Impatient, width = 88.dp)
            Gap(8.dp)
            Text(
                stringResource(R.string.rules_delete_title, ruleName),
                style = RecentrTheme.typography.title1,
                color = RecentrTheme.colors.textPrimary,
                textAlign = TextAlign.Center,
            )
            Gap(6.dp)
            Text(
                when {
                    later != null -> later
                    streakDays > 0 -> pluralStringResource(R.plurals.rules_delete_body_streak, streakDays, streakDays)
                    else -> stringResource(R.string.rules_delete_body)
                },
                style = RecentrTheme.typography.bodyRegular,
                color = RecentrTheme.colors.textSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
