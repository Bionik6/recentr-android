package com.recentr.feature.rules.sheets

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.recentr.core.designsystem.component.RecentrAlertDialog
import com.recentr.core.designsystem.component.RecentrSegmented
import com.recentr.core.designsystem.icon.RecentrIcon
import com.recentr.core.designsystem.icon.RecentrIconImage
import com.recentr.core.designsystem.theme.RecentrShapes
import com.recentr.core.designsystem.theme.RecentrTheme
import com.recentr.core.model.StrictLevel
import com.recentr.feature.rules.R
import com.recentr.feature.rules.ui.components.Gap
import com.recentr.feature.rules.ui.components.RulesPillButton
import com.recentr.feature.rules.ui.components.RulesPillStyle
import com.recentr.feature.rules.ui.components.RulesSheet
import com.recentr.feature.rules.ui.components.SheetBody

/** Figma 32 "Strict mode" explainer: shield badge, Start time / During / Always, orange CTA. */
@Composable
internal fun StrictModeSheet(
    initialLevel: StrictLevel,
    onTurnOn: (StrictLevel) -> Unit,
    onDismiss: () -> Unit,
) {
    var level by rememberSaveable { mutableStateOf(initialLevel) }
    val colors = RecentrTheme.colors
    RulesSheet(onDismissRequest = onDismiss) { closer ->
        SheetBody(
            figmaHeight = 444.dp,
            topGap = 18.dp,
            bottomBar = {
                RulesPillButton(
                    text = stringResource(R.string.rules_strict_turn_on),
                    onClick = { closer.close { onTurnOn(level) } },
                    style = RulesPillStyle.Strict,
                )
            },
        ) {
            Box(
                Modifier.size(56.dp).clip(RecentrShapes.full).background(colors.alphaTangerine14),
                contentAlignment = Alignment.Center,
            ) {
                RecentrIconImage(RecentrIcon.Shield, tint = colors.accentTangerine, size = 31.2.dp)
            }
            Gap(10.dp)
            Text(stringResource(R.string.rules_strict_mode), style = RecentrTheme.typography.title1, color = colors.textPrimary)
            Gap(4.dp)
            Text(
                stringResource(R.string.rules_strict_subtitle),
                style = RecentrTheme.typography.bodyRegular,
                color = colors.textSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
            Gap(18.dp)
            RecentrSegmented(
                options = listOf(
                    stringResource(R.string.rules_strict_start_time),
                    stringResource(R.string.rules_strict_during),
                    stringResource(R.string.rules_strict_always),
                ),
                selectedIndex = level.ordinal,
                onSelect = { level = StrictLevel.entries[it] },
                modifier = Modifier.fillMaxWidth(),
            )
            Gap(14.dp)
            Text(
                stringResource(
                    when (level) {
                        StrictLevel.StartTime -> R.string.rules_strict_explain_start
                        StrictLevel.During -> R.string.rules_strict_explain_during
                        StrictLevel.Always -> R.string.rules_strict_explain_always
                    },
                ),
                style = RecentrTheme.typography.bodyMedium,
                color = colors.textPrimary,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

/** Figma 33 "Turn on Strict mode?" confirmation alert. */
@Composable
internal fun StrictConfirmDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    RecentrAlertDialog(
        title = stringResource(R.string.rules_strict_confirm_title),
        body = stringResource(R.string.rules_strict_confirm_body),
        primaryText = stringResource(R.string.rules_turn_on),
        onPrimary = onConfirm,
        onDismissRequest = onDismiss,
        secondaryText = stringResource(R.string.rules_cancel),
    )
}
