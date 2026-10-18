package com.recentr.core.designsystem.illustration

import android.graphics.Bitmap
import android.graphics.BlurMaskFilter
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.core.content.res.ResourcesCompat
import com.recentr.core.designsystem.R
import com.recentr.core.designsystem.motion.LocalReduceMotion
import com.recentr.core.designsystem.motion.rememberMotionClock
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.sin

/**
 * The hourglass mascot, drawn natively from the Figma geometry (a 160×200 artboard) like the iOS
 * `Mascot`, so it comes alive: it floats over its contact shadow, blinks, its sand glows and keeps
 * flowing; dozing it snores, impatient it shivers and sweats. Still when motion is reduced.
 * Pass the instance [width] used in the screen; the height follows the artboard.
 */
@Composable
fun Mascot(
    mood: MascotMood,
    width: Dp,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
) {
    val context = LocalContext.current
    val reduceMotion = LocalReduceMotion.current
    val clock = rememberMotionClock()
    val snoreTypeface = remember(context) {
        runCatching { ResourcesCompat.getFont(context, R.font.figtree_extrabold) }.getOrNull() ?: Typeface.DEFAULT_BOLD
    }
    Spacer(
        modifier
            .size(width = width, height = width * (ArtboardHeight / ArtboardWidth))
            .then(if (contentDescription != null) Modifier.semantics { this.contentDescription = contentDescription } else Modifier)
            .drawWithCache {
                val scale = size.width / ArtboardWidth
                val body = MascotBody.render(mood, scale)
                val painter = MascotPainter(mood, snoreTypeface)
                onDrawBehind {
                    // Starts a beat in, so the mascot doesn't open with a blink.
                    val time = if (reduceMotion) 0f else clock.value + 1.3f
                    drawIntoCanvas { canvas ->
                        val native = canvas.nativeCanvas
                        native.save()
                        native.scale(scale, scale)
                        painter.draw(native, body, scale, time)
                        native.restore()
                    }
                }
            },
    )
}

private const val ArtboardWidth = 160f
private const val ArtboardHeight = 200f

/** Room around the artboard in the body bitmap for the blurred shadows. */
private const val BodyMargin = 18f

/** SwiftUI shadow and blur radii as Android blur radii (Skia: sigma ≈ 0.577 × radius). */
private fun blur(swiftRadius: Float) = swiftRadius * 1.6f

private fun argb(hex: Long, opacity: Float = 1f): Int {
    val alpha = (opacity.coerceIn(0f, 1f) * 255).toInt()
    return (alpha shl 24) or (hex and 0xFFFFFF).toInt()
}

