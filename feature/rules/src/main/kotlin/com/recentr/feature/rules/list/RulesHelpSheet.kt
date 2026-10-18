package com.recentr.feature.rules.list

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.recentr.core.designsystem.component.RecentrButton
import com.recentr.core.designsystem.component.RecentrSheet
import com.recentr.core.designsystem.illustration.Sticker
import com.recentr.core.designsystem.illustration.StickerImage
import com.recentr.core.designsystem.theme.RecentrTheme
import com.recentr.feature.rules.R

/** A kind of rule the help sheet explains (iOS `RuleKind`): sticker, name and what it does. */
private enum class HelpKind(val sticker: Sticker, @param:StringRes val title: Int, @param:StringRes val body: Int) {
    Routine(Sticker.Moon, R.string.rules_help_routine, R.string.rules_help_routine_body),
    Lock(Sticker.Lock, R.string.rules_help_lock, R.string.rules_help_lock_body),
    Limit(Sticker.Timer, R.string.rules_help_limit, R.string.rules_help_limit_body),
}

/**
 * "How rules work" (the Rules header's "?"), like the iOS `HelpSheet`: Title/1, one row per kind
 * of rule (40dp sticker, Headline name, Body/Regular explanation), the Focus hint and "Got it".
 */
@Composable
internal fun RulesHelpSheet(onDismissRequest: () -> Unit) {
    RecentrSheet(onDismissRequest = onDismissRequest) { controller ->
        RulesHelpContent(onGotIt = { controller.close() })
    }
}

@Composable
private fun RulesHelpContent(onGotIt: () -> Unit) {
    val colors = RecentrTheme.colors
    // iOS pads 32 above the title; the design-system drag handle already takes 21.
    Column(
        Modifier.fillMaxWidth().padding(start = 24.dp, end = 24.dp, top = 11.dp, bottom = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            stringResource(R.string.rules_help_title),
            style = RecentrTheme.typography.title1,
            color = colors.textPrimary,
            modifier = Modifier.semantics { heading() },
        )
        HelpKind.entries.forEach { kind ->
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.Top) {
                StickerImage(kind.sticker, size = 40.dp)
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(stringResource(kind.title), style = RecentrTheme.typography.headline, color = colors.textPrimary)
                    Text(stringResource(kind.body), style = RecentrTheme.typography.bodyRegular, color = colors.textSecondary)
                }
            }
        }
        Text(stringResource(R.string.rules_help_focus), style = RecentrTheme.typography.captionRegular, color = colors.textTertiary)
        Column {
            Spacer(Modifier.height(8.dp))
            RecentrButton(text = stringResource(R.string.rules_help_ok), onClick = onGotIt, modifier = Modifier.fillMaxWidth())
        }
    }
}

@Preview(widthDp = 402)
@Composable
private fun RulesHelpPreview() {
    RecentrTheme { RulesHelpContent(onGotIt = {}) }
}
