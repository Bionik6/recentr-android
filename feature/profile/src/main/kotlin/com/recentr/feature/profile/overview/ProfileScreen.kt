package com.recentr.feature.profile.overview

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.recentr.core.designsystem.component.MiniTile
import com.recentr.core.designsystem.component.RecentrAvatar
import com.recentr.core.designsystem.component.RecentrIconButton
import com.recentr.core.designsystem.icon.RecentrIcon
import com.recentr.core.designsystem.icon.RecentrIconImage
import com.recentr.core.designsystem.illustration.Backdrop
import com.recentr.core.designsystem.illustration.BackdropBox
import com.recentr.core.designsystem.illustration.Sticker
import com.recentr.core.designsystem.illustration.StickerImage
import com.recentr.core.designsystem.theme.RecentrElevation
import com.recentr.core.designsystem.theme.RecentrShapes
import com.recentr.core.designsystem.theme.RecentrTheme
import com.recentr.core.designsystem.theme.recentrShadow
import com.recentr.core.designsystem.util.SystemBarsAppearance
import com.recentr.feature.profile.R
import com.recentr.feature.profile.ui.compactDuration
import com.recentr.feature.profile.ui.style

/**
 * 61 · Profile — Overview: identity, before / now cards, the life-back banner, two tiles and the
 * weekly recap entry. Drawn under the floating tab bar ([contentPadding] bottom).
 */
@Composable
internal fun ProfileScreen(
    state: ProfileUiState,
    contentPadding: PaddingValues,
    onEdit: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenRecap: () -> Unit,
    modifier: Modifier = Modifier,
) {
    SystemBarsAppearance(lightIcons = false)
    val colors = RecentrTheme.colors
    BackdropBox(Backdrop.Aurora, modifier) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .statusBarsPadding()
                .padding(contentPadding)
                .padding(horizontal = RecentrTheme.spacing.screenGutter),
        ) {
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Top) {
                Text(
                    stringResource(R.string.profile_title),
                    style = RecentrTheme.typography.displayLarge,
                    color = colors.textPrimary,
                    modifier = Modifier.weight(1f).semantics { heading() },
                )
                RecentrIconButton(icon = RecentrIcon.Pencil, contentDescription = stringResource(R.string.profile_edit), onClick = onEdit)
                RecentrIconButton(icon = RecentrIcon.Settings, contentDescription = stringResource(R.string.profile_open_settings), onClick = onOpenSettings)
            }
            Spacer(Modifier.height(22.dp))
            Identity(state, onEdit = onEdit)
            Spacer(Modifier.height(26.dp))
            BeforeNowCards(state)
            Spacer(Modifier.height(38.dp))
            LifeBackBanner(state.monthsWonBack)
            Spacer(Modifier.height(2.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MiniTile(
                    label = stringResource(R.string.profile_best_streak),
                    value = pluralStringResource(R.plurals.profile_days, state.streakDays, state.streakDays),
                    sticker = Sticker.Flame,
                    modifier = Modifier.weight(1f),
                )
                MiniTile(
                    label = stringResource(R.string.profile_urges),
                    value = state.urgesResisted.toString(),
                    sticker = Sticker.Lock,
                    modifier = Modifier.weight(1f),
                )
            }
            Spacer(Modifier.height(17.dp))
            RecapRow(onClick = onOpenRecap)
            Spacer(Modifier.height(24.dp))
        }
    }
}

