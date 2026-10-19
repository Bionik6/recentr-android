package com.recentr.feature.profile.settings

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.print.PrintHelper
import com.recentr.core.data.UnlockPass
import com.recentr.core.designsystem.component.RecentrButton
import com.recentr.core.designsystem.component.RecentrButtonStyle
import com.recentr.core.designsystem.component.RecentrIconButton
import com.recentr.core.designsystem.component.RecentrSheet
import com.recentr.core.designsystem.component.SettingRow
import com.recentr.core.designsystem.component.SettingsGroup
import com.recentr.core.designsystem.icon.RecentrIcon
import com.recentr.core.designsystem.illustration.Backdrop
import com.recentr.core.designsystem.illustration.BackdropBox
import com.recentr.core.designsystem.motion.entrance
import com.recentr.core.designsystem.theme.RecentrElevation
import com.recentr.core.designsystem.theme.RecentrTheme
import com.recentr.core.designsystem.theme.recentrShadow
import com.recentr.core.designsystem.util.SystemBarsAppearance
import com.recentr.core.model.Rule
import com.recentr.feature.profile.R
import com.recentr.feature.profile.ui.SheetContent
import com.recentr.feature.profile.ui.SheetHeading
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** 53 · Your unlock pass: [rule]'s QR code to print (or share) and keep far away. */
@Composable
internal fun UnlockPassScreen(rule: Rule, onBack: () -> Unit) {
    val context = LocalContext.current
    val colors = RecentrTheme.colors
    val scope = rememberCoroutineScope()
    val modules = remember(rule.id) { UnlockPass.modules(rule) }
    val code = remember(rule.id) { UnlockPass.code(rule) }
    val logo = painterResource(RecentrIcon.Hourglass.drawable)
    val textMeasurer = rememberTextMeasurer()
    val caption = stringResource(R.string.unlock_pass_caption, code)
    val document = stringResource(R.string.unlock_pass_document)
    val captionStyle = RecentrTheme.typography.captionMedium.copy(color = colors.textTertiary)
    fun pass(): Bitmap = passBitmap(modules, logo, colors.brandSky, textMeasurer, caption, captionStyle).asAndroidBitmap()

    SystemBarsAppearance(lightIcons = false)
    BackdropBox(Backdrop.Aurora) {
        Column(
            Modifier.statusBarsPadding().navigationBarsPadding().padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Column(
                Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth()) {
                    RecentrIconButton(icon = RecentrIcon.ChevronLeft, contentDescription = stringResource(R.string.back), onClick = onBack)
                }
                Spacer(Modifier.height(18.dp))
                Text(
                    stringResource(R.string.unlock_pass_screen_title),
                    style = RecentrTheme.typography.title1,
                    color = colors.textPrimary,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(13.dp))
                Text(
                    stringResource(R.string.unlock_pass_screen_body),
                    style = RecentrTheme.typography.bodyRegular,
                    color = colors.textSecondary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 17.dp),
                )
                Spacer(Modifier.height(42.dp))
                val shape = RoundedCornerShape(32.dp)
                Column(
                    Modifier
                        .entrance(100, blur = false)
                        .offset(x = (-8).dp)
                        .rotate(3f)
                        .recentrShadow(RecentrElevation.float, shape)
                        .clip(shape)
                        .background(colors.bgSurface)
                        .padding(start = 24.dp, end = 24.dp, top = 24.dp, bottom = 22.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Canvas(Modifier.size(210.dp)) { drawQr(modules, size.width, logo, colors.brandSky) }
                    Text(caption, style = captionStyle)
                }
                Spacer(Modifier.height(24.dp))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                RecentrButton(
                    text = stringResource(R.string.unlock_pass_share),
                    onClick = { scope.launch { share(context, pass(), document) } },
                    style = RecentrButtonStyle.Soft,
                    modifier = Modifier.weight(1f),
                )
                RecentrButton(
                    text = stringResource(R.string.unlock_pass_print),
                    onClick = {
                        PrintHelper(context).apply { scaleMode = PrintHelper.SCALE_MODE_FIT }.printBitmap(document, pass())
                    },
                    modifier = Modifier.weight(1f),
                )
            }
            Spacer(Modifier.height(14.dp))
        }
    }
}

