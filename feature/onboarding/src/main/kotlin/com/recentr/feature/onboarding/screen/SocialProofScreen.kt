package com.recentr.feature.onboarding.screen

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.recentr.core.designsystem.component.RecentrButton
import com.recentr.core.designsystem.component.ReviewCard
import com.recentr.core.designsystem.icon.RecentrIcon
import com.recentr.core.designsystem.icon.RecentrIconImage
import com.recentr.core.designsystem.theme.RecentrTheme
import com.recentr.feature.onboarding.R
import com.recentr.feature.onboarding.component.HeaderProgress
import com.recentr.feature.onboarding.component.OnboardingPage
import com.recentr.feature.onboarding.component.figmaLineBox

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
        bottomBar = { RecentrButton(stringResource(R.string.onb_continue), onContinue, Modifier.fillMaxWidth()) },
    ) {
        Spacer(Modifier.height(20.dp))
        Text(
            text = stringResource(R.string.onb_social_title),
            style = RecentrTheme.typography.title1,
            color = colors.textPrimary,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = RecentrTheme.spacing.screenGutter),
        )
        Spacer(Modifier.height(66.dp))
        val ratingDescription = stringResource(R.string.onb_social_rating_description)
        Column(
            modifier = Modifier.clearAndSetSemantics { contentDescription = ratingDescription },
            verticalArrangement = Arrangement.spacedBy(6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = stringResource(R.string.onb_social_score),
                style = RecentrTheme.typography.displayHero,
                color = colors.textPrimary,
                modifier = Modifier.figmaLineBox(72.dp),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                repeat(5) { RecentrIconImage(RecentrIcon.StarFill) }
            }
            Text(stringResource(R.string.onb_social_count), style = RecentrTheme.typography.captionMedium, color = colors.textSecondary)
        }
        // 82dp in Figma to the rotated cards' bounds; the row adds 24dp of padding for their shadows.
        Spacer(Modifier.height(62.dp))
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = RecentrTheme.spacing.screenGutter, vertical = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.Top,
        ) {
            items(Reviews) { review ->
                val name = stringResource(review.name)
                ReviewCard(
                    title = stringResource(review.title),
                    body = stringResource(review.body),
                    name = name,
                    initial = name.take(1),
                    modifier = Modifier
                        .padding(top = if (review.rotation < 0f) 8.7.dp else 0.dp)
                        .rotate(review.rotation)
                        .width(250.dp),
                )
            }
        }
    }
}

@Preview(widthDp = 402, heightDp = 874)
@Composable
private fun SocialProofScreenPreview() {
    RecentrTheme { SocialProofScreen(HeaderProgress(0.2f), onBack = {}, onContinue = {}) }
}
