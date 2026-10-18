package com.recentr.feature.onboarding.screen

import androidx.annotation.StringRes
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseOut
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import com.recentr.core.designsystem.component.RecentrButton
import com.recentr.core.designsystem.component.ReviewCard
import com.recentr.core.designsystem.haptics.SensoryFeedback
import com.recentr.core.designsystem.icon.RecentrIcon
import com.recentr.core.designsystem.icon.RecentrIconImage
import com.recentr.core.designsystem.motion.LocalReduceMotion
import com.recentr.core.designsystem.motion.MotionSpecs
import com.recentr.core.designsystem.motion.entrance
import com.recentr.core.designsystem.motion.rememberMotionClock
import com.recentr.core.designsystem.theme.RecentrTheme
import com.recentr.feature.onboarding.R
import com.recentr.feature.onboarding.component.HeaderProgress
import com.recentr.feature.onboarding.component.OnboardingHeading
import com.recentr.feature.onboarding.component.OnboardingPage
import com.recentr.feature.onboarding.component.figmaLineBox
import java.util.Locale
import kotlinx.coroutines.delay

private data class Review(
    @param:StringRes val title: Int,
    @param:StringRes val body: Int,
    @param:StringRes val name: Int,
    val rotation: Float,
)

private val Reviews = listOf(
    Review(R.string.onb_review1_title, R.string.onb_review1_body, R.string.onb_review1_name, rotation = 2f),
    Review(R.string.onb_review2_title, R.string.onb_review2_body, R.string.onb_review2_name, rotation = -2f),
    Review(R.string.onb_review3_title, R.string.onb_review3_body, R.string.onb_review3_name, rotation = 0f),
)

/** 07 · Onboarding — Social proof (Figma 2023:743). */
@Composable
internal fun SocialProofScreen(
    progress: HeaderProgress,
    onBack: () -> Unit,
    onContinue: () -> Unit,
) {
    val colors = RecentrTheme.colors
    OnboardingPage(
        progress = progress,
        onBack = onBack,
        bottomBar = {
            RecentrButton(stringResource(R.string.onb_continue), onContinue, Modifier.fillMaxWidth().entrance(500))
        },
    ) {
        Spacer(Modifier.height(20.dp))
        OnboardingHeading(title = stringResource(R.string.onb_social_title), centered = true)
        Spacer(Modifier.height(66.dp))
        val ratingDescription = stringResource(R.string.onb_social_rating_description)
        Column(
            modifier = Modifier.clearAndSetSemantics { contentDescription = ratingDescription },
            verticalArrangement = Arrangement.spacedBy(6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            CountingScore(stringResource(R.string.onb_social_score))
            LightingStars()
            Text(stringResource(R.string.onb_social_count), style = RecentrTheme.typography.captionMedium, color = colors.textSecondary)
        }
        // 82dp in Figma to the rotated cards' bounds; the row adds 24dp of padding for their shadows.
        Spacer(Modifier.height(62.dp))
        ReviewsMarquee(Modifier.entrance(900, from = DpOffset(120.dp, 0.dp), blur = false))
    }
}

/** The score ticks up from 0.0 (`.easeOut(duration: 1.2).delay(0.3)`). */
@Composable
private fun CountingScore(score: String) {
    val target = score.replace(',', '.').toFloatOrNull() ?: 0f
    val separator = if (',' in score) ',' else '.'
    val reduceMotion = LocalReduceMotion.current
    val value = remember { Animatable(if (reduceMotion) target else 0f) }
    LaunchedEffect(target) {
        delay(300)
        value.animateTo(target, tween(1200, easing = EaseOut))
    }
    Text(
        text = String.format(Locale.US, "%.1f", value.value).replace('.', separator),
        style = RecentrTheme.typography.displayHero.copy(fontFeatureSettings = "tnum"),
        color = RecentrTheme.colors.textPrimary,
        modifier = Modifier.figmaLineBox(72.dp),
    )
}

/** Five stars popping in one after the other, with a tick for each. */
@Composable
private fun LightingStars() {
    var lit by remember { mutableIntStateOf(0) }
    val reduceMotion = LocalReduceMotion.current
    LaunchedEffect(Unit) {
        for (star in 1..5) {
            delay(if (star == 1) 900 else 110)
            lit = star
        }
    }
    SensoryFeedback(trigger = lit) { selection() }
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        repeat(5) { index ->
            val isLit = index < lit
            val pop by animateFloatAsState(
                targetValue = if (isLit) 1f else 0f,
                animationSpec = if (reduceMotion) MotionSpecs.smooth(0.3f) else MotionSpecs.pop(),
                label = "star",
            )
            RecentrIconImage(
                RecentrIcon.StarFill,
                modifier = Modifier.graphicsLayer {
                    val scale = if (reduceMotion) 1f else 0.2f + 0.8f * pop
                    scaleX = scale
                    scaleY = scale
                    alpha = pop.coerceIn(0f, 1f)
                },
            )
        }
    }
}

/** Reviews drifting slowly sideways (22dp a second), on a loop; still when motion is reduced. */
@Composable
private fun ReviewsMarquee(modifier: Modifier = Modifier) {
    val clock = rememberMotionClock()
    val gutter = RecentrTheme.spacing.screenGutter
    val loopWidth = (ReviewWidth + ReviewSpacing) * Reviews.size
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clipToBounds(),
    ) {
        Row(
            modifier = Modifier
                .wrapContentWidth(Alignment.Start, unbounded = true)
                .graphicsLayer {
                    val loop = loopWidth.toPx()
                    translationX = gutter.toPx() - (clock.value * 22.dp.toPx()) % loop
                }
                .padding(vertical = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(ReviewSpacing),
            verticalAlignment = Alignment.Top,
        ) {
            repeat(2) {
                Reviews.forEach { review ->
                    val name = stringResource(review.name)
                    ReviewCard(
                        title = stringResource(review.title),
                        body = stringResource(review.body),
                        name = name,
                        initial = name.take(1),
                        modifier = Modifier
                            .padding(top = if (review.rotation < 0f) 8.7.dp else 0.dp)
                            .rotate(review.rotation)
                            .width(ReviewWidth),
                    )
                }
            }
        }
    }
}

private val ReviewWidth = 250.dp
private val ReviewSpacing = 14.dp

@Preview(widthDp = 402, heightDp = 874)
@Composable
private fun SocialProofScreenPreview() {
    RecentrTheme { SocialProofScreen(HeaderProgress(0.2f), onBack = {}, onContinue = {}) }
}
