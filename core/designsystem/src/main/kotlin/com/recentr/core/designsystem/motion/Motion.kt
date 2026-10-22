package com.recentr.core.designsystem.motion

import android.animation.ValueAnimator
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.database.ContentObserver
import android.os.BatteryManager
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.provider.Settings
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseInOut
import androidx.compose.animation.core.InfiniteRepeatableSpec
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.StartOffset
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.recentr.core.designsystem.haptics.LocalRecentrHaptics
import com.recentr.core.designsystem.haptics.rememberRecentrHaptics
import kotlin.math.PI
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// Motion shared by every screen, ported from the iOS `Motion.swift`. Each helper settles to its
// resting state when motion is reduced, by the system ("Remove animations") or by Recentr
// (Settings › Appearance › Reduce motion).

/** Whether to settle animations: the system's "Remove animations" or Recentr's own Reduce motion. */
val LocalReduceMotion = staticCompositionLocalOf { false }

/**
 * Provides [LocalReduceMotion] and the haptics of Settings › Appearance to [content]. Wraps the
 * whole app, inside the theme.
 */
@Composable
fun ProvideRecentrMotion(
    reduceMotion: Boolean,
    hapticsEnabled: Boolean,
    content: @Composable () -> Unit,
) {
    val systemReducesMotion by rememberSystemReducesMotion()
    CompositionLocalProvider(
        LocalReduceMotion provides (reduceMotion || systemReducesMotion),
        LocalRecentrHaptics provides rememberRecentrHaptics(hapticsEnabled),
        content = content,
    )
}

/** The system's "Remove animations" (animator duration scale 0), kept up to date while shown. */
@Composable
private fun rememberSystemReducesMotion(): State<Boolean> {
    val context = LocalContext.current
    val state = remember { mutableStateOf(!ValueAnimator.areAnimatorsEnabled()) }
    DisposableEffect(context) {
        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                state.value = !ValueAnimator.areAnimatorsEnabled()
            }
        }
        val uri = Settings.Global.getUriFor(Settings.Global.ANIMATOR_DURATION_SCALE)
        context.contentResolver.registerContentObserver(uri, false, observer)
        onDispose { context.contentResolver.unregisterContentObserver(observer) }
    }
    return state
}

/**
 * Whether the battery runs low: Battery Saver is on, or the phone is unplugged at 20% or less, as on
 * iOS. Decorative motion, like the sky's clouds, holds still then to spare it. Kept up to date while
 * shown.
 */
@Composable
fun rememberBatteryLow(): State<Boolean> {
    val context = LocalContext.current
    val state = remember { mutableStateOf(false) }
    DisposableEffect(context) {
        val powerManager = context.getSystemService(PowerManager::class.java)
        var battery: Intent? = null
        fun update() {
            state.value = powerManager.isPowerSaveMode || battery?.isBatteryLow() == true
        }
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                if (intent.action == Intent.ACTION_BATTERY_CHANGED) battery = intent
                update()
            }
        }
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_BATTERY_CHANGED)
            addAction(PowerManager.ACTION_POWER_SAVE_MODE_CHANGED)
        }
        // The battery broadcast is sticky: registering returns the latest one.
        battery = ContextCompat.registerReceiver(context, receiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
        update()
        onDispose { context.unregisterReceiver(receiver) }
    }
    return state
}

private fun Intent.isBatteryLow(): Boolean {
    val level = getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
    val scale = getIntExtra(BatteryManager.EXTRA_SCALE, -1)
    val unplugged = getIntExtra(BatteryManager.EXTRA_PLUGGED, 0) == 0
    return unplugged && level >= 0 && scale > 0 && level * 5 <= scale
}

/**
 * The SwiftUI `.spring(duration:bounce:)` as a Compose spring: [duration] is the perceptual
 * duration in seconds, [bounce] 0 (no overshoot) … 1.
 */
fun <T> motionSpring(duration: Float, bounce: Float = 0f, visibilityThreshold: T? = null): SpringSpec<T> {
    val angularFrequency = 2 * PI / duration
    return spring(
        dampingRatio = 1f - bounce.coerceIn(0f, 0.95f),
        stiffness = (angularFrequency * angularFrequency).toFloat(),
        visibilityThreshold = visibilityThreshold,
    )
}

/** The springs every screen shares, named after their SwiftUI counterparts. */
object MotionSpecs {
    /** `.spring(duration: 0.7, bounce: 0.28)`: views rising into place. */
    fun <T> entrance(visibilityThreshold: T? = null) = motionSpring(0.7f, 0.28f, visibilityThreshold)

    /** `.spring(duration: 0.3, bounce: 0.4)`: buttons shrinking while pressed. */
    fun <T> press(visibilityThreshold: T? = null) = motionSpring(0.3f, 0.4f, visibilityThreshold)

