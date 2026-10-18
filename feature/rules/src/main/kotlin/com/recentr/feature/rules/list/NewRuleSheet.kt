package com.recentr.feature.rules.list

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.recentr.core.designsystem.component.RecentrTextButton
import com.recentr.core.designsystem.component.RuleTile
import com.recentr.core.designsystem.theme.RecentrTheme
import com.recentr.core.model.RuleType
import com.recentr.core.model.defaultSticker
import com.recentr.feature.rules.R
import com.recentr.feature.rules.ui.asSticker
import com.recentr.feature.rules.ui.components.Gap
import com.recentr.feature.rules.ui.components.RulesSheet
import com.recentr.feature.rules.ui.components.SheetBody
import com.recentr.feature.rules.ui.label

/**
 * "New rule" grid of [RuleTile]s (the Figma Rule Tile component; the kit has no screen for this
 * sheet, so it follows the other rules sheets' layout).
 */
@Composable
internal fun NewRuleSheet(
    onCreate: (RuleType) -> Unit,
    onBrowseTemplates: () -> Unit,
    onDismiss: () -> Unit,
) {
    RulesSheet(onDismissRequest = onDismiss) { closer ->
        SheetBody(
            figmaHeight = null,
            topGap = 18.dp,
            wrapContent = true,
            horizontalAlignment = Alignment.Start,
            bottomBar = {
                RecentrTextButton(
                    text = stringResource(R.string.rules_browse_templates),
                    onClick = { closer.close(onBrowseTemplates) },
                    color = RecentrTheme.colors.brandSky,
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                )
            },
        ) {
            Text(stringResource(R.string.rules_new_rule), style = RecentrTheme.typography.title1, color = RecentrTheme.colors.textPrimary)
            Gap(4.dp)
            Text(
                stringResource(R.string.rules_new_rule_subtitle),
                style = RecentrTheme.typography.bodyRegular,
                color = RecentrTheme.colors.textSecondary,
            )
            Gap(16.dp)
            RuleType.entries.chunked(3).forEachIndexed { index, row ->
                if (index > 0) Gap(12.dp)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    row.forEach { type ->
                        RuleTile(
                            label = type.label(),
                            sticker = type.defaultSticker.asSticker,
                            onClick = { closer.close { onCreate(type) } },
                            modifier = Modifier.weight(1f),
                        )
                    }
                    repeat(3 - row.size) { Box(Modifier.weight(1f)) }
                }
            }
        }
    }
}
