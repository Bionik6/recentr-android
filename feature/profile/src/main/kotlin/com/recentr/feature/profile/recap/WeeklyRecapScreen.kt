package com.recentr.feature.profile.recap

import android.content.Intent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.recentr.core.designsystem.component.MiniTile
import com.recentr.core.designsystem.component.RecentrButton
import com.recentr.core.designsystem.component.RecentrIconButton
import com.recentr.core.designsystem.component.RecentrIconButtonStyle
import com.recentr.core.designsystem.icon.RecentrIcon
import com.recentr.core.designsystem.illustration.Backdrop
import com.recentr.core.designsystem.illustration.BackdropBox
import com.recentr.core.designsystem.illustration.Mascot
import com.recentr.core.designsystem.illustration.MascotMood
import com.recentr.core.designsystem.illustration.Sticker
import com.recentr.core.designsystem.theme.RecentrShapes
import com.recentr.core.designsystem.theme.RecentrTheme
import com.recentr.core.designsystem.util.SystemBarsAppearance
import com.recentr.feature.profile.R
import com.recentr.feature.profile.ui.spacedDuration
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.absoluteValue

/** Milliseconds each story segment takes to fill (headline, then one highlight per segment). */
private const val SEGMENT_MILLIS = 1_100

@Composable
internal fun WeeklyRecapDestination(onClose: () -> Unit) {
    val viewModel: WeeklyRecapViewModel = viewModel(factory = WeeklyRecapViewModel.Factory)
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val shareText = state.recap?.let { recap ->
        stringResource(
            R.string.recap_share_text,
            percentText(recap.changePercent),
            spacedDuration(recap.savedMinutes),
            recap.bestDay?.getDisplayName(TextStyle.FULL, Locale.getDefault()).orEmpty(),
        )
    }
    WeeklyRecapScreen(
        state = state,
        onClose = onClose,
        onShare = {
            if (shareText != null) {
                val send = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, shareText)
                context.startActivity(Intent.createChooser(send, null))
            }
        },
    )
}

/** 63 · Profile — Weekly recap: a story-style card over the sky. */
@Composable
internal fun WeeklyRecapScreen(
    state: WeeklyRecapUiState,
    onClose: () -> Unit,
    onShare: () -> Unit,
    modifier: Modifier = Modifier,
) {
    SystemBarsAppearance(lightIcons = true)
    val colors = RecentrTheme.colors
    val recap = state.recap
    // The four story segments fill one after the other; each reveals its part of the recap.
    val progress = remember { Animatable(if (state.reduceMotion) 4f else 0f) }
    LaunchedEffect(recap != null, state.reduceMotion) {
        if (recap == null) return@LaunchedEffect
        if (state.reduceMotion) progress.snapTo(4f) else progress.animateTo(4f, tween(4 * SEGMENT_MILLIS, easing = LinearEasing))
    }
    fun revealed(segment: Int) = animatedAlpha(progress.value, segment)

    BackdropBox(Backdrop.Sky, modifier) {
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
            StoryProgress(progress.value, Modifier.padding(start = 16.dp, end = 16.dp, top = 4.dp))
            Row(
                Modifier.fillMaxWidth().padding(start = 24.dp, end = 24.dp, top = 14.dp).height(40.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    stringResource(R.string.recap_wordmark),
                    style = RecentrTheme.typography.brandWordmark,
                    color = colors.textOnDark,
                    modifier = Modifier.weight(1f),
                )
                RecentrIconButton(
                    icon = RecentrIcon.X,
                    contentDescription = stringResource(R.string.recap_close),
                    onClick = onClose,
                    style = RecentrIconButtonStyle.OnSky,
                )
            }
            Column(
                Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Spacer(Modifier.height(34.dp))
                Column(
                    Modifier.graphicsLayer { alpha = revealed(0) },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Text(
                        if (state.name.isBlank()) stringResource(R.string.recap_over) else stringResource(R.string.recap_over_name, state.name),
                        style = RecentrTheme.typography.bodyMedium,
                        color = colors.textOnDarkMuted,
                    )
                    Text(
                        recap?.let { percentText(it.changePercent) } ?: "–",
                        style = RecentrTheme.typography.displayNumber,
                        color = colors.textOnDark,
                        maxLines = 1,
                    )
                    Text(
                        stringResource(
                            when {
                                recap == null || recap.changePercent < 0 -> R.string.recap_less
                                recap.changePercent > 0 -> R.string.recap_more
                                else -> R.string.recap_same
                            },
                        ),
                        style = RecentrTheme.typography.title2,
                        color = colors.textOnDark,
                        textAlign = TextAlign.Center,
                    )
                }
                Spacer(Modifier.height(114.dp))
                Highlights(state, revealed = { revealed(it) })
                Spacer(Modifier.height(24.dp))
            }
            RecentrButton(
                text = stringResource(R.string.recap_share),
                onClick = onShare,
                enabled = recap != null,
                modifier = Modifier.fillMaxWidth().padding(start = 24.dp, end = 24.dp, bottom = 14.dp),
            )
        }
    }
}