/** The parts that never move relative to each other (glass, sand, frame), rendered once. */
private object MascotBody {
    fun render(mood: MascotMood, scale: Float): Bitmap {
        val widthPx = ((ArtboardWidth + BodyMargin * 2) * scale).toInt().coerceAtLeast(1)
        val heightPx = ((ArtboardHeight + BodyMargin * 2) * scale).toInt().coerceAtLeast(1)
        val bitmap = Bitmap.createBitmap(widthPx, heightPx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.scale(scale, scale)
        canvas.translate(BodyMargin, BodyMargin)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        drawGlass(canvas, paint)
        drawSand(canvas, paint, mood == MascotMood.Impatient)
        drawSpeculars(canvas, paint)
        drawFrame(canvas, paint)
        return bitmap
    }

    private fun drawGlass(canvas: Canvas, paint: Paint) {
        val glass = MascotPaths.glass
        val bounds = glass.bounds()
        paint.resetAntiAlias()
        paint.shader = LinearGradient(
            bounds.left + 0.39f * bounds.width(), bounds.top + 0.07f * bounds.height(),
            bounds.left + 0.84f * bounds.width(), bounds.top + 0.43f * bounds.height(),
            argb(0xFFFFFF, 0.92f), argb(0xC7DCFF, 0.78f), Shader.TileMode.CLAMP,
        )
        canvas.drawPath(glass, paint)
        canvas.innerShadow(glass, paint, argb(0x3E6FE0, 0.35f), radius = 6f, dx = 4f, dy = -4f)
        canvas.innerShadow(glass, paint, argb(0xFFFFFF), radius = 2.5f, dx = -3f, dy = 3f)
        // A 4pt white stroke clipped to the glass: a 2pt rim inside it.
        canvas.save()
        canvas.clipPath(glass)
        paint.resetAntiAlias()
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 4f
        paint.color = argb(0xFFFFFF, 0.95f)
        canvas.drawPath(glass, paint)
        canvas.restore()
    }

    private fun drawSand(canvas: Canvas, paint: Paint, isImpatient: Boolean) {
        val sand = if (isImpatient) MascotPaths.impatientSand else MascotPaths.sand
        val top = if (isImpatient) MascotPaths.impatientSandTop else MascotPaths.sandTop
        // Glow under the bottom sand.
        paint.resetAntiAlias()
        paint.color = argb(0xFFA51F, 0.45f)
        paint.maskFilter = BlurMaskFilter(blur(7f), BlurMaskFilter.Blur.NORMAL)
        canvas.drawPath(sand, paint)
        for (path in listOf(sand, top)) {
            val bounds = path.bounds()
            paint.resetAntiAlias()
            paint.shader = LinearGradient(
                bounds.left, bounds.top, bounds.centerX(), bounds.bottom,
                argb(0xFFD978), argb(0xF08C00), Shader.TileMode.CLAMP,
            )
            canvas.drawPath(path, paint)
            canvas.innerShadow(path, paint, argb(0xFFFFFF, 0.55f), radius = 1.5f, dx = -2f, dy = 3f)
            canvas.innerShadow(path, paint, argb(0x9A4A00, 0.5f), radius = 3f, dx = 2f, dy = -3f)
        }
    }

    private fun drawSpeculars(canvas: Canvas, paint: Paint) {
        paint.resetAntiAlias()
        paint.maskFilter = BlurMaskFilter(blur(0.75f), BlurMaskFilter.Blur.NORMAL)
        paint.color = argb(0xFFFFFF, 0.9f)
        canvas.drawRotatedOval(45.04f, 54.31f, 3.5f, 18f, 14f, paint)
        paint.color = argb(0xFFFFFF, 0.75f)
        canvas.drawRotatedOval(43.77f, 131.34f, 3f, 13f, 14f, paint)
    }

    private fun drawFrame(canvas: Canvas, paint: Paint) {
        val navyColors = intArrayOf(argb(0x5B86FF), argb(0x1236B0))
        val navyStops = floatArrayOf(0f, 0.714f)
        for (x in floatArrayOf(22f, 128f)) {
            val pillar = Path().apply { addRoundRect(RectF(x, 22f, x + 10f, 178f), 5f, 5f, Path.Direction.CW) }
            paint.resetAntiAlias()
            paint.shader = LinearGradient(x, 0f, x + 10f, 0f, navyColors, navyStops, Shader.TileMode.CLAMP)
            canvas.drawPath(pillar, paint)
            canvas.frameInnerShadows(pillar, paint)
        }
        for ((y, shadowOpacity) in listOf(6f to 0.25f, 172f to 0.3f)) {
            val cap = Path().apply { addRoundRect(RectF(12f, y, 148f, y + 22f), 9f, 9f, Path.Direction.CW) }
            paint.resetAntiAlias()
            paint.color = argb(0x071A5E, shadowOpacity)
            paint.maskFilter = BlurMaskFilter(blur(5f), BlurMaskFilter.Blur.NORMAL)
            canvas.save()
            canvas.translate(0f, 6f)
            canvas.drawPath(cap, paint)
            canvas.restore()
            paint.resetAntiAlias()
            paint.shader = LinearGradient(0f, y, 0f, y + 22f, navyColors, navyStops, Shader.TileMode.CLAMP)
            canvas.drawPath(cap, paint)
            canvas.frameInnerShadows(cap, paint)
        }
        paint.resetAntiAlias()
        paint.color = argb(0xFFFFFF, 0.55f)
        for ((x, y) in listOf(20f to 14f, 134f to 14f, 20f to 180f, 134f to 180f)) {
            canvas.drawRoundRect(RectF(x, y, x + 6f, y + 6f), 3f, 3f, paint)
        }
    }

    private fun Canvas.frameInnerShadows(path: Path, paint: Paint) {
        innerShadow(path, paint, argb(0x071A5E, 0.5f), radius = 3f, dx = 2f, dy = -3f)
        innerShadow(path, paint, argb(0xFFFFFF, 0.55f), radius = 1.5f, dx = -2f, dy = 3f)
    }
}

/** The parts that move every frame: contact shadow, glow, sand stream and face. */
private class MascotPainter(private val mood: MascotMood, private val snoreTypeface: Typeface) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val shadowBlur = BlurMaskFilter(blur(3f), BlurMaskFilter.Blur.NORMAL)
    private val glowBlur = BlurMaskFilter(blur(11f), BlurMaskFilter.Blur.NORMAL)
    private val bodyDestination = RectF(-BodyMargin, -BodyMargin, ArtboardWidth + BodyMargin, ArtboardHeight + BodyMargin)
    private val blinkMatrix = Matrix()
    private val blinkPath = Path()
    private val sweatShader = LinearGradient(0f, 36f, 0f, 55f, argb(0xBFDDFF), argb(0x3D8BFF), Shader.TileMode.CLAMP)

