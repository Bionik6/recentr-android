package com.recentr.feature.rules.sheets

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import com.recentr.core.model.AppId
import com.recentr.core.model.HabitRepeat
import com.recentr.core.model.HabitTask
import com.recentr.core.model.LimitAction
import com.recentr.core.model.LimitReset
import com.recentr.core.model.Rule
import com.recentr.core.model.RuleSticker
import com.recentr.core.model.RuleType
import com.recentr.core.model.StrictLevel
import com.recentr.core.model.UnlockMethod
import com.recentr.feature.rules.R
import com.recentr.feature.rules.ui.habitRepeatLabel
import com.recentr.feature.rules.ui.limitActionLabel
import com.recentr.feature.rules.ui.limitResetLabel
import com.recentr.feature.rules.ui.reminderLabel
import java.util.UUID

/** Sub-sheets a rule editor (or the rule detail) can open. */
internal enum class RuleSheet {
    None, Name, Apps, AllowedApps, BlockMode, Duration, Schedule, BedtimeStart, BedtimeEnd, Strict, Unlock, Wait,
    UnlocksPerDay, Resets, TimeUp, Repeats, Reminders, Note, AddTask,
}

private val ReminderOptions = listOf(null, 5, 10, 15, 30)

/**
 * Renders the sub-sheet [sheet] for [rule] and applies its result with [onUpdate]. Shared by the
 * creation sheets and the rule detail so every option behaves the same everywhere.
 */
