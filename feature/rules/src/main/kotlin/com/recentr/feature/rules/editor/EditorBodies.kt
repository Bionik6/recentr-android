package com.recentr.feature.rules.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.recentr.core.designsystem.component.RecentrButton
import com.recentr.core.designsystem.component.RecentrCheckbox
import com.recentr.core.designsystem.component.RecentrIconButtonStyle
import com.recentr.core.designsystem.component.SettingRow
import com.recentr.core.designsystem.component.StickyNote
import com.recentr.core.designsystem.icon.RecentrIcon
import com.recentr.core.designsystem.icon.RecentrIconImage
import com.recentr.core.designsystem.theme.RecentrElevation
import com.recentr.core.designsystem.theme.RecentrShapes
import com.recentr.core.designsystem.theme.RecentrTheme
import com.recentr.core.designsystem.theme.recentrShadow
import com.recentr.core.model.Rule
import com.recentr.core.model.RuleSchedule
import com.recentr.feature.rules.R
import com.recentr.feature.rules.model.BuiltInTemplate
import com.recentr.feature.rules.model.toCard
import com.recentr.feature.rules.sheets.RuleSheet
import com.recentr.feature.rules.sheets.TimesRow
import com.recentr.feature.rules.ui.appsCategoryLabel
import com.recentr.feature.rules.ui.appsLabel
import com.recentr.feature.rules.ui.asSticker
import com.recentr.feature.rules.ui.compactDuration
import com.recentr.feature.rules.ui.components.DayDots
import com.recentr.feature.rules.ui.components.Gap
import com.recentr.feature.rules.ui.components.MiniTemplateCard
import com.recentr.feature.rules.ui.components.RuleSheetHeader
import com.recentr.feature.rules.ui.components.SectionCaption
import com.recentr.feature.rules.ui.components.SettingsGroup
import com.recentr.feature.rules.ui.components.StrictToggleRow
import com.recentr.feature.rules.ui.components.ValueStepper
import com.recentr.feature.rules.ui.durationLabel
import com.recentr.feature.rules.ui.habitRepeatLabel
import com.recentr.feature.rules.ui.limitActionLabel
import com.recentr.feature.rules.ui.limitResetLabel
import com.recentr.feature.rules.ui.scheduleLabel
import com.recentr.feature.rules.ui.unlockLabel
import com.recentr.feature.rules.ui.unlocksPerDayLabel

/** What the editor bodies can do. */
internal class EditorActions(
    val open: (RuleSheet) -> Unit,
    val update: ((Rule) -> Rule) -> Unit,
    /** Strict mode toggle. */
    val onStrictChange: (Boolean) -> Unit,
    val onSuggestion: (BuiltInTemplate) -> Unit,
    val onCommit: () -> Unit,
    /** The rule is strict and blocking now: Strict mode can't be turned off until it ends. */
    val strictLocked: Boolean = false,
)

@Composable
private fun Header(rule: Rule, title: String, subtitle: String, actions: EditorActions) {
    RuleSheetHeader(
        sticker = rule.sticker.asSticker,
        title = title,
        subtitle = subtitle,
        onEditTitle = { actions.open(RuleSheet.Name) },
    )
}

@Composable
private fun EditorRow(label: Int, value: String, icon: RecentrIcon, onClick: () -> Unit) {
    SettingRow(
        label = stringResource(label),
        value = value,
        onClick = onClick,
        icon = icon,
        modifier = Modifier.fillMaxWidth(),
    )
}

/** A rule needs apps to block before it can be created or saved. */
private val Rule.blocksApps: Boolean get() = apps.isNotEmpty()

/** Primary CTA ("Start" for a new session). Disabled until the rule has apps to block. */
@Composable
internal fun ColumnScope.EditorCta(state: RuleEditorUiState, createLabel: Int, actions: EditorActions) {
    RecentrButton(
        text = stringResource(if (state.isNew) createLabel else R.string.rules_save),
        onClick = actions.onCommit,
        enabled = state.draft.blocksApps,
        modifier = Modifier.fillMaxWidth(),
    )
}

