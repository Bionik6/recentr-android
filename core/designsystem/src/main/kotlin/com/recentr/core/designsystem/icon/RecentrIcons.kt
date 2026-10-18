package com.recentr.core.designsystem.icon

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.recentr.core.designsystem.R

/**
 * The Figma icon set (24px outline, 1.8 stroke, round caps + filled tab variants), shipped as
 * vector drawables converted from the original SVGs. Recolour with `tint` like any Android icon.
 */
enum class RecentrIcon(@param:DrawableRes val drawable: Int) {
    Home(R.drawable.ic_home),
    Lock(R.drawable.ic_lock),
    Users(R.drawable.ic_users),
    User(R.drawable.ic_user),
    Plus(R.drawable.ic_plus),
    Refresh(R.drawable.ic_refresh),
    More(R.drawable.ic_more),
    ChevronDown(R.drawable.ic_chevron_down),
    ChevronRight(R.drawable.ic_chevron_right),
    ChevronLeft(R.drawable.ic_chevron_left),
    X(R.drawable.ic_x),
    Help(R.drawable.ic_help),
    Zap(R.drawable.ic_zap),
    Check(R.drawable.ic_check),
    Moon(R.drawable.ic_moon),
    Leaf(R.drawable.ic_leaf),
    Bell(R.drawable.ic_bell),
    Clock(R.drawable.ic_clock),
    Shield(R.drawable.ic_shield),
    Hourglass(R.drawable.ic_hourglass),
    Chart(R.drawable.ic_chart),
    ArrowRight(R.drawable.ic_arrow_right),
    Apps(R.drawable.ic_apps),
    Sparkle(R.drawable.ic_sparkle),
    Flame(R.drawable.ic_flame),
    Pencil(R.drawable.ic_pencil),
    Qr(R.drawable.ic_qr),
    Note(R.drawable.ic_note),
    HomeFill(R.drawable.ic_home_fill),
    LockFill(R.drawable.ic_lock_fill),
    UsersFill(R.drawable.ic_users_fill),
    UserFill(R.drawable.ic_user_fill),
    /** Orange rating star — keeps its own colour, use `tint = Color.Unspecified`. */
    StarFill(R.drawable.ic_star_fill),
    Minus(R.drawable.ic_minus),
    Sun(R.drawable.ic_sun),
    Settings(R.drawable.ic_settings),
}

/**
 * Draws a [RecentrIcon]. Default size is the 24dp `size/icon` token; the Figma file also scales
 * icons to 16.08 / 18 / 19.2dp inside chips and rows.
 */
@Composable
fun RecentrIconImage(
    icon: RecentrIcon,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    tint: Color = LocalContentColor.current,
    size: Dp = 24.dp,
) {
    Icon(
        painter = painterResource(icon.drawable),
        contentDescription = contentDescription,
        modifier = modifier.size(size),
        tint = if (icon == RecentrIcon.StarFill && tint == LocalContentColor.current) Color.Unspecified else tint,
    )
}
