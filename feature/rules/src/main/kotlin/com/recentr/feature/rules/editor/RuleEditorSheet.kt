package com.recentr.feature.rules.editor

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.recentr.core.model.RuleType
import com.recentr.feature.rules.R
import com.recentr.feature.rules.sheets.RuleSheet
import com.recentr.feature.rules.sheets.RuleSheetsHost
import com.recentr.feature.rules.ui.components.ClearDialogDim
import com.recentr.feature.rules.ui.components.RulesSheet
import com.recentr.feature.rules.ui.components.SheetBody
import kotlinx.coroutines.delay

/**
 * Create / edit sheet of a rule, hosted by the `RuleEditorRoute` dialog destination. The
 * option rows open the shared [RuleSheetsHost] sub-sheets on top of it.
 */
@Composable
internal fun RuleEditorDestination(
    onClose: () -> Unit,
    viewModel: RuleEditorViewModel = viewModel(factory = RuleEditorViewModel.Factory),
) {
    ClearDialogDim()
    // The sheet can report its dismissal more than once (gesture + programmatic hide): pop once.
    var closed by remember { mutableStateOf(false) }
    val close = {
        if (!closed) {
            closed = true
            onClose()
        }
    }
    val state by viewModel.state.collectAsStateWithLifecycle()
    var sheet by rememberSaveable { mutableStateOf(RuleSheet.None) }
    val now by produceState(System.currentTimeMillis()) {
        while (true) {
            delay(1_000)
            value = System.currentTimeMillis()
        }
    }
    RulesSheet(onDismissRequest = close) { closer ->
        val actions = EditorActions(
            open = { sheet = it },
            update = viewModel::update,
            onStrictChange = { on -> viewModel.update { it.copy(strictMode = on) } },
            strictLocked = !state.isNew && state.draft.isStrictLocked(now),
            onSuggestion = viewModel::applySuggestion,
            onSaveTemplate = {
                // Saving closes the sheet so the confirmation toast is visible on the list.
                viewModel.saveAsTemplate()
                closer.close()
            },
            onCommit = {
                viewModel.commit()
                closer.close()
            },
        )
        val draft = state.draft
        val typeTitle = stringResource(
            when (draft.type) {
                RuleType.Session -> R.string.rules_session_title
                RuleType.Lock -> R.string.rules_lock_title
                RuleType.Limit -> R.string.rules_limit_title
                RuleType.Bedtime -> R.string.rules_bedtime_title
                RuleType.Habit -> R.string.rules_habit_title
            },
        )
        val title = if (state.customTitle) draft.name else typeTitle
        val createLabel = when (draft.type) {
            RuleType.Session -> R.string.rules_start
            RuleType.Lock -> R.string.rules_create_lock
            RuleType.Limit -> R.string.rules_create_limit
            RuleType.Bedtime -> R.string.rules_create_routine
            RuleType.Habit -> R.string.rules_create_habit
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
                RuleType.Habit -> HabitEditor(
                    state = state,
                    title = title,
                    habitTitle = draft.apps.firstOrNull()?.let { stringResource(R.string.rules_habit_before, it.displayName) }
                        ?: stringResource(R.string.rules_habit_default_name),
                    actions = actions,
                )
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
}