private fun animatedAlpha(progress: Float, segment: Int): Float = ((progress - segment) * 3f).coerceIn(0f, 1f)

/** "−38%" / "+12%" with a typographic minus like the design. */
private fun percentText(percent: Int): String = when {
    percent < 0 -> "−${percent.absoluteValue}%"
    percent > 0 -> "+$percent%"
    else -> "0%"
}

/** Four 4dp story bars: full white when done, alpha white 24% otherwise. */
@Composable
private fun StoryProgress(progress: Float, modifier: Modifier = Modifier) {
    val colors = RecentrTheme.colors
    Row(modifier.fillMaxWidth().height(4.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        repeat(4) { index ->
            val fill = (progress - index).coerceIn(0f, 1f)
            Box(
                Modifier
                    .weight(1f)
                    .height(4.dp)
                    .clip(RecentrShapes.full)
                    .background(colors.alphaWhite24)
                    .drawBehind {
                        drawRect(colors.textOnDark, size = Size(size.width * fill, size.height))
                    },
            )
        }
    }
}

/**
 * Figma "Highlights" (354×260): three tilted mini tiles and the mascot. Positions are the Figma
 * bounding-box centres, scaled to the width.
 */
@Composable
private fun Highlights(state: WeeklyRecapUiState, revealed: (Int) -> Float) {
    val recap = state.recap
    BoxWithConstraints(Modifier.fillMaxWidth().height(260.dp)) {
        val scale = maxWidth / 354.dp
        val tileWidth = 171.dp * scale.coerceAtMost(1f)
        fun Modifier.place(centerX: Dp, centerY: Dp, height: Dp) = offset(x = centerX * scale - tileWidth / 2, y = centerY - height / 2)
        TiltedTile(
            label = stringResource(R.string.recap_saved),
            value = recap?.let { spacedDuration(it.savedMinutes) } ?: "–",
            sticker = Sticker.Timer,
            rotation = -4f,
            width = tileWidth,
            alpha = revealed(1),
            modifier = Modifier.place(87.6.dp, 37.5.dp, 67.2.dp),
        )
        TiltedTile(
            label = stringResource(R.string.recap_best_day),
            value = recap?.bestDay?.getDisplayName(TextStyle.FULL, Locale.getDefault()) ?: "–",
            sticker = Sticker.Leaf,
            rotation = 4f,
            width = tileWidth,
            alpha = revealed(2),
            modifier = Modifier.place(262.9.dp, 39.5.dp, 67.2.dp),
        )
        TiltedTile(
            label = stringResource(R.string.recap_streak),
            value = recap?.let { pluralStringResource(R.plurals.profile_days, it.streakDays, it.streakDays) } ?: "–",
            sticker = Sticker.Flame,
            rotation = 2f,
            width = tileWidth,
            alpha = revealed(3),
            modifier = Modifier.place(104.3.dp, 136.6.dp, 67.2.dp),
        )
        Mascot(
            mood = if (recap != null && recap.changePercent > 0) MascotMood.Impatient else MascotMood.Calm,
            width = 112.dp,
            modifier = Modifier
                .offset(x = 286.dp * scale - 56.dp, y = 90.dp)
                .graphicsLayer { alpha = revealed(3) },
        )
    }
}

@Composable
private fun TiltedTile(
    label: String,
    value: String,
    sticker: Sticker,
    rotation: Float,
    width: Dp,
    alpha: Float,
    modifier: Modifier = Modifier,
) {
    MiniTile(
        label = label,
        value = value,
        sticker = sticker,
        modifier = modifier
            .width(width)
            .graphicsLayer {
                rotationZ = rotation
                this.alpha = alpha
            },
    )
}
