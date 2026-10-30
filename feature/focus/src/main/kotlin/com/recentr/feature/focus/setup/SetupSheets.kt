package com.recentr.feature.focus.setup

import android.os.Handler
import android.os.Looper
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.recentr.core.designsystem.component.RecentrButton
import com.recentr.core.designsystem.component.RecentrIconButton
import com.recentr.core.designsystem.component.RecentrSheet
import com.recentr.core.designsystem.component.RecentrTextField
import com.recentr.core.designsystem.component.RecentrToggle
import com.recentr.core.designsystem.haptics.LocalRecentrHaptics
import com.recentr.core.designsystem.icon.RecentrIcon
import com.recentr.core.designsystem.theme.RecentrShapes
import com.recentr.core.designsystem.theme.RecentrTheme
import com.recentr.core.model.FocusConfig
import com.recentr.core.model.FocusSound
import com.recentr.core.model.FocusSoundCategory
import com.recentr.core.model.FocusSoundLayer
import com.recentr.feature.focus.R
import com.recentr.feature.focus.sound.AmbientMixer
import com.recentr.feature.focus.ui.CardDivider
import com.recentr.feature.focus.ui.FocusSheetContent
import com.recentr.feature.focus.ui.FocusSheetHeading
import com.recentr.feature.focus.ui.art
import com.recentr.feature.focus.ui.formatDuration
import com.recentr.feature.focus.ui.label
import com.recentr.feature.focus.ui.mixTitle
import kotlin.math.roundToInt

/**
 * 93 · Focus — Sounds: every recording by kind, three to a row. Any of them can be on, each with
 * its own volume under it. With [preview], the sheet plays the mix while it's up, from the first
 * change; otherwise the session itself plays the changes.
 */
