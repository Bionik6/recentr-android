package com.recentr.feature.focus.setup

import android.os.Handler
import android.os.Looper
import androidx.compose.animation.core.animateDpAsState
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.onClick
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
import com.recentr.core.designsystem.icon.RecentrIconImage
import com.recentr.core.designsystem.theme.RecentrElevation
import com.recentr.core.designsystem.theme.RecentrShapes
import com.recentr.core.designsystem.theme.RecentrTheme
import com.recentr.core.designsystem.theme.recentrShadow
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
 * 93 · Focus — Sounds: every recording by kind, one to a row, each at its own volume. The art
 * turns a sound on, mutes it or brings it back; the slider sets how loud it plays and turns it on.
 * With [preview], the sheet plays the mix while it's up, from the first change; otherwise the
 * session itself plays the changes.
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
    // The sounds muted here, at the volume they come back at: forgotten once the sheet closes.
    var muted by remember { mutableStateOf(emptyList<FocusSoundLayer>()) }
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
    val sounds = SoundsMix(mix, muted)
    fun change(updated: SoundsMix) {
        muted = updated.muted
        if (updated.on != mix) {
            touched = true
            onMixChange(updated.on)
        }
    }

    RecentrSheet(onDismissRequest = onDismissRequest) { controller ->
        Box(Modifier.weight(1f, fill = false)) {
            Column(
                Modifier
                    .verticalScroll(rememberScrollState())
                    // Figma 93G: the last row ends 16dp over the fade.
                    .padding(start = 24.dp, end = 24.dp, top = 16.dp, bottom = 40.dp),
            ) {
                Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Box(Modifier.weight(1f)) {
                        FocusSheetHeading(stringResource(R.string.sounds_title), soundsSummary(sounds))
                    }
                    sounds.muteLink?.let { link ->
                        Text(
                            stringResource(if (link == MuteLink.MuteAll) R.string.sounds_mute_all else R.string.sounds_unmute_all),
                            style = RecentrTheme.typography.captionMedium,
                            color = colors.brandSky,
                            modifier = Modifier
                                // The text 5dp down and flush right; the padding only widens the touch.
                                .padding(top = 1.dp)
                                .offset(x = 8.dp)
                                .clip(RecentrShapes.full)
                                .clickable(role = Role.Button) {
                                    haptics.selection()
                                    change(sounds.muteLinkTapped())
                                }
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                        )
                    }
                }
                FocusSoundCategory.entries.forEachIndexed { index, category ->
                    // Figma: 20dp over the first section, 28 over the next ones.
                    Spacer(Modifier.height(if (index == 0) 20.dp else 28.dp))
                    Row(Modifier.fillMaxWidth()) {
                        Text(
                            stringResource(category.label),
                            style = RecentrTheme.typography.captionMedium,
                            color = colors.textSecondary,
                            modifier = Modifier.weight(1f).semantics { heading() },
                        )
                        val count = sounds.onCount(category)
                        if (count > 0) {
                            Text(stringResource(R.string.sounds_on_count, count), style = RecentrTheme.typography.captionMedium, color = colors.brandSky)
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        category.sounds.forEach { sound ->
                            SoundRow(
                                sound = sound,
                                state = sounds.state(sound),
                                volume = sounds.volume(sound),
                                onArtClick = {
                                    haptics.selection()
                                    change(sounds.artTapped(sound))
                                },
                                onVolumeChange = { change(sounds.volumeChanged(sound, it)) },
                            )
                        }
                    }
                }
            }
            // Figma 93G: the list fades out over 24dp above Done.
            Box(
                Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(24.dp)
                    .background(Brush.verticalGradient(listOf(colors.bgSurface.copy(alpha = 0f), colors.bgSurface))),
            )
        }
        RecentrButton(
            text = stringResource(R.string.sounds_done),
            onClick = { controller.close() },
            modifier = Modifier.fillMaxWidth().padding(start = 24.dp, end = 24.dp, top = 12.dp, bottom = 8.dp),
        )
    }
}

/** "Soft rain + 2 on", "Soft rain on · 1 muted", "Nothing playing · 3 muted", or how it works with nothing on. */
@Composable
private fun soundsSummary(sounds: SoundsMix): String = when {
    sounds.on.isEmpty() && sounds.muted.isEmpty() -> stringResource(R.string.sounds_subtitle_none)
    sounds.on.isEmpty() -> stringResource(R.string.sounds_subtitle_muted, sounds.muted.size)
    sounds.muted.isEmpty() -> stringResource(R.string.sounds_subtitle_on, mixTitle(sounds.on))
    else -> stringResource(R.string.sounds_subtitle_on_muted, mixTitle(sounds.on), sounds.muted.size)
}

/**
 * Sound Row (Components › Sound Mixer): the art in a 60dp tile, the name, and the volume under it.
 * Playing, the tile is sky and wears a speaker; muted, it greys and the speaker is crossed out.
 * TalkBack reads the row as one adjustable element, "Soft rain, 60%", whose action turns it on,
 * mutes it or brings it back.
 */