    /** `.spring(duration: 0.6, bounce: 0.18)`: onboarding screens sliding in. */
    fun <T> step(visibilityThreshold: T? = null) = motionSpring(0.6f, 0.18f, visibilityThreshold)

    /** `.snappy(duration: 0.35)`: values changing under a finger (steppers, segments). */
    fun <T> snappy(duration: Float = 0.35f, visibilityThreshold: T? = null) =
        motionSpring(duration, 0.15f, visibilityThreshold)

    /** `.smooth(duration: 0.3)`: colours and states settling without overshoot. */
    fun <T> smooth(duration: Float = 0.3f, visibilityThreshold: T? = null) =
        motionSpring(duration, 0f, visibilityThreshold)

    /** `.spring(duration: 0.45, bounce: 0.55)`: checks, stars and badges popping in. */
    fun <T> pop(visibilityThreshold: T? = null) = motionSpring(0.45f, 0.55f, visibilityThreshold)
}

/**
 * Seconds since this composable entered, ticking every frame; frozen at 0 when motion is reduced.
 * Read it in a draw or layer lambda so only that phase is redone every frame.
 */
@Composable
fun rememberMotionClock(): State<Float> {
    val reduceMotion = LocalReduceMotion.current
    return produceState(0f, reduceMotion) {
        if (reduceMotion) {
            value = 0f
            return@produceState
        }
        val start = withFrameNanos { it }
        while (true) {
            withFrameNanos { value = (it - start) / 1_000_000_000f }
        }
    }
}

/**
 * The SwiftUI `phaseAnimator([from, to])`: goes back and forth between [from] and [to] forever,
 * easing in and out over [durationMillis] each way. Stays at [to] when motion is reduced.
 */
@Composable
fun rememberPingPong(
    from: Float,
    to: Float,
    durationMillis: Int,
    delayMillis: Int = 0,
): State<Float> {
    val reduceMotion = LocalReduceMotion.current
    val resting = rememberUpdatedState(to)
    if (reduceMotion) return resting
    val transition = rememberInfiniteTransition(label = "pingPong")
    return transition.animateFloat(
        initialValue = from,
        targetValue = to,
        animationSpec = pingPongSpec(durationMillis, delayMillis),
        label = "pingPong",
    )
}

private fun pingPongSpec(durationMillis: Int, delayMillis: Int): InfiniteRepeatableSpec<Float> = infiniteRepeatable(
    animation = tween(durationMillis, easing = EaseInOut),
    repeatMode = RepeatMode.Reverse,
    initialStartOffset = StartOffset(delayMillis),
)

/** A value that becomes true once, [delayMillis] after entering: the SwiftUI `.task { sleep; flag = true }`. */
@Composable
fun rememberAfterDelay(delayMillis: Long): State<Boolean> = produceState(false) {
    delay(delayMillis)
    value = true
}

/**
 * Grows chart bars up from their base when they appear (iOS: each bar's `scaleEffect(y:)` from 5%,
 * `.spring(duration: 0.7, bounce: 0.35)`), [staggerMillis] apart, again whenever [key] changes.
 * Returns each bar's height factor, to read while drawing; 1 straight away when motion is reduced.
 */
@Composable
fun rememberBarsGrowth(count: Int, staggerMillis: Int, key: Any? = Unit): (index: Int) -> Float {
    val reduceMotion = LocalReduceMotion.current
    val bars = remember(count, key) { List(count) { Animatable(if (reduceMotion) 1f else 0.05f) } }
    LaunchedEffect(bars) {
        bars.forEachIndexed { index, bar ->
            launch {
                delay(index * staggerMillis.toLong())
                bar.animateTo(1f, motionSpring(0.7f, 0.35f))
            }
        }
    }
    return { index -> bars.getOrNull(index)?.value ?: 1f }
}

/**
 * Fills a gauge (progress bar, ring) from empty to [target] when it appears, after [delayMillis]
 * (iOS `.spring(duration: 0.9, bounce: 0.2)`), then follows [target] as it changes. Full straight
 * away when motion is reduced.
 */
@Composable
fun rememberFill(target: Float, delayMillis: Int = 150, duration: Float = 0.9f): State<Float> {
    val reduceMotion = LocalReduceMotion.current
    val fill = remember { Animatable(if (reduceMotion) target else 0f) }
    val hasFilled = remember { booleanArrayOf(reduceMotion) }
    LaunchedEffect(target) {
        if (hasFilled[0]) {
            fill.animateTo(target, MotionSpecs.smooth(0.45f))
        } else {
            delay(delayMillis.toLong())
            hasFilled[0] = true
            fill.animateTo(target, motionSpring(duration, 0.2f))
        }
    }
    return fill.asState()
}
