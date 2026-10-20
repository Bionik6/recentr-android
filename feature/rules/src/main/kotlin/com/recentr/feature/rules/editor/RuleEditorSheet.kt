package com.recentr.feature.rules.editor

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.recentr.core.data.BlockingAccess
import com.recentr.core.data.RecentrData
import com.recentr.core.designsystem.component.BlockingAccessSheet
import com.recentr.core.model.RuleType
import com.recentr.feature.rules.R
import com.recentr.feature.rules.sheets.BlockRightAwayDialog
import com.recentr.feature.rules.sheets.RuleSheet
import com.recentr.feature.rules.sheets.RuleSheetsHost
import com.recentr.feature.rules.ui.components.ClearDialogDim
import com.recentr.feature.rules.ui.components.RulesSheet
import com.recentr.feature.rules.ui.components.SheetBody
import com.recentr.feature.rules.ui.components.SheetCloser
import com.recentr.feature.rules.ui.rememberWallClock
import kotlinx.coroutines.delay

/**
 * Create / edit sheet of a rule, hosted by the `RuleEditorRoute` dialog destination. The
 * option rows open the shared [RuleSheetsHost] sub-sheets on top of it. Once it creates a rule,
 * it goes with [onCreated] and the rule's id instead of [onClose]. Saving an edit that blocks more
 * apps right away (the rule holds them now) asks first.
 */
@Composable
internal fun RuleEditorDestination(
    onClose: () -> Unit,
    onCreated: (ruleId: String) -> Unit,
    viewModel: RuleEditorViewModel = viewModel(factory = RuleEditorViewModel.Factory),
) {
    ClearDialogDim()
    var created by rememberSaveable { mutableStateOf<String?>(null) }
    // The sheet can report its dismissal more than once (gesture + programmatic hide): pop once.
    var closed by remember { mutableStateOf(false) }
    val close = {
        if (!closed) {
            closed = true
            created?.let(onCreated) ?: onClose()
        }
    }
    val state by viewModel.state.collectAsStateWithLifecycle()
    var sheet by rememberSaveable { mutableStateOf(RuleSheet.None) }
    val blockingOn by RecentrData.blockingEnabled.collectAsStateWithLifecycle()
    var explainBlocking by rememberSaveable { mutableStateOf(false) }
    var confirmBlock by rememberSaveable { mutableStateOf(false) }
    var editorCloser by remember { mutableStateOf<SheetCloser?>(null) }
    val clock = rememberWallClock()
    val now by produceState(System.currentTimeMillis()) {
        while (true) {
            delay(1_000)
            value = System.currentTimeMillis()
        }
    }
    /** Saves, then closes — once blocking is explained when the rule can't put apps on hold yet. */
    val commit: () -> Unit = {
        if (state.isNew) created = state.draft.id
        viewModel.commit(clock)
        when {
            !blockingOn -> explainBlocking = true
            else -> editorCloser?.close() ?: close()
        }
    }
    RulesSheet(onDismissRequest = close) { closer ->
        editorCloser = closer
        val actions = EditorActions(
            open = { sheet = it },
            update = viewModel::update,
            onStrictChange = { on -> viewModel.update { it.copy(strictMode = on) } },
            onSuggestion = viewModel::applySuggestion,
            onCommit = { if (viewModel.preview()?.blocksMore == true) confirmBlock = true else commit() },
        )
        val draft = state.draft
        val typeTitle = stringResource(
            when (draft.type) {
                RuleType.Session -> R.string.rules_session_title
                RuleType.Lock -> R.string.rules_lock_title
                RuleType.Limit -> R.string.rules_limit_title
                RuleType.Bedtime -> R.string.rules_bedtime_title
            },
        )
        val title = if (state.customTitle) draft.name else typeTitle
        val createLabel = when (draft.type) {
            RuleType.Session -> R.string.rules_hold_create_session
            RuleType.Lock -> R.string.rules_hold_create_lock
            RuleType.Limit -> R.string.rules_hold_create_limit
            RuleType.Bedtime -> R.string.rules_hold_create_routine
        }
        SheetBody(
            figmaHeight = null,
            topGap = 16.dp,
            bottomBar = { EditorCta(state, createLabel, actions) },
        ) {
            when (draft.type) {
                RuleType.Session -> SessionEditor(state, title, actions)
                RuleType.Lock -> LockEditor(state, title, actions)
                RuleType.Limit -> LimitEditor(state, title, actions)
                RuleType.Bedtime -> BedtimeEditor(state, title, actions)
            }
        }
    }
    RuleSheetsHost(
        sheet = sheet,
        rule = state.draft,
        now = now,
        onSheet = { sheet = it },
        onUpdate = viewModel::update,
        onRename = viewModel::rename,
        onApps = viewModel::setApps,
        noteTitle = stringResource(if (state.draft.type == RuleType.Lock) R.string.rules_lock_note_title else R.string.rules_note_title),
    )
    if (confirmBlock) {
        BlockRightAwayDialog(
            onConfirm = {
                confirmBlock = false
                commit()
            },
            onDismiss = { confirmBlock = false },
        )
    }
    if (explainBlocking) {
        val context = LocalContext.current
        BlockingAccessSheet(
            onOpenSettings = { BlockingAccess.openSettings(context) },
            onDismissRequest = {
                explainBlocking = false
                editorCloser?.close() ?: close()
            },
        )
    }
}
