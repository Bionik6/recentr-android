package com.recentr.feature.rules.sheets

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.recentr.core.designsystem.component.RecentrAlertDialog
import com.recentr.core.designsystem.component.StrictModeSheet as DesignSystemStrictModeSheet
import com.recentr.core.designsystem.component.StrictOption
import com.recentr.core.model.StrictLevel
import com.recentr.feature.rules.R

/**
 * Figma 32 "Strict mode" for a rule (editor and detail): Start time / During / Always as option
 * rows, then the tangerine "Turn on Strict mode" (followed by the Figma 33 confirmation).
 */
@Composable
internal fun StrictModeSheet(
    initialLevel: StrictLevel,
    onTurnOn: (StrictLevel) -> Unit,
    onDismiss: () -> Unit,
) {
    DesignSystemStrictModeSheet(
        subtitle = stringResource(R.string.rules_strict_subtitle),
        options = StrictLevel.entries.map { level ->
            when (level) {
                StrictLevel.StartTime -> StrictOption(stringResource(R.string.rules_strict_start_time), stringResource(R.string.rules_strict_desc_start))
                StrictLevel.During -> StrictOption(stringResource(R.string.rules_strict_during), stringResource(R.string.rules_strict_desc_during))
                StrictLevel.Always -> StrictOption(stringResource(R.string.rules_strict_always), stringResource(R.string.rules_strict_desc_always))
            }
        },
        initialIndex = initialLevel.ordinal,
        confirmText = { stringResource(R.string.rules_strict_turn_on) },
        onConfirm = { index -> onTurnOn(StrictLevel.entries[index]) },
        onDismissRequest = onDismiss,
    )
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