    fun draw(canvas: Canvas, body: Bitmap, scale: Float, time: Float) {
        val float = sin(time * 2 * PI / 3.4).toFloat()
        val lift = max(0f, float)

        // Contact shadow tightens as the hourglass rises.
        paint.resetAntiAlias()
        paint.color = argb(0x0E1220, 0.22f - 0.05f * lift)
        paint.maskFilter = shadowBlur
        canvas.save()
        canvas.scale(1f - 0.08f * lift, 1f, 80f, 194f)
        canvas.drawOval(30f, 188f, 130f, 200f, paint)
        canvas.restore()

        canvas.save()
        canvas.translate(shake(time), -4f * float)

        paint.resetAntiAlias()
        paint.color = argb(0xFFB627, 0.35f * (0.5f + 0.15f * sin(time * 2 * PI / 2.6).toFloat()))
        paint.maskFilter = glowBlur
        canvas.drawOval(40f, 100f, 120f, 170f, paint)

        paint.resetAntiAlias()
        paint.isFilterBitmap = true
        canvas.drawBitmap(body, null, bodyDestination, paint)
        drawStream(canvas, time)
        drawFace(canvas, time)
        canvas.restore()
    }

    /** The falling stream, with grains of light sliding down it. */
    private fun drawStream(canvas: Canvas, time: Float) {
        val length = if (mood == MascotMood.Impatient) 14f else 34f
        val stream = RectF(79f, 100f, 81f, 100f + length)
        paint.resetAntiAlias()
        paint.color = argb(0xFFB627)
        canvas.drawRoundRect(stream, 1f, 1f, paint)
        canvas.save()
        canvas.clipRect(stream)
        paint.color = argb(0xFFFFFF, 0.75f)
        repeat(3) { index ->
            val progress = fract(time / 0.8f + index / 3f)
            val y = stream.top + progress * (length + 4f) - 4f
            canvas.drawOval(stream.left, y, stream.right, y + 3f, paint)
        }
        canvas.restore()
    }

    private fun drawFace(canvas: Canvas, time: Float) {
        val ink = argb(0x0B1433)
        paint.resetAntiAlias()
        when (mood) {
            MascotMood.Calm -> {
                paint.color = ink
                canvas.drawBlinking(MascotPaths.calmEyes, centerY = 54f, time)
                paint.color = argb(0xFFFFFF)
                canvas.drawBlinking(MascotPaths.glints, centerY = 54f, time)
            }
            MascotMood.Dozing -> {
                paint.color = ink
                canvas.drawPath(MascotPaths.dozingEyes, paint)
                drawSnore(canvas, size = 18f, x = 110f, y = 28f, phase = 0f, time = time)
                drawSnore(canvas, size = 13f, x = 126f, y = 16f, phase = 0.5f, time = time)
            }
            MascotMood.Impatient -> {
                paint.color = ink
                canvas.drawBlinking(MascotPaths.impatientEyes, centerY = 58f, time)
                canvas.drawRoundRect(RectF(74f, 72f, 86f, 75f), 1.5f, 1.5f, paint)
                drawSweat(canvas, time)
            }
        }
    }

    /** Closes the eyes for a beat every few seconds. */
    private fun Canvas.drawBlinking(path: Path, centerY: Float, time: Float) {
        val phase = time % 4.2f
        val scaleY = if (phase < 0.18f) 1f - 0.9f * sin(phase / 0.18f * PI).toFloat() else 1f
        if (scaleY == 1f) {
            drawPath(path, paint)
            return
        }
        blinkMatrix.setScale(1f, scaleY, 0f, centerY)
        path.transform(blinkMatrix, blinkPath)
        drawPath(blinkPath, paint)
    }

