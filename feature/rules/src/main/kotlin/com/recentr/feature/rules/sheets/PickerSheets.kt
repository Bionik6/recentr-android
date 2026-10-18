package com.recentr.feature.rules.sheets

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.recentr.core.designsystem.component.OptionRow
import com.recentr.core.designsystem.component.RecentrButton
import com.recentr.core.designsystem.component.RecentrTextField
import com.recentr.core.designsystem.component.SettingToggleRow
import com.recentr.core.designsystem.icon.RecentrIcon
import com.recentr.core.designsystem.icon.RecentrIconImage
import com.recentr.core.designsystem.illustration.StickerImage
import com.recentr.core.designsystem.theme.RecentrTheme
import com.recentr.core.model.RuleSchedule
import com.recentr.core.model.RuleSticker
import com.recentr.feature.rules.R
import com.recentr.feature.rules.model.RulesMessages
import com.recentr.feature.rules.ui.asSticker
import com.recentr.feature.rules.ui.clockDigits
import com.recentr.feature.rules.ui.components.DayDots
import com.recentr.feature.rules.ui.components.Gap
import com.recentr.feature.rules.ui.components.RulesSheet
import com.recentr.feature.rules.ui.components.SectionCaption
import com.recentr.feature.rules.ui.components.SettingsGroup
import com.recentr.feature.rules.ui.components.SheetBody
import com.recentr.feature.rules.ui.components.TimeCard
import com.recentr.feature.rules.ui.isPm

