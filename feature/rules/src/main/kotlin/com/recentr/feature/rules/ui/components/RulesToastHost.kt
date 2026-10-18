package com.recentr.feature.rules.ui.components

import android.os.SystemClock
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.recentr.feature.rules.model.RulesMessages
import kotlinx.coroutines.delay

/** Shows the pending [RulesMessages] toast for a couple of seconds (green success pill). */
@Composable
internal fun RulesToastHost(modifier: Modifier = Modifier) {
    val toast by RulesMessages.toast.collectAsStateWithLifecycle()
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
        enter = fadeIn() + slideInVertically { it / 2 } + scaleIn(initialScale = 0.9f),
        exit = fadeOut() + slideOutVertically { it / 2 } + scaleOut(targetScale = 0.9f),
    ) {
        RulesToastPill(text, Modifier.semantics { liveRegion = LiveRegionMode.Polite })
    }
}