/** Avatar 68 · name + "Recentr-ing for N days" chip (Figma "Identity"). */
@Composable
private fun Identity(state: ProfileUiState, onEdit: () -> Unit) {
    val colors = RecentrTheme.colors
    Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
        RecentrAvatar(initial = state.initial, style = state.avatar.style)
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            if (state.name.isBlank()) {
                // No name yet (onboarding skipped): invite to set one.
                Text(
                    stringResource(R.string.profile_add_name),
                    style = RecentrTheme.typography.title1,
                    color = colors.textTertiary,
                    maxLines = 1,
                    modifier = Modifier.clickable(role = Role.Button, onClick = onEdit),
                )
            } else {
                Text(
                    state.name,
                    style = RecentrTheme.typography.title1,
                    color = colors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Row(
                Modifier
                    .clip(RecentrShapes.full)
                    .background(colors.bgSunken)
                    .padding(start = 10.dp, end = 12.dp, top = 6.dp, bottom = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RecentrIconImage(RecentrIcon.Flame, tint = colors.accentTangerine, size = 16.08.dp)
                Text(
                    pluralStringResource(R.plurals.profile_member_days, state.memberDays, state.memberDays),
                    style = RecentrTheme.typography.captionMedium,
                    color = colors.textSecondary,
                    maxLines = 1,
                )
            }
        }
    }
}

/**
 * The tilted "Before" (−4°) and "Now" (+3°) cards (Figma "Now / Before", 354×150). Positions are
 * the Figma bounding-box centres, scaled to the available width.
 */
@Composable
private fun BeforeNowCards(state: ProfileUiState) {
    val colors = RecentrTheme.colors
    BoxWithConstraints(Modifier.fillMaxWidth().height(150.dp)) {
        val scale = maxWidth / 354.dp
        val cardWidth = 170.dp * scale.coerceAtMost(1f)
        TiltedStatCard(
            label = stringResource(R.string.profile_before),
            labelColor = colors.feedbackDanger,
            value = compactDuration(state.beforeMinutes),
            caption = stringResource(R.string.profile_daily_average),
            rotation = -4f,
            width = cardWidth,
            modifier = Modifier.offset(x = 92.6.dp * scale - cardWidth / 2, y = 10.dp),
        )
        val now = state.nowMinutes
        TiltedStatCard(
            label = stringResource(R.string.profile_now),
            labelColor = colors.brandSky,
            value = when {
                now != null -> compactDuration(now)
                state.nowSynced -> "–"
                else -> stringResource(R.string.profile_now_loading)
            },
            caption = if (state.nowDays <= 1) {
                stringResource(R.string.profile_today)
            } else {
                pluralStringResource(R.plurals.profile_last_days, state.nowDays, state.nowDays)
            },
            rotation = 3f,
            width = cardWidth,
            modifier = Modifier.offset(x = 258.dp * scale - cardWidth / 2, y = 8.5.dp),
        )
    }
}

@Composable
private fun TiltedStatCard(
    label: String,
    labelColor: Color,
    value: String,
    caption: String,
    rotation: Float,
    width: Dp,
    modifier: Modifier = Modifier,
) {
    val colors = RecentrTheme.colors
    val shape = RoundedCornerShape(26.dp)
    Column(
        modifier = modifier
            .width(width)
            .graphicsLayer { rotationZ = rotation }
            .recentrShadow(RecentrElevation.float, shape)
            .clip(shape)
            .background(colors.bgSurface)
            .padding(horizontal = 18.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(label, style = RecentrTheme.typography.headline, color = labelColor, maxLines = 1)
        Text(value, style = RecentrTheme.typography.title1, color = colors.textPrimary, maxLines = 1)
        Text(caption, style = RecentrTheme.typography.captionRegular, color = colors.textTertiary, maxLines = 1)
    }
}

/** "At this pace · You’re winning back 4 years of your life" (sky 12% banner). */
@Composable
private fun LifeBackBanner(monthsWonBack: Int?) {
    val colors = RecentrTheme.colors
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(colors.alphaSky12)
            .padding(start = 16.dp, end = 18.dp, top = 16.dp, bottom = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        StickerImage(Sticker.Leaf, size = 55.2.dp)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(stringResource(R.string.profile_life_over), style = RecentrTheme.typography.captionMedium, color = colors.brandSky)
            val years = (monthsWonBack ?: 0) / 12
            Text(
                when {
                    monthsWonBack == null || monthsWonBack <= 0 -> stringResource(R.string.profile_life_none)
                    years >= 1 -> pluralStringResource(R.plurals.profile_life_years, years, years)
                    else -> pluralStringResource(R.plurals.profile_life_months, monthsWonBack, monthsWonBack)
                },
                style = RecentrTheme.typography.headline,
                color = colors.textPrimary,
            )
        }
    }
}

/** "✦ Your weekly recap is ready ›" card. */
@Composable
private fun RecapRow(onClick: () -> Unit) {
    val colors = RecentrTheme.colors
    val shape = RoundedCornerShape(22.dp)
    Row(
        Modifier
            .fillMaxWidth()
            .recentrShadow(RecentrElevation.card, shape)
            .clip(shape)
            .background(colors.bgSurface)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(start = 16.dp, end = 14.dp, top = 14.dp, bottom = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RecentrIconImage(RecentrIcon.Sparkle, tint = colors.brandSky)
        Text(
            stringResource(R.string.profile_recap_ready),
            style = RecentrTheme.typography.bodyMedium,
            color = colors.textPrimary,
            modifier = Modifier.weight(1f),
        )
        RecentrIconImage(RecentrIcon.ChevronRight, tint = colors.textTertiary)
    }
}

@Preview
@Composable
private fun ProfileScreenPreview() {
    RecentrTheme {
        Box(Modifier.background(RecentrTheme.colors.bgPage)) {
            ProfileScreen(
                state = ProfileUiState(name = "Mia", initial = "M", memberDays = 12, nowMinutes = 130, nowDays = 5, streakDays = 5, urgesResisted = 38),
                contentPadding = PaddingValues(bottom = 94.dp),
                onEdit = {},
                onOpenSettings = {},
                onOpenRecap = {},
            )
        }
    }
}
