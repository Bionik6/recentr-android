package com.recentr.feature.home.ui.components

import androidx.annotation.StringRes
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.recentr.core.designsystem.component.RecentrChip
import com.recentr.core.designsystem.component.RecentrChipStyle
import com.recentr.core.designsystem.component.SurfaceCard
import com.recentr.core.designsystem.icon.RecentrIcon
import com.recentr.core.designsystem.icon.RecentrIconImage
import com.recentr.core.designsystem.illustration.InstalledAppIcon
import com.recentr.core.designsystem.motion.rememberFill
import com.recentr.core.designsystem.theme.RecentrShapes
import com.recentr.core.designsystem.theme.RecentrTheme
import com.recentr.core.model.AppCategory
import com.recentr.core.model.AppUsage
import com.recentr.feature.home.R
import com.recentr.feature.home.UsageGrouping
import com.recentr.feature.home.ui.formatDuration

/** Number of top apps that get the "Time well spent?" prompt (Figma 23). */
/** Rows of the "Apps" grouping (the "Categories" grouping sums every app). */
private const val MostUsedApps = 5

/**
 * Figma 23 "Most used": title + "Apps ▾" grouping chip, then a white list card with one row per
 * app (icon, name, time and usage track). [usage] is sorted, most used first.
 */
@Composable
internal fun MostUsedSection(
    usage: List<AppUsage>,
    grouping: UsageGrouping,
    onSelectGrouping: (UsageGrouping) -> Unit,
    modifier: Modifier = Modifier,
    onOpenApp: (packageName: String) -> Unit = {},
) {
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(R.string.home_most_used),
                style = RecentrTheme.typography.title2,
                color = RecentrTheme.colors.textPrimary,
                modifier = Modifier.weight(1f),
            )
            GroupingChip(grouping, onSelectGrouping)
        }
        SurfaceCard(Modifier.fillMaxWidth()) {
            Column(
                Modifier.padding(18.dp).animateContentSize(),
                verticalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                when (grouping) {
                    UsageGrouping.Apps -> {
                        val top = usage.take(MostUsedApps)
                        val max = top.maxOfOrNull { it.minutes }?.coerceAtLeast(1) ?: 1
                        top.forEach { item ->
                            AppUsageRow(
                                usage = item,
                                fraction = item.minutes / max.toFloat(),
                                onOpen = { onOpenApp(item.app.packageName) },
                            )
                        }
                    }
                    UsageGrouping.Categories -> {
                        val groups = usage.groupBy { it.app.category }
                            .map { (category, apps) -> category to apps.sumOf { it.minutes } }
                            .sortedByDescending { it.second }
                        val max = groups.maxOfOrNull { it.second }?.coerceAtLeast(1) ?: 1
                        groups.forEach { (category, minutes) ->
                            CategoryUsageRow(category, minutes, minutes / max.toFloat())
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GroupingChip(grouping: UsageGrouping, onSelect: (UsageGrouping) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        RecentrChip(
            label = stringResource(grouping.label),
            style = RecentrChipStyle.Outline,
            icon = RecentrIcon.ChevronDown,
            onClick = { expanded = true },
            modifier = Modifier.height(32.dp),
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            shape = RecentrShapes.md,
            containerColor = RecentrTheme.colors.bgSurface,
            shadowElevation = 8.dp,
        ) {
            UsageGrouping.entries.forEach { option ->
                DropdownMenuItem(
                    text = {
                        Text(stringResource(option.label), style = RecentrTheme.typography.bodyMedium, color = RecentrTheme.colors.textPrimary)
                    },
                    trailingIcon = if (option == grouping) {
                        { RecentrIconImage(RecentrIcon.Check, tint = RecentrTheme.colors.brandSky, size = 20.dp) }
                    } else {
                        null
                    },
                    onClick = {
                        expanded = false
                        onSelect(option)
                    },
                )
            }
        }
    }
}

@Composable
private fun AppUsageRow(
    usage: AppUsage,
    fraction: Float,
    onOpen: () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable(role = Role.Button, onClick = onOpen),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        InstalledAppIcon(usage.app.packageName, size = 36.dp)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            UsageHeader(usage.app.label, usage.minutes)
            UsageTrack(fraction)
        }
    }
}

@Composable
private fun CategoryUsageRow(category: AppCategory, minutes: Int, fraction: Float) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(
            Modifier.size(36.dp).clip(RoundedCornerShape(10.dp)).background(RecentrTheme.colors.bgSunken),
            contentAlignment = Alignment.Center,
        ) {
            RecentrIconImage(category.icon, tint = RecentrTheme.colors.textPrimary, size = 20.dp)
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            UsageHeader(stringResource(category.label), minutes)
            UsageTrack(fraction)
        }
    }
}

@Composable
private fun UsageHeader(name: String, minutes: Int) {
    Row(Modifier.fillMaxWidth()) {
        Text(
            text = name,
            style = RecentrTheme.typography.headline,
            color = RecentrTheme.colors.textPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = formatDuration(minutes),
            style = RecentrTheme.typography.bodyMedium,
            color = RecentrTheme.colors.textSecondary,
            maxLines = 1,
        )
    }
}

/** 6dp sunken track with the sky fill, filling in on entry (`.spring(duration: 0.9, bounce: 0.2).delay(0.2)`). */
@Composable
private fun UsageTrack(fraction: Float) {
    val fill by rememberFill(fraction.coerceIn(0f, 1f), delayMillis = 200)
    val animated = fill.coerceIn(0f, 1f)
    Box(
        Modifier
            .fillMaxWidth()
            .height(6.dp)
            .clip(RoundedCornerShape(3.dp))
            .background(RecentrTheme.colors.bgSunken),
    ) {
        Box(
            Modifier
                .fillMaxWidth(animated)
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(RecentrTheme.colors.brandSky),
        )
    }
}

@get:StringRes
private val UsageGrouping.label: Int
    get() = when (this) {
        UsageGrouping.Apps -> R.string.home_filter_apps
        UsageGrouping.Categories -> R.string.home_filter_categories
    }

/** Label and icon of the app categories in the "Categories" grouping. */
@get:StringRes
private val AppCategory.label: Int
    get() = when (this) {
        AppCategory.Social -> R.string.home_category_social
        AppCategory.Entertainment -> R.string.home_category_entertainment
        AppCategory.Music -> R.string.home_category_music
        AppCategory.Games -> R.string.home_category_games
        AppCategory.Productivity -> R.string.home_category_productivity
        AppCategory.Other -> R.string.home_category_other
    }

private val AppCategory.icon: RecentrIcon
    get() = when (this) {
        AppCategory.Social -> RecentrIcon.Users
        AppCategory.Entertainment -> RecentrIcon.Sparkle
        AppCategory.Music -> RecentrIcon.Sun
        AppCategory.Games -> RecentrIcon.Zap
        AppCategory.Productivity -> RecentrIcon.Check
        AppCategory.Other -> RecentrIcon.Apps
    }
