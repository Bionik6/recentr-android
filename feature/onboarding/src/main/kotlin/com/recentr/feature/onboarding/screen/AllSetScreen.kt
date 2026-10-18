package com.recentr.feature.onboarding.screen

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import com.recentr.core.designsystem.component.MiniTile
import com.recentr.core.designsystem.component.RecentrButton
import com.recentr.core.designsystem.component.RecentrButtonStyle
import com.recentr.core.designsystem.component.StickyNote
import com.recentr.core.designsystem.haptics.SensoryFeedback
import com.recentr.core.designsystem.icon.RecentrIcon
import com.recentr.core.designsystem.illustration.Backdrop
import com.recentr.core.designsystem.illustration.Mascot
import com.recentr.core.designsystem.illustration.MascotMood
import com.recentr.core.designsystem.illustration.Sticker
import com.recentr.core.designsystem.motion.Confetti
import com.recentr.core.designsystem.motion.LocalReduceMotion
import com.recentr.core.designsystem.motion.entrance
import com.recentr.core.designsystem.motion.motionSpring
import com.recentr.core.designsystem.motion.rememberAfterDelay
import com.recentr.core.designsystem.theme.RecentrTheme
import com.recentr.feature.onboarding.OnboardingUiState
import com.recentr.feature.onboarding.PainPoint
import com.recentr.feature.onboarding.R
import com.recentr.feature.onboarding.UsageBucket
import com.recentr.feature.onboarding.component.OnboardingHeading
import com.recentr.feature.onboarding.component.OnboardingPage
import com.recentr.feature.onboarding.component.compactDuration
import kotlinx.coroutines.delay

/**
 * 21 · Onboarding — All set (Figma 2024:1511). Name, goal, first rule and reason come from the
 * answers ("Mia", 4h20, Bedtime for "It hurts my sleep", the TikTok note for "I scroll…").
 * A beat after it opens the mascot hops, the note straightens and confetti bursts, with a success tap.
 */
@Composable
internal fun AllSetScreen(
    state: OnboardingUiState,
    onStart: () -> Unit,
) {
    val hasCelebrated by rememberAfterDelay(350)
    SensoryFeedback(trigger = hasCelebrated) { success() }
    val noteTilt by animateFloatAsState(
        targetValue = if (hasCelebrated) 0f else -3f,
        animationSpec = motionSpring(0.8f, 0.4f),
        label = "noteTilt",
    )
    Box(Modifier.fillMaxSize()) {
        OnboardingPage(
            backdrop = Backdrop.Sky,
            bottomBar = {
                RecentrButton(
                    text = stringResource(R.string.onb_all_set_cta),
                    onClick = onStart,
                    modifier = Modifier
                        .fillMaxWidth()
                        .entrance(900, blur = false),
                    style = RecentrButtonStyle.Brand,
                    icon = RecentrIcon.Plus,
                )
            },
        ) {
            Spacer(Modifier.height(56.dp))
            HoppingMascot(hasCelebrated, Modifier.entrance(50, from = DpOffset(0.dp, 60.dp)))
            Spacer(Modifier.height(20.dp))
            OnboardingHeading(
                title = if (state.isNameValid) {
                    stringResource(R.string.onb_all_set_title, state.trimmedName)
                } else {
                    stringResource(R.string.onb_all_set_title_no_name)
                },
                subtitle = stringResource(R.string.onb_all_set_subtitle),
                centered = true,
                spacing = 6.dp,
                delayMillis = 300,
            )
            Spacer(Modifier.height(40.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = RecentrTheme.spacing.screenGutter),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    MiniTile(
                        label = stringResource(R.string.onb_all_set_daily_goal),
                        value = compactDuration(state.dailyGoalMinutes),
                        sticker = Sticker.Timer,
                        modifier = Modifier
                            .weight(1f)
                            .entrance(500, from = DpOffset((-40).dp, 20.dp), blur = false),
                    )
                    val rule = state.firstRule
                    MiniTile(
                        label = stringResource(R.string.onb_all_set_first_rule),
                        value = stringResource(rule.label),
                        sticker = rule.sticker,
                        modifier = Modifier
                            .weight(1f)
                            .entrance(600, from = DpOffset(40.dp, 20.dp), blur = false),
                    )
                }
                StickyNote(
                    title = stringResource(R.string.onb_all_set_note_title),
                    body = stringResource(state.reason),
                    modifier = Modifier
                        .fillMaxWidth()
                        .entrance(750, from = DpOffset(0.dp, 40.dp), blur = false)
                        .graphicsLayer { rotationZ = noteTilt },
                )
            }
            Spacer(Modifier.height(24.dp))
        }
        Confetti(isActive = hasCelebrated, modifier = Modifier.matchParentSize())
    }
}

/** The calm mascot, hopping once when [hasCelebrated] turns true (iOS keyframes: up 36pt, bouncy landing). */
@Composable
private fun HoppingMascot(hasCelebrated: Boolean, modifier: Modifier = Modifier) {
    val hop = remember { Animatable(0f) }
    val reduceMotion = LocalReduceMotion.current
    LaunchedEffect(hasCelebrated) {
        if (!hasCelebrated || reduceMotion) return@LaunchedEffect
        delay(300)
        hop.animateTo(36f, motionSpring(0.25f, 0.15f))
        hop.animateTo(0f, motionSpring(0.5f, 0.3f))
    }
    Mascot(
        mood = MascotMood.Calm,
        width = 176.dp,
        modifier = modifier.graphicsLayer { translationY = -hop.value.dp.toPx() },
    )
}
@Preview(widthDp = 402, heightDp = 874)
@Composable
private fun AllSetScreenPreview() {
    RecentrTheme {
        AllSetScreen(
            state = OnboardingUiState(
                name = "Mia",
                painPoints = setOf(PainPoint.Sleep, PainPoint.Scroll),
                usageBucket = UsageBucket.From5To7,
            ),
            onStart = {},
        )
    }
}
