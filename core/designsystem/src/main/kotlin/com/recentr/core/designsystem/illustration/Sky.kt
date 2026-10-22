package com.recentr.core.designsystem.illustration

import android.graphics.Bitmap
import android.graphics.BlurMaskFilter
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.toArgb
import com.recentr.core.designsystem.motion.LocalReduceMotion
import com.recentr.core.designsystem.motion.rememberBatteryLow
import com.recentr.core.designsystem.theme.RecentrColors
import com.recentr.core.designsystem.theme.RecentrTheme
import kotlin.math.PI
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlinx.coroutines.delay

/**
 * Figma `Backdrop/Sky`, drawn natively like the iOS `Backdrop(.sky)` so its clouds can move (see
 * [Cloud]). Its intrinsic size is the 402×874 artboard: draw it scaled to the screen width, anchored
 * at the top. When motion is reduced it is the Figma artwork; while the battery runs low its clouds
 * hold still where they are.
 */
@Composable
internal fun rememberSkyPainter(): Painter {
    val colors = RecentrTheme.colors
    val time = rememberSkyClock()
    return remember(colors, time) { SkyPainter(colors, time) }
}

/** Seconds of the sky's motion, about 30 times a second like iOS. */
@Composable
private fun rememberSkyClock(): State<Float> {
    val reduceMotion = LocalReduceMotion.current
    val batteryLow by rememberBatteryLow()
    return produceState(0f, reduceMotion, batteryLow) {
        if (reduceMotion) {
            value = 0f
            return@produceState
        }
        if (batteryLow) return@produceState
        val resumedAt = value
        val start = withFrameNanos { it }
        while (true) {
            withFrameNanos { value = resumedAt + (it - start) / 1_000_000_000f }
            delay(25)
        }
    }
}

private class SkyPainter(private val colors: RecentrColors, private val time: State<Float>) : Painter() {
    override val intrinsicSize = Size(ArtboardWidth, ArtboardHeight)

    // `hero/sky-1` to `hero/sky-4`: at night the sky darkens into the page.
    private val sky = Brush.verticalGradient(
        0f to colors.heroSky1,
        0.28f to colors.heroSky2,
        0.5f to colors.heroSky3,
        0.66f to colors.heroSky4,
        0.78f to colors.bgPage,
    )
    private val paint = Paint(Paint.FILTER_BITMAP_FLAG).apply { color = colors.heroCloud.toArgb() }
    private val bounds = RectF()

    override fun DrawScope.onDraw() {
        val scale = size.width / ArtboardWidth
        val seconds = time.value.toDouble()
        clipRect {
            drawRect(sky)
            drawIntoCanvas { canvas ->
                for (cloud in Cloud.Sky) cloud.draw(canvas.nativeCanvas, paint, bounds, seconds, scale)
            }
        }
    }
}

private const val ArtboardWidth = 402f
private const val ArtboardHeight = 874f

/**
 * A cloud of `Backdrop/Sky`, in Figma points on its artboard: a blurred ellipse with puffs tucked
 * inside it. Over time it drifts with the wind, sways, bobs and swells, and its puffs rise out of its
 * top and sink back, so it billows like a real cloud. At time 0 it is the Figma ellipse.
 *
 * iOS draws the same clouds (`Backdrop.swift`): change both together. iOS blurs each cloud with its
 * puffs every frame; blurring on every frame is too slow here, so the ellipse and each puff are
 * blurred once ([Sprite]) and only moved, stretched and faded, the puffs fading in as they rise.
 */
