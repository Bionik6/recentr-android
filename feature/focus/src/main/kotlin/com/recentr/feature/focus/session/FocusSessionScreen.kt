package com.recentr.feature.focus.session

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideIn
import androidx.compose.animation.slideOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
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
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.recentr.core.designsystem.component.OverlappingInstalledAppIcons
import com.recentr.core.designsystem.component.RecentrCheckbox
import com.recentr.core.designsystem.component.RecentrIconButton
import com.recentr.core.designsystem.component.RecentrIconButtonStyle
import com.recentr.core.designsystem.format.clockCountdown
import com.recentr.core.designsystem.haptics.LocalRecentrHaptics
import com.recentr.core.designsystem.icon.RecentrIcon
import com.recentr.core.designsystem.icon.RecentrIconImage
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
import com.recentr.core.model.FocusSoundLayer
import com.recentr.feature.focus.R
import com.recentr.feature.focus.ui.FocusMenu
import com.recentr.feature.focus.ui.FocusTopBar
import com.recentr.feature.focus.ui.drawFocusRing
import com.recentr.feature.focus.ui.formatClock
import com.recentr.feature.focus.ui.art
import com.recentr.feature.focus.ui.mixTitle
import com.recentr.feature.focus.ui.stopwatch

/** Running background: #1B3A8C → ink, with the blurred sky glow behind the ring. */
private val RunningBackground = Brush.verticalGradient(listOf(Color(0xFF1B3A8C), Color(0xFF0E1220)))

/** Paused (Figma 92B): the sky greys over. */
private val PausedBackground = Brush.verticalGradient(listOf(Color(0xFF262B42), Color(0xFF0E1220)))

/** Callbacks of the session screens. */
internal class FocusSessionActions(
    val onClose: () -> Unit,
    val onMinimize: () -> Unit,
    val onPause: () -> Unit,
    val onResume: () -> Unit,
    /** Opens the "You’re N min into focus" dialog: End never ends at once (Figma 92D). */
    val onEnd: () -> Unit,
    val onRestart: () -> Unit,
    val onUndoRestart: () -> Unit,
    val onSkipBreak: () -> Unit,
    val onBlockedApps: () -> Unit,
    val onChangeSound: () -> Unit,
    val onOpenLog: () -> Unit,
)

/**
 * 92 · Focus — Running: dark sky, white ring, countdown, sound pill, apps on hold, and the Timer
 * Controls (Figma 92A–F). Paused, the sky greys over and Resume wears a halo (92B); a restarted
 * round's toast stands where the apps were (92C); a round waiting to start offers "Start round"
 * (92E); Strict mode locks the middle control (92F).
 */