@Composable
private fun SoundRow(
    sound: FocusSound,
    state: SoundState,
    volume: Float,
    onArtClick: () -> Unit,
    onVolumeChange: (Float) -> Unit,
) {
    val colors = RecentrTheme.colors
    val name = stringResource(sound.label)
    val tile = RoundedCornerShape(18.dp)
    var sliding by remember { mutableStateOf(false) }
    val value = if (state == SoundState.Off) {
        stringResource(R.string.sound_state_off)
    } else {
        stringResource(R.string.sounds_volume_value, (volume * 100).roundToInt())
    }
    val spoken = if (state == SoundState.Muted) stringResource(R.string.sound_state_muted) else value
    val action = stringResource(
        when (state) {
            SoundState.Off -> R.string.sound_turn_on
            SoundState.On -> R.string.sound_mute
            SoundState.Muted -> R.string.sound_unmute
        },
    )
    Row(
        Modifier
            .fillMaxWidth()
            .height(60.dp)
            .clearAndSetSemantics {
                contentDescription = name
                stateDescription = spoken
                progressBarRangeInfo = ProgressBarRangeInfo(volume, 0f..1f, steps = 9)
                setProgress { onVolumeChange(it); true }
                onClick(label = action) { onArtClick(); true }
            },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        // Not clipped: the badge hangs past the tile's corner.
        Box(Modifier.size(60.dp)) {
            Box(
                Modifier
                    .matchParentSize()
                    .clip(tile)
                    .background(if (state == SoundState.On) colors.alphaSky12 else colors.bgSunken)
                    .then(if (state == SoundState.On) Modifier.border(1.5.dp, colors.brandSky, tile) else Modifier)
                    .clickable(onClick = onArtClick),
                contentAlignment = Alignment.Center,
            ) {
                Image(
                    painterResource(sound.art),
                    contentDescription = null,
                    modifier = Modifier.size(48.dp).alpha(if (state == SoundState.Muted) 0.4f else 1f),
                )
            }
            if (state != SoundState.Off) {
                SoundBadge(muted = state == SoundState.Muted, modifier = Modifier.offset(x = 44.dp, y = 44.dp))
            }
        }
        Column(Modifier.weight(1f)) {
            Text(
                name,
                style = RecentrTheme.typography.bodyMedium,
                color = if (state == SoundState.Muted) colors.textSecondary else colors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                VolumeSlider(
                    value = volume,
                    muted = state == SoundState.Muted,
                    onValueChange = onVolumeChange,
                    onSlidingChange = { sliding = it },
                    modifier = Modifier.weight(1f),
                )
                Text(
                    value,
                    style = RecentrTheme.typography.captionMedium,
                    color = when {
                        sliding -> colors.brandSky
                        state == SoundState.On -> colors.textPrimary
                        else -> colors.textTertiary
                    },
                    textAlign = TextAlign.End,
                    maxLines = 1,
                    modifier = Modifier.width(40.dp),
                )
            }
        }
    }
}

/** The speaker pinned on a sound's tile (Sound Row › Badge): sky while it plays, crossed out in grey while muted. */
@Composable
private fun SoundBadge(muted: Boolean, modifier: Modifier = Modifier) {
    val colors = RecentrTheme.colors
    Box(
        modifier
            .size(22.dp)
            .recentrShadow(RecentrElevation.badge, CircleShape)
            .clip(CircleShape)
            .background(colors.bgSurface)
            .border(1.dp, colors.borderSubtle, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        RecentrIconImage(
            if (muted) RecentrIcon.VolumeOff else RecentrIcon.Volume,
            tint = if (muted) colors.textSecondary else colors.brandSky,
            size = 13.dp,
        )
    }
}

/**
 * Volume Slider (Components › Sound Mixer): a 6dp sunken track filled up to the thumb, in sky or,
 * while [muted], in grey at the level it comes back at. The thumb's centre runs from 10dp to the
 * width − 10dp; it's 20dp and white at rest and grows to 26dp with a sky ring while dragged. A tap
 * or a sideways drag sets the volume; a drag that starts up or down scrolls the sheet.
 */
@Composable
private fun VolumeSlider(
    value: Float,
    muted: Boolean,
    onValueChange: (Float) -> Unit,
    onSlidingChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = RecentrTheme.colors
    val onChange by rememberUpdatedState(onValueChange)
    val onSliding by rememberUpdatedState(onSlidingChange)
    var sliding by remember { mutableStateOf(false) }
    fun slide(to: Boolean) {
        sliding = to
        onSliding(to)
    }
    val knob by animateDpAsState(if (sliding) 26.dp else 20.dp, label = "knob")
    BoxWithConstraints(
        modifier
            .height(28.dp)
            .pointerInput(Unit) {
                detectTapGestures { onChange(volumeAt(it.x, size.width, 10.dp.toPx())) }
            }
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragStart = {
                        slide(true)
                        onChange(volumeAt(it.x, size.width, 10.dp.toPx()))
                    },
                    onDragEnd = { slide(false) },
                    onDragCancel = { slide(false) },
                ) { change, _ ->
                    change.consume()
                    onChange(volumeAt(change.position.x, size.width, 10.dp.toPx()))
                }
            },
        contentAlignment = Alignment.CenterStart,
    ) {
        val center = 10.dp + (maxWidth - 20.dp) * value.coerceIn(0f, 1f)
        Box(Modifier.fillMaxWidth().height(6.dp).clip(RecentrShapes.full).background(colors.bgSunken))
        Box(Modifier.width(center).height(6.dp).clip(RecentrShapes.full).background(if (muted) colors.fillGrabber else colors.brandSky))
        Box(
            Modifier
                .offset(x = center - knob / 2)
                .size(knob)
                .recentrShadow(if (sliding) RecentrElevation.knobPressed else RecentrElevation.knob, CircleShape)
                .clip(CircleShape)
                .background(Color.White)
                .border(if (sliding) 2.dp else 1.dp, if (sliding) colors.brandSky else colors.borderSubtle, CircleShape),
        )
    }
}

/** The volume under [x] on a slider [width] wide whose thumb stops [inset] from either end. */
private fun volumeAt(x: Float, width: Int, inset: Float): Float = ((x - inset) / (width - 2 * inset)).coerceIn(0f, 1f)

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
