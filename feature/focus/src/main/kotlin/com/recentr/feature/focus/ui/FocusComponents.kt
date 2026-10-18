package com.recentr.feature.focus.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.recentr.core.designsystem.theme.RecentrElevation
import com.recentr.core.designsystem.theme.RecentrTheme
import com.recentr.core.designsystem.theme.recentrShadow

/**
 * Bar of the focus screens (Figma "Header", 42dp): [leading] button, a centred two-line title,
 * then [trailing] (or nothing — the title then centres in the remaining width, like the design).
 */
@Composable
internal fun FocusTopBar(
    title: String,
    subtitle: String?,
    titleColor: Color,
    subtitleColor: Color,
    modifier: Modifier = Modifier,
    leading: @Composable () -> Unit,
    trailing: (@Composable () -> Unit)? = null,
) {
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        leading()
        Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp)) {
            if (title.isNotEmpty()) {
                Text(title, style = RecentrTheme.typography.headline, color = titleColor, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            if (subtitle != null) {
                Text(subtitle, style = RecentrTheme.typography.captionRegular, color = subtitleColor, maxLines = 1)
            }
        }
        trailing?.invoke()
    }
}

/** White 24dp card of stacked rows separated by 1dp subtle dividers (Figma "Settings", "Sessions"). */
@Composable
internal fun FocusCard(
    modifier: Modifier = Modifier,
    horizontalPadding: Dp = 16.dp,
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = RoundedCornerShape(24.dp)
    Column(
        modifier
            .fillMaxWidth()
            .recentrShadow(RecentrElevation.card, shape)
            .clip(shape)
            .background(RecentrTheme.colors.bgSurface)
            .padding(horizontal = horizontalPadding, vertical = 4.dp),
        content = content,
    )
}

@Composable
internal fun CardDivider() {
    Box(Modifier.fillMaxWidth().height(1.dp).background(RecentrTheme.colors.borderSubtle))
}

/** Sheet body under the design-system drag handle (21dp): [topGap] is the Figma gap minus 6dp. */
@Composable
internal fun FocusSheetContent(
    topGap: Dp,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(start = 24.dp, end = 24.dp, top = (topGap - 6.dp).coerceAtLeast(0.dp), bottom = 10.dp),
        content = content,
    )
}

/** Title/2 + Body/Regular heading of the focus sheets ("Focus sounds", "Rounds"). */
@Composable
internal fun FocusSheetHeading(title: String, subtitle: String) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(title, style = RecentrTheme.typography.title2, color = RecentrTheme.colors.textPrimary)
        Text(subtitle, style = RecentrTheme.typography.bodyRegular, color = RecentrTheme.colors.textSecondary)
    }
}

/** Design-system styled dropdown for the "more" buttons. */
@Composable
internal fun FocusMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    items: List<Pair<String, () -> Unit>>,
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismissRequest,
        shape = RoundedCornerShape(16.dp),
        containerColor = RecentrTheme.colors.bgSurface,
    ) {
        items.forEach { (label, action) ->
            DropdownMenuItem(
                text = { Text(label, style = RecentrTheme.typography.bodyMedium) },
                onClick = {
                    onDismissRequest()
                    action()
                },
                colors = MenuDefaults.itemColors(textColor = RecentrTheme.colors.textPrimary),
            )
        }
    }
}

/** Row content helper: weight-1 label in Body/Medium. */
@Composable
internal fun RowScope.RowLabel(text: String, color: Color = RecentrTheme.colors.textPrimary) {
    Text(text, style = RecentrTheme.typography.bodyMedium, color = color, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
}
