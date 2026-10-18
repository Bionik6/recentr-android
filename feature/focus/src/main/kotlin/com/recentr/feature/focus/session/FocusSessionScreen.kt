package com.recentr.feature.focus.session

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.recentr.core.designsystem.component.OverlappingAppIcons
import com.recentr.core.designsystem.component.RecentrButton
import com.recentr.core.designsystem.component.RecentrButtonStyle
import com.recentr.core.designsystem.component.RecentrCheckbox
import com.recentr.core.designsystem.component.RecentrIconButton
import com.recentr.core.designsystem.component.RecentrIconButtonStyle
import com.recentr.core.designsystem.icon.RecentrIcon
import com.recentr.core.designsystem.illustration.Mascot
import com.recentr.core.designsystem.illustration.MascotMood
import com.recentr.core.designsystem.illustration.Sticker
import com.recentr.core.designsystem.illustration.StickerImage
import com.recentr.core.designsystem.theme.RecentrElevation
import com.recentr.core.designsystem.theme.RecentrShapes
import com.recentr.core.designsystem.theme.RecentrTheme
import com.recentr.core.designsystem.theme.recentrShadow
import com.recentr.core.designsystem.util.SystemBarsAppearance
import com.recentr.core.model.FocusRun
import com.recentr.core.model.FocusSound
import com.recentr.feature.focus.R
import com.recentr.feature.focus.ui.FocusMenu
import com.recentr.feature.focus.ui.FocusTopBar
import com.recentr.feature.focus.ui.demoApp
import com.recentr.feature.focus.ui.drawFocusRing
import com.recentr.feature.focus.ui.formatClock
import com.recentr.feature.focus.ui.formatCountdown
import com.recentr.feature.focus.ui.label
import com.recentr.feature.focus.ui.sticker

/** Running background: #1B3A8C → ink, with the blurred sky glow behind the ring. */
private val RunningBackground = Brush.verticalGradient(listOf(Color(0xFF1B3A8C), Color(0xFF0E1220)))

/** Callbacks of the session screens. */
internal class FocusSessionActions(
    val onClose: () -> Unit,
    val onMinimize: () -> Unit,
    val onPause: () -> Unit,
    val onResume: () -> Unit,
    val onEnd: () -> Unit,
    val onSkipBreak: () -> Unit,
    val onBlockedApps: () -> Unit,
    val onChangeSound: () -> Unit,
    val onOpenLog: () -> Unit,
)

