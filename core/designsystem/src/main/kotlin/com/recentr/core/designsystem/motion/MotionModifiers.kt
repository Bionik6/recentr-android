package com.recentr.core.designsystem.motion

import android.os.Build
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.node.CompositionLocalConsumerModifierNode
import androidx.compose.ui.node.LayoutModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.currentValueOf
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.sin

/**
 * Rises, sharpens and fades the element in when it appears, after [delayMillis]: the iOS
 * `.entrance(delay:from:)`. It starts [from] its resting place, at 94% and blurred, and springs in.
 * With motion reduced only the fade plays.
 *
 * Pass `blur = false` for elements casting a shadow: a blurred layer clips what it draws outside
 * its bounds, so the shadow would pop in at the end.
 */
fun Modifier.entrance(
    delayMillis: Int = 0,
    from: DpOffset = DpOffset(0.dp, 24.dp),
    blur: Boolean = true,
): Modifier = this then EntranceElement(delayMillis, from, blur)

private data class EntranceElement(
    val delayMillis: Int,
    val from: DpOffset,
    val blur: Boolean,
) : ModifierNodeElement<EntranceNode>() {
    override fun create() = EntranceNode(delayMillis, from, blur)

    override fun update(node: EntranceNode) {
        node.from = from
        node.blur = blur
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "entrance"
        properties["delayMillis"] = delayMillis
        properties["from"] = from
        properties["blur"] = blur
    }
}

private class EntranceNode(
    private val delayMillis: Int,
    var from: DpOffset,
    var blur: Boolean,
) : Modifier.Node(), LayoutModifierNode, CompositionLocalConsumerModifierNode {
    private val progress = Animatable(0f)
    private var isSettled = false

    override fun onAttach() {
        isSettled = currentValueOf(LocalReduceMotion)
        coroutineScope.launch {
            if (delayMillis > 0) delay(delayMillis.toLong())
            progress.animateTo(1f, if (isSettled) MotionSpecs.smooth(0.5f) else MotionSpecs.entrance())
        }
    }

    private val layerBlock: GraphicsLayerScope.() -> Unit = {
        val value = progress.value
        alpha = value.coerceIn(0f, 1f)
        // Without blur, fade each drawing on its own: an offscreen layer would clip the shadow.
        compositingStrategy = if (blur) CompositingStrategy.Auto else CompositingStrategy.ModulateAlpha
        if (!isSettled) {
            val rest = 1f - value
            translationX = from.x.toPx() * rest
            translationY = from.y.toPx() * rest
            val scale = 0.94f + 0.06f * value
            scaleX = scale
            scaleY = scale
            val radius = 6.dp.toPx() * rest.coerceIn(0f, 1f)
            renderEffect = if (blur && radius > 0.5f && Build.VERSION.SDK_INT >= 31) {
                BlurEffect(radius, radius, TileMode.Decal)
            } else {
                null
            }
        }
    }

    override fun MeasureScope.measure(measurable: Measurable, constraints: Constraints): MeasureResult {
        val placeable = measurable.measure(constraints)
        return layout(placeable.width, placeable.height) {
            placeable.placeWithLayer(0, 0, layerBlock = layerBlock)
        }
    }
}

/**
 * Bobs the element up and down forever: the iOS `.floating(amplitude:period:phase:)`. [phase]
 * (0…1) staggers elements sharing a period. Still when motion is reduced.
 */
fun Modifier.floating(amplitude: Dp = 6.dp, periodMillis: Int = 4000, phase: Float = 0f): Modifier =
    this then FloatingElement(amplitude, periodMillis, phase)

private data class FloatingElement(
    val amplitude: Dp,
    val periodMillis: Int,
    val phase: Float,
) : ModifierNodeElement<FloatingNode>() {
    override fun create() = FloatingNode(amplitude, periodMillis, phase)

    override fun update(node: FloatingNode) {
        node.amplitude = amplitude
        node.periodMillis = periodMillis
        node.phase = phase
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "floating"
        properties["amplitude"] = amplitude
        properties["periodMillis"] = periodMillis
        properties["phase"] = phase
    }
}

private class FloatingNode(
    var amplitude: Dp,
    var periodMillis: Int,
    var phase: Float,
) : Modifier.Node(), LayoutModifierNode, CompositionLocalConsumerModifierNode {
    private var seconds by mutableFloatStateOf(0f)
    private var job: Job? = null

    override fun onAttach() {
        if (currentValueOf(LocalReduceMotion)) return
        job = coroutineScope.launch {
            val start = withFrameNanos { it }
            while (isActive) {
                withFrameNanos { seconds = (it - start) / 1_000_000_000f }
            }
        }
    }

    override fun onDetach() {
        job?.cancel()
        job = null
        seconds = 0f
    }

    private val layerBlock: GraphicsLayerScope.() -> Unit = {
        val cycle = seconds * 1000f / periodMillis + phase
        translationY = amplitude.toPx() * sin(cycle * 2 * PI).toFloat()
    }

    override fun MeasureScope.measure(measurable: Measurable, constraints: Constraints): MeasureResult {
        val placeable = measurable.measure(constraints)
        return layout(placeable.width, placeable.height) {
            placeable.placeWithLayer(0, 0, layerBlock = layerBlock)
        }
    }
}

/**
 * Shrinks the element to [pressedScale] while [interactionSource] is pressed, springing back on
 * release: the iOS pill (0.97), circle (0.92) and pressable (0.98) button styles.
 */
fun Modifier.pressScale(interactionSource: InteractionSource, pressedScale: Float = 0.97f): Modifier = composed {
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) pressedScale else 1f,
        animationSpec = MotionSpecs.press(),
        label = "pressScale",
    )
    graphicsLayer {
        scaleX = scale
        scaleY = scale
        transformOrigin = TransformOrigin.Center
    }
}
