package com.recentr.feature.focus.setup

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.recentr.core.designsystem.component.AppRow
import com.recentr.core.designsystem.component.RecentrButton
import com.recentr.core.designsystem.component.RecentrIconButton
import com.recentr.core.designsystem.component.RecentrSheet
import com.recentr.core.designsystem.component.RecentrTextField
import com.recentr.core.designsystem.component.RecentrToggle
import com.recentr.core.designsystem.icon.RecentrIcon
import com.recentr.core.designsystem.illustration.StickerImage
import com.recentr.core.designsystem.theme.RecentrShapes
import com.recentr.core.designsystem.theme.RecentrTheme
import com.recentr.core.model.AppId
import com.recentr.core.model.FocusConfig
import com.recentr.core.model.FocusSound
import com.recentr.feature.focus.R
import com.recentr.feature.focus.ui.CardDivider
import com.recentr.feature.focus.ui.FocusSheetContent
import com.recentr.feature.focus.ui.FocusSheetHeading
import com.recentr.feature.focus.ui.demoApp
import com.recentr.feature.focus.ui.formatDuration
import com.recentr.feature.focus.ui.hint
import com.recentr.feature.focus.ui.label
import com.recentr.feature.focus.ui.sticker
import kotlin.math.roundToInt

/** 93 · Focus — Sounds: 3 × 2 grid of ambient sounds and the volume slider. */
@Composable
internal fun SoundsSheet(
    sound: FocusSound,
    volume: Float,
    onSound: (FocusSound) -> Unit,
    onVolume: (Float) -> Unit,
    onDismissRequest: () -> Unit,
) {
    val colors = RecentrTheme.colors
    RecentrSheet(onDismissRequest = onDismissRequest) { controller ->
        FocusSheetContent(topGap = 22.dp) {
            FocusSheetHeading(stringResource(R.string.sounds_title), stringResource(R.string.sounds_subtitle))
            Spacer(Modifier.height(18.dp))
            Column(Modifier.selectableGroup(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                FocusSound.entries.chunked(3).forEach { row ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        row.forEach { item ->
                            SoundTile(item, selected = item == sound, onClick = { onSound(item) }, modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.sounds_volume), style = RecentrTheme.typography.captionMedium, color = colors.textSecondary)
                VolumeSlider(volume, onVolume, Modifier.weight(1f))
                Text(
                    stringResource(R.string.sounds_volume_value, (volume * 100).roundToInt()),
                    style = RecentrTheme.typography.captionMedium,
                    color = colors.textSecondary,
                    textAlign = TextAlign.End,
                    modifier = Modifier.width(36.dp),
                )
            }
            Spacer(Modifier.height(18.dp))
            RecentrButton(text = stringResource(R.string.sounds_done), onClick = { controller.close() }, modifier = Modifier.fillMaxWidth())
        }
    }
}

/** "Sound · Rain on window": 124dp tile, sky outline when selected, sunken otherwise. */
@Composable
private fun SoundTile(sound: FocusSound, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = RecentrTheme.colors
    val shape = RoundedCornerShape(20.dp)
    val border = if (selected) 2.dp else 0.dp
    Column(
        modifier
            .height(124.dp)
            .clip(shape)
            .background(if (selected) colors.alphaSky12 else colors.bgSunken)
            .then(if (selected) Modifier.border(border, colors.brandSky, shape) else Modifier)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            // Fixed 124dp tile, content from the top: a two-line name eats into the bottom padding.
            .padding(start = 12.dp + border, end = 12.dp + border, top = 14.dp + border),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        StickerImage(sound.sticker, size = 44.dp)
        Text(stringResource(sound.label), style = RecentrTheme.typography.captionMedium, color = colors.textPrimary, maxLines = 2)
        Text(stringResource(sound.hint), style = RecentrTheme.typography.labelSmall, color = colors.textSecondary, maxLines = 1)
    }
}

/** 6dp sunken track with a sky fill; tap or drag to set. */
@Composable
private fun VolumeSlider(value: Float, onValueChange: (Float) -> Unit, modifier: Modifier = Modifier) {
    val colors = RecentrTheme.colors
    val onChange by rememberUpdatedState(onValueChange)
    val description = stringResource(R.string.sounds_volume)
    BoxWithConstraints(
        modifier
            .height(24.dp)
            .semantics {
                contentDescription = description
                progressBarRangeInfo = ProgressBarRangeInfo(value, 0f..1f, steps = 9)
                setProgress { onChange(it.coerceIn(0f, 1f)); true }
            }
            .pointerInput(Unit) { detectTapGestures { onChange((it.x / size.width).coerceIn(0f, 1f)) } }
            .pointerInput(Unit) {
                detectHorizontalDragGestures { change, _ -> onChange((change.position.x / size.width).coerceIn(0f, 1f)) }
            },
        contentAlignment = Alignment.CenterStart,
    ) {
        val fill = maxWidth * value
        Box(Modifier.fillMaxWidth().height(6.dp).clip(RecentrShapes.full).background(colors.bgSunken)) {
            Box(Modifier.fillMaxHeight().width(fill).clip(RecentrShapes.full).background(colors.brandSky))
        }
    }
}

/** Apps put on hold during focus (design-system App Row list). */
@Composable
internal fun BlockAppsSheet(
    selected: List<AppId>,
    onChange: (List<AppId>) -> Unit,
    onDismissRequest: () -> Unit,
) {
    RecentrSheet(onDismissRequest = onDismissRequest) { controller ->
        FocusSheetContent(topGap = 22.dp) {
            FocusSheetHeading(stringResource(R.string.block_title), stringResource(R.string.block_subtitle))
            Spacer(Modifier.height(12.dp))
            AppId.entries.forEach { app ->
                AppRow(
                    app = app.demoApp,
                    checked = app in selected,
                    onCheckedChange = { checked ->
                        onChange(if (checked) AppId.entries.filter { it in selected || it == app } else selected - app)
                    },
                )
            }
            Spacer(Modifier.height(18.dp))
            RecentrButton(text = stringResource(R.string.block_done), onClick = { controller.close() }, modifier = Modifier.fillMaxWidth())
        }
    }
}

/** 98 · Focus — Rounds: four steppers, auto-start toggle with the total, "Save rounds". */
@Composable
internal fun RoundsSheet(config: FocusConfig, onSave: (FocusConfig) -> Unit, onDismissRequest: () -> Unit) {
    val colors = RecentrTheme.colors
    var focus by rememberSaveable { mutableIntStateOf(config.focusMinutes) }
    var shortBreak by rememberSaveable { mutableIntStateOf(config.shortBreakMinutes) }
    var rounds by rememberSaveable { mutableIntStateOf(config.rounds) }
    var longBreak by rememberSaveable { mutableIntStateOf(config.longBreakMinutes) }
    var autoStart by rememberSaveable { mutableStateOf(config.autoStartNextRound) }
    val draft = config.copy(
        focusMinutes = focus,
        shortBreakMinutes = shortBreak,
        rounds = rounds,
        longBreakMinutes = longBreak,
        autoStartNextRound = autoStart,
    )
    RecentrSheet(onDismissRequest = onDismissRequest) { controller ->
        FocusSheetContent(topGap = 22.dp) {
            FocusSheetHeading(stringResource(R.string.rounds_title), stringResource(R.string.rounds_subtitle))
            Spacer(Modifier.height(14.dp))
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(colors.bgSunken)
                    .padding(horizontal = 16.dp, vertical = 4.dp),
            ) {
                StepperRow(stringResource(R.string.rounds_focus), stringResource(R.string.rounds_minutes, focus), focus > FocusConfig.MIN_FOCUS, focus < FocusConfig.MAX_FOCUS, { focus -= 5 }, { focus += 5 })
                CardDivider()
                StepperRow(stringResource(R.string.rounds_short_break), stringResource(R.string.rounds_minutes, shortBreak), shortBreak > 1, shortBreak < 30, { shortBreak-- }, { shortBreak++ })
                CardDivider()
                StepperRow(stringResource(R.string.rounds_count), rounds.toString(), rounds > 1, rounds < FocusConfig.MAX_ROUNDS, { rounds-- }, { rounds++ })
                CardDivider()
                StepperRow(stringResource(R.string.rounds_long_break), stringResource(R.string.rounds_minutes, longBreak), longBreak > 5, longBreak < 60, { longBreak -= 5 }, { longBreak += 5 })
            }
            Spacer(Modifier.height(10.dp))
            Row(
                Modifier
                    .fillMaxWidth()
                    .selectable(selected = autoStart, role = Role.Switch, onClick = { autoStart = !autoStart })
                    .padding(start = 4.dp, top = 12.dp, bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(stringResource(R.string.rounds_auto_start), style = RecentrTheme.typography.bodyMedium, color = colors.textPrimary)
                    Text(
                        stringResource(R.string.rounds_total, formatDuration(draft.totalMinutes)),
                        style = RecentrTheme.typography.captionRegular,
                        color = colors.textSecondary,
                    )
                }
                RecentrToggle(checked = autoStart, onCheckedChange = null)
            }
            Spacer(Modifier.height(14.dp))
            RecentrButton(
                text = stringResource(R.string.rounds_save),
                onClick = {
                    onSave(draft)
                    controller.close()
                },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun StepperRow(
    label: String,
    value: String,
    canDecrease: Boolean,
    canIncrease: Boolean,
    onDecrease: () -> Unit,
    onIncrease: () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = RecentrTheme.typography.bodyMedium, color = RecentrTheme.colors.textPrimary, modifier = Modifier.weight(1f))
        RecentrIconButton(
            icon = RecentrIcon.Minus,
            contentDescription = stringResource(R.string.rounds_decrease, label),
            onClick = onDecrease,
            enabled = canDecrease,
            modifier = Modifier.alpha(if (canDecrease) 1f else 0.4f),
        )
        Text(
            value,
            style = RecentrTheme.typography.headline,
            color = RecentrTheme.colors.textPrimary,
            textAlign = TextAlign.Center,
            maxLines = 1,
            modifier = Modifier.width(64.dp),
        )
        RecentrIconButton(
            icon = RecentrIcon.Plus,
            contentDescription = stringResource(R.string.rounds_increase, label),
            onClick = onIncrease,
            enabled = canIncrease,
            modifier = Modifier.alpha(if (canIncrease) 1f else 0.4f),
        )
    }
}

/** Renames the session ("Deep work"), shown under the dial and in the focus log. */
@Composable
internal fun RenameSheet(label: String, onSave: (String) -> Unit, onDismissRequest: () -> Unit) {
    var value by rememberSaveable { mutableStateOf(label) }
    RecentrSheet(onDismissRequest = onDismissRequest) { controller ->
        FocusSheetContent(topGap = 22.dp) {
            Text(stringResource(R.string.focus_rename_title), style = RecentrTheme.typography.title2, color = RecentrTheme.colors.textPrimary)
            Spacer(Modifier.height(16.dp))
            RecentrTextField(
                value = value,
                onValueChange = { value = it.take(40) },
                placeholder = label,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = {
                    if (value.isNotBlank()) {
                        onSave(value.trim())
                        controller.close()
                    }
                }),
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(18.dp))
            RecentrButton(
                text = stringResource(R.string.focus_rename_save),
                onClick = {
                    onSave(value.trim())
                    controller.close()
                },
                enabled = value.isNotBlank(),
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
