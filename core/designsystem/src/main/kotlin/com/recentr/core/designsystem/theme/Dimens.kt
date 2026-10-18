package com.recentr.core.designsystem.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** "Dimension" collection — spacing scale bound to gaps and paddings. */
@Immutable
data class RecentrSpacing(
    val space2: Dp = 2.dp,
    val space4: Dp = 4.dp,
    val space6: Dp = 6.dp,
    val space8: Dp = 8.dp,
    val space12: Dp = 12.dp,
    val space16: Dp = 16.dp,
    val space20: Dp = 20.dp,
    val space24: Dp = 24.dp,
    val space32: Dp = 32.dp,
    val space40: Dp = 40.dp,
    val space48: Dp = 48.dp,
    /** Horizontal screen gutter used by every flow (402 − 354) / 2. */
    val screenGutter: Dp = 24.dp,
)

/** Corner radius tokens. */
@Immutable
data class RecentrRadius(
    val xs: Dp = 8.dp,
    val sm: Dp = 12.dp,
    val md: Dp = 16.dp,
    val lg: Dp = 20.dp,
    val xl: Dp = 24.dp,
    val card: Dp = 28.dp,
    val sheet: Dp = 36.dp,
    val screen: Dp = 48.dp,
    val full: Dp = 999.dp,
)

/** Component size tokens. */
@Immutable
data class RecentrSizes(
    val icon: Dp = 24.dp,
    val control: Dp = 44.dp,
    val button: Dp = 54.dp,
    val tabBar: Dp = 64.dp,
    val avatar: Dp = 40.dp,
    val iconButton: Dp = 40.dp,
)

val LocalRecentrSpacing = staticCompositionLocalOf { RecentrSpacing() }
val LocalRecentrRadius = staticCompositionLocalOf { RecentrRadius() }
val LocalRecentrSizes = staticCompositionLocalOf { RecentrSizes() }

/** Shapes derived from [RecentrRadius]. */
object RecentrShapes {
    val xs = RoundedCornerShape(8.dp)
    val sm = RoundedCornerShape(12.dp)
    val md = RoundedCornerShape(16.dp)
    val lg = RoundedCornerShape(20.dp)
    val xl = RoundedCornerShape(24.dp)
    val card = RoundedCornerShape(28.dp)
    val sheet = RoundedCornerShape(topStart = 36.dp, topEnd = 36.dp)
    val screen = RoundedCornerShape(48.dp)
    val full = RoundedCornerShape(percent = 50)
}
