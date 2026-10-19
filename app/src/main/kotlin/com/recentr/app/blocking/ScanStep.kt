package com.recentr.app.blocking

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.ImageAnalysis
import androidx.camera.view.CameraController
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.recentr.app.R
import com.recentr.core.data.QrReader
import com.recentr.core.data.UnlockPass
import com.recentr.core.designsystem.component.RecentrButton
import com.recentr.core.designsystem.component.RecentrButtonStyle
import com.recentr.core.designsystem.component.RecentrChip
import com.recentr.core.designsystem.component.RecentrChipStyle
import com.recentr.core.designsystem.component.RecentrIconButton
import com.recentr.core.designsystem.component.RecentrIconButtonStyle
import com.recentr.core.designsystem.haptics.SensoryFeedback
import com.recentr.core.designsystem.icon.RecentrIcon
import com.recentr.core.designsystem.motion.rememberPingPong
import com.recentr.core.designsystem.theme.RecentrTheme
import com.recentr.core.designsystem.util.SystemBarsAppearance
import com.recentr.core.model.Rule
import java.util.concurrent.Executors
import kotlinx.coroutines.delay

/**
 * 52 · Scan your Recentr code: the camera reads the rule's printed pass, in a dark room around the
 * viewfinder. The pass's four digits can be typed instead ([onUsePasscode]).
 */
@Composable
internal fun ScanStep(rule: Rule, onClose: () -> Unit, onUsePasscode: () -> Unit, onUnlock: () -> Unit) {
    val context = LocalContext.current
    val colors = RecentrTheme.colors
    var hasCamera by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
    }
    var requests by rememberSaveable { mutableIntStateOf(0) }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        hasCamera = granted
    }
    fun askForCamera() {
        if (requests >= 2) {
            // Asked twice already: Android won't ask again, only its Settings can allow it.
            context.startActivity(
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, "package:${context.packageName}".toUri())
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
        } else {
            requests++
            permission.launch(Manifest.permission.CAMERA)
        }
    }
    LaunchedEffect(Unit) { if (!hasCamera && requests == 0) askForCamera() }

    var wrongPass by remember { mutableStateOf(false) }
    LaunchedEffect(wrongPass) {
        if (wrongPass) {
            delay(2_000)
            wrongPass = false
        }
    }
    SensoryFeedback(wrongPass) { if (wrongPass) error() }
    var unlocked by remember { mutableStateOf(false) }
    val currentOnUnlock by rememberUpdatedState(onUnlock)
    val onScanned: (String) -> Unit = { text ->
        if (!unlocked) {
            if (UnlockPass.opens(rule, text)) {
                unlocked = true
                currentOnUnlock()
            } else {
                wrongPass = true
            }
        }
    }

    SystemBarsAppearance(lightIcons = true)
    Box(Modifier.fillMaxSize().background(ScanRoom)) {
        Column(
            Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Column(
                Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.End) {
                    RecentrIconButton(
                        icon = RecentrIcon.X,
                        contentDescription = stringResource(R.string.unlock_close),
                        onClick = onClose,
                        style = RecentrIconButtonStyle.OnSky,
                    )
                }
                Spacer(Modifier.height(18.dp))
                Text(stringResource(R.string.unlock_scan_title), style = RecentrTheme.typography.title1, color = colors.textOnDark, textAlign = TextAlign.Center)
                Spacer(Modifier.height(7.dp))
                Text(
                    stringResource(R.string.unlock_scan_subtitle),
                    style = RecentrTheme.typography.bodyRegular,
                    color = colors.textOnDarkMuted,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(69.dp))
                Viewfinder(sweeping = hasCamera) {
                    if (hasCamera) {
                        CameraPreview(onScanned, Modifier.fillMaxSize().padding(12.dp).clip(RoundedCornerShape(20.dp)))
                    } else {
                        Text(
                            stringResource(R.string.unlock_scan_no_camera),
                            style = RecentrTheme.typography.captionMedium,
                            color = colors.textOnDarkMuted,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.align(Alignment.Center).padding(40.dp),
                        )
                    }
                }
                Spacer(Modifier.height(30.dp))
                when {
                    !hasCamera -> RecentrChip(
                        stringResource(R.string.unlock_scan_allow_camera),
                        style = RecentrChipStyle.OnSky,
                        icon = RecentrIcon.Qr,
                        onClick = ::askForCamera,
                    )
                    wrongPass -> RecentrChip(stringResource(R.string.unlock_scan_wrong_pass), style = RecentrChipStyle.OnSky, icon = RecentrIcon.X)
                    else -> RecentrChip(stringResource(R.string.unlock_scan_steady), style = RecentrChipStyle.OnSky, icon = RecentrIcon.Qr)
                }
                Spacer(Modifier.height(24.dp))
            }
            RecentrButton(
                text = stringResource(R.string.unlock_scan_use_passcode),
                onClick = onUsePasscode,
                style = RecentrButtonStyle.Secondary,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(14.dp))
        }
    }
}