    private fun drawSnore(canvas: Canvas, size: Float, x: Float, y: Float, phase: Float, time: Float) {
        val progress = fract(time / 2.8f + phase)
        paint.resetAntiAlias()
        paint.typeface = snoreTypeface
        paint.textSize = size
        paint.color = argb(0x1236B0, sin(progress * PI).toFloat())
        val metrics = paint.fontMetrics
        val textWidth = paint.measureText("z")
        val textHeight = metrics.descent - metrics.ascent
        val left = x + 4f * progress
        val top = y - 10f * progress
        val zoom = 0.8f + 0.4f * progress
        canvas.save()
        canvas.scale(zoom, zoom, left + textWidth / 2, top + textHeight / 2)
        canvas.drawText("z", left, top - metrics.ascent, paint)
        canvas.restore()
    }

    private fun drawSweat(canvas: Canvas, time: Float) {
        val progress = fract(time / 1.8f)
        val drop = MascotPaths.sweat
        canvas.save()
        canvas.translate(0f, 5f * progress)
        val alpha = (1f - progress * progress).coerceIn(0f, 1f)
        canvas.saveLayerAlpha(100f, 30f, 126f, 60f, (alpha * 255).toInt())
        paint.resetAntiAlias()
        paint.shader = sweatShader
        canvas.drawPath(drop, paint)
        canvas.innerShadow(drop, paint, argb(0xFFFFFF, 0.8f), radius = 1f, dx = -1f, dy = 1.5f)
        canvas.restore()
        canvas.restore()
    }

    /** An impatient shiver every couple of seconds. */
    private fun shake(time: Float): Float {
        if (mood != MascotMood.Impatient) return 0f
        val phase = time % 2.2f
        if (phase >= 0.45f) return 0f
        return 1.6f * sin(phase * 60.0).toFloat() * sin(phase / 0.45 * PI).toFloat()
    }
}

private fun fract(value: Float) = value - kotlin.math.floor(value)

private fun Paint.resetAntiAlias() {
    reset()
    isAntiAlias = true
}

private fun Path.bounds() = RectF().also { computeBounds(it, true) }

private fun Canvas.drawRotatedOval(cx: Float, cy: Float, rx: Float, ry: Float, degrees: Float, paint: Paint) {
    save()
    rotate(degrees, cx, cy)
    drawOval(cx - rx, cy - ry, cx + rx, cy + ry, paint)
    restore()
}

/**
 * SwiftUI's `.shadow(.inner(color:radius:x:y:))`: the outside of [shape], moved by ([dx], [dy])
 * and blurred, drawn inside the shape.
 */
private fun Canvas.innerShadow(shape: Path, paint: Paint, color: Int, radius: Float, dx: Float, dy: Float) {
    val bounds = shape.bounds()
    val pad = blur(radius) * 2 + abs(dx) + abs(dy) + 4f
    val outside = Path().apply {
        addRect(bounds.left - pad, bounds.top - pad, bounds.right + pad, bounds.bottom + pad, Path.Direction.CW)
        op(Path(shape).apply { offset(dx, dy) }, Path.Op.DIFFERENCE)
    }
    save()
    clipPath(shape)
    paint.resetAntiAlias()
    paint.color = color
    paint.maskFilter = BlurMaskFilter(blur(radius), BlurMaskFilter.Blur.NORMAL)
    drawPath(outside, paint)
    restore()
}

/** The Figma geometry, in the 160×200 artboard. */
private object MascotPaths {
    val glass: Path = Path().apply {
        moveTo(44f, 0f)
        cubicTo(68.3005f, 0f, 88f, 17.4609f, 88f, 39f)
        cubicTo(88f, 56.2197f, 75.409f, 70.8315f, 57.9424f, 76f)
        cubicTo(75.409f, 81.1685f, 88f, 95.7803f, 88f, 113f)
        cubicTo(88f, 134.539f, 68.3005f, 152f, 44f, 152f)
        cubicTo(19.6995f, 152f, 0f, 134.539f, 0f, 113f)
        cubicTo(0f, 95.7806f, 12.5905f, 81.1687f, 30.0566f, 76f)
        cubicTo(12.5905f, 70.8313f, 0f, 56.2194f, 0f, 39f)
        cubicTo(0f, 17.4609f, 19.6995f, 0f, 44f, 0f)
        close()
        offset(36f, 24f)
    }

