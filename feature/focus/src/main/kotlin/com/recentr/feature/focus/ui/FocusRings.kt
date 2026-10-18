package com.recentr.feature.focus.ui

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
import com.recentr.core.designsystem.theme.RecentrTheme
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/** Knob drop shadow of the Figma rings (0 2 6 · rgba(13, 18, 51, 0.25)). */
private val KnobShadowColor = Color(0x400D1233)

/**
 * A ring progress from 12 o'clock, clockwise, with butt ends and a white knob at the end of the
 * arc (Figma "Dial" / "Ring"). [fraction] can exceed 1 for a second lap, drawn over the first.
 */
internal fun DrawScope.drawFocusRing(
    fraction: Float,
    strokeWidth: Dp,
    track: Brush,
    progress: Brush,
    knobRadius: Dp,
    secondLap: Brush? = null,
) {
    val stroke = strokeWidth.toPx()
    val inset = stroke / 2
    val arcSize = Size(size.width - stroke, size.height - stroke)
    val topLeft = Offset(inset, inset)
    drawArc(track, startAngle = 0f, sweepAngle = 360f, useCenter = false, topLeft = topLeft, size = arcSize, style = Stroke(stroke))
    val lap1 = fraction.coerceIn(0f, 1f)
    drawArc(progress, -90f, 360f * lap1, false, topLeft, arcSize, style = Stroke(stroke, cap = StrokeCap.Butt))
    if (fraction > 1f && secondLap != null) {
        drawArc(secondLap, -90f, 360f * (fraction - 1f).coerceAtMost(1f), false, topLeft, arcSize, style = Stroke(stroke, cap = StrokeCap.Butt))
    }
    // Knob at the end of the arc, on the stroke centre line.
    val angle = Math.toRadians((360.0 * (fraction % 1f).let { if (fraction > 0f && it == 0f) 1f else it }) - 90.0)
    val radius = (size.minDimension - stroke) / 2
    val center = Offset(size.width / 2 + radius * cos(angle).toFloat(), size.height / 2 + radius * sin(angle).toFloat())
    drawKnob(center, knobRadius.toPx())
}

private fun DrawScope.drawKnob(center: Offset, radius: Float) {
    drawIntoCanvas { canvas ->
        val paint = android.graphics.Paint().apply {
            isAntiAlias = true
            color = Color.White.toArgb()
            setShadowLayer(6.dp.toPx(), 0f, 2.dp.toPx(), KnobShadowColor.toArgb())
        }
        canvas.nativeCanvas.drawCircle(center.x, center.y, radius, paint)
    }
}

/** Gradient of the set-up dial: Figma `linear-gradient` from (20, −15) to (170, 185) on a 250 dial. */
private fun dialBrush(sizePx: Float, light: Color, deep: Color): Brush = Brush.linearGradient(
    colors = listOf(light, deep),
    start = Offset(20f / 250f * sizePx, -15f / 250f * sizePx),
    end = Offset(170f / 250f * sizePx, 185f / 250f * sizePx),
)

/**
 * 91 · Focus — Set up "Dial": 250dp, 18dp sunken track, sky gradient progress (one lap = 60
 * minutes) and a draggable 24dp knob. Dragging snaps to 5 minutes between [min] and [max]; past
 * 60 minutes a second, deeper lap is drawn.
 */
@Composable
internal fun FocusDial(
    minutes: Int,
    onMinutesChange: (Int) -> Unit,
    contentDescription: String,
    modifier: Modifier = Modifier,
    min: Int = 5,
    max: Int = 120,
) {
    val colors = RecentrTheme.colors
    val currentMinutes by rememberUpdatedState(minutes)
    val onChange by rememberUpdatedState(onMinutesChange)
    val drag = remember { DialDrag() }
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
                    // A tap sets the minutes on the first lap (or the second one past 60).
                    val dx = position.x - size.width / 2f
                    val dy = position.y - size.height / 2f
                    val degrees = ((Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())) + 450.0) % 360.0).toFloat()
                    val lapOffset = if (currentMinutes > 60) 60 else 0
                    onChange((((degrees / 6f) / 5f).roundToInt() * 5 + lapOffset).coerceIn(min, max))
                }
            },
    ) {
        val brush = dialBrush(size.width, colors.brandSkyLight, colors.brandSkyDeep)
        drawFocusRing(
            fraction = minutes / 60f,
            strokeWidth = 18.dp,
            track = SolidColor(colors.bgSunken),
            progress = brush,
            knobRadius = 12.dp,
            secondLap = SolidColor(colors.brandSkyDeep),
        )
    }
}

private class DialDrag {
    var lastAngle = 0f
    var totalDegrees = 0f
}