/** 92 · Focus — Running: dark sky, white ring, countdown, sound pill, apps on hold, controls. */
@Composable
internal fun FocusRunningContent(
    run: FocusRun,
    now: Long,
    reduceMotion: Boolean,
    actions: FocusSessionActions,
    modifier: Modifier = Modifier,
) {
    SystemBarsAppearance(lightIcons = true)
    val colors = RecentrTheme.colors
    val remaining = run.remainingMillis(now)
    val waitingToStart = run.isPaused && remaining == run.phaseLengthMillis && run.round > 1
    var menu by rememberSaveable { mutableStateOf(false) }
    Box(modifier.fillMaxSize().background(RunningBackground)) {
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding(), horizontalAlignment = Alignment.CenterHorizontally) {
            Column(
                Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Spacer(Modifier.height(12.dp))
                FocusTopBar(
                    title = run.config.label,
                    subtitle = stringResource(R.string.focus_round_of, run.round, run.config.rounds),
                    titleColor = colors.textOnDark,
                    subtitleColor = colors.textOnDarkMuted,
                    modifier = Modifier.padding(horizontal = 24.dp),
                    leading = {
                        RecentrIconButton(RecentrIcon.X, stringResource(R.string.focus_close), actions.onClose, style = RecentrIconButtonStyle.OnSky)
                    },
                    trailing = {
                        Box {
                            RecentrIconButton(RecentrIcon.More, stringResource(R.string.focus_more), { menu = true }, style = RecentrIconButtonStyle.OnSky)
                            FocusMenu(
                                expanded = menu,
                                onDismissRequest = { menu = false },
                                items = listOf(
                                    stringResource(R.string.focus_change_sound) to actions.onChangeSound,
                                    stringResource(R.string.focus_log) to actions.onOpenLog,
                                ),
                            )
                        }
                    },
                )
                Spacer(Modifier.height(42.dp))
                Box(
                    Modifier
                        .size(280.dp)
                        .drawBehind {
                            // Figma "Glow": 420dp sky disc (45%) blurred by 60, centred 70dp under the ring centre.
                            val center = Offset(size.width / 2, size.height / 2 + 70.dp.toPx())
                            val radius = 330.dp.toPx()
                            val glow = Color(0xFF2F7BF6)
                            drawCircle(
                                Brush.radialGradient(
                                    0f to glow.copy(alpha = 0.45f),
                                    0.455f to glow.copy(alpha = 0.38f),
                                    0.636f to glow.copy(alpha = 0.225f),
                                    0.818f to glow.copy(alpha = 0.07f),
                                    1f to glow.copy(alpha = 0f),
                                    center = center,
                                    radius = radius,
                                ),
                                radius = radius,
                                center = center,
                            )
                        },
                    contentAlignment = Alignment.TopCenter,
                ) {
                    Canvas(Modifier.size(280.dp)) {
                        drawFocusRing(
                            fraction = 1f - remaining.toFloat() / run.phaseLengthMillis,
                            strokeWidth = 14.dp,
                            track = SolidColor(colors.alphaWhite24),
                            progress = SolidColor(Color.White),
                            knobRadius = 10.dp,
                        )
                    }
                    Mascot(
                        mood = if (run.isPaused) MascotMood.Dozing else MascotMood.Calm,
                        width = 99.2.dp,
                        modifier = Modifier.padding(top = 60.dp),
                    )
                }
                Spacer(Modifier.height(34.dp))
                Text(formatCountdown(remaining), style = RecentrTheme.typography.displayHero, color = colors.textOnDark, maxLines = 1)
                Text(
                    if (run.isPaused) stringResource(R.string.focus_paused) else stringResource(R.string.focus_left_ends, formatClock(now + remaining)),
                    style = RecentrTheme.typography.bodyMedium,
                    color = colors.textOnDarkMuted,
                )
                Spacer(Modifier.height(33.dp))
                SoundPill(
                    sound = run.config.sound,
                    playing = !run.isPaused && !reduceMotion && run.config.sound != FocusSound.Silence,
                    onClick = actions.onChangeSound,
                )
                Spacer(Modifier.height(24.dp))
            }
            if (run.config.blockedApps.isNotEmpty()) {
                Row(
                    Modifier
                        .clip(RecentrShapes.full)
                        .clickable(role = Role.Button, onClick = actions.onBlockedApps)
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    OverlappingAppIcons(run.config.blockedApps.take(3).map { it.demoApp }, size = 22.dp, overlap = 6.dp)
                    Text(
                        pluralStringResource(R.plurals.focus_apps_on_hold, run.config.blockedApps.size, run.config.blockedApps.size),
                        style = RecentrTheme.typography.captionMedium,
                        color = colors.textOnDarkMuted,
                    )
                }
            }
            Spacer(Modifier.height(30.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                RecentrButton(
                    text = stringResource(
                        when {
                            waitingToStart -> R.string.focus_start_round
                            run.isPaused -> R.string.focus_resume
                            else -> R.string.focus_pause
                        },
                    ),
                    onClick = if (run.isPaused) actions.onResume else actions.onPause,
                    style = RecentrButtonStyle.Secondary,
                    modifier = Modifier.width(170.dp),
                )
                RecentrButton(
                    text = stringResource(R.string.focus_end),
                    onClick = actions.onEnd,
                    style = RecentrButtonStyle.Soft,
                    modifier = Modifier.width(110.dp),
                )
            }
            Spacer(Modifier.height(30.dp))
        }
    }
}