/** Single-choice list (OptionRow) — selecting closes the sheet. Generic picker for rule options. */
@Composable
internal fun OptionPickerSheet(
    title: String,
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    RulesSheet(onDismissRequest = onDismiss) { closer ->
        SheetBody(figmaHeight = null, topGap = 18.dp, wrapContent = true, horizontalAlignment = Alignment.Start, bottomBar = {}) {
            Text(title, style = RecentrTheme.typography.title1, color = RecentrTheme.colors.textPrimary)
            Gap(16.dp)
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                options.forEachIndexed { index, label ->
                    OptionRow(
                        label = label,
                        selected = index == selectedIndex,
                        onClick = { closer.close { onSelect(index) } },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}

/** One text field + Save (reason note, new habit task). */
@Composable
internal fun TextInputSheet(
    title: String,
    initial: String,
    placeholder: String,
    onSave: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var text by rememberSaveable { mutableStateOf(initial) }
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }
    RulesSheet(onDismissRequest = onDismiss) { closer ->
        val save = { if (text.isNotBlank()) closer.close { onSave(text.trim()) } }
        SheetBody(
            figmaHeight = null,
            topGap = 18.dp,
            wrapContent = true,
            horizontalAlignment = Alignment.Start,
            bottomBar = {
                RecentrButton(
                    text = stringResource(R.string.rules_save),
                    onClick = save,
                    enabled = text.isNotBlank(),
                    modifier = Modifier.fillMaxWidth(),
                )
            },
        ) {
            Text(title, style = RecentrTheme.typography.title1, color = RecentrTheme.colors.textPrimary)
            Gap(14.dp)
            RecentrTextField(
                value = text,
                onValueChange = { text = it },
                placeholder = placeholder,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { save() }),
                modifier = Modifier.fillMaxWidth().focusRequester(focus),
            )
        }
    }
}

private val StickerChoices = listOf(
    RuleSticker.Bolt, RuleSticker.Lock, RuleSticker.Timer, RuleSticker.Moon,
    RuleSticker.Leaf, RuleSticker.Note, RuleSticker.Flame,
)

/** Figma 46 "Edit rule": name field and sticker grid. */
@Composable
internal fun EditNameStickerSheet(
    initialName: String,
    initialSticker: RuleSticker,
    onSave: (String, RuleSticker) -> Unit,
    onDismiss: () -> Unit,
) {
    var name by rememberSaveable { mutableStateOf(initialName) }
    var sticker by rememberSaveable { mutableStateOf(initialSticker) }
    val colors = RecentrTheme.colors
    val moreSoon = stringResource(R.string.rules_more_stickers_soon)
    RulesSheet(onDismissRequest = onDismiss) { closer ->
        SheetBody(
            figmaHeight = 574.dp,
            topGap = 18.dp,
            horizontalAlignment = Alignment.Start,
            bottomBar = {
                RecentrButton(
                    text = stringResource(R.string.rules_save),
                    onClick = { closer.close { onSave(name.trim(), sticker) } },
                    enabled = name.isNotBlank(),
                    modifier = Modifier.fillMaxWidth(),
                )
            },
        ) {
            Text(stringResource(R.string.rules_edit_title), style = RecentrTheme.typography.title1, color = colors.textPrimary)
            Gap(14.dp)
            RecentrTextField(
                value = name,
                onValueChange = { name = it },
                placeholder = stringResource(R.string.rules_name_placeholder),
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Done),
                modifier = Modifier.fillMaxWidth(),
            )
            Gap(18.dp)
            SectionCaption(stringResource(R.string.rules_sticker))
            Gap(8.dp)
            val cells: List<RuleSticker?> = StickerChoices + null
            cells.chunked(4).forEachIndexed { row, items ->
                if (row > 0) Gap(10.dp)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    items.forEach { item ->
                        val selected = item == sticker
                        val shape = RoundedCornerShape(22.dp)
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(78.dp)
                                .clip(shape)
                                .background(if (selected) colors.alphaSky12 else colors.bgSunken)
                                .then(if (selected) Modifier.border(2.dp, colors.brandSky, shape) else Modifier)
                                .selectable(selected = selected, role = Role.RadioButton) {
                                    if (item != null) sticker = item else RulesMessages.show(moreSoon)
                                }
                                .then(if (item == null) Modifier.semantics { contentDescription = moreSoon } else Modifier),
                            contentAlignment = Alignment.Center,
                        ) {
                            if (item != null) {
                                StickerImage(item.asSticker, size = 60.dp)
                            } else {
                                RecentrIconImage(RecentrIcon.Plus, tint = colors.textTertiary)
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * FROM → UNTIL tilted time cards (Figma 39 "Times", 120dp tall). Positions are the Figma card
 * centres relative to the 354dp row centre.
 */
@Composable
internal fun TimesRow(
    startMinute: Int,
    endMinute: Int,
    onStart: (() -> Unit)?,
    onEnd: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val am = stringResource(R.string.rules_am)
    val pm = stringResource(R.string.rules_pm)
    val startMeridiem = if (isPm(startMinute)) pm else am
    val endMeridiem = if (isPm(endMinute)) pm else am
    val startCaption = if (startMinute >= 18 * 60) {
        stringResource(R.string.rules_tonight, startMeridiem)
    } else {
        stringResource(R.string.rules_today, startMeridiem)
    }
    val endCaption = when {
        endMinute <= startMinute -> stringResource(R.string.rules_tomorrow, endMeridiem)
        endMinute >= 18 * 60 -> stringResource(R.string.rules_tonight, endMeridiem)
        else -> stringResource(R.string.rules_today, endMeridiem)
    }
    Box(modifier.fillMaxWidth().height(120.dp), contentAlignment = Alignment.Center) {
        TimeCard(
            label = stringResource(R.string.rules_from),
            value = clockDigits(startMinute),
            caption = startCaption,
            rotation = -4f,
            onClick = onStart,
            modifier = Modifier.offset(x = (-98).dp, y = 0.4.dp),
        )
        RecentrIconImage(
            RecentrIcon.ArrowRight,
            tint = RecentrTheme.colors.textTertiary,
            modifier = Modifier.offset(y = 2.dp),
        )
        TimeCard(
            label = stringResource(R.string.rules_until),
            value = clockDigits(endMinute),
            caption = endCaption,
            rotation = 4f,
            onClick = onEnd,
            modifier = Modifier.offset(x = 75.7.dp, y = 4.6.dp),
        )
    }
}

/** "Active" schedule editor: optional all-day toggle, FROM/UNTIL cards and day dots. */
@Composable
internal fun ScheduleSheet(
    title: String,
    initial: RuleSchedule?,
    allowAllDay: Boolean,
    onSave: (RuleSchedule?) -> Unit,
    onDismiss: () -> Unit,
) {
    val fallback = initial?.takeIf { it.lengthMinutes < RuleSchedule.MINUTES_PER_DAY } ?: RuleSchedule(9 * 60, 17 * 60)
    var allDay by rememberSaveable { mutableStateOf(allowAllDay && (initial == null || initial.lengthMinutes >= RuleSchedule.MINUTES_PER_DAY)) }
    var start by rememberSaveable { mutableIntStateOf(fallback.startMinute) }
    var end by rememberSaveable { mutableIntStateOf(fallback.endMinute) }
    var days by rememberSaveable { mutableStateOf((initial?.days ?: (1..7).toSet()).toList()) }
    var editing by rememberSaveable { mutableStateOf(TimeField.None) }
    RulesSheet(onDismissRequest = onDismiss) { closer ->
        SheetBody(
            figmaHeight = null,
            topGap = 18.dp,
            wrapContent = true,
            bottomBar = {
                RecentrButton(
                    text = stringResource(R.string.rules_save),
                    enabled = days.isNotEmpty(),
                    onClick = {
                        val result = when {
                            allDay && days.size == 7 -> null
                            allDay -> RuleSchedule(0, 0, days.toSet())
                            else -> RuleSchedule(start, end, days.toSet())
                        }
                        closer.close { onSave(result) }
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
            },
        ) {
            Text(
                title,
                style = RecentrTheme.typography.title1,
                color = RecentrTheme.colors.textPrimary,
                modifier = Modifier.fillMaxWidth(),
            )
            Gap(16.dp)
            if (allowAllDay) {
                SettingsGroup {
                    SettingToggleRow(
                        label = stringResource(R.string.rules_all_day),
                        checked = allDay,
                        onCheckedChange = { allDay = it },
                        icon = RecentrIcon.Sun,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                Gap(16.dp)
            }
            if (!allDay) {
                TimesRow(
                    startMinute = start,
                    endMinute = end,
                    onStart = { editing = TimeField.Start },
                    onEnd = { editing = TimeField.End },
                )
                Gap(14.dp)
            }
            DayDots(days = days.toSet(), onToggle = { day -> days = if (day in days) days - day else (days + day).sorted() })
        }
    }
    when (editing) {
        TimeField.None -> Unit
        TimeField.Start -> TimeOfDayDialog(
            title = stringResource(R.string.rules_starts_at),
            initialMinuteOfDay = start,
            onConfirm = { start = it; editing = TimeField.None },
            onDismiss = { editing = TimeField.None },
        )
        TimeField.End -> TimeOfDayDialog(
            title = stringResource(R.string.rules_ends_at_title),
            initialMinuteOfDay = end,
            onConfirm = { end = it; editing = TimeField.None },
            onDismiss = { editing = TimeField.None },
        )
    }
}

internal enum class TimeField { None, Start, End }
