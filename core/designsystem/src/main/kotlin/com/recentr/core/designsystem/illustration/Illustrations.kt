package com.recentr.core.designsystem.illustration

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.recentr.core.designsystem.R
import com.recentr.core.designsystem.theme.RecentrTheme

/**
 * Clay stickers (Figma "Sticker" components, 120×120 artboard). The exported bitmaps keep their soft
 * contact shadow, which can spill below the artboard: [exportHeightRatio] is export height / width.
 */
enum class Sticker(@param:DrawableRes val drawable: Int, val exportHeightRatio: Float) {
    /** Lock rule — block apps entirely. */
    Lock(R.drawable.sticker_lock, 1f),
    /** Session rule — focus now. */
    Bolt(R.drawable.sticker_bolt, 508f / 480f),
    /** Limit rule — daily time budget. */
    Timer(R.drawable.sticker_timer, 1f),
    /** Routine rule — evenings & nights. */
    Moon(R.drawable.sticker_moon, 1f),
    /** Habit — grow a good habit. */
    Leaf(R.drawable.sticker_leaf, 1f),
    /** Reflection / reason — sticky note. */
    Note(R.drawable.sticker_note, 492f / 480f),
    /** Streak — days in a row. */
    Flame(R.drawable.sticker_flame, 496f / 480f),
}

/** Draws a [Sticker] in a square slot of [size] (the Figma instance size). */
@Composable
fun StickerImage(
    sticker: Sticker,
    size: Dp,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
) {
    OverflowingImage(
        drawable = sticker.drawable,
        exportHeightRatio = sticker.exportHeightRatio,
        contentDescription = contentDescription,
        modifier = modifier.size(size),
    )
}

/**
 * Offly/Recentr mascot moods. Calm = default / under goal; Impatient = near limit / unlock
 * countdown; Dozing = night routines, offline, log out.
 */
enum class MascotMood(@param:DrawableRes val drawable: Int) {
    Calm(R.drawable.mascot_calm),
    Impatient(R.drawable.mascot_impatient),
    Dozing(R.drawable.mascot_dozing),
}

/** The hourglass mascot. Artboard is 160×200; pass the instance [width] used in the screen. */
@Composable
fun Mascot(
    mood: MascotMood,
    width: Dp,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
) {
    OverflowingImage(
        drawable = mood.drawable,
        exportHeightRatio = 840f / 640f,
        contentDescription = contentDescription,
        modifier = modifier.size(width = width, height = width * (200f / 160f)),
    )
}

/**
 * Lays out an exported illustration at its artboard size while letting the baked-in shadow
 * overflow at the bottom, like the Figma instance does.
 */
@Composable
private fun OverflowingImage(
    @DrawableRes drawable: Int,
    exportHeightRatio: Float,
    contentDescription: String?,
    modifier: Modifier,
) {
    Box(modifier) {
        Image(
            painter = painterResource(drawable),
            contentDescription = contentDescription,
            contentScale = ContentScale.FillBounds,
            modifier = Modifier
                .wrapContentHeight(align = Alignment.Top, unbounded = true)
                .fillMaxWidth()
                .aspectRatio(1f / exportHeightRatio),
        )
    }
}

enum class Backdrop(@param:DrawableRes val drawable: Int) {
    /** Home hero sky — blue gradient fading to page grey at ~78%, three soft clouds. */
    Sky(R.drawable.backdrop_sky),
    /** Neutral page backdrop: page grey + one faint blue glow behind the header. */
    Aurora(R.drawable.backdrop_aurora),
}

/**
 * Full-screen backdrop. The 402×874 artwork is scaled to the screen width and anchored at the
 * top; the remaining height is filled with `bg/page`, which is where both artworks fade to.
 */
@Composable
fun BackdropBox(
    backdrop: Backdrop,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit = {},
) {
    Box(modifier.fillMaxSize().background(RecentrTheme.colors.bgPage)) {
        Image(
            painter = painterResource(backdrop.drawable),
            contentDescription = null,
            contentScale = ContentScale.FillWidth,
            alignment = Alignment.TopCenter,
            modifier = Modifier.fillMaxWidth().aspectRatio(402f / 874f),
        )
        content()
    }
}

/** Demo app icons from the Figma "App Icon" component (32px artboard, 9px corner radius). */
enum class DemoApp(val displayName: String, @param:DrawableRes val drawable: Int) {
    Instagram("Instagram", R.drawable.app_instagram),
    X("X", R.drawable.app_x),
    TikTok("TikTok", R.drawable.app_tiktok),
    Spotify("Spotify", R.drawable.app_spotify),
    Roblox("Roblox", R.drawable.app_roblox),
    Gmail("Gmail", R.drawable.app_gmail),
    Facebook("Facebook", R.drawable.app_facebook),
    YouTube("YouTube", R.drawable.app_youtube),
}

@Composable
fun AppIcon(
    app: DemoApp,
    modifier: Modifier = Modifier,
    size: Dp = 32.dp,
    contentDescription: String? = app.displayName,
) {
    Image(
        painter = painterResource(app.drawable),
        contentDescription = contentDescription,
        modifier = modifier.size(size),
    )
}