@Composable
internal fun FocusRunningContent(
    run: FocusRun,
    now: Long,
    reduceMotion: Boolean,
    actions: FocusSessionActions,
    modifier: Modifier = Modifier,
    restartedRound: Int? = null,
) {
    SystemBarsAppearance(lightIcons = true)
    val colors = RecentrTheme.colors
    val remaining = run.remainingMillis(now)
    val waiting = run.awaitingStart
    val paused = run.isPaused && !waiting
    var menu by rememberSaveable { mutableStateOf(false) }
    Box(modifier.fillMaxSize().background(if (paused) PausedBackground else RunningBackground)) {
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding(), horizontalAlignment = Alignment.CenterHorizontally) {
            Column(
                Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Spacer(Modifier.height(12.dp))
                FocusTopBar(
                    title = run.config.label,
                    subtitle = stringResource(
                        when {
                            waiting -> R.string.focus_ready_round_of
                            paused -> R.string.focus_paused_round_of
                            else -> R.string.focus_round_of
                        },
                        run.round,
                        run.config.rounds,
                    ),
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
                    contentAlignment = Alignment.Center,
                ) {
                    Canvas(Modifier.size(280.dp)) {
                        drawFocusRing(
                            fraction = 1f - remaining.toFloat() / run.phaseLengthMillis,
                            strokeWidth = 14.dp,
                            track = SolidColor(colors.alphaWhite24),
                            progress = SolidColor(Color.White),
                            knobRadius = 10.dp,
                            showsProgress = !waiting,
                            progressAlpha = if (paused) 0.4f else 1f,
                        )
                    }
                    // Right in the middle of the ring (Figma has it 18dp higher).
                    Mascot(
                        mood = if (run.isPaused) MascotMood.Dozing else MascotMood.Calm,
                        width = 99.2.dp,
                    )
                }
                // Figma 92: the time's digits 40dp under the ring, the caption 23dp under them.
                Spacer(Modifier.height(23.dp))
                if (waiting) {
                    Text(
                        stringResource(R.string.focus_round_n, run.round),
                        style = RecentrTheme.typography.displayHero,
                        color = colors.textOnDark,
                        maxLines = 1,
                    )
                } else {
                    // Figma 92B: paused, the digits at 56%.
                    ClockText(
                        remaining,
                        colors.textOnDark.copy(alpha = if (paused) 0.56f else 1f),
                        Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                    )
                }
                Text(
                    when {
                        waiting -> stringResource(R.string.focus_take_your_time)
                        paused -> stringResource(R.string.focus_paused_for, stopwatch(now - (run.pausedAt ?: now)))
                        else -> stringResource(R.string.focus_left_ends, formatClock(now + remaining))
                    },
                    style = RecentrTheme.typography.bodyMedium,
                    color = colors.textOnDarkMuted,
                )
                Spacer(Modifier.height(33.dp))
                SoundPill(
                    mix = run.config.sounds,
                    playing = !run.isPaused && !reduceMotion,
                    silent = run.isPaused,
                    onClick = actions.onChangeSound,
                )
                // Figma 92F: nothing to tap below, so the note sits under the sound.
                if (run.isStrict) {
                    Spacer(Modifier.height(16.dp))
                    StrictNote(color = colors.textOnDarkMuted)
                }
                Spacer(Modifier.height(24.dp))
            }
            // Figma 92C: a restarted round's toast stands where the apps on hold were, for 5 s.
            Box(Modifier.height(22.dp), contentAlignment = Alignment.Center) {
                if (restartedRound == null && run.config.blockedApps.isNotEmpty()) {
                    Row(
                        Modifier
                            .clip(RecentrShapes.full)
                            .clickable(role = Role.Button, onClick = actions.onBlockedApps)
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        OverlappingInstalledAppIcons(run.config.blockedApps.take(3).map { it.packageName }, size = 22.dp, overlap = 6.dp)
                        val count = run.config.blockedApps.size
                        Text(
                            pluralStringResource(if (paused) R.plurals.focus_apps_still_on_hold else R.plurals.focus_apps_on_hold, count, count),
                            style = RecentrTheme.typography.captionMedium,
                            color = colors.textOnDarkMuted,
                        )
                    }
                }
                // The Box's own, not the Column's around it.
                androidx.compose.animation.AnimatedVisibility(
                    visible = restartedRound != null,
                    enter = fadeIn() + scaleIn(initialScale = 0.96f),
                    exit = fadeOut(),
                    modifier = Modifier.wrapContentSize(unbounded = true),
                ) {
                    RestartToast(round = restartedRound ?: run.round, onUndo = actions.onUndoRestart)
                }
            }
            // Figma 92: the apps 24dp over the controls, the controls 24dp over the safe area.
            Spacer(Modifier.height(24.dp))
            TimerControls(run, actions)
            Spacer(Modifier.height(24.dp))
        }
    }
}

/**
 * Figma 92A–F: the one action that matters now in the middle (Pause, Resume, Start round), Restart
 * and End either side. Strict mode locks the middle and leaves no sides. A round waiting to start
 * shows Start round alone; once tapped, Restart and End slide out from behind it.
 */
@Composable
private fun TimerControls(run: FocusRun, actions: FocusSessionActions) {
    val waiting = run.awaitingStart
    val showsSides = !run.isStrict && !waiting
    Row(horizontalArrangement = Arrangement.spacedBy(36.dp), verticalAlignment = Alignment.CenterVertically) {
        SideControl(visible = showsSides, fromEnd = true) {
            TimerControl(TimerControlKind.Secondary, RecentrIcon.Restart, stringResource(R.string.focus_restart_round), actions.onRestart)
        }
        when {
            waiting -> TimerControl(TimerControlKind.Primary, RecentrIcon.Play, stringResource(R.string.focus_start_round), actions.onResume)
            run.isStrict -> TimerControl(TimerControlKind.Locked, RecentrIcon.LockFill, stringResource(R.string.focus_strict_note))
            run.isPaused -> TimerControl(TimerControlKind.Primary, RecentrIcon.Play, stringResource(R.string.focus_resume), actions.onResume, halo = true)
            else -> TimerControl(TimerControlKind.Primary, RecentrIcon.Pause, stringResource(R.string.focus_pause), actions.onPause)
        }
        SideControl(visible = showsSides, fromEnd = false) {
            TimerControl(TimerControlKind.Secondary, RecentrIcon.Stop, stringResource(R.string.focus_end_session), actions.onEnd)
        }
    }
}

