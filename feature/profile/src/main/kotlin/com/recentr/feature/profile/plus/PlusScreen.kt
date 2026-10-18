package com.recentr.feature.profile.plus

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.recentr.core.designsystem.component.RecentrAlertDialog
import com.recentr.core.designsystem.component.RecentrButton
import com.recentr.core.designsystem.component.RecentrButtonStyle
import com.recentr.core.designsystem.component.RecentrChip
import com.recentr.core.designsystem.component.RecentrChipStyle
import com.recentr.core.designsystem.component.RecentrIconButton
import com.recentr.core.designsystem.component.RecentrIconButtonStyle
import com.recentr.core.designsystem.icon.RecentrIcon
import com.recentr.core.designsystem.illustration.Backdrop
import com.recentr.core.designsystem.illustration.BackdropBox
import com.recentr.core.designsystem.illustration.Mascot
import com.recentr.core.designsystem.illustration.MascotMood
import com.recentr.core.designsystem.illustration.Sticker
import com.recentr.core.designsystem.illustration.StickerImage
import com.recentr.core.designsystem.theme.RecentrElevation
import com.recentr.core.designsystem.theme.RecentrTheme
import com.recentr.core.designsystem.theme.recentrShadow
import com.recentr.core.designsystem.util.SystemBarsAppearance
import com.recentr.core.model.PlusPlan
import com.recentr.feature.profile.R

/** 64 · Profile — Recentr Plus: benefits, yearly / monthly plans and the free-trial CTA. */
@Composable
internal fun PlusScreen(
    onClose: () -> Unit,
    onStartTrial: (PlusPlan) -> Unit,
    modifier: Modifier = Modifier,
) {
    SystemBarsAppearance(lightIcons = true)
    val colors = RecentrTheme.colors
    var plan by rememberSaveable { mutableStateOf(PlusPlan.Yearly) }
    var restoreDialog by rememberSaveable { mutableStateOf(false) }
    BackdropBox(Backdrop.Sky, modifier) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(Modifier.fillMaxWidth()) {
                Mascot(MascotMood.Calm, width = 112.dp, modifier = Modifier.align(Alignment.TopCenter).padding(top = 22.dp))
                RecentrIconButton(
                    icon = RecentrIcon.X,
                    contentDescription = stringResource(R.string.recap_close),
                    onClick = onClose,
                    style = RecentrIconButtonStyle.OnSky,
                    modifier = Modifier.align(Alignment.TopEnd).padding(top = 8.dp),
                )
            }
            Spacer(Modifier.height(10.dp))
            Text(stringResource(R.string.plus_title), style = RecentrTheme.typography.displayLarge, color = colors.textOnDark)
            Spacer(Modifier.height(4.dp))
            Text(stringResource(R.string.plus_subtitle), style = RecentrTheme.typography.bodyMedium, color = colors.textOnDarkMuted)
            Spacer(Modifier.height(22.dp))
            Benefits()
            Spacer(Modifier.height(38.dp))
            Row(Modifier.fillMaxWidth().selectableGroup(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                PlanCard(
                    name = stringResource(R.string.plus_yearly),
                    price = stringResource(R.string.plus_yearly_price),
                    caption = stringResource(R.string.plus_yearly_per_month),
                    badge = stringResource(R.string.plus_yearly_discount),
                    selected = plan == PlusPlan.Yearly,
                    onClick = { plan = PlusPlan.Yearly },
                    modifier = Modifier.weight(1f),
                )
                PlanCard(
                    name = stringResource(R.string.plus_monthly),
                    price = stringResource(R.string.plus_monthly_price),
                    caption = stringResource(R.string.plus_billed_monthly),
                    badge = null,
                    selected = plan == PlusPlan.Monthly,
                    onClick = { plan = PlusPlan.Monthly },
                    modifier = Modifier.weight(1f),
                )
            }
            Spacer(Modifier.height(29.dp))
            RecentrButton(
                text = stringResource(R.string.plus_start_trial),
                onClick = { onStartTrial(plan) },
                style = RecentrButtonStyle.Brand,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(20.dp))
            Row {
                Text(stringResource(R.string.plus_cancel_anytime), style = RecentrTheme.typography.captionMedium, color = colors.textTertiary)
                Text(
                    stringResource(R.string.plus_restore),
                    style = RecentrTheme.typography.captionMedium,
                    color = colors.textTertiary,
                    modifier = Modifier.clickable(role = Role.Button) { restoreDialog = true },
                )
            }
            Spacer(Modifier.height(42.dp))
        }
    }
    if (restoreDialog) {
        RecentrAlertDialog(
            title = stringResource(R.string.plus_restore_none_title),
            body = stringResource(R.string.plus_restore_none_body),
            primaryText = stringResource(R.string.dialog_ok),
            onPrimary = { restoreDialog = false },
            onDismissRequest = { restoreDialog = false },
        )
    }
}

/** White "Benefits" card: four sticker rows. */
@Composable
private fun Benefits() {
    val shape = RoundedCornerShape(26.dp)
    Column(
        Modifier
            .fillMaxWidth()
            .recentrShadow(RecentrElevation.float, shape)
            .clip(shape)
            .background(RecentrTheme.colors.bgSurface)
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        listOf(
            Sticker.Lock to R.string.plus_benefit_rules,
            Sticker.Timer to R.string.plus_benefit_strict,
            Sticker.Leaf to R.string.plus_benefit_habits,
            Sticker.Note to R.string.plus_benefit_recaps,
        ).forEach { (sticker, label) ->
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                StickerImage(sticker, size = 36.dp)
                Text(stringResource(label), style = RecentrTheme.typography.bodyMedium, color = RecentrTheme.colors.textPrimary)
            }
        }
    }
}

/** "Plan · Yearly" / "Plan · Monthly": sky 12% + 2dp sky outline when selected. */
@Composable
private fun PlanCard(
    name: String,
    price: String,
    caption: String,
    badge: String?,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = RecentrTheme.colors
    val shape = RoundedCornerShape(22.dp)
    val border = if (selected) 2.dp else 1.dp
    Column(
        modifier = modifier
            .clip(shape)
            .background(if (selected) colors.alphaSky12 else colors.bgSurface)
            .border(border, if (selected) colors.brandSky else colors.borderSubtle, shape)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .padding(horizontal = 16.dp + border, vertical = 14.dp + border),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(name, style = RecentrTheme.typography.headline, color = colors.textPrimary, modifier = Modifier.weight(1f), maxLines = 1)
            if (badge != null) RecentrChip(label = badge, style = RecentrChipStyle.Success)
        }
        Text(price, style = RecentrTheme.typography.title2, color = colors.textPrimary, maxLines = 1)
        Text(caption, style = RecentrTheme.typography.captionRegular, color = colors.textSecondary, maxLines = 1)
    }
}
