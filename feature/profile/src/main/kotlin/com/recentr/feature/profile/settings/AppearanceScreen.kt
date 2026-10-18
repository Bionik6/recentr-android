package com.recentr.feature.profile.settings

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.recentr.core.designsystem.component.SettingToggleRow
import com.recentr.core.designsystem.component.SettingsGroup
import com.recentr.core.designsystem.component.SettingsSectionLabel
import com.recentr.core.designsystem.icon.RecentrIcon
import com.recentr.core.designsystem.illustration.BrandIconImage
import com.recentr.core.designsystem.theme.RecentrElevation
import com.recentr.core.designsystem.theme.RecentrTheme
import com.recentr.core.designsystem.theme.recentrShadow
import com.recentr.core.model.AppIconStyle
import com.recentr.core.model.AppSettings
import com.recentr.core.model.ThemeMode
import com.recentr.feature.profile.R
import com.recentr.feature.profile.ui.SettingsPage
import com.recentr.feature.profile.ui.brandIcon
import com.recentr.feature.profile.ui.label

private val PreviewLight = Color(0xFFF6F7FB)
private val PreviewDark = Color(0xFF0B1020)
private val PreviewDarkCard = Color(0xFF1A2136)

/** 68 · Settings — Appearance: theme, app icon, mascot and motion toggles. */
@Composable
internal fun AppearanceScreen(
    settings: AppSettings,
    onBack: () -> Unit,
    onUpdate: ((AppSettings) -> AppSettings) -> Unit,
    modifier: Modifier = Modifier,
) {
    SettingsPage(title = stringResource(R.string.appearance_title), onBack = onBack, modifier = modifier) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SettingsSectionLabel(stringResource(R.string.appearance_theme))
            Row(Modifier.fillMaxWidth().selectableGroup(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ThemeMode.entries.forEach { mode ->
                    ThemeCard(
                        mode = mode,
                        selected = settings.theme == mode,
                        onClick = { onUpdate { it.copy(theme = mode) } },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
            Spacer(Modifier.height(14.dp))
            SettingsSectionLabel(stringResource(R.string.appearance_app_icon))
            Row(Modifier.fillMaxWidth().selectableGroup(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                AppIconStyle.entries.forEach { icon ->
                    AppIconChoice(
                        icon = icon,
                        selected = settings.appIcon == icon,
                        onClick = { onUpdate { it.copy(appIcon = icon) } },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
            Spacer(Modifier.height(14.dp))
            SettingsGroup {
                SettingToggleRow(
                    label = stringResource(R.string.appearance_mascot),
                    checked = settings.showMascotOnHome,
                    onCheckedChange = { checked -> onUpdate { it.copy(showMascotOnHome = checked) } },
                    icon = RecentrIcon.Sparkle,
                    modifier = Modifier.fillMaxWidth(),
                )
                SettingToggleRow(
                    label = stringResource(R.string.appearance_reduce_motion),
                    checked = settings.reduceMotion,
                    onCheckedChange = { checked -> onUpdate { it.copy(reduceMotion = checked) } },
                    icon = RecentrIcon.Refresh,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

/** "Theme · Light / Dark / Auto": mini phone preview + label, sky outline when selected. */
@Composable
private fun ThemeCard(mode: ThemeMode, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = RecentrTheme.colors
    val shape = RoundedCornerShape(22.dp)
    val border = if (selected) 2.dp else 1.dp
    Column(
        modifier = modifier
            .clip(shape)
            .background(if (selected) colors.alphaSky12 else colors.bgSurface)
            .border(border, if (selected) colors.brandSky else colors.borderSubtle, shape)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .padding(vertical = 12.dp + border),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        val previewShape = RoundedCornerShape(14.dp)
        Canvas(
            Modifier
                .size(width = 64.dp, height = 110.dp)
                .clip(previewShape)
                .border(1.dp, colors.borderSubtle, previewShape),
        ) {
            drawThemePreview(mode, colors.brandSky)
        }
        Text(
            stringResource(mode.label),
            style = RecentrTheme.typography.captionMedium,
            color = if (selected) colors.brandSky else colors.textSecondary,
        )
    }
}

/** Figma "Preview": page, three cards and the sky CTA bar (Auto = light / dark halves). */
private fun DrawScope.drawThemePreview(mode: ThemeMode, sky: Color) {
    val dp = density
    drawRect(if (mode == ThemeMode.Dark) PreviewDark else PreviewLight)
    if (mode == ThemeMode.Auto) drawRect(PreviewDark, topLeft = Offset(32 * dp, 0f), size = Size(32 * dp, size.height))
    val card = if (mode == ThemeMode.Light) Color.White else PreviewDarkCard
    fun bar(top: Float, height: Float, color: Color, radius: Float) = drawRoundRect(
        color = color,
        topLeft = Offset(8 * dp, (top + 1) * dp),
        size = Size(48 * dp, height * dp),
        cornerRadius = CornerRadius(radius * dp),
    )
    bar(13f, 10f, card, 5f)
    bar(29f, 34f, card, 5f)
    bar(69f, 16f, card, 5f)
    bar(91f, 6f, sky, 3f)
}

/** "Icon · Sky…": 70dp brand icon, 3dp sky ring outside the tile when selected. */
@Composable
private fun AppIconChoice(icon: AppIconStyle, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = RecentrTheme.colors
    val shape = RoundedCornerShape(18.dp)
    Column(
        modifier = modifier.selectable(selected = selected, role = Role.RadioButton, onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(
            Modifier
                .size(70.dp)
                .recentrShadow(if (selected) RecentrElevation.control else RecentrElevation.card, shape)
                .drawWithContent {
                    drawContent()
                    if (selected) {
                        val stroke = 3.dp.toPx()
                        drawRoundRect(
                            color = colors.brandSky,
                            topLeft = Offset(-stroke / 2, -stroke / 2),
                            size = Size(size.width + stroke, size.height + stroke),
                            cornerRadius = CornerRadius(18.dp.toPx() + stroke / 2),
                            style = Stroke(stroke),
                        )
                    }
                },
        ) {
            BrandIconImage(icon.brandIcon, size = 70.dp)
        }
        Text(
            stringResource(icon.label),
            style = RecentrTheme.typography.captionMedium,
            color = if (selected) colors.brandSky else colors.textSecondary,
        )
    }
}
