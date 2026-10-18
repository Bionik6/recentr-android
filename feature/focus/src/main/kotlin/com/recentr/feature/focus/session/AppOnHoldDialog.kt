package com.recentr.feature.focus.session

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import com.recentr.core.designsystem.component.RecentrButton
import com.recentr.core.designsystem.component.RecentrButtonStyle
import com.recentr.core.designsystem.illustration.AppIcon
import com.recentr.core.designsystem.illustration.DemoApp
import com.recentr.core.designsystem.illustration.Mascot
import com.recentr.core.designsystem.illustration.MascotMood
import com.recentr.core.designsystem.theme.RecentrElevation
import com.recentr.core.designsystem.theme.RecentrTheme
import com.recentr.core.designsystem.theme.recentrShadow

/**
 * 96 · Focus — App on hold: impatient mascot (+ the held app, tilted 10°), "You’re 32 min into
 * focus", "Back to focus" / "End session early". Without [app] it guards leaving the session.
 */
@Composable
internal fun AppOnHoldDialog(
    title: String,
    body: String,
    backText: String,
    endText: String,
    app: DemoApp?,
    onBack: () -> Unit,
    onEnd: () -> Unit,
    onDismissRequest: () -> Unit,
) {
    val colors = RecentrTheme.colors
    Dialog(onDismissRequest = onDismissRequest, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        // Figma dims with overlay/scrim (ink 36%).
        val window = (LocalView.current.parent as? DialogWindowProvider)?.window
        SideEffect { window?.setDimAmount(0.36f) }
        val shape = RoundedCornerShape(32.dp)
        Column(
            Modifier
                .padding(horizontal = 31.dp)
                .widthIn(max = 340.dp)
                .fillMaxWidth()
                .recentrShadow(RecentrElevation.card, shape)
                .clip(shape)
                .background(colors.bgSurface)
                .padding(start = 22.dp, end = 22.dp, top = 24.dp, bottom = 22.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Box(Modifier.size(width = 160.dp, height = 110.dp)) {
                Mascot(MascotMood.Impatient, width = 88.dp, modifier = Modifier.offset(x = if (app != null) 20.dp else 36.dp))
                if (app != null) {
                    AppIcon(
                        app,
                        size = 52.dp,
                        contentDescription = null,
                        modifier = Modifier
                            .offset(x = 95.dp, y = 56.dp)
                            .graphicsLayer { rotationZ = 10f },
                    )
                }
            }
            Text(title, style = RecentrTheme.typography.title2, color = colors.textPrimary, textAlign = TextAlign.Center)
            Text(body, style = RecentrTheme.typography.bodyRegular, color = colors.textSecondary, textAlign = TextAlign.Center)
            Spacer(Modifier.height(4.dp))
            RecentrButton(text = backText, onClick = onBack, style = RecentrButtonStyle.Brand, modifier = Modifier.fillMaxWidth())
            RecentrButton(text = endText, onClick = onEnd, style = RecentrButtonStyle.Soft, modifier = Modifier.fillMaxWidth())
        }
    }
}
