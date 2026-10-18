package com.recentr.core.designsystem.illustration

import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.AdaptiveIconDrawable
import android.util.LruCache
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.recentr.core.designsystem.theme.RecentrTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

/**
 * Launcher icon of an installed app, drawn like [AppIcon] (rounded square, 9/32 corner radius):
 * adaptive icons are rendered full-bleed from their layers, legacy icons as they are. Loaded off
 * the main thread and cached; a sunken placeholder holds the slot meanwhile (or if the package
 * is not visible).
 */
@Composable
fun InstalledAppIcon(
    packageName: String,
    modifier: Modifier = Modifier,
    size: Dp = 32.dp,
    contentDescription: String? = null,
) {
    val context = LocalContext.current
    val sizePx = with(LocalDensity.current) { size.roundToPx() }
    val shape = RoundedCornerShape(size * (9f / 32f))
    val icon by produceState(InstalledAppIcons.cached(packageName, sizePx), packageName, sizePx) {
        if (value == null) value = withContext(Dispatchers.IO) { InstalledAppIcons.load(context, packageName, sizePx) }
    }
    val described = if (contentDescription != null) {
        Modifier.semantics { this.contentDescription = contentDescription }
    } else {
        Modifier
    }
    val iconModifier = modifier.size(size).then(described)
    val image = icon
    when {
        image == null -> Box(iconModifier.clip(shape).background(RecentrTheme.colors.bgSunken))
        image.adaptive -> Image(image.bitmap, contentDescription = null, modifier = iconModifier.clip(shape))
        else -> Image(image.bitmap, contentDescription = null, modifier = iconModifier)
    }
}

private class LoadedIcon(val bitmap: ImageBitmap, val adaptive: Boolean)

private object InstalledAppIcons {
    private val cache = LruCache<String, LoadedIcon>(64)

    fun cached(packageName: String, sizePx: Int): LoadedIcon? = cache.get("$packageName@$sizePx")

    fun load(context: Context, packageName: String, sizePx: Int): LoadedIcon? {
        val drawable = try {
            context.packageManager.getApplicationIcon(packageName)
        } catch (_: PackageManager.NameNotFoundException) {
            return null
        }
        val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val adaptive = drawable is AdaptiveIconDrawable
        if (drawable is AdaptiveIconDrawable) {
            // Layers are 108dp with the 72dp safe zone in the middle: draw them 1.5× and centred.
            val inset = (sizePx * 0.25f).roundToInt()
            listOfNotNull(drawable.background, drawable.foreground).forEach { layer ->
                layer.setBounds(-inset, -inset, sizePx + inset, sizePx + inset)
                layer.draw(canvas)
            }
        } else {
            drawable.setBounds(0, 0, sizePx, sizePx)
            drawable.draw(canvas)
        }
        return LoadedIcon(bitmap.asImageBitmap(), adaptive).also { cache.put("$packageName@$sizePx", it) }
    }
}
