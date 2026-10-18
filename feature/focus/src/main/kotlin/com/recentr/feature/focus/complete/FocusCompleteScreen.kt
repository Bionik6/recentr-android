package com.recentr.feature.focus.complete

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import com.recentr.core.designsystem.component.MiniTile
import com.recentr.core.designsystem.component.RecentrButton
import com.recentr.core.designsystem.component.RecentrButtonStyle
import com.recentr.core.designsystem.component.RecentrIconButton
import com.recentr.core.designsystem.component.StickyNoteField
import com.recentr.core.designsystem.haptics.LocalRecentrHaptics
import com.recentr.core.designsystem.icon.RecentrIcon
import com.recentr.core.designsystem.illustration.Backdrop
import com.recentr.core.designsystem.illustration.BackdropBox
import com.recentr.core.designsystem.illustration.Sticker
import com.recentr.core.designsystem.illustration.StickerImage
import com.recentr.core.designsystem.motion.entrance
import com.recentr.core.designsystem.motion.floating
import com.recentr.core.designsystem.theme.RecentrTheme
import com.recentr.core.designsystem.util.SystemBarsAppearance
import com.recentr.feature.focus.FocusCompleteUiState
import com.recentr.feature.focus.R

/**
 * 95 · Focus — Session complete: celebration art, summary, streak tiles and the "finished" note.
 * Opens with a success tap; the art, title, tiles and note rise in one after the other.
 */
@Composable
internal fun FocusCompleteScreen(
    state: FocusCompleteUiState,
    onNoteChange: (String) -> Unit,
    onStartAnother: () -> Unit,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
) {
    SystemBarsAppearance(lightIcons = false)
    val colors = RecentrTheme.colors
    val record = state.record
    var note by rememberSaveable(record?.id) { mutableStateOf(record?.note.orEmpty()) }
    val haptics = LocalRecentrHaptics.current
    LaunchedEffect(Unit) { haptics.success() }
    BackdropBox(Backdrop.Aurora, modifier) {
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
            Column(
                Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Spacer(Modifier.height(12.dp))
                Row(Modifier.fillMaxWidth()) {
                    RecentrIconButton(icon = RecentrIcon.X, contentDescription = stringResource(R.string.focus_close), onClick = onDone)
                }
                Spacer(Modifier.height(6.dp))
                CelebrationArt()
                Spacer(Modifier.height(14.dp))
                if (record != null) {
                    Column(Modifier.entrance(100), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            pluralStringResource(R.plurals.complete_title, record.focusedMinutes, record.focusedMinutes, record.title.lowercase()),
                            style = RecentrTheme.typography.title1,
                            color = colors.textPrimary,
                            textAlign = TextAlign.Center,
                        )
                        Spacer(Modifier.height(6.dp))
                        val rounds = pluralStringResource(R.plurals.complete_rounds, record.rounds, record.rounds)
                        val unlocks = if (record.unlocks == 0) {
                            stringResource(R.string.complete_zero_unlocks)
                        } else {
                            pluralStringResource(R.plurals.complete_unlocks, record.unlocks, record.unlocks)
                        }
                        Text(
                            stringResource(if (state.longestThisWeek) R.string.complete_longest else R.string.complete_summary, rounds, unlocks),
                            style = RecentrTheme.typography.bodyRegular,
                            color = colors.textSecondary,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
                Spacer(Modifier.height(13.dp))
                Row(
                    Modifier.fillMaxWidth().entrance(180, blur = false),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    MiniTile(
                        label = stringResource(R.string.complete_focus_streak),
                        value = pluralStringResource(R.plurals.complete_days, state.streakDays, state.streakDays),
                        sticker = Sticker.Flame,
                        modifier = Modifier.weight(1f),
                    )
                    MiniTile(
                        label = stringResource(R.string.complete_urges),
                        value = stringResource(R.string.complete_urges_today, record?.urgesResisted ?: 0),
                        sticker = Sticker.Lock,
                        modifier = Modifier.weight(1f),
                    )
                }
                Spacer(Modifier.height(23.dp))
                StickyNoteField(
                    title = stringResource(R.string.complete_note_title),
                    value = note,
                    onValueChange = {
                        note = it
                        onNoteChange(it)
                    },
                    placeholder = stringResource(R.string.complete_note_placeholder),
                    modifier = Modifier
                        .fillMaxWidth()
                        .entrance(260, blur = false)
                        .graphicsLayer { rotationZ = 1.5f },
                )
                Spacer(Modifier.height(24.dp))
            }
            Column(Modifier.padding(start = 24.dp, end = 24.dp, bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                RecentrButton(
                    text = stringResource(R.string.complete_another),
                    onClick = onStartAnother,
                    style = RecentrButtonStyle.Brand,
                    modifier = Modifier.fillMaxWidth(),
                )
                RecentrButton(
                    text = stringResource(R.string.complete_done),
                    onClick = onDone,
                    style = RecentrButtonStyle.Secondary,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

/** Figma "Art" (220×150): 130dp headphones rising in, and two tilted stars bobbing out of step. */
@Composable
private fun CelebrationArt() {
    Box(Modifier.size(width = 220.dp, height = 150.dp)) {
        StickerImage(
            Sticker.Headphones,
            size = 130.dp,
            modifier = Modifier.offset(x = 45.dp, y = 10.dp).entrance(from = DpOffset(0.dp, 30.dp)),
        )
        // Bounding-box centres (29.1, 17.5) and (197.1, 40.2) of the rotated 48 / 34dp stars.
        StickerImage(
            Sticker.Star,
            size = 48.dp,
            modifier = Modifier
                .offset(x = 5.1.dp, y = (-6.5).dp)
                .floating(amplitude = 4.dp, periodMillis = 3200)
                .graphicsLayer { rotationZ = -14f },
        )
        StickerImage(
            Sticker.Star,
            size = 34.dp,
            modifier = Modifier
                .offset(x = 180.1.dp, y = 23.2.dp)
                .floating(amplitude = 3.dp, periodMillis = 2600, phase = 0.4f)
                .graphicsLayer { rotationZ = 12f },
        )
    }
}