/** "Sound" pill: sticker, sound name and the four equaliser bars (animated while playing). */
@Composable
private fun SoundPill(sound: FocusSound, playing: Boolean, onClick: () -> Unit) {
    val colors = RecentrTheme.colors
    Row(
        Modifier
            .clip(RecentrShapes.full)
            .background(colors.alphaWhite24)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(start = 10.dp, end = 16.dp, top = 10.dp, bottom = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        StickerImage(sound.sticker, size = 28.dp)
        Text(stringResource(sound.label), style = RecentrTheme.typography.captionMedium, color = colors.textOnDark, maxLines = 1)
        Equalizer(playing)
    }
}

@Composable
private fun Equalizer(playing: Boolean) {
    val rest = listOf(8f, 14f, 10f, 16f)
    val phase = if (playing) {
        val transition = rememberInfiniteTransition(label = "eq")
        transition.animateFloat(0f, 1f, infiniteRepeatable(tween(900), RepeatMode.Reverse), label = "eqPhase").value
    } else {
        null
    }
    val white = RecentrTheme.colors.textOnDark
    Canvas(Modifier.size(width = 21.dp, height = 16.dp)) {
        rest.forEachIndexed { index, height ->
            val h = if (phase == null) {
                height
            } else {
                // Each bar swings between ~5 and 16dp, out of phase with its neighbours.
                val p = (phase + index * 0.37f) % 1f
                5f + 11f * kotlin.math.abs(kotlin.math.sin(p * Math.PI.toFloat() * 2f + index))
            }
            val barHeight = h.dp.toPx()
            drawRoundRect(
                color = white,
                topLeft = Offset(index * 6.dp.toPx(), size.height - barHeight),
                size = Size(3.dp.toPx(), barHeight),
                cornerRadius = CornerRadius(2.dp.toPx()),
            )
        }
    }
}

/** 94 · Focus — Break: mint page, green ring with the break countdown, ideas, "Skip break". */
@Composable
internal fun FocusBreakContent(
    run: FocusRun,
    now: Long,
    actions: FocusSessionActions,
    modifier: Modifier = Modifier,
) {
    SystemBarsAppearance(lightIcons = false)
    val colors = RecentrTheme.colors
    val remaining = run.remainingMillis(now)
    val toGo = run.config.rounds - run.round
    // Ticks are per break: a new break starts with fresh checkboxes.
    var done by rememberSaveable(run.round) { mutableStateOf(setOf<Int>()) }
    Box(modifier.fillMaxSize().background(colors.auroraMint)) {
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding(), horizontalAlignment = Alignment.CenterHorizontally) {
            Column(
                Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Spacer(Modifier.height(12.dp))
                FocusTopBar(
                    title = stringResource(R.string.break_title),
                    subtitle = stringResource(R.string.break_round_done, run.round, toGo),
                    titleColor = colors.textPrimary,
                    subtitleColor = colors.textSecondary,
                    modifier = Modifier.padding(horizontal = 24.dp),
                    leading = { RecentrIconButton(RecentrIcon.X, stringResource(R.string.focus_close), actions.onMinimize) },
                )
                Spacer(Modifier.height(32.dp))
                Box(Modifier.size(230.dp)) {
                    Canvas(Modifier.size(230.dp)) {
                        drawFocusRing(
                            fraction = remaining.toFloat() / run.phaseLengthMillis,
                            strokeWidth = 14.dp,
                            track = SolidColor(colors.alphaWhite80),
                            progress = SolidColor(colors.feedbackSuccess),
                            knobRadius = 10.dp,
                        )
                    }
                    Column(Modifier.align(Alignment.TopCenter).padding(top = 70.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(formatCountdown(remaining), style = RecentrTheme.typography.displayHero, color = colors.textPrimary, maxLines = 1)
                        Text(stringResource(R.string.break_until, run.round + 1), style = RecentrTheme.typography.bodyMedium, color = colors.textSecondary)
                    }
                    Mascot(MascotMood.Dozing, width = 57.6.dp, modifier = Modifier.offset(x = 230.dp, y = 178.dp))
                }
                Spacer(Modifier.height(40.dp))
                BreakIdeas(done = done, onToggle = { index -> done = if (index in done) done - index else done + index })
                Spacer(Modifier.height(24.dp))
            }
            RecentrButton(
                text = stringResource(R.string.break_skip),
                onClick = actions.onSkipBreak,
                style = RecentrButtonStyle.Secondary,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
            )
            Spacer(Modifier.height(26.dp))
        }
    }
}

private val BreakIdeaList = listOf(
    Triple(Sticker.Leaf, R.string.break_idea_window, R.string.break_idea_window_hint),
    Triple(Sticker.Target, R.string.break_idea_water, R.string.break_idea_water_hint),
    Triple(Sticker.Heart, R.string.break_idea_stretch, R.string.break_idea_stretch_hint),
)

/** "Try this instead of scrolling" card with three checkable ideas. */
@Composable
private fun BreakIdeas(done: Set<Int>, onToggle: (Int) -> Unit) {
    val colors = RecentrTheme.colors
    val shape = RoundedCornerShape(24.dp)
    Column(
        Modifier
            .padding(horizontal = 24.dp)
            .fillMaxWidth()
            .recentrShadow(RecentrElevation.card, shape)
            .clip(shape)
            .background(colors.bgSurface)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text(stringResource(R.string.break_ideas_title), style = RecentrTheme.typography.headline, color = colors.textPrimary)
        BreakIdeaList.forEachIndexed { index, (sticker, title, hint) ->
            Row(
                Modifier.fillMaxWidth().clickable(role = Role.Checkbox) { onToggle(index) },
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                StickerImage(sticker, size = 40.dp)
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(stringResource(title), style = RecentrTheme.typography.bodyMedium, color = colors.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(stringResource(hint), style = RecentrTheme.typography.captionRegular, color = colors.textSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                RecentrCheckbox(checked = index in done, onCheckedChange = null)
            }
        }
    }
}
