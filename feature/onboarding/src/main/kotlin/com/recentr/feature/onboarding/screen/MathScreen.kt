package com.recentr.feature.onboarding.screen

import android.graphics.BlurMaskFilter
import androidx.annotation.StringRes
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseInOut
import androidx.compose.animation.core.EaseOut
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.recentr.core.designsystem.haptics.SensoryFeedback
import com.recentr.core.designsystem.icon.RecentrIcon
import com.recentr.core.designsystem.icon.RecentrIconImage
import com.recentr.core.designsystem.motion.MotionSpecs
import com.recentr.core.designsystem.motion.entrance
import com.recentr.core.designsystem.theme.RecentrShapes
import com.recentr.core.designsystem.theme.RecentrTheme
import com.recentr.feature.onboarding.R
import com.recentr.feature.onboarding.component.OnboardingHeading
import com.recentr.feature.onboarding.component.OnboardingPage
import kotlin.math.roundToInt
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

private val MathSteps = listOf(
    R.string.onb_math_step_habits,
    R.string.onb_math_step_goal,
    R.string.onb_math_step_plan,
)

/**
 * 13 · Onboarding — Doing the math (Figma 2024:1020). Every second the ring fills another third
 * (`.easeInOut(duration: 0.9)`) and the next check pops in with a success tap; a second after the
 * last one the flow moves on by itself ([onFinished]).
 */
@Composable
internal fun MathScreen(onFinished: () -> Unit) {
    val progress = remember { Animatable(0f) }
    var completed by remember { mutableIntStateOf(0) }
    val currentOnFinished by rememberUpdatedState(onFinished)
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(Unit) {
        for (checkpoint in 1..MathSteps.size) {
            delay(1000)
            completed = checkpoint
            launch { progress.animateTo(checkpoint / MathSteps.size.toFloat(), tween(900, easing = EaseInOut)) }
        }
        delay(1000)
        // If the app went to the background meanwhile, move on once the user is back.
        lifecycle.currentStateFlow.first { it.isAtLeast(Lifecycle.State.RESUMED) }
        currentOnFinished()
    }
    SensoryFeedback(trigger = completed) { success() }
    MathContent(progress = progress.value, completed = completed)
}

@Composable
private fun MathContent(progress: Float, completed: Int) {
    OnboardingPage {
        Spacer(Modifier.height(136.dp))
        ProgressRing(progress, Modifier.entrance(100))
        Spacer(Modifier.height(40.dp))
        OnboardingHeading(title = stringResource(R.string.onb_math_title), centered = true, delayMillis = 250)
        Spacer(Modifier.height(37.dp))
        Box(Modifier.fillMaxWidth().padding(horizontal = 72.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                MathSteps.forEachIndexed { index, label ->
                    MathStep(label, done = index < completed, modifier = Modifier.entrance(350 + index * 80))
                }
            }
        }
    }
}

/** 200dp ring: 14dp sunken track + sky arc with the Glow/Sky shadow, percentage in the middle. */
@Composable
private fun ProgressRing(progress: Float, modifier: Modifier = Modifier) {
    val colors = RecentrTheme.colors
    val glow = colors.brandSky.copy(alpha = 0.35f)
    Box(
        modifier = modifier
            .size(200.dp)
            .clipToBounds()
            .semantics { progressBarRangeInfo = ProgressBarRangeInfo(progress, 0f..1f) },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = 14.dp.toPx()
            val inset = stroke / 2
            val arcSize = Size(size.width - stroke, size.height - stroke)
            val topLeft = Offset(inset, inset)
            drawArc(colors.bgSunken, 0f, 360f, useCenter = false, topLeft = topLeft, size = arcSize, style = Stroke(stroke))
            val sweep = 360f * progress
            if (sweep > 0f) {
                // Glow/Sky: 0 10 24 · sky 35% — a blurred copy of the arc drawn below it.
                drawIntoCanvas { canvas ->
                    val paint = Paint().asFrameworkPaint().apply {
                        isAntiAlias = true
                        style = android.graphics.Paint.Style.STROKE
                        strokeWidth = stroke
                        color = glow.toArgb()
                        maskFilter = BlurMaskFilter(12.dp.toPx(), BlurMaskFilter.Blur.NORMAL)
                    }
                    val dy = 10.dp.toPx()
                    canvas.nativeCanvas.drawArc(
                        topLeft.x, topLeft.y + dy, topLeft.x + arcSize.width, topLeft.y + dy + arcSize.height,
                        -90f, sweep, false, paint,
                    )
                }
                drawArc(colors.brandSky, -90f, sweep, useCenter = false, topLeft = topLeft, size = arcSize, style = Stroke(stroke))
            }
        }
        Text(
            text = stringResource(R.string.onb_math_percent, (progress * 100).roundToInt()),
            style = RecentrTheme.typography.title1.copy(fontFeatureSettings = "tnum"),
            color = colors.textPrimary,
        )
    }
}

@Composable
private fun MathStep(@StringRes label: Int, done: Boolean, modifier: Modifier = Modifier) {
    val colors = RecentrTheme.colors
    val circle by animateColorAsState(if (done) colors.feedbackSuccess else colors.bgSunken, MotionSpecs.smooth(0.3f), label = "stepCircle")
    val text by animateColorAsState(if (done) colors.textPrimary else colors.textTertiary, tween(300, easing = EaseOut), label = "stepText")
    // The check pops in: `.spring(duration: 0.45, bounce: 0.55)` from 20%.
    val check by animateFloatAsState(if (done) 1f else 0f, MotionSpecs.pop(), label = "stepCheck")
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .size(24.dp)
                .clip(RecentrShapes.full)
                .background(circle),
            contentAlignment = Alignment.Center,
        ) {
            RecentrIconImage(
                RecentrIcon.Check,
                tint = colors.textOnDark,
                size = 16.08.dp,
                modifier = Modifier.graphicsLayer {
                    val scale = 0.2f + 0.8f * check
                    scaleX = scale
                    scaleY = scale
                    alpha = check.coerceIn(0f, 1f)
                },
            )
        }
        Text(stringResource(label), style = RecentrTheme.typography.bodyMedium, color = text)
    }
}

@Preview(widthDp = 402, heightDp = 874)
@Composable
private fun MathScreenPreview() {
    RecentrTheme { MathContent(progress = 1f, completed = 3) }
}