/**
 * A side control's 56dp slot: the control comes out from behind the middle one, and goes back
 * behind it. [fromEnd]: it sits left of the middle, so it comes from its right.
 */
@Composable
private fun SideControl(visible: Boolean, fromEnd: Boolean, content: @Composable () -> Unit) {
    val spec = spring<IntOffset>(dampingRatio = 0.7f, stiffness = Spring.StiffnessMediumLow)
    val distance = with(LocalDensity.current) { SideControlDistance.roundToPx() }
    val towardMiddle = if (fromEnd) distance else -distance
    Box(Modifier.size(56.dp), contentAlignment = Alignment.Center) {
        // The Box's own, not the Row's around it.
        androidx.compose.animation.AnimatedVisibility(
            visible = visible,
            enter = slideIn(spec) { IntOffset(towardMiddle, 0) } + scaleIn(initialScale = 0.6f) + fadeIn(),
            exit = slideOut(spec) { IntOffset(towardMiddle, 0) } + scaleOut(targetScale = 0.6f) + fadeOut(),
        ) {
            content()
        }
    }
}

/** From the middle control's centre to a side control's: half of each, and the gap between. */
private val SideControlDistance = 40.dp + 36.dp + 28.dp

private enum class TimerControlKind { Primary, Secondary, Locked }

/**
 * Timer Control (Figma Components): Primary is the one action that matters now, an 80dp white disc
 * with a sky glow; Secondary the 56dp glass utilities either side; Locked a strict session's
 * middle, nothing to tap. [halo]: Figma 92B's two faint rings round Resume.
 */
@Composable
private fun TimerControl(
    kind: TimerControlKind,
    icon: RecentrIcon,
    label: String,
    onClick: () -> Unit = {},
    halo: Boolean = false,
) {
    val colors = RecentrTheme.colors
    val haptics = LocalRecentrHaptics.current
    val primary = kind == TimerControlKind.Primary
    Box(
        Modifier
            .size(if (kind == TimerControlKind.Secondary) 56.dp else 80.dp)
            .then(
                if (halo) {
                    Modifier.drawBehind {
                        val stroke = Stroke(1.5.dp.toPx())
                        drawCircle(Color.White.copy(alpha = 0.18f), radius = 50.dp.toPx() - stroke.width / 2, style = stroke)
                        drawCircle(Color.White.copy(alpha = 0.08f), radius = 60.dp.toPx() - stroke.width / 2, style = stroke)
                    }
                } else {
                    Modifier
                },
            )
            .then(if (primary) Modifier.recentrShadow(RecentrElevation.glowControl, CircleShape) else Modifier)
            .clip(CircleShape)
            .background(if (primary) Color.White else colors.alphaWhite24)
            .then(if (primary) Modifier else Modifier.border(1.dp, colors.alphaWhite24, CircleShape))
            .then(
                if (kind == TimerControlKind.Locked) {
                    Modifier
                } else {
                    Modifier.clickable(role = Role.Button) {
                        haptics.selection()
                        onClick()
                    }
                },
            )
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center,
    ) {
        RecentrIconImage(
            icon,
            tint = when (kind) {
                // The disc stays white on the dark round: ink on it.
                TimerControlKind.Primary -> Color(0xFF0E1220)
                TimerControlKind.Secondary -> colors.textOnDark
                TimerControlKind.Locked -> colors.textOnDarkMuted
            },
            size = when (kind) {
                TimerControlKind.Primary -> 32.dp
                TimerControlKind.Secondary -> 24.dp
                TimerControlKind.Locked -> 28.dp
            },
        )
    }
}

