package com.recentr.app.blocking

import android.Manifest
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.recentr.app.R
import com.recentr.core.data.StepDetector
import com.recentr.core.data.Walk
import com.recentr.core.designsystem.component.OnboardingProgress
import com.recentr.core.designsystem.component.RecentrButton
import com.recentr.core.designsystem.component.RecentrButtonStyle
import com.recentr.core.designsystem.component.RecentrChip
import com.recentr.core.designsystem.component.RecentrChipStyle
import com.recentr.core.designsystem.haptics.SensoryFeedback
import com.recentr.core.designsystem.icon.RecentrIcon
import com.recentr.core.designsystem.icon.RecentrIconImage
import com.recentr.core.designsystem.illustration.InstalledAppIcon
import com.recentr.core.designsystem.illustration.Mascot
import com.recentr.core.designsystem.illustration.MascotMood
import com.recentr.core.designsystem.illustration.Sticker
import com.recentr.core.designsystem.illustration.StickerImage
import com.recentr.core.designsystem.motion.entrance
import com.recentr.core.designsystem.motion.floating
import com.recentr.core.designsystem.theme.RecentrElevation
import com.recentr.core.designsystem.theme.RecentrShapes
import com.recentr.core.designsystem.theme.RecentrTheme
import com.recentr.core.designsystem.theme.recentrShadow
import com.recentr.core.model.Rule
import kotlin.math.sqrt
import kotlinx.coroutines.delay

// Walk to unlock (Figma › Unlock — Walk 20 steps, Walk in progress, Walk complete): the steps come
// from the accelerometer while the screen is on, and from the phone's step counter, so the phone
// can stay in a pocket.

/** Why the steps can't be counted. */
private enum class StepProblem {
    /** Physical activity isn't allowed for Recentr. */
    NoPermission,

    /** The phone has no step counter. */
    NoSensor,
}

/**
 * Walk 20 steps to unlock, then "A few more steps" while they're counted. [onUnlock] runs once
 * they're all walked; Cancel and Cancel walk leave the app closed ([onCancel]).
 */
