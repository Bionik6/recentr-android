package com.recentr.core.designsystem.haptics

import android.os.Build
import android.view.HapticFeedbackConstants
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalView

/**
 * Haptic feedback, mirroring the iOS `sensoryFeedback` cases. Silent when Settings › Appearance ›
 * Haptics is off; otherwise played through the view's native haptic constants, which also follow
 * the system's own touch-feedback setting.
 */
@Stable
interface RecentrHaptics {
    /** A choice changed: an option, a segment, a stepper value, a star lighting up. */
    fun selection()

    /** A switch flipped on or off. */
    fun toggle(on: Boolean)

    /** A light tap: the onboarding moved to another screen. */
    fun lightImpact()

    /** A firm tap: pressing or releasing a hold-to-unlock button. */
    fun mediumImpact()

    /** Something worked out: a plan built, a badge earned, a session done. */
    fun success()

    /** A heads-up before something destructive. */
    fun warning()

    /** Something went wrong: a typo, a wrong code. */
    fun error()
}

/** The app's haptics, provided by `ProvideRecentrMotion`. Silent until provided (previews). */
val LocalRecentrHaptics = staticCompositionLocalOf<RecentrHaptics> { SilentHaptics }

/** Haptics that play on [LocalView], or stay silent when [enabled] is false. */
@Composable
fun rememberRecentrHaptics(enabled: Boolean): RecentrHaptics {
    val view = LocalView.current
    return remember(view, enabled) { if (enabled && !view.isInEditMode) ViewHaptics(view) else SilentHaptics }
}

/**
 * The SwiftUI `.sensoryFeedback(_, trigger:)`: plays [feedback] each time [trigger] changes, never
 * for its first value.
 */
@Composable
fun SensoryFeedback(trigger: Any?, feedback: RecentrHaptics.() -> Unit) {
    val haptics = LocalRecentrHaptics.current
    val currentFeedback = rememberUpdatedState(feedback)
    val isFirst = remember { booleanArrayOf(true) }
    LaunchedEffect(trigger) {
        if (isFirst[0]) {
            isFirst[0] = false
        } else {
            currentFeedback.value(haptics)
        }
    }
}

private object SilentHaptics : RecentrHaptics {
    override fun selection() = Unit
    override fun toggle(on: Boolean) = Unit
    override fun lightImpact() = Unit
    override fun mediumImpact() = Unit
    override fun success() = Unit
    override fun warning() = Unit
    override fun error() = Unit
}

private class ViewHaptics(private val view: View) : RecentrHaptics {
    override fun selection() = perform(
        if (Build.VERSION.SDK_INT >= 34) HapticFeedbackConstants.SEGMENT_TICK else HapticFeedbackConstants.CLOCK_TICK,
    )

    override fun toggle(on: Boolean) = perform(
        when {
            Build.VERSION.SDK_INT < 34 -> HapticFeedbackConstants.CLOCK_TICK
            on -> HapticFeedbackConstants.TOGGLE_ON
            else -> HapticFeedbackConstants.TOGGLE_OFF
        },
    )

    override fun lightImpact() = perform(HapticFeedbackConstants.KEYBOARD_TAP)

    override fun mediumImpact() = perform(HapticFeedbackConstants.VIRTUAL_KEY)

    override fun success() = perform(
        if (Build.VERSION.SDK_INT >= 30) HapticFeedbackConstants.CONFIRM else HapticFeedbackConstants.VIRTUAL_KEY,
    )

    override fun warning() = perform(
        if (Build.VERSION.SDK_INT >= 34) HapticFeedbackConstants.GESTURE_THRESHOLD_ACTIVATE else HapticFeedbackConstants.LONG_PRESS,
    )

    override fun error() = perform(
        if (Build.VERSION.SDK_INT >= 30) HapticFeedbackConstants.REJECT else HapticFeedbackConstants.LONG_PRESS,
    )

    private fun perform(constant: Int) {
        view.performHapticFeedback(constant)
    }
}
