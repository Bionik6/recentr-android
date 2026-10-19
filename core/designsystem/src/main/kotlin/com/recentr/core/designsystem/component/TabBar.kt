package com.recentr.core.designsystem.component

import androidx.annotation.StringRes
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.max
import com.recentr.core.designsystem.R
import com.recentr.core.designsystem.icon.RecentrIcon
import com.recentr.core.designsystem.icon.RecentrIconImage
import com.recentr.core.designsystem.theme.RecentrElevation
import com.recentr.core.designsystem.theme.RecentrShapes
import com.recentr.core.designsystem.theme.RecentrTheme
import com.recentr.core.designsystem.theme.recentrShadow

/** Top-level destinations of the floating tab bar. */
enum class RecentrTab(
    @param:StringRes val label: Int,
    val icon: RecentrIcon,
    val selectedIcon: RecentrIcon,
) {
    Home(R.string.ds_tab_home, RecentrIcon.Home, RecentrIcon.HomeFill),
    Rules(R.string.ds_tab_rules, RecentrIcon.Lock, RecentrIcon.LockFill),
    /** Focus timer. The UI kit has no filled hourglass: the selected tab keeps the line icon. */
    Timer(R.string.ds_tab_timer, RecentrIcon.Hourglass, RecentrIcon.Hourglass),
    /** Insights overview. No filled chart either: the selected tab keeps the line icon. */
    Insights(R.string.ds_tab_insights, RecentrIcon.Chart, RecentrIcon.Chart),
}

/**
 * Floating glass tab bar (Figma "Tab Bar"). Sits at the bottom of the screen, above the system
 * navigation bar; the active tab gets the selection pill and its filled icon.
 */
@Composable
fun RecentrTabBar(
    selected: RecentrTab,
    onSelect: (RecentrTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = RecentrTheme.colors
    val bottomInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 6.dp, bottom = max(bottomInset, 20.dp)),
        contentAlignment = Alignment.TopCenter,
    ) {
        Row(
            modifier = Modifier
                .recentrShadow(RecentrElevation.float, RecentrShapes.full)
                .clip(RecentrShapes.full)
                .background(colors.alphaWhite80.copy(alpha = 0.92f))
                .border(1.dp, colors.bgSurface, RecentrShapes.full)
                // 6dp padding + the 1dp white stroke Figma counts in the pill size (68dp high).
                .padding(7.dp)
                .selectableGroup(),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            RecentrTab.entries.forEach { tab ->
                val isSelected = tab == selected
                val background by animateColorAsState(
                    if (isSelected) colors.fillSelection else Color.Transparent,
                    label = "tabBackground",
                )
                Column(
                    modifier = Modifier
                        .size(width = 84.dp, height = 54.dp)
                        .clip(RecentrShapes.full)
                        .background(background)
                        .selectable(
                            selected = isSelected,
                            onClick = { onSelect(tab) },
                            role = Role.Tab,
                        ),
                    verticalArrangement = Arrangement.spacedBy(2.dp, Alignment.CenterVertically),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    val content = if (isSelected) colors.textPrimary else colors.textSecondary
                    RecentrIconImage(
                        icon = if (isSelected) tab.selectedIcon else tab.icon,
                        tint = content,
                    )
                    Text(
                        text = stringResource(tab.label),
                        style = RecentrTheme.typography.captionMedium,
                        color = content,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}
