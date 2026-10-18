package com.recentr.feature.rules.sheets

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.recentr.core.designsystem.component.AppRow
import com.recentr.core.designsystem.component.RecentrButton
import com.recentr.core.designsystem.component.RecentrTextField
import com.recentr.core.designsystem.illustration.InstalledAppIcon
import com.recentr.core.designsystem.theme.RecentrTheme
import com.recentr.core.model.AppCategory
import com.recentr.core.model.UsageApp
import com.recentr.feature.rules.R
import com.recentr.feature.rules.ui.rememberInstalledApps
import com.recentr.feature.rules.ui.sectionLabel
import com.recentr.feature.rules.ui.components.Gap
import com.recentr.feature.rules.ui.components.RulesSheet
import com.recentr.feature.rules.ui.components.SectionCaption
import com.recentr.feature.rules.ui.components.SheetBody

/**
 * Figma 36 "Choose apps": search, the device's launcher apps by category with checkboxes,
 * "Done · N apps". Chosen apps that are no longer installed stay listed so they can be removed.
 */
@Composable
internal fun ChooseAppsSheet(
    initial: List<UsageApp>,
    onDone: (List<UsageApp>) -> Unit,
    onDismiss: () -> Unit,
) {
    val selected = remember { mutableStateListOf<UsageApp>().apply { addAll(initial) } }
    var query by rememberSaveable { mutableStateOf("") }
    val installed = rememberInstalledApps(refresh = true)
    RulesSheet(onDismissRequest = onDismiss) { closer ->
        SheetBody(
            figmaHeight = null,
            topGap = 18.dp,
            // Figma 36: the picker sits 38pt lower than the rule sheet it is stacked on.
            stackedOffset = 38.dp,
            horizontalAlignment = Alignment.Start,
            bottomBar = {
                RecentrButton(
                    text = pluralStringResource(R.plurals.rules_done_apps, selected.size, selected.size),
                    onClick = {
                        val apps = selected.toList()
                        closer.close { onDone(apps) }
                    },
                    enabled = selected.isNotEmpty(),
                    modifier = Modifier.fillMaxWidth(),
                )
            },
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.rules_choose_apps), style = RecentrTheme.typography.title1, color = RecentrTheme.colors.textPrimary, modifier = Modifier.weight(1f))
                Text(
                    stringResource(R.string.rules_selected, selected.size),
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
            if (installed == null) {
                CircularProgressIndicator(
                    color = RecentrTheme.colors.brandSky,
                    strokeWidth = 3.dp,
                    modifier = Modifier.padding(vertical = 24.dp).size(28.dp).align(Alignment.CenterHorizontally),
                )
            } else {
                AppSections(
                    apps = remember(installed) {
                        installed + initial.filter { chosen -> installed.none { it.packageName == chosen.packageName } }
                    },
                    query = query.trim(),
                    isSelected = { app -> selected.any { it.packageName == app.packageName } },
                    onToggle = { app, checked ->
                        if (checked) selected.add(app) else selected.removeAll { it.packageName == app.packageName }
                    },
                )
            }
        }
    }
}

/** The [apps] matching [query], one sunken group per category ("SOCIAL", "ENTERTAINMENT"…). */
@Composable
private fun ColumnScope.AppSections(
    apps: List<UsageApp>,
    query: String,
    isSelected: (UsageApp) -> Boolean,
    onToggle: (UsageApp, Boolean) -> Unit,
) {
    val matches = AppCategory.entries.map { category ->
        category to apps.filter { it.category == category && (query.isEmpty() || it.label.contains(query, ignoreCase = true)) }
    }.filter { it.second.isNotEmpty() }
    if (matches.isEmpty()) {
        Text(
            stringResource(R.string.rules_no_apps_found, query),
            style = RecentrTheme.typography.bodyRegular,
            color = RecentrTheme.colors.textSecondary,
        )
    }
    matches.forEachIndexed { index, (category, categoryApps) ->
        if (index > 0) Gap(14.dp)
        SectionCaption(stringResource(category.sectionLabel))
        Gap(8.dp)
        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(22.dp))
                .background(RecentrTheme.colors.bgSunken)
                .padding(horizontal = 16.dp, vertical = 6.dp),
        ) {
            categoryApps.forEach { app ->
                key(app.packageName) {
                    AppRow(name = app.label, checked = isSelected(app), onCheckedChange = { onToggle(app, it) }) {
                        InstalledAppIcon(app.packageName, size = 32.dp)
                    }
                }
            }
        }
    }
}
