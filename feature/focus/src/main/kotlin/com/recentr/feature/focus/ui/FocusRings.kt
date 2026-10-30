package com.recentr.feature.focus.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.recentr.core.designsystem.haptics.LocalRecentrHaptics
import com.recentr.core.designsystem.motion.MotionSpecs
import com.recentr.core.designsystem.theme.RecentrTheme
import kotlin.math.atan2
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/** Knob drop shadow of the Figma rings (0 2 6 · rgba(13, 18, 51, 0.25)). */
private val KnobShadowColor = Color(0x400D1233)

/**
 * A ring progress from 12 o'clock, clockwise, with butt ends and a white knob at the end of the
 * arc (Figma "Dial" / "Ring"). With [secondLap], [fraction] can exceed 1: each lap is drawn over
 * the full one before it, the two brushes taking turns so the laps stand apart.
 */
internal fun DrawScope.drawFocusRing(
    fraction: Float,
    strokeWidth: Dp,
    track: Brush,
    progress: Brush,
    knobRadius: Dp,
    secondLap: Brush? = null,
    /** Off for an empty ring (a round waiting to start): no arc, no knob. */
    showsProgress: Boolean = true,
    /** The arc and knob's opacity: 40% on a paused round (Figma 92B). */
    progressAlpha: Float = 1f,
) {
    val stroke = strokeWidth.toPx()
    val inset = stroke / 2
    val arcSize = Size(size.width - stroke, size.height - stroke)
    val topLeft = Offset(inset, inset)
    drawArc(track, startAngle = 0f, sweepAngle = 360f, useCenter = false, topLeft = topLeft, size = arcSize, style = Stroke(stroke))
    if (!showsProgress) return
    val laps = if (secondLap != null && fraction > 1f) ceil(fraction).toInt() - 1 else 0
    val lapBrush = { lap: Int -> if (lap % 2 == 0 || secondLap == null) progress else secondLap }
    if (laps > 0) {
        drawArc(lapBrush(laps - 1), -90f, 360f, false, topLeft, arcSize, alpha = progressAlpha, style = Stroke(stroke, cap = StrokeCap.Butt))
    }
    val current = (fraction - laps).coerceIn(0f, 1f)
    drawArc(lapBrush(laps), -90f, 360f * current, false, topLeft, arcSize, alpha = progressAlpha, style = Stroke(stroke, cap = StrokeCap.Butt))
    // Knob at the end of the arc, on the stroke centre line.
    val angle = Math.toRadians((360.0 * (fraction % 1f).let { if (fraction > 0f && it == 0f) 1f else it }) - 90.0)
    val radius = (size.minDimension - stroke) / 2
    val center = Offset(size.width / 2 + radius * cos(angle).toFloat(), size.height / 2 + radius * sin(angle).toFloat())
    drawKnob(center, knobRadius.toPx(), progressAlpha)
}

private fun DrawScope.drawKnob(center: Offset, radius: Float, alpha: Float = 1f) {
    drawIntoCanvas { canvas ->
        val paint = android.graphics.Paint().apply {
            isAntiAlias = true
            color = Color.White.copy(alpha = alpha).toArgb()
            if (alpha == 1f) setShadowLayer(6.dp.toPx(), 0f, 2.dp.toPx(), KnobShadowColor.toArgb())
        }
        canvas.nativeCanvas.drawCircle(center.x, center.y, radius, paint)
    }
}

/** Ends of the set-up dial gradient: fixed sky values in Figma, the same in the dark palette. */
private val DialLight = Color(0xFF7CB8FF)
private val DialDeep = Color(0xFF1452D4)

/** Gradient of the set-up dial: Figma `linear-gradient` from (20, −15) to (170, 185) on a 250 dial. */
private fun dialBrush(sizePx: Float): Brush = Brush.linearGradient(
    colors = listOf(DialLight, DialDeep),
    start = Offset(20f / 250f * sizePx, -15f / 250f * sizePx),
    end = Offset(170f / 250f * sizePx, 185f / 250f * sizePx),
)

/**
 * 91 · Focus — Set up "Dial": 250dp, 18dp sunken track, sky gradient progress (one lap = 60
 * minutes) and a draggable 24dp knob. Dragging snaps to 5 minutes between [min] and [max], with a
 * tick for each step; each hour past the first winds another lap, deep and sky in turn. The arc
 * and knob glide to a new value (`.snappy(duration: 0.2)`).
 */
@Composable
internal fun FocusDial(
    minutes: Int,
    onMinutesChange: (Int) -> Unit,
    contentDescription: String,
    modifier: Modifier = Modifier,
    min: Int = 5,
    max: Int = 180,
) {
    val colors = RecentrTheme.colors
    val currentMinutes by rememberUpdatedState(minutes)
    val haptics = LocalRecentrHaptics.current
    val onChange by rememberUpdatedState<(Int) -> Unit> { value ->
        if (value != minutes) haptics.selection()
        onMinutesChange(value)
    }
    val drag = remember { DialDrag() }
    val shownMinutes by animateFloatAsState(minutes.toFloat(), MotionSpecs.snappy(0.2f), label = "dialMinutes")
    Canvas(
        modifier
            .size(250.dp)
            .semantics {
                this.contentDescription = contentDescription
                progressBarRangeInfo = ProgressBarRangeInfo(minutes.toFloat(), min.toFloat()..max.toFloat(), steps = (max - min) / 5 - 1)
                setProgress { value ->
                    onChange((value / 5f).roundToInt() * 5)
                    true
                }
            }
            .pointerInput(min, max) {
                fun angleOf(position: Offset): Float {
                    val dx = position.x - size.width / 2f
                    val dy = position.y - size.height / 2f
                    // 0° at 12 o'clock, clockwise.
                    return ((Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())) + 450.0) % 360.0).toFloat()
                }
                detectDragGestures(
                    onDragStart = { position ->
                        drag.lastAngle = angleOf(position)
                        drag.totalDegrees = currentMinutes * 6f
                    },
                ) { change, _ ->
                    val angle = angleOf(change.position)
                    var delta = angle - drag.lastAngle
                    if (delta > 180f) delta -= 360f
                    if (delta < -180f) delta += 360f
                    drag.lastAngle = angle
                    drag.totalDegrees = (drag.totalDegrees + delta).coerceIn(min * 6f, max * 6f)
                    val snapped = ((drag.totalDegrees / 6f / 5f).roundToInt() * 5).coerceIn(min, max)
                    if (snapped != currentMinutes) onChange(snapped)
                }
            }
            .pointerInput(min, max) {
                detectTapGestures { position ->
                    // A tap sets the minutes on the lap the dial is on.
                    val dx = position.x - size.width / 2f
                    val dy = position.y - size.height / 2f
                    val degrees = ((Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())) + 450.0) % 360.0).toFloat()
                    val lapOffset = (currentMinutes - 1).coerceAtLeast(0) / 60 * 60
                    onChange((((degrees / 6f) / 5f).roundToInt() * 5 + lapOffset).coerceIn(min, max))
                }
            },
    ) {
        val brush = dialBrush(size.width)
        drawFocusRing(
            fraction = shownMinutes / 60f,
            strokeWidth = 18.dp,
            track = SolidColor(colors.bgSunken),
            progress = brush,
            knobRadius = 12.dp,
            secondLap = SolidColor(DialDeep),
        )
    }
}

private class DialDrag {
    var lastAngle = 0f
    var totalDegrees = 0f
}