/** Figma 92C: "Round 2 restarted · Undo". */
@Composable
private fun RestartToast(round: Int, onUndo: () -> Unit) {
    val colors = RecentrTheme.colors
    Row(
        Modifier
            .clip(RecentrShapes.full)
            .background(Color(0xB80E1220))
            .border(1.dp, colors.textOnDark, RecentrShapes.full)
            .padding(start = 14.dp, end = 16.dp, top = 10.dp, bottom = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RecentrIconImage(RecentrIcon.Restart, tint = colors.textOnDark, size = 16.dp)
        Text(stringResource(R.string.focus_round_restarted, round), style = RecentrTheme.typography.captionMedium, color = colors.textOnDark)
        Text("·", style = RecentrTheme.typography.captionMedium, color = colors.textOnDark)
        Text(
            stringResource(R.string.focus_undo),
            style = RecentrTheme.typography.captionMedium,
            color = colors.brandSkyLight,
            modifier = Modifier.clickable(role = Role.Button, onClick = onUndo),
        )
    }
}

/** Where Pause and End would be on a strict session. */
@Composable
internal fun StrictNote(color: Color, modifier: Modifier = Modifier) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
        RecentrIconImage(RecentrIcon.Shield, tint = RecentrTheme.colors.accentTangerine, size = 16.dp)
        Text(stringResource(R.string.focus_strict_note), style = RecentrTheme.typography.captionMedium, color = color, textAlign = TextAlign.Center)
    }
}

/**
 * The time left of a round or a break, to the second ("24:38", "03:24:57"), on tabular digits so it
 * holds still as it ticks; it shrinks to fit [modifier]'s width.
 */
@Composable
private fun ClockText(millis: Long, color: Color, modifier: Modifier = Modifier) {
    val style = RecentrTheme.typography.displayHero
    BasicText(
        text = clockCountdown(millis),
        style = style.copy(color = color, textAlign = TextAlign.Center, fontFeatureSettings = "tnum"),
        maxLines = 1,
        autoSize = TextAutoSize.StepBased(minFontSize = 36.sp, maxFontSize = style.fontSize, stepSize = 2.sp),
        modifier = modifier,
    )
}

/**
 * "Sound" pill: the sounds' illustrations (three at most), the mix's name and the four equaliser
 * bars (animated while playing). With no sound on, it offers one.
 */
@Composable
private fun SoundPill(mix: List<FocusSoundLayer>, playing: Boolean, onClick: () -> Unit, silent: Boolean = false) {
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
        if (mix.isEmpty()) {
            StickerImage(Sticker.Headphones, size = 28.dp)
            Text(stringResource(R.string.sound_add), style = RecentrTheme.typography.captionMedium, color = colors.textOnDark, maxLines = 1)
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy((-10).dp)) {
                mix.take(3).forEach { layer ->
                    Image(painterResource(layer.sound.art), contentDescription = null, modifier = Modifier.size(28.dp))
                }
            }
            Text(mixTitle(mix), style = RecentrTheme.typography.captionMedium, color = colors.textOnDark, maxLines = 1)
            Equalizer(playing, silent)
        }
    }
}

/** Four bars, swinging while [playing]; flat at 4dp while [silent] (Figma 92B, 92E). */
@Composable
private fun Equalizer(playing: Boolean, silent: Boolean = false) {
    val rest = if (silent) listOf(4f, 4f, 4f, 4f) else listOf(8f, 14f, 10f, 16f)
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
                        ClockText(remaining, colors.textPrimary, Modifier.widthIn(max = 190.dp))
                        Text(stringResource(R.string.break_until, run.round + 1), style = RecentrTheme.typography.bodyMedium, color = colors.textSecondary)
                    }
                    Mascot(MascotMood.Dozing, width = 57.6.dp, modifier = Modifier.offset(x = 230.dp, y = 178.dp))
                }
                Spacer(Modifier.height(40.dp))
                BreakIdeas(done = done, onToggle = { index -> done = if (index in done) done - index else done + index })
                Spacer(Modifier.height(24.dp))
            }
            // Figma 94A: no control bar on a break, only a quiet pill 24dp over the safe area.
            Row(
                Modifier
                    .clip(RecentrShapes.full)
                    .background(colors.alphaInk06)
                    .clickable(role = Role.Button, onClick = actions.onSkipBreak)
                    .padding(start = 18.dp, end = 14.dp, top = 10.dp, bottom = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(stringResource(R.string.break_skip), style = RecentrTheme.typography.bodyMedium, color = colors.textSecondary)
                RecentrIconImage(RecentrIcon.SkipForward, tint = colors.textSecondary, size = 18.dp)
            }
            Spacer(Modifier.height(24.dp))
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
    val haptics = LocalRecentrHaptics.current
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
                Modifier.fillMaxWidth().clickable(role = Role.Checkbox) {
                    haptics.selection()
                    onToggle(index)
                },
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
