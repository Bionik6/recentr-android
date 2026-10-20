package com.recentr.feature.rules.sheets

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.recentr.core.designsystem.component.RecentrAlertDialog
import com.recentr.feature.rules.R

/**
 * Figma 33 "Turn on Strict mode?": asked when Strict mode would hold right away (the rule is
 * blocking now): until it ends, only an emergency unlock gets past it.
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

/**
 * Asked before saving a change that blocks more apps right away: apps added to a rule that holds
 * them now, a limit's budget lowered to what's already used.
 */
@Composable
internal fun BlockRightAwayDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    RecentrAlertDialog(
        title = stringResource(R.string.rules_block_now_title),
        body = stringResource(R.string.rules_block_now_body),
        primaryText = stringResource(R.string.rules_block_now),
        onPrimary = onConfirm,
        onDismissRequest = onDismiss,
        secondaryText = stringResource(R.string.rules_cancel),
    )
}