@Composable
internal fun RuleSheetsHost(
    sheet: RuleSheet,
    rule: Rule,
    now: Long,
    onSheet: (RuleSheet) -> Unit,
    onUpdate: ((Rule) -> Rule) -> Unit,
    onRename: (String, RuleSticker) -> Unit,
    onApps: (List<AppId>) -> Unit,
    noteTitle: String = stringResource(R.string.rules_note_title),
) {
    var pendingStrict by rememberSaveable { mutableStateOf<StrictLevel?>(null) }
    val close = { onSheet(RuleSheet.None) }
    when (sheet) {
        RuleSheet.None -> Unit
        RuleSheet.Name -> EditNameStickerSheet(
            initialName = rule.name,
            initialSticker = rule.sticker,
            onSave = onRename,
            onDismiss = close,
        )
        RuleSheet.Apps -> ChooseAppsSheet(initial = rule.apps, onDone = onApps, onDismiss = close)
        RuleSheet.AllowedApps -> ChooseAppsSheet(
            initial = rule.allowedApps,
            onDone = { allowed -> onUpdate { it.copy(allowedApps = allowed) } },
            onDismiss = close,
            title = stringResource(R.string.rules_always_allowed_title),
            allowEmpty = true,
        )
        RuleSheet.BlockMode -> OptionPickerSheet(
            title = stringResource(R.string.rules_block_mode_title),
            options = listOf(stringResource(R.string.rules_block_all_except), stringResource(R.string.rules_block_only_selected)),
            selectedIndex = if (rule.blockAllApps) 0 else 1,
            onSelect = { index ->
                onUpdate { it.copy(blockAllApps = index == 0) }
                if (index == 1) onSheet(RuleSheet.Apps)
            },
            onDismiss = close,
        )
        RuleSheet.Duration -> DurationSheet(
            initialMinutes = rule.durationMinutes ?: 30,
            now = rule.activeSince ?: now,
            onDone = { minutes -> onUpdate { it.copy(durationMinutes = minutes) } },
            onDismiss = close,
        )
        RuleSheet.Schedule -> ScheduleSheet(
            title = stringResource(R.string.rules_row_active),
            initial = rule.schedule,
            allowAllDay = rule.type == RuleType.Lock,
            onSave = { schedule -> onUpdate { it.copy(schedule = schedule) } },
            onDismiss = close,
        )
        RuleSheet.BedtimeStart, RuleSheet.BedtimeEnd -> rule.schedule?.let { schedule ->
            val start = sheet == RuleSheet.BedtimeStart
            TimeOfDayDialog(
                title = stringResource(if (start) R.string.rules_starts_at else R.string.rules_ends_at_title),
                initialMinuteOfDay = if (start) schedule.startMinute else schedule.endMinute,
                onConfirm = { minute ->
                    onUpdate {
                        val current = it.schedule ?: schedule
                        it.copy(schedule = if (start) current.copy(startMinute = minute) else current.copy(endMinute = minute))
                    }
                    close()
                },
                onDismiss = close,
            )
        }
        RuleSheet.Strict -> StrictModeSheet(
            initialLevel = rule.strictLevel,
            onTurnOn = { level -> pendingStrict = level },
            onDismiss = close,
        )
        RuleSheet.Unlock -> UnlockMethodSheet(
            initial = rule.unlockMethod,
            includeNone = rule.type != RuleType.Lock,
            onContinue = { method ->
                onUpdate { it.copy(unlockMethod = method) }
                if (method == UnlockMethod.Wait) onSheet(RuleSheet.Wait)
            },
            onDismiss = close,
        )
        RuleSheet.Wait -> WaitSettingsSheet(
            initialSeconds = rule.waitSeconds,
            initialPerDay = rule.unlocksPerDay,
            initialLength = rule.unlockDurationMinutes,
            onSave = { seconds, perDay, length ->
                onUpdate { it.copy(waitSeconds = seconds, unlocksPerDay = perDay, unlockDurationMinutes = length) }
            },
            onDismiss = close,
        )
        RuleSheet.UnlocksPerDay -> OptionPickerSheet(
            title = stringResource(R.string.rules_unlocks_per_day_title),
            options = UnlocksPerDayOptions.map { com.recentr.feature.rules.ui.unlocksPerDayLabel(it) },
            selectedIndex = UnlocksPerDayOptions.indexOf(rule.unlocksPerDay),
            onSelect = { index -> onUpdate { it.copy(unlocksPerDay = UnlocksPerDayOptions[index]) } },
            onDismiss = close,
        )
        RuleSheet.Resets -> OptionPickerSheet(
            title = stringResource(R.string.rules_resets_title),
            options = LimitReset.entries.map { limitResetLabel(it) },
            selectedIndex = rule.limitReset.ordinal,
            onSelect = { index -> onUpdate { it.copy(limitReset = LimitReset.entries[index]) } },
            onDismiss = close,
        )
        RuleSheet.TimeUp -> OptionPickerSheet(
            title = stringResource(R.string.rules_time_up_title),
            options = LimitAction.entries.map { limitActionLabel(it) },
            selectedIndex = rule.limitAction.ordinal,
            onSelect = { index -> onUpdate { it.copy(limitAction = LimitAction.entries[index]) } },
            onDismiss = close,
        )
        RuleSheet.Repeats -> OptionPickerSheet(
            title = stringResource(R.string.rules_repeats_title),
            options = HabitRepeat.entries.map { habitRepeatLabel(it) },
            selectedIndex = rule.habitRepeat.ordinal,
            onSelect = { index -> onUpdate { it.copy(habitRepeat = HabitRepeat.entries[index]) } },
            onDismiss = close,
        )
        RuleSheet.Reminders -> OptionPickerSheet(
            title = stringResource(R.string.rules_reminders_title),
            options = ReminderOptions.map { reminderLabel(it) },
            selectedIndex = ReminderOptions.indexOf(rule.reminderMinutesBefore),
            onSelect = { index -> onUpdate { it.copy(reminderMinutesBefore = ReminderOptions[index]) } },
            onDismiss = close,
        )
        RuleSheet.Note -> TextInputSheet(
            title = noteTitle,
            initial = rule.note.orEmpty(),
            placeholder = stringResource(R.string.rules_note_placeholder),
            onSave = { note -> onUpdate { it.copy(note = note) } },
            onDismiss = close,
        )
        RuleSheet.AddTask -> TextInputSheet(
            title = stringResource(R.string.rules_new_task_title),
            initial = "",
            placeholder = stringResource(R.string.rules_new_task_placeholder),
            onSave = { title -> onUpdate { it.copy(habitTasks = it.habitTasks + HabitTask(UUID.randomUUID().toString(), title)) } },
            onDismiss = close,
        )
    }
    pendingStrict?.let { level ->
        StrictConfirmDialog(
            onConfirm = {
                onUpdate { it.copy(strictMode = true, strictLevel = level) }
                pendingStrict = null
            },
            onDismiss = { pendingStrict = null },
        )
    }
}
