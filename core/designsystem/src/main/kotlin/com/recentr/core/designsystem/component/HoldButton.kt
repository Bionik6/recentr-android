package com.recentr.core.designsystem.component

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.recentr.core.designsystem.haptics.LocalRecentrHaptics
import com.recentr.core.designsystem.motion.motionSpring
import com.recentr.core.designsystem.theme.RecentrShapes
import com.recentr.core.designsystem.theme.RecentrTheme
import kotlinx.coroutines.launch

/** How long a [RecentrHoldButton] is held by default. */
const val HOLD_TO_CONFIRM_MILLIS = 1_500L

/**
 * Press-and-hold pill, styled like the primary [RecentrButton] ("Hold to create lock"): it fills
 * from left to right while pressed, empties again when let go too soon, and runs [onComplete] once
 * held for [holdMillis], with a success haptic — once. TalkBack's click runs [onComplete] right
 * away.
 */
@Composable
fun RecentrHoldButton(
    text: String,
    onComplete: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    holdMillis: Long = HOLD_TO_CONFIRM_MILLIS,
) {
    val colors = RecentrTheme.colors
    val haptics = LocalRecentrHaptics.current
    val scope = rememberCoroutineScope()
    val progress = remember { Animatable(0f) }
    /** Held to the end once: later presses do nothing. */
    var completed by remember { mutableStateOf(false) }
    val complete by rememberUpdatedState(onComplete)
    BoxWithConstraints(
        modifier
            .alpha(if (enabled) 1f else 0.4f)
            .height(RecentrTheme.sizes.button)
            .defaultMinSize(minWidth = 120.dp)
            .clip(RecentrShapes.full)
            .background(colors.actionPrimary)
            .semantics {
                role = Role.Button
                if (enabled) {
                    onClick {
                        if (!completed) {
                            completed = true
                            complete()
                        }
                        true
                    }
                } else {
                    disabled()
                }
            }
            .pointerInput(enabled) {
                if (!enabled) return@pointerInput
                detectTapGestures(
                    onPress = {
                        if (completed) return@detectTapGestures
                        haptics.lightImpact()
                        val filling = scope.launch {
                            val left = (holdMillis * (1f - progress.value)).toInt()
                            progress.animateTo(1f, tween(left, easing = LinearEasing))
                            completed = true
                            haptics.success()
                            complete()
                        }
                        try {
                            tryAwaitRelease()
                        } finally {
                            // Let go too soon, or the gesture was cancelled: empty it again.
                            if (!completed) {
                                filling.cancel()
                                scope.launch { progress.animateTo(0f, motionSpring(0.4f)) }
                            }
                        }
                    },
                )
            },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier
                .align(Alignment.CenterStart)
                .fillMaxHeight()
                .width(maxWidth * progress.value)
                .background(colors.textOnAction.copy(alpha = 0.22f)),
        )
        Text(
            text = text,
            style = RecentrTheme.typography.headline,
            color = colors.textOnAction,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 24.dp),
        )
    }
}

@Preview
@Composable
private fun RecentrHoldButtonPreview() {
    RecentrTheme {
        Column(Modifier.background(RecentrTheme.colors.bgPage).padding(24.dp)) {
            RecentrHoldButton("Hold to create lock", onComplete = {}, modifier = Modifier.fillMaxWidth())
        }
    }
}
