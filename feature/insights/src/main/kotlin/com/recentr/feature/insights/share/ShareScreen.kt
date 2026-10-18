package com.recentr.feature.insights.share

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.recentr.core.designsystem.component.RecentrButton
import com.recentr.core.designsystem.component.RecentrButtonStyle
import com.recentr.core.designsystem.component.RecentrChip
import com.recentr.core.designsystem.component.RecentrChipStyle
import com.recentr.core.designsystem.icon.RecentrIcon
import com.recentr.core.designsystem.icon.RecentrIconImage
import com.recentr.core.designsystem.illustration.AppIcon
import com.recentr.core.designsystem.illustration.DemoApp
import com.recentr.core.designsystem.illustration.LogoMark
import com.recentr.core.designsystem.illustration.Mascot
import com.recentr.core.designsystem.illustration.MascotMood
import com.recentr.core.designsystem.illustration.StickerImage
import com.recentr.core.designsystem.motion.entrance
import com.recentr.core.designsystem.theme.Figtree
import com.recentr.core.designsystem.theme.RecentrElevation
import com.recentr.core.designsystem.theme.RecentrTheme
import com.recentr.core.designsystem.theme.recentrShadow
import com.recentr.core.designsystem.util.SystemBarsAppearance
import com.recentr.core.model.Achievement
import com.recentr.core.model.CalmLevel
import com.recentr.feature.insights.R
import com.recentr.feature.insights.report.MonthlyReport
import com.recentr.feature.insights.report.wonBackText
import com.recentr.feature.insights.ui.InsightsTopBar
import com.recentr.feature.insights.ui.copy
import com.recentr.feature.insights.ui.monthName
import com.recentr.feature.insights.ui.name
import com.recentr.feature.insights.ui.signedDuration
import java.time.YearMonth
import kotlinx.coroutines.launch

/** What the share card celebrates. */
@Immutable
internal sealed interface ShareSubject {
    data class Month(val report: MonthlyReport) : ShareSubject
    data class Badge(val achievement: Achievement, val level: CalmLevel) : ShareSubject
}

/** Figma "Share card": #3D8FFF → #1452D4, 32dp corners. */
private val CardBackground = Brush.verticalGradient(listOf(Color(0xFF3D8FFF), Color(0xFF1452D4)))
private val CardShape = RoundedCornerShape(32.dp)
private val CardShadow = Shadow(radius = 30.dp, color = Color(0x4D143399), offset = DpOffset(0.dp, 16.dp))

/** Figma "Logo/Lockup · On blue" wordmark: Figtree Black 37.6, −5% tracking. */
private val LockupWordmark = TextStyle(fontFamily = Figtree, fontWeight = FontWeight.Black, fontSize = 37.6.sp, lineHeight = 37.6.sp, letterSpacing = (-0.05).em)

/**
 * 110 · Achievements — Share: the tilted share card (month report or badge), the share targets
 * and "Done". The card is rendered to an image for the targets.
 */
@Composable
internal fun ShareScreen(subject: ShareSubject?, onClose: () -> Unit, modifier: Modifier = Modifier) {
    SystemBarsAppearance(lightIcons = false)
    val colors = RecentrTheme.colors
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val cardLayer = rememberGraphicsLayer()
    val link = stringResource(R.string.share_link)
    val shareText = when (subject) {
        is ShareSubject.Month -> stringResource(R.string.share_text_month, wonBackText(subject.report.wonBackMinutes), monthName(subject.report.month), link)
        is ShareSubject.Badge -> stringResource(R.string.share_text_badge, stringResource(subject.achievement.copy.title), link)
        null -> link
    }
    fun send(target: ShareTarget) {
        if (subject == null) return
        scope.launch {
            val bitmap = cardLayer.toImageBitmap().asAndroidBitmap()
            Sharing.send(context, target, bitmap, shareText)
        }
    }

    Column(modifier.fillMaxSize().background(colors.bgSunken).statusBarsPadding().navigationBarsPadding()) {
        InsightsTopBar(
            title = stringResource(R.string.share_title),
            onBack = onClose,
            backIcon = RecentrIcon.X,
            modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 12.dp),
        )
        Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
            if (subject != null) {
                Column(
                    Modifier
                        // Rises in (iOS `.entrance(from: (0, 40))`); outside the layer recorded for sharing.
                        .entrance(from = DpOffset(0.dp, 40.dp), blur = false)
                        .graphicsLayer { rotationZ = 3f }
                        .width(300.dp)
                        .recentrShadow(CardShadow, CardShape)
                        .drawWithContent {
                            cardLayer.record { this@drawWithContent.drawContent() }
                            drawLayer(cardLayer)
                        }
                        .clip(CardShape)
                        .background(CardBackground)
                        .padding(start = 24.dp, end = 24.dp, top = 26.dp, bottom = 22.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    ShareCardContent(subject)
                }
            }
        }
        Row(Modifier.fillMaxWidth().padding(horizontal = 24.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            TargetButton(stringResource(R.string.share_stories), onClick = { send(ShareTarget.Stories) }) { AppIcon(DemoApp.Instagram, size = 56.dp, contentDescription = null) }
            TargetButton(stringResource(R.string.share_post), onClick = { send(ShareTarget.Post) }) { AppIcon(DemoApp.X, size = 56.dp, contentDescription = null) }
            TargetButton(stringResource(R.string.share_facebook), onClick = { send(ShareTarget.Facebook) }) { AppIcon(DemoApp.Facebook, size = 56.dp, contentDescription = null) }
            TargetButton(stringResource(R.string.share_copy_link), onClick = { send(ShareTarget.CopyLink) }) { RoundIcon(RecentrIcon.Note) }
            TargetButton(stringResource(R.string.share_save), onClick = { send(ShareTarget.Save) }) { RoundIcon(RecentrIcon.ChevronDown) }
        }
        Spacer(Modifier.height(26.dp))
        RecentrButton(
            text = stringResource(R.string.share_done),
            onClick = onClose,
            style = RecentrButtonStyle.Secondary,
            modifier = Modifier.fillMaxWidth().padding(start = 24.dp, end = 24.dp, bottom = 14.dp),
        )
    }
}

