package com.recentr.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.recentr.core.designsystem.R
import com.recentr.core.designsystem.theme.RecentrElevation
import com.recentr.core.designsystem.theme.RecentrShapes
import com.recentr.core.designsystem.theme.RecentrTheme
import com.recentr.core.designsystem.theme.recentrShadow

/**
 * In place of the screen-time numbers without usage access (Home, Insights): what Recentr needs it
 * for, and "Connect" ([onConnect], Settings › Usage access). Its empty numbers would read as a day
 * without any screen time.
 */
@Composable
fun UsageAccessCard(onConnect: () -> Unit, modifier: Modifier = Modifier) {
    val colors = RecentrTheme.colors
    Column(
        modifier = modifier
            .fillMaxWidth()
            .recentrShadow(RecentrElevation.float, RecentrShapes.card)
            .clip(RecentrShapes.card)
            .background(colors.bgSurface)
            .padding(start = 20.dp, end = 20.dp, top = 22.dp, bottom = 20.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(stringResource(R.string.ds_usage_access_title), style = RecentrTheme.typography.title2, color = colors.textPrimary)
        Text(stringResource(R.string.ds_usage_access_body), style = RecentrTheme.typography.bodyRegular, color = colors.textSecondary)
        Spacer(Modifier.height(8.dp))
        RecentrButton(
            text = stringResource(R.string.ds_usage_access_connect),
            onClick = onConnect,
            style = RecentrButtonStyle.Primary,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Preview
@Composable
private fun UsageAccessCardPreview() {
    RecentrTheme {
        Box(Modifier.background(RecentrTheme.colors.bgPage).padding(24.dp)) { UsageAccessCard(onConnect = {}) }
    }
}
