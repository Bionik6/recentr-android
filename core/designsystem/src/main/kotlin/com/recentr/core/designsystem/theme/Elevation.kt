package com.recentr.core.designsystem.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp

/**
 * Effect tokens from "Foundations · 04 Effects". Values are the Figma DROP_SHADOW blur radius,
 * offset and colour, rendered with Compose's CSS-like [dropShadow].
 */
object RecentrElevation {
    /** Elevation/Card — cards on page. */
    val card = Shadow(radius = 16.dp, color = Color(0x0F0E1220), offset = DpOffset(0.dp, 4.dp))

    /** Small card shadow used by buttons, icon buttons and tiles (0 4 8 · ink 6%). */
    val control = Shadow(radius = 8.dp, color = Color(0x0F0E1220), offset = DpOffset(0.dp, 4.dp))

    /** Elevation/Float — sheets, floating bars. */
    val float = Shadow(radius = 32.dp, color = Color(0x1F0E1220), offset = DpOffset(0.dp, 12.dp))

    /** Raised card shadow used by the Today card and sticky note (0 12 16 · ink 12%). */
    val raised = Shadow(radius = 16.dp, color = Color(0x1F0E1220), offset = DpOffset(0.dp, 12.dp))

    /** Glow/Sky — Start blocking CTA. */
    val glowSky = Shadow(radius = 24.dp, color = Color(0x592E7AF5), offset = DpOffset(0.dp, 10.dp))

    /** Brand button glow (0 10 12 · sky 35%). */
    val glowButton = Shadow(radius = 12.dp, color = Color(0x592E7AF5), offset = DpOffset(0.dp, 10.dp))

    /** Strict button glow (0 10 12 · tangerine 35%). */
    val glowStrict = Shadow(radius = 12.dp, color = Color(0x59FF661A), offset = DpOffset(0.dp, 10.dp))

    /** Timer Control, primary (0 8 16 · sky-light 45%). */
    val glowControl = Shadow(radius = 16.dp, color = Color(0x737CB8FF), offset = DpOffset(0.dp, 8.dp))

    /** Badges pinned on a tile, like a Focus sound's speaker (0 1 3 · ink 12%). */
    val badge = Shadow(radius = 3.dp, color = Color(0x1F0E1220), offset = DpOffset(0.dp, 1.dp))

    /** Volume Knob at rest (0 1 4 · ink 16%). */
    val knob = Shadow(radius = 4.dp, color = Color(0x290E1220), offset = DpOffset(0.dp, 1.dp))

    /** Volume Knob while it's dragged (0 2 8 · ink 18%). */
    val knobPressed = Shadow(radius = 8.dp, color = Color(0x2E0E1220), offset = DpOffset(0.dp, 2.dp))
}

/** Applies a design-system shadow behind [shape]. */
fun Modifier.recentrShadow(shadow: Shadow, shape: Shape): Modifier = dropShadow(shape, shadow)

fun Modifier.recentrShadow(shadow: Shadow, cornerRadius: Dp): Modifier =
    dropShadow(RoundedCornerShape(cornerRadius), shadow)