@Composable
internal fun WalkStep(rule: Rule, packageName: String, label: String, onCancel: () -> Unit, onUnlock: () -> Unit) {
    val context = LocalContext.current
    val sensors = remember { context.getSystemService(SensorManager::class.java) }
    val counter = remember { sensors?.getDefaultSensor(Sensor.TYPE_STEP_COUNTER) }
    // The emulator has no step counter: debug builds pretend to walk instead.
    val pretends = counter == null && context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0

    var walking by rememberSaveable { mutableStateOf(false) }
    var walk by rememberSaveable(stateSaver = WalkSaver) { mutableStateOf(Walk()) }
    var problem by rememberSaveable { mutableStateOf(if (counter == null && !pretends) StepProblem.NoSensor else null) }
    var requests by rememberSaveable { mutableIntStateOf(0) }

    // Physical activity is a runtime permission from Android 10.
    fun canCount() = pretends || Build.VERSION.SDK_INT < Build.VERSION_CODES.Q ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACTIVITY_RECOGNITION) == PackageManager.PERMISSION_GRANTED

    fun start() {
        walk = Walk()
        problem = null
        walking = true
    }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) start() else problem = StepProblem.NoPermission
    }
    fun startWalking() {
        when {
            canCount() -> start()
            // Asked twice already: Android won't ask again, only its Settings can allow it.
            requests >= 2 -> context.startActivity(
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, "package:${context.packageName}".toUri())
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
            else -> {
                requests++
                permission.launch(Manifest.permission.ACTIVITY_RECOGNITION)
            }
        }
    }
    // Back from Settings with Physical activity allowed.
    LifecycleResumeEffect(Unit) {
        if (problem == StepProblem.NoPermission && canCount()) problem = null
        onPauseOrDispose {}
    }

    if (walking && counter != null && sensors != null) {
        DisposableEffect(Unit) {
            // The accelerometer shows each step at once; the counter catches up on the others.
            val detector = StepDetector()
            val detectedBefore = walk.detected
            val listener = object : SensorEventListener {
                override fun onSensorChanged(event: SensorEvent) {
                    if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
                        val (x, y, z) = event.values
                        val magnitude = sqrt((x * x + y * y + z * z).toDouble()) / SensorManager.GRAVITY_EARTH
                        detector.add(magnitude, event.timestamp / 1e9)
                        val detected = detectedBefore + detector.steps
                        if (detected != walk.detected) walk = walk.detected(detected)
                    } else {
                        walk = walk.counted(event.values[0])
                    }
                }

                override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) = Unit
            }
            // No batching: each step shows as soon as it's counted.
            sensors.registerListener(listener, counter, SensorManager.SENSOR_DELAY_UI, 0)
            sensors.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)?.let {
                sensors.registerListener(listener, it, SensorManager.SENSOR_DELAY_GAME)
            }
            onDispose { sensors.unregisterListener(listener) }
        }
    } else if (walking && pretends) {
        LaunchedEffect(Unit) {
            while (!walk.isDone) {
                delay(500)
                walk = walk.stepped()
            }
        }
    }
    val currentOnUnlock by rememberUpdatedState(onUnlock)
    LaunchedEffect(walk.isDone) { if (walk.isDone) currentOnUnlock() }
    SensoryFeedback(walking) { if (walking) mediumImpact() }

    val minutes = pluralStringResource(R.plurals.unlock_walk_minutes, rule.unlockDurationMinutes, rule.unlockDurationMinutes)
    UnlockPage(
        actions = {
            if (walking) {
                Box(Modifier.fillMaxWidth().height(54.dp), contentAlignment = Alignment.Center) {
                    Text(
                        pluralStringResource(R.plurals.unlock_walk_steps_left, walk.left, walk.left),
                        style = RecentrTheme.typography.bodyMedium,
                        color = RecentrTheme.colors.textSecondary,
                        textAlign = TextAlign.Center,
                    )
                }
                Spacer(Modifier.height(10.dp))
                RecentrButton(
                    text = stringResource(R.string.unlock_walk_cancel),
                    onClick = onCancel,
                    style = RecentrButtonStyle.Soft,
                    modifier = Modifier.fillMaxWidth(),
                )
            } else {
                if (problem != StepProblem.NoSensor) {
                    RecentrButton(
                        text = stringResource(R.string.unlock_walk_start),
                        onClick = { startWalking() },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(10.dp))
                }
                RecentrButton(
                    text = stringResource(R.string.unlock_cancel),
                    onClick = onCancel,
                    style = RecentrButtonStyle.Soft,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
    ) {
        Spacer(Modifier.height(8.dp))
        AppChip(packageName, stringResource(R.string.unlock_locked_by, label, rule.name))
        Spacer(Modifier.height(28.dp))
        if (walking) {
            WalkHeading(
                stringResource(R.string.unlock_walking_title),
                stringResource(R.string.unlock_walking_subtitle, label, Walk.STEPS),
            )
        } else {
            WalkHeading(
                stringResource(R.string.unlock_walk_title, Walk.STEPS),
                stringResource(R.string.unlock_walk_subtitle, Walk.STEPS, minutes),
            )
        }
        Spacer(Modifier.height(12.dp))
        Mascot(if (walking) MascotMood.Impatient else MascotMood.Calm, width = 112.dp, modifier = Modifier.entrance(100))
        Spacer(Modifier.height(14.dp))
        StepsCard(
            steps = walk.steps,
            status = if (walking) stringResource(R.string.unlock_walk_more_to_go, walk.left) else stringResource(R.string.unlock_walk_ready),
            modifier = Modifier.entrance(200, blur = false),
        )
        BottomSpacer()
        when (problem) {
            StepProblem.NoPermission -> WalkNote(
                RecentrIcon.ShieldCheck,
                stringResource(R.string.unlock_walk_no_permission_title),
                stringResource(R.string.unlock_walk_no_permission_body),
            )
            StepProblem.NoSensor -> WalkNote(
                RecentrIcon.ShieldCheck,
                stringResource(R.string.unlock_walk_no_sensor_title),
                stringResource(R.string.unlock_walk_no_sensor_body),
            )
            null -> WalkNote(
                RecentrIcon.ShieldCheck,
                stringResource(R.string.unlock_walk_safety_title),
                stringResource(R.string.unlock_walk_safety_body),
            )
        }
        Spacer(Modifier.height(6.dp))
    }
}

/** Walk complete: the steps are walked, and the app is open until the timer ends or "Lock it again". */
@Composable
internal fun WalkCompleteStep(packageName: String, label: String, minutes: Int, onOpen: () -> Unit, onLockAgain: () -> Unit) {
    UnlockPage(
        actions = {
            RecentrButton(
                text = stringResource(R.string.unlock_open_app, label),
                onClick = onOpen,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(10.dp))
            RecentrButton(
                text = stringResource(R.string.unlock_lock_again),
                onClick = onLockAgain,
                style = RecentrButtonStyle.Soft,
                modifier = Modifier.fillMaxWidth(),
            )
        },
    ) {
        Spacer(Modifier.height(8.dp))
        AppChip(packageName, stringResource(R.string.unlock_walk_unlock_chip, label, minutes))
        Spacer(Modifier.height(28.dp))
        WalkHeading(
            stringResource(R.string.unlock_walk_done_title, label),
            stringResource(R.string.unlock_walk_done_subtitle, Walk.STEPS, minutes),
        )
        Spacer(Modifier.height(20.dp))
        // The app, a little tilted and a little higher than the mascot beside it.
        Box(Modifier.size(width = 126.dp, height = 108.dp).entrance(from = DpOffset(0.dp, 40.dp), blur = false)) {
            InstalledAppIcon(
                packageName,
                size = 64.dp,
                modifier = Modifier.align(Alignment.Center).offset(y = (-12).dp).rotate(-6f),
            )
            Mascot(
                MascotMood.Calm,
                width = 80.dp,
                modifier = Modifier.align(Alignment.Center).offset(x = 106.dp, y = 26.dp).entrance(200),
            )
        }
        Spacer(Modifier.height(38.dp))
        StepsCard(steps = Walk.STEPS, status = stringResource(R.string.unlock_walk_goal_reached))
        BottomSpacer()
        WalkNote(
            RecentrIcon.Clock,
            pluralStringResource(R.plurals.unlock_walk_done_note_title, minutes, minutes),
            stringResource(R.string.unlock_walk_done_note_body, label),
        )
        Spacer(Modifier.height(6.dp))
    }
}

/**
 * Between the steps and the note: 18dp at least, more when the page has room, so the note sits
 * just above the buttons (22dp, with [UnlockPage]'s gap).
 */
@Composable
private fun ColumnScope.BottomSpacer() {
    Spacer(Modifier.weight(1f))
    Spacer(Modifier.height(18.dp))
}

@Composable
private fun WalkHeading(title: String, subtitle: String) {
    Title(title)
    Spacer(Modifier.height(8.dp))
    Text(
        subtitle,
        style = RecentrTheme.typography.bodyRegular,
        color = RecentrTheme.colors.textSecondary,
        textAlign = TextAlign.Center,
        modifier = Modifier.padding(horizontal = 16.dp),
    )
}

/** The steps walked out of 20, on a white card (Figma › Step progress): under the lock until they're all walked. */
@Composable
private fun StepsCard(steps: Int, status: String, modifier: Modifier = Modifier) {
    val colors = RecentrTheme.colors
    val isDone = steps >= Walk.STEPS
    val shape = RoundedCornerShape(36.dp)
    Box(modifier.widthIn(max = 320.dp).fillMaxWidth()) {
        Column(
            Modifier
                .fillMaxWidth()
                .heightIn(min = 190.dp)
                .recentrShadow(RecentrElevation.card, shape)
                .clip(shape)
                .background(colors.bgSurface)
                .padding(start = 24.dp, top = 16.dp, end = 24.dp, bottom = 18.dp)
                .semantics(mergeDescendants = true) {},
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // Only the steps walked change: "/20" stays put.
            val countColor = if (isDone) colors.feedbackSuccess else colors.textPrimary
            Row {
                AnimatedContent(
                    targetState = steps,
                    transitionSpec = {
                        (fadeIn() + slideInVertically { it / 3 }) togetherWith (fadeOut() + slideOutVertically { -it / 3 }) using
                            SizeTransform(clip = false)
                    },
                    label = "steps",
                ) { count ->
                    Text(count.toString(), style = RecentrTheme.typography.displayHero, color = countColor, maxLines = 1)
                }
                Text(
                    stringResource(R.string.unlock_walk_goal, Walk.STEPS),
                    style = RecentrTheme.typography.displayHero,
                    color = countColor,
                    maxLines = 1,
                )
            }
            Text(status, style = RecentrTheme.typography.captionMedium, color = colors.textSecondary)
            OnboardingProgress(progress = steps / Walk.STEPS.toFloat(), width = 183.dp)
            if (isDone) {
                RecentrChip(stringResource(R.string.unlock_walk_complete_chip, Walk.STEPS), style = RecentrChipStyle.Success, icon = RecentrIcon.Check)
            } else {
                RecentrChip(stringResource(R.string.unlock_walk_locked_until, Walk.STEPS))
            }
        }
        if (!isDone) {
            StickerImage(
                Sticker.Lock,
                size = 54.dp,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 3.dp, y = (-38).dp)
                    .rotate(-12f)
                    .floating(amplitude = 4.dp),
            )
        }
    }
}

/** A note above the buttons: an icon, a bold line, and what it means. */
@Composable
private fun WalkNote(icon: RecentrIcon, title: String, body: String) {
    val colors = RecentrTheme.colors
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RecentrShapes.lg)
            .background(colors.bgSunken)
            .padding(16.dp)
            .semantics(mergeDescendants = true) {},
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        RecentrIconImage(icon, tint = colors.textSecondary, size = 20.dp)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, style = RecentrTheme.typography.captionMedium, color = colors.textPrimary)
            Text(body, style = RecentrTheme.typography.captionRegular, color = colors.textSecondary)
        }
    }
}

/** A walk survives the hold screen being made again (a theme change…) mid-walk. */
private val WalkSaver = listSaver<Walk, Any>(
    save = { listOf(it.start ?: -1f, it.detected, it.steps) },
    restore = { Walk(start = (it[0] as Float).takeIf { start -> start >= 0f }, detected = it[1] as Int, steps = it[2] as Int) },
)
