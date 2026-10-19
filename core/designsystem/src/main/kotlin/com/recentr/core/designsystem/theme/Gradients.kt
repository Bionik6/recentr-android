package com.recentr.core.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.LinearGradientShader
import androidx.compose.ui.graphics.Shader
import androidx.compose.ui.graphics.ShaderBrush
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

/**
 * A CSS-style `linear-gradient(<angle>deg, …)` brush: 0° points up, 90° points right and the
 * gradient line spans the box so that the corners reach the first / last stops, exactly like the
 * gradients exported from Figma.
 */
@Immutable
class CssLinearGradient(
    private val angleDegrees: Float,
    private val colorStops: List<Pair<Float, Color>>,
) : ShaderBrush() {
    override fun createShader(size: Size): Shader {
        val radians = Math.toRadians(angleDegrees.toDouble())
        val dx = sin(radians).toFloat()
        val dy = -cos(radians).toFloat()
        val halfLength = (abs(size.width * dx) + abs(size.height * dy)) / 2f
        val center = Offset(size.width / 2f, size.height / 2f)
        return LinearGradientShader(
            from = center - Offset(dx * halfLength, dy * halfLength),
            to = center + Offset(dx * halfLength, dy * halfLength),
            colors = colorStops.map { it.second },
            colorStops = colorStops.map { it.first },
        )
    }

    override fun equals(other: Any?): Boolean =
        other is CssLinearGradient && other.angleDegrees == angleDegrees && other.colorStops == colorStops

    override fun hashCode(): Int = 31 * angleDegrees.hashCode() + colorStops.hashCode()
}

/** Gradients from "Foundations · Gradients" plus the rule template colours. */
object RecentrGradients {
    /** Brand CTA — Start blocking button, Glow/Sky. */
    val brandCta: Brush = Brush.horizontalGradient(listOf(Color(0xFF7CB8FF), Color(0xFF2F7BF6)))

    val brandDeep: Brush = CssLinearGradient(
        160.9f,
        listOf(0f to Color(0xFF7CB8FF), 0.71429f to Color(0xFF1452D4)),
    )

    val success: Brush = CssLinearGradient(
        160.9f,
        listOf(0f to Color(0xFF78E59E), 0.71429f to Color(0xFF17A859)),
    )

    val strict: Brush = Brush.horizontalGradient(listOf(Color(0xFFFF9E59), Color(0xFFFF5933)))

    val shield: Brush = CssLinearGradient(
        160.9f,
        listOf(0f to Color(0xFF1F5CDB), 0.71429f to Color(0xFF0D1740)),
    )

    /** Full-screen "App blocked shield" (Unlock friction 56): sky at the top, night at the bottom. */
    val shieldScreen: Brush = Brush.verticalGradient(listOf(Color(0xFF1F5CDB), Color(0xFF0A1233)))

    // Template card colours (Rules · Template Card · Color)
    val templateSky: Brush = CssLinearGradient(
        136.33f,
        listOf(0f to Color(0xFF7CB8FF), 0.71429f to Color(0xFF1452D4)),
    )
    val templateNight: Brush = CssLinearGradient(
        136.33f,
        listOf(0f to Color(0xFF4B4A8F), 0.71429f to Color(0xFF141433)),
    )
    val templateMint: Brush = CssLinearGradient(
        136.33f,
        listOf(0f to Color(0xFF7BE3B0), 0.71429f to Color(0xFF169C6A)),
    )
    val templateSunset: Brush = CssLinearGradient(
        136.33f,
        listOf(0f to Color(0xFFFFB37A), 0.71429f to Color(0xFFF0507A)),
    )
}