@Composable
internal fun SoundsSheet(
    mix: List<FocusSoundLayer>,
    onMixChange: (List<FocusSoundLayer>) -> Unit,
    onDismissRequest: () -> Unit,
    preview: Boolean = true,
) {
    val colors = RecentrTheme.colors
    val context = LocalContext.current
    val haptics = LocalRecentrHaptics.current
    // The volume of each sound turned off here, for when it comes back on.
    val volumes = remember { mutableMapOf<FocusSound, Float>() }
    var touched by remember { mutableStateOf(false) }
    val mixer = remember(preview) { if (preview) AmbientMixer(context) else null }
    DisposableEffect(mixer) {
        onDispose {
            mixer?.stop()
            // Once faded out.
            mixer?.let { Handler(Looper.getMainLooper()).postDelayed(it::release, 1_000) }
        }
    }
    LaunchedEffect(mixer, mix, touched) { if (touched) mixer?.play(mix) }
    fun change(updated: List<FocusSoundLayer>) {
        touched = true
        onMixChange(updated)
    }
    fun volumeOf(sound: FocusSound) =
        mix.firstOrNull { it.sound == sound }?.volume ?: volumes[sound] ?: FocusSoundLayer.DEFAULT_VOLUME

    RecentrSheet(onDismissRequest = onDismissRequest) { controller ->
        Column(Modifier.fillMaxWidth().padding(start = 24.dp, end = 24.dp, top = 16.dp, bottom = 10.dp)) {
            Column(Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState())) {
                Row(verticalAlignment = Alignment.Top) {
                    Box(Modifier.weight(1f)) {
                        FocusSheetHeading(
                            stringResource(R.string.sounds_title),
                            if (mix.isEmpty()) {
                                stringResource(R.string.sounds_subtitle_none)
                            } else {
                                stringResource(R.string.sounds_subtitle_on, mixTitle(mix))
                            },
                        )
                    }
                    if (mix.isNotEmpty()) {
                        Text(
                            stringResource(R.string.sounds_turn_all_off),
                            style = RecentrTheme.typography.captionMedium,
                            color = colors.brandSky,
                            modifier = Modifier
                                .padding(top = 6.dp)
                                .clip(RecentrShapes.full)
                                .clickable(role = Role.Button) {
                                    mix.forEach { volumes[it.sound] = it.volume }
                                    haptics.selection()
                                    change(emptyList())
                                }
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                        )
                    }
                }
                FocusSoundCategory.entries.forEach { category ->
                    Spacer(Modifier.height(22.dp))
                    Text(
                        stringResource(category.label),
                        style = RecentrTheme.typography.captionMedium,
                        color = colors.textSecondary,
                        modifier = Modifier.semantics { heading() },
                    )
                    Spacer(Modifier.height(10.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        category.sounds.chunked(3).forEach { row ->
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                row.forEach { sound ->
                                    val on = mix.any { it.sound == sound }
                                    SoundTile(
                                        sound = sound,
                                        on = on,
                                        volume = volumeOf(sound),
                                        onToggle = {
                                            haptics.selection()
                                            if (on) {
                                                volumes[sound] = volumeOf(sound)
                                                change(mix.filterNot { it.sound == sound })
                                            } else {
                                                change(mix + FocusSoundLayer(sound, volumeOf(sound)))
                                            }
                                        },
                                        onVolume = { volume ->
                                            change(mix.map { if (it.sound == sound) it.copy(volume = volume) else it })
                                        },
                                        modifier = Modifier.weight(1f),
                                    )
                                }
                                repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
                            }
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
            }
            Spacer(Modifier.height(8.dp))
            RecentrButton(text = stringResource(R.string.sounds_done), onClick = { controller.close() }, modifier = Modifier.fillMaxWidth())
        }
    }
}

/**
 * "Sound · Soft rain": the illustration and name, a sky outline while on with the volume under it,
 * sunken while off. A tap turns it on or off; the bar sets how loud it plays.
 */
@Composable
private fun SoundTile(
    sound: FocusSound,
    on: Boolean,
    volume: Float,
    onToggle: () -> Unit,
    onVolume: (Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = RecentrTheme.colors
    val shape = RoundedCornerShape(20.dp)
    val name = stringResource(sound.label)
    val state = if (on) stringResource(R.string.sound_state_on, (volume * 100).roundToInt()) else stringResource(R.string.sound_state_off)
    Box(
        modifier
            .height(128.dp)
            .clip(shape)
            .background(if (on) colors.alphaSky12 else colors.bgSunken)
            .then(if (on) Modifier.border(2.dp, colors.brandSky, shape) else Modifier),
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .toggleable(value = on, role = Role.Switch, onValueChange = { onToggle() })
                .semantics { stateDescription = state }
                .padding(start = 8.dp, end = 8.dp, top = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Image(painterResource(sound.art), contentDescription = null, modifier = Modifier.size(52.dp))
            Text(name, style = RecentrTheme.typography.captionMedium, color = colors.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        // Above the tile's toggle, so a drag on the bar doesn't turn the sound off.
        if (on) {
            VolumeSlider(
                value = volume,
                onValueChange = onVolume,
                description = stringResource(R.string.sounds_volume_of, name),
                modifier = Modifier.align(Alignment.BottomCenter).padding(start = 12.dp, end = 12.dp, bottom = 6.dp).fillMaxWidth(),
            )
        }
    }
}

/** 6dp sunken track with a sky fill; tap or drag to set. */
@Composable
private fun VolumeSlider(value: Float, onValueChange: (Float) -> Unit, description: String, modifier: Modifier = Modifier) {
    val colors = RecentrTheme.colors
    val onChange by rememberUpdatedState(onValueChange)
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
                detectHorizontalDragGestures { change, _ ->
                    change.consume()
                    onChange((change.position.x / size.width).coerceIn(0f, 1f))
                }
            },
        contentAlignment = Alignment.CenterStart,
    ) {
        val fill = maxWidth * value
        Box(Modifier.fillMaxWidth().height(6.dp).clip(RecentrShapes.full).background(colors.bgSurface)) {
            Box(Modifier.fillMaxHeight().width(fill).clip(RecentrShapes.full).background(colors.brandSky))
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