@Composable
private fun ShareCardContent(subject: ShareSubject) {
    val colors = RecentrTheme.colors
    when (subject) {
        is ShareSubject.Month -> {
            val report = subject.report
            Mascot(MascotMood.Calm, width = 96.dp)
            Text(stringResource(R.string.share_won_back), style = RecentrTheme.typography.headline, color = colors.textOnDarkMuted)
            Text(wonBackText(report.wonBackMinutes), style = RecentrTheme.typography.displayLarge, color = colors.textOnDark, maxLines = 1)
            Text(stringResource(R.string.share_in_month, monthName(report.month)), style = RecentrTheme.typography.headline, color = colors.textOnDarkMuted)
            val chips = listOfNotNull(
                report.bestStreak.takeIf { it > 0 }?.let { stringResource(R.string.share_streak_chip, it) },
                report.dailyAverageChange?.takeIf { it < 0 }?.let { stringResource(R.string.share_daily_chip, signedDuration(it)) },
            )
            if (chips.isNotEmpty()) {
                Row(Modifier.padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    chips.forEach { RecentrChip(it, style = RecentrChipStyle.OnSky) }
                }
            }
        }
        is ShareSubject.Badge -> {
            StickerImage(subject.achievement.copy.sticker, size = 120.dp)
            Text(stringResource(R.string.share_earned), style = RecentrTheme.typography.headline, color = colors.textOnDarkMuted)
            Text(
                stringResource(subject.achievement.copy.title),
                style = RecentrTheme.typography.displayLarge,
                color = colors.textOnDark,
                textAlign = TextAlign.Center,
            )
            Text(stringResource(R.string.share_with_recentr), style = RecentrTheme.typography.headline, color = colors.textOnDarkMuted)
            Row(Modifier.padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                RecentrChip("${stringResource(R.string.share_level_chip, subject.level.level)} · ${subject.level.name()}", style = RecentrChipStyle.OnSky)
            }
        }
    }
    Row(horizontalArrangement = Arrangement.spacedBy(6.266.dp), verticalAlignment = Alignment.CenterVertically) {
        LogoMark(width = 34.465.dp)
        Text(stringResource(R.string.share_wordmark), style = LockupWordmark, color = Color.White, maxLines = 1)
    }
}

@Composable
private fun TargetButton(label: String, onClick: () -> Unit, icon: @Composable () -> Unit) {
    Column(
        // Labels are wider than the 56dp icons: keep them clear of the ripple's rounded corners.
        Modifier.clip(RoundedCornerShape(16.dp)).clickable(role = Role.Button, onClick = onClick).padding(horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        icon()
        Text(label, style = RecentrTheme.typography.captionMedium, color = RecentrTheme.colors.textSecondary, maxLines = 1)
    }
}

/** 56dp white disc with a 24dp icon (Figma "Copy link", "Save"). */
@Composable
private fun RoundIcon(icon: RecentrIcon) {
    Box(
        Modifier
            .size(56.dp)
            .recentrShadow(RecentrElevation.card, CircleShape)
            .clip(CircleShape)
            .background(RecentrTheme.colors.bgSurface),
        contentAlignment = Alignment.Center,
    ) {
        RecentrIconImage(icon, tint = RecentrTheme.colors.textPrimary)
    }
}

@Preview(widthDp = 402, heightDp = 874)
@Composable
private fun ShareMonthPreview() {
    RecentrTheme {
        ShareScreen(
            subject = ShareSubject.Month(MonthlyReport(YearMonth.of(2026, 10), 31 * 60, 130, -48, 38, -12, 12, true)),
            onClose = {},
        )
    }
}