/** The 260dp viewfinder: its glow, white corners and the scan line sweeping over [content]. */
@Composable
private fun Viewfinder(sweeping: Boolean, content: @Composable BoxScope.() -> Unit) {
    val lineColor = RecentrTheme.colors.brandSkyLight
    val sweep by rememberPingPong(from = 20f, to = 237f, durationMillis = 1_800)
    Box(
        Modifier
            .size(260.dp)
            .drawBehind {
                // Figma: a 420pt sky disc at 25%, blurred 60 — a soft glow wider than the viewfinder.
                val radius = 330.dp.toPx()
                val glowCenter = Offset(size.width / 2f, size.height / 2f + 10.dp.toPx())
                drawCircle(
                    Brush.radialGradient(
                        0f to Glow.copy(alpha = 0.25f),
                        0.45f to Glow.copy(alpha = 0.24f),
                        0.64f to Glow.copy(alpha = 0.125f),
                        1f to Color.Transparent,
                        center = glowCenter,
                        radius = radius,
                    ),
                    radius = radius,
                    center = glowCenter,
                )
            },
    ) {
        content()
        Canvas(Modifier.fillMaxSize()) {
            val stroke = 3.dp.toPx()
            val inset = stroke / 2f
            val arm = 34.dp.toPx()
            val corner = 8.dp.toPx()
            val path = Path()
            val (w, h) = size
            listOf(Offset(inset, inset) to Offset(1f, 1f), Offset(w - inset, inset) to Offset(-1f, 1f), Offset(inset, h - inset) to Offset(1f, -1f), Offset(w - inset, h - inset) to Offset(-1f, -1f))
                .forEach { (point, direction) ->
                    path.moveTo(point.x, point.y + direction.y * arm)
                    path.lineTo(point.x, point.y + direction.y * corner)
                    path.quadraticTo(point.x, point.y, point.x + direction.x * corner, point.y)
                    path.lineTo(point.x + direction.x * arm, point.y)
                }
            drawPath(path, Color.White, style = Stroke(width = stroke, cap = StrokeCap.Round))
        }
        val line = RoundedCornerShape(1.5.dp)
        if (sweeping) Box(
            Modifier
                .offset(x = 20.dp, y = sweep.dp)
                .size(width = 220.dp, height = 3.dp)
                .dropShadow(line, Shadow(radius = 12.dp, color = lineColor))
                .background(lineColor, line),
        )
    }
}

/** The back camera's picture, read for QR codes a frame at a time. */
@Composable
private fun CameraPreview(onScanned: (String) -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val currentOnScanned by rememberUpdatedState(onScanned)
    val controller = remember {
        LifecycleCameraController(context).apply {
            setEnabledUseCases(CameraController.IMAGE_ANALYSIS)
            imageAnalysisBackpressureStrategy = ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST
        }
    }
    DisposableEffect(lifecycleOwner) {
        val analysis = Executors.newSingleThreadExecutor()
        val main = ContextCompat.getMainExecutor(context)
        val reader = QrReader()
        var lastText: String? = null
        var lastAt = 0L
        controller.setImageAnalysisAnalyzer(analysis) { image ->
            try {
                val plane = image.planes[0]
                val buffer = plane.buffer
                val luminance = ByteArray(buffer.remaining()).also(buffer::get)
                val text = reader.read(luminance, image.width, image.height, plane.rowStride)
                val at = System.currentTimeMillis()
                // The same code in frame after frame: hand it over once a second at most.
                if (text != null && (text != lastText || at - lastAt > 1_000)) {
                    lastText = text
                    lastAt = at
                    main.execute { currentOnScanned(text) }
                }
            } finally {
                image.close()
            }
        }
        controller.bindToLifecycle(lifecycleOwner)
        onDispose {
            controller.unbind()
            controller.clearImageAnalysisAnalyzer()
            analysis.shutdown()
        }
    }
    AndroidView(
        factory = {
            PreviewView(it).apply {
                // A TextureView, so the rounded clip applies to the picture.
                implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                scaleType = PreviewView.ScaleType.FILL_CENTER
                this.controller = controller
            }
        },
        modifier = modifier,
    )
}

/** Figma 52: the dark room around the viewfinder, and its sky glow. */
private val ScanRoom = Color(0xFF0A0F21)
private val Glow = Color(0xFF2E7AF5)
