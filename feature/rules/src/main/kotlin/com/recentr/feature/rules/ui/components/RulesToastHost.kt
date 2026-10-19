package com.recentr.feature.rules.ui.components

import android.os.SystemClock
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntOffset
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.recentr.core.designsystem.motion.MotionSpecs
import com.recentr.core.designsystem.motion.motionSpring
import com.recentr.feature.rules.model.RulesMessages
import kotlinx.coroutines.delay

/**
 * Shows the pending [RulesMessages] toast for a couple of seconds (green success pill), once its
 * screen is the one in front: not under the editor sheet, nor while the screen is on its way out,
 * so a toast lands where the editor leads (the screen underneath, or a new rule's page).
 */
@Composable
internal fun RulesToastHost(modifier: Modifier = Modifier) {
    val toast by RulesMessages.toast.collectAsStateWithLifecycle(minActiveState = Lifecycle.State.RESUMED)
    var visible by remember { mutableStateOf(false) }
    var text by remember { mutableStateOf("") }
    var shownAt by remember { mutableStateOf(0L) }
    // A toast seen on this screen is not replayed on the next one; one posted right before
    // leaving (e.g. "Rule deleted" then back) is left for the screen underneath.
    val current by rememberUpdatedState(toast)
    DisposableEffect(Unit) {
        onDispose {
            if (visible && SystemClock.elapsedRealtime() - shownAt > 500) current?.let(RulesMessages::consume)
        }
    }
    LaunchedEffect(toast) {
        val pending = toast ?: return@LaunchedEffect
        if (!pending.isFresh) {
            RulesMessages.consume(pending)
            return@LaunchedEffect
        }
        text = pending.text
        shownAt = SystemClock.elapsedRealtime()
        visible = true
        delay(2_600)
        visible = false
        RulesMessages.consume(pending)
    }
    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        // In with `.spring(duration: 0.4, bounce: 0.3)`, out with `.smooth`.
        enter = fadeIn(MotionSpecs.smooth(0.3f)) +
            slideInVertically(motionSpring(0.4f, 0.3f, IntOffset.VisibilityThreshold)) { it / 2 } +
            scaleIn(motionSpring(0.4f, 0.3f), initialScale = 0.9f),
        exit = fadeOut(MotionSpecs.smooth(0.3f)) +
            slideOutVertically(MotionSpecs.smooth(0.4f, IntOffset.VisibilityThreshold)) { it / 2 } +
            scaleOut(MotionSpecs.smooth(0.4f), targetScale = 0.9f),
    ) {
        RulesToastPill(text, Modifier.semantics { liveRegion = LiveRegionMode.Polite })
    }
}
