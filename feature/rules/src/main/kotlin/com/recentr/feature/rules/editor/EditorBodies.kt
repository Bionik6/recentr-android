package com.recentr.feature.rules.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.recentr.core.designsystem.component.RecentrButton
import com.recentr.core.designsystem.component.RecentrIconButtonStyle
import com.recentr.core.designsystem.component.SettingRow
import com.recentr.core.designsystem.component.StickyNote
import com.recentr.core.designsystem.icon.RecentrIcon
import com.recentr.core.designsystem.theme.RecentrShapes
import com.recentr.core.designsystem.theme.RecentrTheme
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
import com.recentr.feature.rules.ui.components.InfoRow
import com.recentr.feature.rules.ui.components.MiniTemplateCard
import com.recentr.feature.rules.ui.components.RuleSheetHeader
import com.recentr.feature.rules.ui.components.SectionCaption
import com.recentr.feature.rules.ui.components.SettingsGroup
import com.recentr.feature.rules.ui.components.StrictToggleRow
import com.recentr.feature.rules.ui.components.UnlockMethodRow
import com.recentr.feature.rules.ui.components.ValueStepper
import com.recentr.feature.rules.ui.durationLabel
import com.recentr.feature.rules.ui.unlocksPerDayLabel

/** What the editor bodies can do. */
internal class EditorActions(
    val open: (RuleSheet) -> Unit,
    val update: ((Rule) -> Rule) -> Unit,
    /** Strict mode toggle. */
    val onStrictChange: (Boolean) -> Unit,
    val onSuggestion: (BuiltInTemplate) -> Unit,
    val onCommit: () -> Unit,
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

/** Primary CTA ("Create session", "Save"). Disabled until the rule has apps to block. */
@Composable
internal fun ColumnScope.EditorCta(state: RuleEditorUiState, createLabel: Int, actions: EditorActions) {
    RecentrButton(
        text = stringResource(if (state.isNew) createLabel else R.string.rules_save),
        onClick = actions.onCommit,
        enabled = state.draft.blocksApps,
        modifier = Modifier.fillMaxWidth(),
    )
}

/** Figma 31 "Session": suggestions, Block / Active (its length) / Unlock method, Strict mode. */
@Composable
internal fun ColumnScope.SessionEditor(state: RuleEditorUiState, title: String, actions: EditorActions) {
    val rule = state.draft
    Header(
        rule = rule,
        title = title,
        subtitle = stringResource(R.string.rules_session_subtitle, durationLabel(rule.durationMinutes ?: 30)),
        actions = actions,
    )
    Gap(20.dp)
    if (state.isNew) {
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
        EditorRow(R.string.rules_row_active, durationLabel(rule.durationMinutes ?: 30), RecentrIcon.Clock) { actions.open(RuleSheet.Duration) }
        UnlockMethodRow(rule, onClick = { actions.open(RuleSheet.Unlock) }, icon = RecentrIcon.Lock)
    }
    Gap(10.dp)
    SettingsGroup {
        StrictToggleRow(rule.strictMode, actions.onStrictChange)
    }
}

/** Figma 35 "Lock": Block / Unlock method / Unlocks per day + "Why this lock?" note. No days or hours: Activate turns it on. */
@Composable
internal fun ColumnScope.LockEditor(state: RuleEditorUiState, title: String, actions: EditorActions) {
    val rule = state.draft
    Header(rule, title, stringResource(R.string.rules_lock_subtitle), actions)
    Gap(20.dp)
    SettingsGroup {
        EditorRow(R.string.rules_row_block, appsLabel(rule.apps), RecentrIcon.Apps) { actions.open(RuleSheet.Apps) }
        UnlockMethodRow(rule, onClick = { actions.open(RuleSheet.Unlock) })
        EditorRow(R.string.rules_row_unlocks_per_day, unlocksPerDayLabel(rule.unlocksPerDay), RecentrIcon.Refresh) { actions.open(RuleSheet.UnlocksPerDay) }
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

/**
 * Figma 38 "Limit": the budget for today (stepper), then the Apps row and "Resets automatically at
 * midnight.": started by hand, a limit runs for today only. No unlocks: only an emergency unlock
 * gets past it.
 */
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
        Text(stringResource(R.string.rules_budget_today), style = RecentrTheme.typography.captionMedium, color = colors.brandSky)
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
        InfoRow(stringResource(R.string.rules_limit_resets), RecentrIcon.Clock)
    }
}

/** Figma 39 "Bedtime": FROM / UNTIL cards, weekday dots, Block / Unlock method / Strict mode. */
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
        UnlockMethodRow(rule, onClick = { actions.open(RuleSheet.Unlock) })
        StrictToggleRow(rule.strictMode, actions.onStrictChange)
    }
}