/** Figma 31 "Session": suggestions, Block / Active / Unlocks, Strict mode. */
@Composable
internal fun ColumnScope.SessionEditor(state: RuleEditorUiState, title: String, actions: EditorActions) {
    val rule = state.draft
    val schedule = rule.schedule
    Header(
        rule = rule,
        title = title,
        subtitle = if (schedule == null) {
            stringResource(R.string.rules_session_subtitle, durationLabel(rule.durationMinutes ?: 30))
        } else {
            stringResource(R.string.rules_session_subtitle_scheduled, scheduleLabel(schedule))
        },
        actions = actions,
    )
    Gap(20.dp)
    if (state.isNew && schedule == null) {
        SectionCaption(stringResource(R.string.rules_suggestions))
        Gap(8.dp)
        androidx.compose.foundation.layout.Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            BuiltInTemplate.sessionSuggestions.forEach { template ->
                val card = template.toCard()
                MiniTemplateCard(
                    title = card.title,
                    badge = card.badge,
                    sticker = card.sticker.asSticker,
                    color = card.color,
                    onClick = { actions.onSuggestion(template) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
        Gap(18.dp)
    }
    SettingsGroup {
        EditorRow(R.string.rules_row_block, appsLabel(rule.apps), RecentrIcon.Apps) { actions.open(RuleSheet.Apps) }
        if (schedule == null) {
            EditorRow(R.string.rules_row_active, durationLabel(rule.durationMinutes ?: 30), RecentrIcon.Clock) { actions.open(RuleSheet.Duration) }
        } else {
            EditorRow(R.string.rules_row_active, scheduleLabel(schedule), RecentrIcon.Clock) { actions.open(RuleSheet.Schedule) }
        }
        EditorRow(R.string.rules_row_unlocks, unlockLabel(rule), RecentrIcon.Lock) { actions.open(RuleSheet.Unlock) }
    }
    Gap(10.dp)
    SettingsGroup {
        StrictToggleRow(rule.strictMode, actions.onStrictChange, locked = actions.strictLocked)
    }
}

/** Figma 35 "Lock": Block / Unlock method / Unlocks per day / Active + "Why this lock?" note. */
@Composable
internal fun ColumnScope.LockEditor(state: RuleEditorUiState, title: String, actions: EditorActions) {
    val rule = state.draft
    Header(rule, title, stringResource(R.string.rules_lock_subtitle), actions)
    Gap(20.dp)
    SettingsGroup {
        EditorRow(R.string.rules_row_block, appsLabel(rule.apps), RecentrIcon.Apps) { actions.open(RuleSheet.Apps) }
        EditorRow(R.string.rules_row_unlock_method, unlockLabel(rule), RecentrIcon.Hourglass) { actions.open(RuleSheet.Unlock) }
        EditorRow(R.string.rules_row_unlocks_per_day, unlocksPerDayLabel(rule.unlocksPerDay), RecentrIcon.Refresh) { actions.open(RuleSheet.UnlocksPerDay) }
        EditorRow(R.string.rules_row_active, scheduleLabel(rule.schedule), RecentrIcon.Clock) { actions.open(RuleSheet.Schedule) }
    }
    Gap(16.dp)
    StickyNote(
        title = stringResource(R.string.rules_lock_note_title),
        body = rule.note?.takeIf { it.isNotBlank() } ?: stringResource(R.string.rules_why_placeholder),
        onClick = { actions.open(RuleSheet.Note) },
        modifier = Modifier.fillMaxWidth().graphicsLayer { rotationZ = 1.5f },
    )
}

private const val LIMIT_STEP = 15
private const val LIMIT_MIN = 15
private const val LIMIT_MAX = 12 * 60

/** Figma 38 "Limit": daily budget stepper + Apps / Resets / When time is up. */
@Composable
internal fun ColumnScope.LimitEditor(state: RuleEditorUiState, title: String, actions: EditorActions) {
    val rule = state.draft
    val colors = RecentrTheme.colors
    val limit = rule.limitMinutes ?: 60
    Header(rule, title, stringResource(R.string.rules_limit_subtitle), actions)
    Gap(20.dp)
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RecentrShapes.card)
            .background(colors.alphaSky12)
            .padding(horizontal = 16.dp, vertical = 18.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(stringResource(R.string.rules_daily_budget), style = RecentrTheme.typography.captionMedium, color = colors.brandSky)
        ValueStepper(
            value = compactDuration(limit),
            onDecrease = { actions.update { it.copy(limitMinutes = (limit - LIMIT_STEP).coerceAtLeast(LIMIT_MIN)) } },
            onIncrease = { actions.update { it.copy(limitMinutes = (limit + LIMIT_STEP).coerceAtMost(LIMIT_MAX)) } },
            buttonSize = 48.dp,
            buttonStyle = RecentrIconButtonStyle.Surface,
            decreaseEnabled = limit > LIMIT_MIN,
            increaseEnabled = limit < LIMIT_MAX,
        )
    }
    Gap(14.dp)
    SettingsGroup {
        EditorRow(R.string.rules_row_apps, appsCategoryLabel(rule.apps), RecentrIcon.Apps) { actions.open(RuleSheet.Apps) }
        EditorRow(R.string.rules_row_resets, limitResetLabel(rule.limitReset), RecentrIcon.Refresh) { actions.open(RuleSheet.Resets) }
        EditorRow(R.string.rules_row_when_time_up, limitActionLabel(rule.limitAction), RecentrIcon.Lock) { actions.open(RuleSheet.TimeUp) }
    }
}

/** Figma 39 "Bedtime": FROM / UNTIL cards, weekday dots, Block / Strict mode. */
@Composable
internal fun ColumnScope.BedtimeEditor(state: RuleEditorUiState, title: String, actions: EditorActions) {
    val rule = state.draft
    val schedule = rule.schedule ?: RuleSchedule(23 * 60, 7 * 60)
    Header(rule, title, stringResource(R.string.rules_bedtime_subtitle), actions)
    Gap(20.dp)
    TimesRow(
        startMinute = schedule.startMinute,
        endMinute = schedule.endMinute,
        onStart = { actions.open(RuleSheet.BedtimeStart) },
        onEnd = { actions.open(RuleSheet.BedtimeEnd) },
    )
    Gap(14.dp)
    DayDots(
        days = schedule.days,
        onToggle = { day ->
            actions.update {
                val current = it.schedule ?: schedule
                val days = if (day in current.days) current.days - day else current.days + day
                if (days.isEmpty()) it else it.copy(schedule = current.copy(days = days))
            }
        },
    )
    Gap(16.dp)
    SettingsGroup {
        EditorRow(R.string.rules_row_block, appsLabel(rule.apps), RecentrIcon.Apps) { actions.open(RuleSheet.Apps) }
        StrictToggleRow(rule.strictMode, actions.onStrictChange, locked = actions.strictLocked)
    }
}

/** Figma 40 "Habit": task checklist card + Unlocks / Repeats. */
@Composable
internal fun ColumnScope.HabitEditor(state: RuleEditorUiState, title: String, habitTitle: String, actions: EditorActions) {
    val rule = state.draft
    Header(rule, title, stringResource(R.string.rules_habit_subtitle), actions)
    Gap(20.dp)
    HabitTasksCard(rule = rule, title = habitTitle, actions = actions)
    Gap(16.dp)
    SettingsGroup {
        EditorRow(R.string.rules_row_unlocks, appsLabel(rule.apps), RecentrIcon.Apps) { actions.open(RuleSheet.Apps) }
        EditorRow(R.string.rules_row_repeats, habitRepeatLabel(rule.habitRepeat), RecentrIcon.Refresh) { actions.open(RuleSheet.Repeats) }
    }
}

@Composable
internal fun HabitTasksCard(rule: Rule, title: String, actions: EditorActions, modifier: Modifier = Modifier) {
    val colors = RecentrTheme.colors
    val shape = RoundedCornerShape(24.dp)
    var menu by remember { mutableStateOf(false) }
    Column(
        modifier = modifier
            .fillMaxWidth()
            .recentrShadow(RecentrElevation.float, shape)
            .clip(shape)
            .background(colors.bgSurface)
            .border(1.dp, colors.borderSubtle, shape)
            .padding(horizontal = 18.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        androidx.compose.foundation.layout.Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(title, style = RecentrTheme.typography.headline, color = colors.textPrimary, modifier = Modifier.weight(1f))
            Box {
                RecentrIconImage(
                    RecentrIcon.More,
                    contentDescription = stringResource(R.string.rules_task_menu),
                    tint = colors.textTertiary,
                    modifier = Modifier.clip(RecentrShapes.full).clickable(role = Role.Button) { menu = true },
                )
                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }, containerColor = colors.bgSurface) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.rules_reset_tasks), style = RecentrTheme.typography.bodyMedium) },
                        onClick = {
                            menu = false
                            actions.update { it.copy(habitTasks = it.habitTasks.map { task -> task.copy(done = false) }) }
                        },
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.rules_remove_done), style = RecentrTheme.typography.bodyMedium) },
                        onClick = {
                            menu = false
                            actions.update { it.copy(habitTasks = it.habitTasks.filterNot { task -> task.done }) }
                        },
                    )
                }
            }
        }
        rule.habitTasks.forEach { task ->
            androidx.compose.foundation.layout.Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .toggleable(value = task.done, role = Role.Checkbox) { done ->
                        actions.update {
                            it.copy(habitTasks = it.habitTasks.map { t -> if (t.id == task.id) t.copy(done = done) else t })
                        }
                    },
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RecentrCheckbox(checked = task.done, onCheckedChange = null)
                Text(
                    task.title,
                    style = RecentrTheme.typography.bodyMedium,
                    color = if (task.done) colors.textTertiary else colors.textPrimary,
                )
            }
        }
        androidx.compose.foundation.layout.Row(
            modifier = Modifier.clip(RecentrShapes.xs).clickable(role = Role.Button) { actions.open(RuleSheet.AddTask) },
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RecentrIconImage(RecentrIcon.Plus, tint = colors.brandSky)
            Text(stringResource(R.string.rules_add_task), style = RecentrTheme.typography.bodyMedium, color = colors.brandSky)
        }
    }
}
