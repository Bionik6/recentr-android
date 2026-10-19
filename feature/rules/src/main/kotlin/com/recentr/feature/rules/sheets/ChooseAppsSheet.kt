package com.recentr.feature.rules.sheets

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.recentr.core.designsystem.component.RecentrButton
import com.recentr.core.designsystem.component.RecentrCheckbox
import com.recentr.core.designsystem.component.RecentrTextField
import com.recentr.core.designsystem.component.RecentrTriStateCheckbox
import com.recentr.core.designsystem.haptics.LocalRecentrHaptics
import com.recentr.core.designsystem.icon.RecentrIcon
import com.recentr.core.designsystem.icon.RecentrIconImage
import com.recentr.core.designsystem.illustration.InstalledAppIcon
import com.recentr.core.designsystem.theme.RecentrTheme
import com.recentr.core.model.AppCategory
import com.recentr.core.model.UsageApp
import com.recentr.feature.rules.R
import com.recentr.feature.rules.ui.components.Gap
import com.recentr.feature.rules.ui.components.RulesSheet
import com.recentr.feature.rules.ui.components.SheetBody
import com.recentr.feature.rules.ui.rememberInstalledApps
import com.recentr.feature.rules.ui.title

/**
 * Figma 36 "Choose apps", laid out like iOS' app picker: "All apps & categories", then the
 * device's launcher apps by category, Social first, all folded ([AppPicker]). A category's box
 * picks all of its apps (partly checked when only some are), its row unfolds them; a search lists
 * the matching apps. "Block · N apps". Chosen apps that are no longer installed stay listed so
 * they can be removed.
 */
@Composable
internal fun ChooseAppsSheet(
    initial: List<UsageApp>,
    onDone: (List<UsageApp>) -> Unit,
    onDismiss: () -> Unit,
) {
    var picks by remember { mutableStateOf(initial.map { it.packageName }.toSet()) }
    var query by rememberSaveable { mutableStateOf("") }
    var unfolded by rememberSaveable { mutableStateOf(emptyList<String>()) }
    val installed = rememberInstalledApps(refresh = true)
    val apps = remember(installed) {
        installed?.let { list -> list + initial.filter { chosen -> list.none { it.packageName == chosen.packageName } } }
    }
    RulesSheet(onDismissRequest = onDismiss) { closer ->
        SheetBody(
            figmaHeight = null,
            topGap = 18.dp,
            // Figma 36: the picker sits 38pt lower than the rule sheet it is stacked on.
            stackedOffset = 38.dp,
            horizontalAlignment = Alignment.Start,
            bottomBar = {
                RecentrButton(
                    text = pluralStringResource(R.plurals.rules_block_apps, picks.size, picks.size),
                    onClick = {
                        val chosen = AppPicker.pickedApps(apps ?: initial, picks)
                        closer.close { onDone(chosen) }
                    },
                    enabled = picks.isNotEmpty(),
                    modifier = Modifier.fillMaxWidth(),
                )
            },
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.rules_choose_apps), style = RecentrTheme.typography.title1, color = RecentrTheme.colors.textPrimary, modifier = Modifier.weight(1f))
                Text(
                    stringResource(R.string.rules_selected, picks.size),
                    style = RecentrTheme.typography.bodyMedium,
                    color = RecentrTheme.colors.brandSky,
                )
            }
            Gap(14.dp)
            RecentrTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = stringResource(R.string.rules_search_apps),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                modifier = Modifier.fillMaxWidth(),
            )
            Gap(16.dp)
            when {
                apps == null -> CircularProgressIndicator(
                    color = RecentrTheme.colors.brandSky,
                    strokeWidth = 3.dp,
                    modifier = Modifier.padding(vertical = 24.dp).size(28.dp).align(Alignment.CenterHorizontally),
                )
                query.isNotBlank() -> SearchResults(AppPicker.search(apps, query), query.trim(), picks) { picks = it }
                else -> Categories(
                    apps = apps,
                    picks = picks,
                    unfolded = unfolded,
                    onPicks = { picks = it },
                    onFold = { category ->
                        unfolded = if (category.name in unfolded) unfolded - category.name else unfolded + category.name
                    },
                )
            }
        }
    }
}

