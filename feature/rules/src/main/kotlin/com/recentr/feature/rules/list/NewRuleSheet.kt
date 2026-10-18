package com.recentr.feature.rules.list

import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.recentr.core.designsystem.component.RecentrTextButton
import com.recentr.core.designsystem.icon.RecentrIcon
import com.recentr.core.designsystem.icon.RecentrIconImage
import com.recentr.core.designsystem.illustration.StickerImage
import com.recentr.core.designsystem.theme.RecentrTheme
import com.recentr.core.model.RuleType
import com.recentr.core.model.defaultSticker
import com.recentr.feature.rules.R
import com.recentr.feature.rules.ui.asSticker
import com.recentr.feature.rules.ui.components.Gap
import com.recentr.feature.rules.ui.components.RulesSheet
import com.recentr.feature.rules.ui.components.SheetBody
import com.recentr.feature.rules.ui.label

/** The kinds of rule the sheet offers, in order, with what each one does. */
private val Kinds = listOf(
    RuleType.Bedtime to R.string.rules_kind_routine_body,
    RuleType.Session to R.string.rules_kind_session_body,
    RuleType.Lock to R.string.rules_kind_lock_body,
    RuleType.Limit to R.string.rules_kind_limit_body,
)

/**
 * "New rule" (the Rules header's "+"), laid out like "How rules work": one row per kind of rule —
 * 40dp sticker, Headline name, Body/Regular explanation — with a chevron, since each opens its own
 * editor. "Browse templates" stays at the bottom.
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
            NewRuleContent(onCreate = { type -> closer.close { onCreate(type) } })
        }
    }
}

@Composable
private fun NewRuleContent(onCreate: (RuleType) -> Unit) {
    val colors = RecentrTheme.colors
    Text(stringResource(R.string.rules_new_rule), style = RecentrTheme.typography.title1, color = colors.textPrimary)
    Gap(4.dp)
    Text(stringResource(R.string.rules_new_rule_subtitle), style = RecentrTheme.typography.bodyRegular, color = colors.textSecondary)
    Gap(8.dp)
    Kinds.forEach { (type, body) ->
        RuleKindRow(type, body, onClick = { onCreate(type) })
    }
}

/** A kind of rule: sticker, name and explanation, and the chevron of its own flow. */
@Composable
private fun RuleKindRow(type: RuleType, @StringRes body: Int, onClick: () -> Unit) {
    val colors = RecentrTheme.colors
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(role = Role.Button, onClick = onClick)
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        StickerImage(type.defaultSticker.asSticker, size = 40.dp, modifier = Modifier.align(Alignment.Top))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(type.label(), style = RecentrTheme.typography.headline, color = colors.textPrimary)
            Text(stringResource(body), style = RecentrTheme.typography.bodyRegular, color = colors.textSecondary)
        }
        RecentrIconImage(RecentrIcon.ChevronRight, tint = colors.textTertiary, size = 20.dp)
    }
}

@Preview(widthDp = 402)
@Composable
private fun NewRulePreview() {
    RecentrTheme {
        Column(Modifier.padding(24.dp)) { NewRuleContent(onCreate = {}) }
    }
}
