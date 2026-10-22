package com.recentr.feature.home.ui.sheets

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.recentr.core.designsystem.component.RecentrButton
import com.recentr.core.designsystem.component.RecentrSheet
import com.recentr.core.designsystem.illustration.Sticker
import com.recentr.core.designsystem.illustration.StickerImage
import com.recentr.core.designsystem.theme.RecentrTheme
import com.recentr.feature.home.R

/**
 * Closing the Always allowed card: it's still in Settings › Protection › Always allowed.
 * [onDismissRequest] runs once the sheet is gone, however it was closed: the card goes then.
 */
@Composable
internal fun AlwaysAllowedInSettingsSheet(onDismissRequest: () -> Unit) {
    val colors = RecentrTheme.colors
    RecentrSheet(onDismissRequest = onDismissRequest) { controller ->
        Column(
            Modifier.fillMaxWidth().padding(start = 24.dp, end = 24.dp, top = 16.dp, bottom = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            StickerImage(Sticker.Shield, size = 96.dp)
            Spacer(Modifier.height(12.dp))
            Text(
                stringResource(R.string.home_always_allowed_settings_title),
                style = RecentrTheme.typography.title2,
                color = colors.textPrimary,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                stringResource(R.string.home_always_allowed_settings_body),
                style = RecentrTheme.typography.bodyRegular,
                color = colors.textSecondary,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(20.dp))
            RecentrButton(
                text = stringResource(R.string.home_always_allowed_settings_got_it),
                onClick = { controller.close() },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