/** Settings › Unlock pass with no rule, or several rules, asking for a pass: which one to print. */
@Composable
internal fun UnlockPassSheet(rules: List<Rule>, onOpen: (ruleId: String) -> Unit, onDismissRequest: () -> Unit) {
    RecentrSheet(onDismissRequest = onDismissRequest) { controller ->
        SheetContent(topGap = 16.dp) {
            SheetHeading(
                stringResource(R.string.unlock_pass_title),
                stringResource(if (rules.isEmpty()) R.string.unlock_pass_none else R.string.unlock_pass_body),
            )
            if (rules.isNotEmpty()) {
                Spacer(Modifier.height(22.dp))
                SettingsGroup {
                    rules.forEach { rule ->
                        SettingRow(
                            label = rule.name,
                            value = stringResource(R.string.unlock_pass_code, UnlockPass.code(rule)),
                            icon = RecentrIcon.Qr,
                            onClick = { controller.close { onOpen(rule.id) } },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }
        }
    }
}

/**
 * Draws a pass's QR [modules] in a [side]-wide square like the Figma pass: rounded dots, rounded
 * finder squares and the hourglass on a white tile at the centre.
 */
private fun DrawScope.drawQr(modules: List<BooleanArray>, side: Float, logo: Painter, logoTint: Color) {
    val count = modules.size
    val pitch = side / count
    val ink = Color(0xFF0E1220)
    val paper = Color.White
    val tile = side * (44f / 210f)
    val tileStart = (side - tile) / 2f
    fun inFinder(x: Int, y: Int) = (x < 7 && y < 7) || (x >= count - 7 && y < 7) || (x < 7 && y >= count - 7)
    fun underTile(x: Int, y: Int): Boolean {
        val cx = (x + 0.5f) * pitch
        val cy = (y + 0.5f) * pitch
        return cx > tileStart - pitch / 2 && cx < tileStart + tile + pitch / 2 && cy > tileStart - pitch / 2 && cy < tileStart + tile + pitch / 2
    }
    modules.forEachIndexed { y, row ->
        row.forEachIndexed { x, dark ->
            if (dark && !inFinder(x, y) && !underTile(x, y)) {
                drawRoundRect(
                    ink,
                    topLeft = Offset(x * pitch + pitch * 0.05f, y * pitch + pitch * 0.05f),
                    size = Size(pitch * 0.9f, pitch * 0.9f),
                    cornerRadius = CornerRadius(pitch * 0.3f),
                )
            }
        }
    }
    listOf(Offset.Zero, Offset((count - 7) * pitch, 0f), Offset(0f, (count - 7) * pitch)).forEach { origin ->
        drawRoundRect(ink, origin, Size(7 * pitch, 7 * pitch), CornerRadius(1.4f * pitch))
        drawRoundRect(paper, origin + Offset(pitch, pitch), Size(5 * pitch, 5 * pitch), CornerRadius(pitch))
        drawRoundRect(ink, origin + Offset(2 * pitch, 2 * pitch), Size(3 * pitch, 3 * pitch), CornerRadius(0.6f * pitch))
    }
    drawRoundRect(paper, Offset(tileStart, tileStart), Size(tile, tile), CornerRadius(side * (12f / 210f)))
    val icon = side * (24f / 210f)
    translate((side - icon) / 2f, (side - icon) / 2f) {
        with(logo) { draw(Size(icon, icon), colorFilter = ColorFilter.tint(logoTint)) }
    }
}

/** The pass as an image to print or share: the QR code and its caption on white. */
private fun passBitmap(
    modules: List<BooleanArray>,
    logo: Painter,
    logoTint: Color,
    textMeasurer: TextMeasurer,
    caption: String,
    captionStyle: TextStyle,
): ImageBitmap {
    val width = 1_200
    val margin = 120f
    val side = width - 2 * margin
    val text = textMeasurer.measure(caption, captionStyle.copy(fontSize = 44.sp, lineHeight = 60.sp), density = Density(1f))
    val height = (margin + side + 48f + text.size.height + margin).toInt()
    val bitmap = ImageBitmap(width, height)
    CanvasDrawScope().draw(Density(1f), LayoutDirection.Ltr, Canvas(bitmap), Size(width.toFloat(), height.toFloat())) {
        drawRect(Color.White)
        translate(margin, margin) { drawQr(modules, side, logo, logoTint) }
        drawText(text, topLeft = Offset((width - text.size.width) / 2f, margin + side + 48f))
    }
    return bitmap
}

private suspend fun share(context: Context, pass: Bitmap, title: String) {
    val uri = withContext(Dispatchers.IO) {
        val directory = File(context.cacheDir, "passes").apply { mkdirs() }
        val file = File(directory, "recentr-pass.png")
        file.outputStream().use { pass.compress(Bitmap.CompressFormat.PNG, 100, it) }
        FileProvider.getUriForFile(context, "${context.packageName}.profile.passes", file)
    }
    val send = Intent(Intent.ACTION_SEND)
        .setType("image/png")
        .putExtra(Intent.EXTRA_STREAM, uri)
        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    send.clipData = ClipData.newRawUri(null, uri)
    context.startActivity(Intent.createChooser(send, title))
}

/** The pass images' FileProvider (cache/passes). Its own class: the manifest allows one entry a class. */
class UnlockPassFileProvider : FileProvider()
