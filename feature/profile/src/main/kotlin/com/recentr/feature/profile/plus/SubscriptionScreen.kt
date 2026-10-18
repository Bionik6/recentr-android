package com.recentr.feature.profile.plus

import android.content.ActivityNotFoundException
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import com.recentr.core.designsystem.component.RecentrAlertDialog
import com.recentr.core.designsystem.component.SettingRow
import com.recentr.core.designsystem.component.SettingsGroup
import com.recentr.core.designsystem.icon.RecentrIcon
import com.recentr.core.designsystem.illustration.Sticker
import com.recentr.core.designsystem.illustration.StickerImage
import com.recentr.core.designsystem.theme.CssLinearGradient
import com.recentr.core.designsystem.theme.RecentrElevation
import com.recentr.core.designsystem.theme.RecentrTheme
import com.recentr.core.designsystem.theme.recentrShadow
import com.recentr.core.model.AppSettings
import com.recentr.core.model.PlusPlan
import com.recentr.feature.profile.R
import com.recentr.feature.profile.ui.SettingsPage
import com.recentr.feature.profile.ui.label
import com.recentr.feature.profile.ui.price
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/** Plan card gradient (Figma 163.59°, sky light → sky deep). */
private val PlanCardBrush = CssLinearGradient(163.59f, listOf(0f to Color(0xFF7CB8FF), 0.71429f to Color(0xFF1452D4)))

private enum class SubscriptionDialog { ChangePlan, Restore, Cancel }

/** 65 · Profile — Subscription: tilted plan card and the plan settings. */
@Composable
internal fun SubscriptionScreen(
    settings: AppSettings,
    onBack: () -> Unit,
    onSwitchPlan: (PlusPlan) -> Unit,
    onCancelPlan: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val plan = settings.plusPlan ?: PlusPlan.Yearly
    val otherPlan = if (plan == PlusPlan.Yearly) PlusPlan.Monthly else PlusPlan.Yearly
    var dialog by rememberSaveable { mutableStateOf<SubscriptionDialog?>(null) }
    SettingsPage(title = stringResource(R.string.sub_title), onBack = onBack, modifier = modifier) {
        Box(Modifier.fillMaxWidth().height(150.dp), contentAlignment = Alignment.Center) {
            PlanCard(plan = plan, renewsAtMillis = settings.plusRenewsAtMillis)
        }
        SettingsGroup {
            SettingRow(
                label = stringResource(R.string.sub_change_plan),
                value = stringResource(otherPlan.label),
                icon = RecentrIcon.Refresh,
                onClick = { dialog = SubscriptionDialog.ChangePlan },
                modifier = Modifier.fillMaxWidth(),
            )
            SettingRow(
                label = stringResource(R.string.sub_restore),
                value = null,
                icon = RecentrIcon.Check,
                onClick = { dialog = SubscriptionDialog.Restore },
                modifier = Modifier.fillMaxWidth(),
            )
            SettingRow(
                label = stringResource(R.string.sub_invoices),
                value = null,
                icon = RecentrIcon.Note,
                onClick = {
                    // Receipts of Play subscriptions live in the user's Play account.
                    val intent = Intent(Intent.ACTION_VIEW, "https://play.google.com/store/account/subscriptions".toUri())
                    try {
                        context.startActivity(intent)
                    } catch (_: ActivityNotFoundException) {
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            )
            SettingRow(
                label = stringResource(R.string.sub_cancel),
                value = null,
                icon = RecentrIcon.X,
                iconTint = RecentrTheme.colors.feedbackDanger,
                labelColor = RecentrTheme.colors.feedbackDanger,
                onClick = { dialog = SubscriptionDialog.Cancel },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
    when (dialog) {
        SubscriptionDialog.ChangePlan -> RecentrAlertDialog(
            title = stringResource(R.string.sub_change_title, stringResource(otherPlan.label)),
            body = stringResource(R.string.sub_change_body),
            primaryText = stringResource(R.string.sub_change_confirm),
            onPrimary = {
                dialog = null
                onSwitchPlan(otherPlan)
            },
            onDismissRequest = { dialog = null },
            secondaryText = stringResource(R.string.sub_not_now),
        )
        SubscriptionDialog.Restore -> RecentrAlertDialog(
            title = stringResource(R.string.plus_restore_ok_title),
            body = stringResource(R.string.plus_restore_ok_body),
            primaryText = stringResource(R.string.dialog_ok),
            onPrimary = { dialog = null },
            onDismissRequest = { dialog = null },
        )
        SubscriptionDialog.Cancel -> RecentrAlertDialog(
            title = stringResource(R.string.sub_cancel_title),
            body = stringResource(R.string.sub_cancel_body),
            primaryText = stringResource(R.string.sub_keep),
            onPrimary = { dialog = null },
            onDismissRequest = { dialog = null },
            secondaryText = stringResource(R.string.sub_cancel_confirm),
            onSecondary = {
                dialog = null
                onCancelPlan()
            },
        )
        null -> Unit
    }
}

/** Sky-gradient "Plan card", tilted 2° with the Glow/Sky shadow. */
@Composable
private fun PlanCard(plan: PlusPlan, renewsAtMillis: Long?) {
    val colors = RecentrTheme.colors
    val shape = RoundedCornerShape(28.dp)
    Column(
        Modifier
            .fillMaxWidth()
            .graphicsLayer { rotationZ = 2f }
            .recentrShadow(RecentrElevation.glowSky, shape)
            .clip(shape)
            .background(PlanCardBrush)
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.plus_title), style = RecentrTheme.typography.title2, color = colors.textOnDark, modifier = Modifier.weight(1f))
            StickerImage(Sticker.Flame, size = 48.dp)
        }
        Text(
            stringResource(R.string.sub_plan_line, stringResource(plan.label), stringResource(plan.price)),
            style = RecentrTheme.typography.bodyMedium,
            color = colors.textOnDark,
        )
        if (renewsAtMillis != null) {
            val date = Instant.ofEpochMilli(renewsAtMillis).atZone(ZoneId.systemDefault()).toLocalDate()
                .format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM))
            Text(stringResource(R.string.sub_renews, date), style = RecentrTheme.typography.captionRegular, color = colors.textOnDarkMuted)
        }
    }
}
