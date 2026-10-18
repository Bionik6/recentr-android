package com.recentr.core.designsystem.motion

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.rotateRad
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import com.recentr.core.designsystem.theme.RecentrTheme
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * A one-off burst of confetti in the brand colours, thrown up from the middle of the screen and
 * falling under gravity for 3.2s, once [isActive] turns true (iOS `Confetti`). Nothing when motion
 * is reduced. Lay it over the screen; it doesn't take touches.
 */
@Composable
fun Confetti(isActive: Boolean, modifier: Modifier = Modifier) {
    val colors = RecentrTheme.colors
    val palette = remember(colors) {
        listOf(colors.brandSky, colors.brandSkyLight, colors.accentTangerine, colors.accentNote, colors.feedbackSuccess, colors.auroraPink)
    }
    val reduceMotion = LocalReduceMotion.current
    var seconds by remember { mutableFloatStateOf(-1f) }
    LaunchedEffect(isActive, reduceMotion) {
        if (!isActive || reduceMotion || seconds >= 0f) return@LaunchedEffect
        val start = withFrameNanos { it }
        while (true) {
            val elapsed = withFrameNanos { (it - start) / 1_000_000_000f }
            seconds = elapsed
            if (elapsed >= ConfettiSeconds) break
        }
    }
    Canvas(modifier) {
        val time = seconds
        if (time < 0f || time >= ConfettiSeconds) return@Canvas
        val alpha = (1f - time / ConfettiSeconds).coerceIn(0f, 1f)
        val unit = density
        for (piece in ConfettiPieces) {
            val vx = cos(piece.angle) * piece.speed * unit
            val vy = sin(piece.angle) * piece.speed * unit
            val x = size.width * piece.origin + vx * time * 0.6f
            val y = size.height * 0.42f + vy * time + 0.5f * 900f * unit * time * time
            val width = piece.width * unit
            val height = piece.height * unit
            translate(x, y) {
                rotateRad(piece.spin * time, pivot = Offset.Zero) {
                    scale(1f, abs(cos(piece.spin * time * 0.7f)) * 0.8f + 0.2f, pivot = Offset.Zero) {
                        drawRoundRect(
                            color = palette[piece.colorIndex % palette.size],
                            topLeft = Offset(-width / 2, -height / 2),
                            size = Size(width, height),
                            cornerRadius = CornerRadius(2f * unit),
                            alpha = alpha,
                        )
                    }
                }
            }
        }
    }
}

private const val ConfettiSeconds = 3.2f

private class ConfettiPiece(
    val angle: Float,
    val speed: Float,
    val spin: Float,
    val width: Float,
    val height: Float,
    val colorIndex: Int,
    val origin: Float,
)

/** Seeded, so the burst looks the same every time. */
private val ConfettiPieces: List<ConfettiPiece> = List(90) { index ->
    val random = Random(index * 2_654_435_761L)
    fun between(from: Float, to: Float) = from + random.nextFloat() * (to - from)
    ConfettiPiece(
        angle = between(-0.95f * PI.toFloat(), -0.05f * PI.toFloat()),
        speed = between(380f, 820f),
        spin = between(-9f, 9f),
        width = between(6f, 11f),
        height = between(9f, 16f),
        colorIndex = index,
        origin = between(0.2f, 0.8f),
    )
}