/** "All apps & categories", then each category with its apps under it when unfolded. */
@Composable
private fun Categories(
    apps: List<UsageApp>,
    picks: Set<String>,
    unfolded: List<String>,
    onPicks: (Set<String>) -> Unit,
    onFold: (AppCategory) -> Unit,
) {
    PickerCard {
        GroupRow(
            icon = RecentrIcon.Apps,
            label = stringResource(R.string.rules_all_apps_and_categories),
            picked = AppPicker.picked(apps, picks),
            onToggle = { onPicks(AppPicker.toggle(apps, picks)) },
        )
        AppPicker.groups(apps).forEach { (category, categoryApps) ->
            key(category) {
                val open = category.name in unfolded
                val picked = AppPicker.picked(categoryApps, picks)
                GroupRow(
                    icon = category.icon,
                    label = stringResource(category.title),
                    picked = picked,
                    onToggle = { onPicks(AppPicker.toggle(categoryApps, picks)) },
                    trailing = when (picked) {
                        Picked.All -> stringResource(R.string.rules_picked_all)
                        Picked.Some -> categoryApps.count { it.packageName in picks }.toString()
                        Picked.None -> null
                    },
                    unfolded = open,
                    onFold = { onFold(category) },
                )
                AnimatedVisibility(
                    visible = open,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut(),
                ) {
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .padding(bottom = 6.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(RecentrTheme.colors.bgSurface)
                            .padding(horizontal = 4.dp, vertical = 2.dp),
                    ) {
                        categoryApps.forEach { app ->
                            key(app.packageName) {
                                AppPickRow(app, picked = app.packageName in picks) { checked ->
                                    onPicks(if (checked) picks + app.packageName else picks - app.packageName)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchResults(results: List<UsageApp>, query: String, picks: Set<String>, onPicks: (Set<String>) -> Unit) {
    if (results.isEmpty()) {
        Text(
            stringResource(R.string.rules_no_apps_found, query),
            style = RecentrTheme.typography.bodyRegular,
            color = RecentrTheme.colors.textSecondary,
        )
        return
    }
    PickerCard {
        results.forEach { app ->
            key(app.packageName) {
                AppPickRow(app, picked = app.packageName in picks) { checked ->
                    onPicks(if (checked) picks + app.packageName else picks - app.packageName)
                }
            }
        }
    }
}

/** The sunken group the rows sit in. */
@Composable
private fun PickerCard(content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(RecentrTheme.colors.bgSunken)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        content = content,
    )
}

/**
 * A category (or every app): its box picks all of them, the rest of the row unfolds its apps
 * ([onFold]; the row picks when there's nothing to unfold). [trailing]: "All" or how many are picked.
 */
@Composable
private fun GroupRow(
    icon: RecentrIcon,
    label: String,
    picked: Picked,
    onToggle: () -> Unit,
    trailing: String? = null,
    unfolded: Boolean? = null,
    onFold: (() -> Unit)? = null,
) {
    val colors = RecentrTheme.colors
    val chevron by animateFloatAsState(if (unfolded == true) 90f else 0f, label = "chevron")
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(role = Role.Button, onClick = onFold ?: onToggle)
            .padding(end = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RecentrTriStateCheckbox(state = picked.toggleable, onClick = onToggle)
        Box(
            Modifier.size(32.dp).clip(RoundedCornerShape(10.dp)).background(colors.bgSurface),
            contentAlignment = Alignment.Center,
        ) {
            RecentrIconImage(icon, tint = colors.textPrimary, size = 19.dp)
        }
        Text(
            label,
            style = RecentrTheme.typography.bodyMedium,
            color = colors.textPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f).padding(start = 4.dp),
        )
        if (trailing != null) {
            Text(trailing, style = RecentrTheme.typography.bodyRegular, color = colors.textTertiary, maxLines = 1)
        }
        if (unfolded != null) {
            RecentrIconImage(RecentrIcon.ChevronRight, tint = colors.textTertiary, size = 18.dp, modifier = Modifier.rotate(chevron))
        }
    }
}

/** One app: the whole row picks it. */
@Composable
private fun AppPickRow(app: UsageApp, picked: Boolean, onPick: (Boolean) -> Unit) {
    val haptics = LocalRecentrHaptics.current
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .toggleable(value = picked, role = Role.Checkbox) { value ->
                haptics.selection()
                onPick(value)
            }
            .height(48.dp)
            // Inside the unfolded card (4dp), its box lines up with the categories' boxes.
            .padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RecentrCheckbox(checked = picked, onCheckedChange = null)
        InstalledAppIcon(app.packageName, size = 32.dp)
        Text(
            app.label,
            style = RecentrTheme.typography.bodyMedium,
            color = RecentrTheme.colors.textPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
    }
}

private val Picked.toggleable: ToggleableState
    get() = when (this) {
        Picked.None -> ToggleableState.Off
        Picked.Some -> ToggleableState.Indeterminate
        Picked.All -> ToggleableState.On
    }

/** Like Home's "Categories" grouping. */
private val AppCategory.icon: RecentrIcon
    get() = when (this) {
        AppCategory.Social -> RecentrIcon.Users
        AppCategory.Entertainment -> RecentrIcon.Sparkle
        AppCategory.Music -> RecentrIcon.Sun
        AppCategory.Games -> RecentrIcon.Zap
        AppCategory.Productivity -> RecentrIcon.Check
        AppCategory.Other -> RecentrIcon.More
    }
