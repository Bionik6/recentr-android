package com.recentr.feature.profile.settings

import android.content.ActivityNotFoundException
import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import com.recentr.core.designsystem.component.RecentrAlertDialog
import com.recentr.core.designsystem.component.RecentrButton
import com.recentr.core.designsystem.component.SettingRow
import com.recentr.core.designsystem.component.SettingsGroup
import com.recentr.core.designsystem.component.SettingsSectionLabel
import com.recentr.core.designsystem.component.StickyNoteField
import com.recentr.core.designsystem.icon.RecentrIcon
import com.recentr.feature.profile.R
import com.recentr.feature.profile.ui.SettingsPage

private enum class HelpTopic(val title: Int, val body: Int, val icon: RecentrIcon) {
    NotBlocking(R.string.help_not_blocking, R.string.help_not_blocking_body, RecentrIcon.Shield),
    Strict(R.string.help_strict, R.string.help_strict_body, RecentrIcon.Help),
    Billing(R.string.help_billing, R.string.help_billing_body, RecentrIcon.StarFill),
}

/** 67 · Settings — Help & feedback: three FAQ rows and a sticky note sent by email. */
@Composable
internal fun HelpScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var topic by rememberSaveable { mutableStateOf<HelpTopic?>(null) }
    var note by rememberSaveable { mutableStateOf("") }
    val placeholder = stringResource(R.string.help_note_placeholder)
    val subject = stringResource(R.string.help_feedback_subject)
    val email = stringResource(R.string.help_feedback_email)
    SettingsPage(
        title = stringResource(R.string.help_title),
        subtitle = stringResource(R.string.help_subtitle),
        onBack = onBack,
        modifier = modifier,
        bottomBar = {
            RecentrButton(
                text = stringResource(R.string.help_send),
                onClick = {
                    val intent = Intent(Intent.ACTION_SENDTO, "mailto:".toUri())
                        .putExtra(Intent.EXTRA_EMAIL, arrayOf(email))
                        .putExtra(Intent.EXTRA_SUBJECT, subject)
                        .putExtra(Intent.EXTRA_TEXT, note.ifBlank { placeholder })
                    try {
                        context.startActivity(intent)
                    } catch (_: ActivityNotFoundException) {
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            )
        },
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SettingsGroup {
                HelpTopic.entries.forEach { item ->
                    SettingRow(
                        label = stringResource(item.title),
                        value = null,
                        icon = item.icon,
                        onClick = { topic = item },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
            SettingsSectionLabel(stringResource(R.string.help_note_section))
            // The Figma note is tilted by 1° (clockwise).
            StickyNoteField(
                title = stringResource(R.string.help_note_title),
                value = note,
                onValueChange = { note = it },
                placeholder = placeholder,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp)
                    .graphicsLayer { rotationZ = 1f },
            )
        }
    }
    topic?.let { item ->
        RecentrAlertDialog(
            title = stringResource(item.title),
            body = stringResource(item.body),
            primaryText = stringResource(R.string.dialog_ok),
            onPrimary = { topic = null },
            onDismissRequest = { topic = null },
        )
    }
}