private class Cloud(
    val x: Float,
    val y: Float,
    val radiusX: Float,
    val radiusY: Float,
    val blur: Float,
    /** How far the wind carries it: the bigger, nearer cloud drifts furthest. */
    val drift: Float,
    val sway: Wave,
    val bob: Wave,
    /** Widens as it flattens, and back. */
    val swell: Wave,
    val puffs: List<Puff>,
) {
    /** [rise]: how high it rises out of the cloud, there and back over [period] seconds. */
    class Puff(val x: Float, val y: Float, val radius: Float, val rise: Float, val period: Float)

    private val sprite by lazy { Sprite(radiusX, radiusY, blur) }
    private val puffSprites by lazy { puffs.map { Sprite(it.radius, it.radius, blur) } }

    fun draw(canvas: Canvas, paint: Paint, bounds: RectF, time: Double, scale: Float) {
        val swelling = swell(time)
        val stretchX = (1 + swelling) * scale
        val stretchY = (1 - swelling / 2) * scale
        val centerX = (x + drift * wind(time) + sway(time)) * scale
        val centerY = (y + bob(time)) * scale
        paint.alpha = 255
        sprite.draw(canvas, paint, bounds, centerX, centerY, stretchX, stretchY)
        puffs.forEachIndexed { index, puff ->
            val rising = ((1 - cos(2 * PI * time / puff.period)) / 2).toFloat()
            paint.alpha = (rising * 255).roundToInt()
            if (paint.alpha == 0) return@forEachIndexed
            val size = (1 + 0.2f * rising) * scale
            puffSprites[index].draw(
                canvas,
                paint,
                bounds,
                centerX + puff.x * stretchX,
                centerY + (puff.y - puff.rise * rising) * stretchY,
                size,
                size,
            )
        }
    }

    companion object {
        val Sky = listOf(
            Cloud(
                x = 70f, y = 305f, radiusX = 160f, radiusY = 55f, blur = 20f,
                drift = 24f, sway = Wave(6f, 37f), bob = Wave(4f, 23f), swell = Wave(0.05f, 31f),
                puffs = listOf(
                    Puff(x = -58f, y = -10f, radius = 40f, rise = 18f, period = 19f),
                    Puff(x = 42f, y = -10f, radius = 36f, rise = 17f, period = 26f),
                ),
            ),
            Cloud(
                x = 340f, y = 350f, radiusX = 150f, radiusY = 50f, blur = 20f,
                drift = 20f, sway = Wave(5f, 41f), bob = Wave(3f, 27f), swell = Wave(0.05f, 34f),
                puffs = listOf(
                    Puff(x = -48f, y = -8f, radius = 36f, rise = 17f, period = 23f),
                    Puff(x = 38f, y = -10f, radius = 32f, rise = 16f, period = 17f),
                ),
            ),
            Cloud(
                x = 210f, y = 510f, radiusX = 250f, radiusY = 90f, blur = 25f,
                drift = 34f, sway = Wave(8f, 43f), bob = Wave(5f, 29f), swell = Wave(0.04f, 37f),
                puffs = listOf(
                    Puff(x = -95f, y = -16f, radius = 60f, rise = 26f, period = 24f),
                    Puff(x = 70f, y = -20f, radius = 56f, rise = 24f, period = 31f),
                    Puff(x = -5f, y = -28f, radius = 52f, rise = 20f, period = 21f),
                ),
            ),
        )

        /** The wind every cloud drifts with, from -1 to 1: two slow gusts that never quite repeat. */
        fun wind(time: Double): Float = (0.7 * sin(2 * PI * time / 71) + 0.3 * sin(2 * PI * time / 29)).toFloat()
    }
}

/**
 * An ellipse of [radiusX] × [radiusY] Figma points blurred by [blur] (the Figma blur, a sigma),
 * rendered once at [Density] as an alpha mask that the paint's colour tints. Blurred content is so
 * smooth that drawing it scaled up loses nothing.
 */
private class Sprite(radiusX: Float, radiusY: Float, blur: Float) {
    private val bitmap = Bitmap.createBitmap(
        ceil(2 * (radiusX + 3 * blur) * Density).toInt(),
        ceil(2 * (radiusY + 3 * blur) * Density).toInt(),
        Bitmap.Config.ALPHA_8,
    ).also { bitmap ->
        // Skia blurs by sigma ≈ 0.577 × radius + 0.5.
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            maskFilter = BlurMaskFilter((blur * Density - 0.5f) / 0.57735f, BlurMaskFilter.Blur.NORMAL)
        }
        val centerX = bitmap.width / 2f
        val centerY = bitmap.height / 2f
        Canvas(bitmap).drawOval(
            centerX - radiusX * Density,
            centerY - radiusY * Density,
            centerX + radiusX * Density,
            centerY + radiusY * Density,
            paint,
        )
    }

    /** Half the sprite's size in Figma points. */
    private val halfWidth = bitmap.width / 2f / Density
    private val halfHeight = bitmap.height / 2f / Density

    /** Draws it centred on [centerX], [centerY], with [scaleX] and [scaleY] pixels a point. */
    fun draw(canvas: Canvas, paint: Paint, bounds: RectF, centerX: Float, centerY: Float, scaleX: Float, scaleY: Float) {
        bounds.set(
            centerX - halfWidth * scaleX,
            centerY - halfHeight * scaleY,
            centerX + halfWidth * scaleX,
            centerY + halfHeight * scaleY,
        )
        canvas.drawBitmap(bitmap, null, bounds, paint)
    }
}

/** Sprite pixels a Figma point: a 20 pt blur is 10 pixels. */
private const val Density = 0.5f

/** A sine of [amplitude], [period] seconds long, at 0 when the time is. */
private class Wave(val amplitude: Float, val period: Float) {
    operator fun invoke(time: Double): Float = (amplitude * sin(2 * PI * time / period)).toFloat()
}
