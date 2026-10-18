package com.recentr.feature.rules.sheets

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.recentr.core.designsystem.component.RecentrAlertDialog
import com.recentr.feature.rules.R

/**
 * Figma 33 "Turn on Strict mode?": asked when Strict mode would lock a rule right away (it is
 * blocking now), since it can't be turned off until the session ends.
 */
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