    val sand: Path = Path().apply {
        moveTo(58f, 14f)
        cubicTo(73.6855f, 14f, 86.7339f, 17.8801f, 89.4707f, 23f)
        lineTo(101.985f, 23f)
        cubicTo(101.995f, 23.3323f, 102f, 23.6657f, 102f, 24f)
        cubicTo(102f, 45.5391f, 82.3005f, 63f, 58f, 63f)
        cubicTo(33.6995f, 63f, 14f, 45.5391f, 14f, 24f)
        cubicTo(14f, 23.6657f, 14.0052f, 23.3323f, 14.0146f, 23f)
        lineTo(26.5293f, 23f)
        cubicTo(29.2661f, 17.8801f, 42.3145f, 14f, 58f, 14f)
        close()
        offset(22f, 113f)
    }

    val sandTop: Path = Path().apply {
        moveTo(71.0664f, 0f)
        cubicTo(63.0621f, 9.69858f, 50.1289f, 16f, 35.5332f, 16f)
        cubicTo(20.9375f, 16f, 8.00435f, 9.69858f, 0f, 0f)
        close()
        offset(44.47f, 86f)
    }

    val impatientSand: Path = Path().apply {
        moveTo(80f, 107f)
        cubicTo(95.6855f, 107f, 108.734f, 110.88f, 111.471f, 116f)
        lineTo(117.083f, 116f)
        cubicTo(121.462f, 122.062f, 124f, 129.268f, 124f, 137f)
        cubicTo(124f, 158.539f, 104.301f, 176f, 80f, 176f)
        cubicTo(55.6995f, 176f, 36f, 158.539f, 36f, 137f)
        cubicTo(36f, 129.268f, 38.5385f, 122.062f, 42.917f, 116f)
        lineTo(48.5293f, 116f)
        cubicTo(51.2661f, 110.88f, 64.3145f, 107f, 80f, 107f)
        close()
    }

    val impatientSandTop: Path = Path().apply {
        moveTo(101.564f, 97f)
        cubicTo(95.1918f, 100.182f, 87.8356f, 102f, 80f, 102f)
        cubicTo(72.1644f, 102f, 64.8082f, 100.182f, 58.4355f, 97f)
        close()
    }

    val calmEyes: Path = Path().apply {
        addRoundRect(RectF(64f, 44f, 74f, 64f), 5f, 5f, Path.Direction.CW)
        addRoundRect(RectF(86f, 44f, 96f, 64f), 5f, 5f, Path.Direction.CW)
    }

    val glints: Path = Path().apply {
        addRoundRect(RectF(69f, 47f, 72f, 52f), 1.5f, 1.5f, Path.Direction.CW)
        addRoundRect(RectF(91f, 47f, 94f, 52f), 1.5f, 1.5f, Path.Direction.CW)
    }

    val dozingEyes: Path = Path().apply {
        addRoundRect(RectF(63f, 56f, 75f, 60f), 2f, 2f, Path.Direction.CW)
        addRoundRect(RectF(85f, 56f, 97f, 60f), 2f, 2f, Path.Direction.CW)
    }

    val impatientEyes: Path = Path().apply {
        moveTo(63f, 46f)
        lineTo(75f, 51f)
        lineTo(75f, 61f)
        cubicTo(75f, 64f, 72.5f, 66f, 69f, 66f)
        cubicTo(65.5f, 66f, 63f, 64f, 63f, 61f)
        close()
        moveTo(97f, 46f)
        lineTo(85f, 51f)
        lineTo(85f, 61f)
        cubicTo(85f, 64f, 87.5f, 66f, 91f, 66f)
        cubicTo(94.5f, 66f, 97f, 64f, 97f, 61f)
        close()
    }

    val sweat: Path = Path().apply {
        moveTo(114f, 36f)
        cubicTo(117f, 42f, 120f, 45f, 120f, 49f)
        cubicTo(120f, 52.5f, 117.3f, 55f, 114f, 55f)
        cubicTo(110.7f, 55f, 108f, 52.5f, 108f, 49f)
        cubicTo(108f, 45f, 111f, 42f, 114f, 36f)
        close()
    }
}
