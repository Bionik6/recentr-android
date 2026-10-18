package com.recentr.feature.insights.achievements

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.recentr.core.designsystem.component.RecentrButton
import com.recentr.core.designsystem.component.RecentrButtonStyle
import com.recentr.core.designsystem.icon.RecentrIcon
import com.recentr.core.designsystem.illustration.Sticker
import com.recentr.core.designsystem.illustration.StickerImage
import com.recentr.core.designsystem.theme.RecentrTheme
import com.recentr.core.designsystem.util.SystemBarsAppearance
import com.recentr.core.model.Achievement
import com.recentr.feature.insights.R
import com.recentr.feature.insights.ui.IconChip
import com.recentr.feature.insights.ui.copy
import com.recentr.feature.insights.ui.shortDate
import java.time.LocalDate
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Figma 106 background: #3D8FFF → #1452D4, top to bottom. */
private val UnlockedBackground = Brush.verticalGradient(listOf(Color(0xFF3D8FFF), Color(0xFF1452D4)))

/** A star of the "Art" around the badge: centre in the 300×280 art, size and tilt. */
private data class ArtStar(val centerX: Dp, val centerY: Dp, val size: Dp, val rotation: Float)

private val ArtStars = listOf(
    ArtStar(32.73.dp, 49.67.dp, 54.dp, -14f),
    ArtStar(255.4.dp, 33.72.dp, 40.dp, 12f),
    ArtStar(276.65.dp, 218.65.dp, 46.dp, -10f),
    ArtStar(20.91.dp, 231.42.dp, 34.dp, 18f),
)

/**
 * 106 · Achievements — Unlocked: the badge sticker among four stars on the deep sky, "New badge"
 * (or the day it was earned when opened from the grid), its celebration line, "Nice!" and
 * "Share badge". The sticker pops in, then the stars.
 */
@Composable
internal fun BadgeUnlockedScreen(
    achievement: Achievement,
    earnedOn: LocalDate?,
    fresh: Boolean,
    onNice: () -> Unit,
    onShare: () -> Unit,
    modifier: Modifier = Modifier,
) {
    SystemBarsAppearance(lightIcons = true)
    val colors = RecentrTheme.colors
    val copy = achievement.copy
    val pop = remember { Animatable(0.6f) }
    val stars = remember { ArtStars.map { Animatable(0f) } }
    LaunchedEffect(Unit) {
        launch { pop.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)) }
        stars.forEach { star ->
            delay(90)
            launch { star.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy)) }
        }
    }
    Box(modifier.fillMaxSize().background(UnlockedBackground)) {
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
            Column(
                Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Spacer(Modifier.height(72.dp))
                Box(Modifier.size(width = 300.dp, height = 280.dp)) {
                    // Figma "Glow": 360dp white radial glow at 60%, 10dp under the art's centre.
                    Box(
                        Modifier
                            .align(Alignment.Center)
                            .offset(y = 10.dp)
                            .requiredSize(360.dp)
                            .graphicsLayer { alpha = 0.6f }
                            .clip(CircleShape)
                            .background(Brush.radialGradient(listOf(Color.White.copy(alpha = 0.75f), Color.White.copy(alpha = 0f)))),
                    )
                    StickerImage(
                        copy.sticker,
                        size = 200.dp,
                        modifier = Modifier
                            .offset(x = 50.dp, y = 40.dp)
                            .graphicsLayer {
                                scaleX = pop.value
                                scaleY = pop.value
                            },
                    )
                    ArtStars.forEachIndexed { index, star ->
                        StickerImage(
                            Sticker.Star,
                            size = star.size,
                            modifier = Modifier
                                .offset(x = star.centerX - star.size / 2, y = star.centerY - star.size / 2)
                                .graphicsLayer {
                                    val value = stars[index].value
                                    alpha = value.coerceIn(0f, 1f)
                                    scaleX = value
                                    scaleY = value
                                    rotationZ = star.rotation
                                },
                        )
                    }
                }
                Spacer(Modifier.height(26.dp))
                IconChip(
                    label = if (fresh || earnedOn == null) {
                        stringResource(R.string.achievements_new_badge)
                    } else {
                        stringResource(R.string.achievements_earned_on, shortDate(earnedOn))
                    },
                    icon = RecentrIcon.Sparkle,
                    iconTint = colors.textOnDark,
                    background = colors.alphaWhite24,
                    contentColor = colors.textOnDark,
                )
                Spacer(Modifier.height(8.dp))
                Text(stringResource(copy.title), style = RecentrTheme.typography.displayLarge, color = colors.textOnDark, textAlign = TextAlign.Center)
                Spacer(Modifier.height(8.dp))
                Text(stringResource(copy.cheer), style = RecentrTheme.typography.bodyRegular, color = colors.textOnDarkMuted, textAlign = TextAlign.Center)
                Spacer(Modifier.height(24.dp))
            }
            Column(Modifier.padding(start = 24.dp, end = 24.dp, bottom = 14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                RecentrButton(text = stringResource(R.string.achievements_nice), onClick = onNice, modifier = Modifier.fillMaxWidth())
                RecentrButton(
                    text = stringResource(R.string.achievements_share_badge),
                    onClick = onShare,
                    style = RecentrButtonStyle.Secondary,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Preview(widthDp = 402, heightDp = 874)
@Composable
private fun BadgeUnlockedPreview() {
    RecentrTheme {
        BadgeUnlockedScreen(Achievement.NightOwlRetired, LocalDate.now(), fresh = true, onNice = {}, onShare = {})
    }
}
