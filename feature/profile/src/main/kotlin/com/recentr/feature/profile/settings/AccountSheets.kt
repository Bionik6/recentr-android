package com.recentr.feature.profile.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.recentr.core.designsystem.component.OptionRow
import com.recentr.core.designsystem.component.RecentrButton
import com.recentr.core.designsystem.component.RecentrButtonStyle
import com.recentr.core.designsystem.component.RecentrSheet
import com.recentr.core.designsystem.illustration.Mascot
import com.recentr.core.designsystem.illustration.MascotMood
import com.recentr.core.designsystem.theme.RecentrTheme
import com.recentr.core.model.StrictScope
import com.recentr.feature.profile.R
import com.recentr.feature.profile.ui.SheetContent
import com.recentr.feature.profile.ui.SheetHeading

/** 71 · Settings — Log out: dozing mascot, "Log out" / "Stay". */
@Composable
internal fun LogOutSheet(onLogOut: () -> Unit, onDismissRequest: () -> Unit) {
    RecentrSheet(onDismissRequest = onDismissRequest) { controller ->
        SheetContent(topGap = 16.dp) {
            Mascot(MascotMood.Dozing, width = 96.dp)
            Spacer(Modifier.height(8.dp))
            SheetHeading(stringResource(R.string.logout_title), stringResource(R.string.logout_body))
            Spacer(Modifier.height(22.dp))
            RecentrButton(
                text = stringResource(R.string.logout_confirm),
                onClick = { controller.close(onLogOut) },
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(10.dp))
            RecentrButton(
                text = stringResource(R.string.logout_stay),
                onClick = { controller.close() },
                style = RecentrButtonStyle.Soft,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

/** 72 · Settings — Delete account: impatient mascot, type DELETE to enable the red button. */
@Composable
internal fun DeleteAccountSheet(onDelete: () -> Unit, onDismissRequest: () -> Unit) {
    val word = stringResource(R.string.delete_word)
    var typed by rememberSaveable { mutableStateOf("") }
    val confirmed = typed.trim() == word
    RecentrSheet(onDismissRequest = onDismissRequest) { controller ->
        SheetContent(topGap = 16.dp) {
            Mascot(MascotMood.Impatient, width = 96.dp)
            Spacer(Modifier.height(8.dp))
            SheetHeading(stringResource(R.string.delete_title), stringResource(R.string.delete_body))
            Spacer(Modifier.height(16.dp))
            ConfirmWordField(
                value = typed,
                onValueChange = { typed = it.uppercase().take(word.length + 2) },
                placeholder = stringResource(R.string.delete_placeholder),
                onDone = { if (confirmed) controller.close(onDelete) },
            )
            Spacer(Modifier.height(16.dp))
            RecentrButton(
                text = stringResource(R.string.delete_confirm),
                onClick = { controller.close(onDelete) },
                style = RecentrButtonStyle.Danger,
                enabled = confirmed,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(10.dp))
            RecentrButton(
                text = stringResource(R.string.delete_cancel),
                onClick = { controller.close() },
                style = RecentrButtonStyle.Soft,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

/**
 * The focused Figma "Text Field" (white, 2dp sky outline, sky caret) of the delete sheet,
 * focused on open. Characters are upper-cased as typed.
 */
@Composable
private fun ConfirmWordField(value: String, onValueChange: (String) -> Unit, placeholder: String, onDone: () -> Unit) {
    val colors = RecentrTheme.colors
    val shape = RoundedCornerShape(18.dp)
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        textStyle = RecentrTheme.typography.bodyMedium.copy(color = colors.textPrimary),
        cursorBrush = SolidColor(colors.brandSky),
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters, autoCorrectEnabled = false, imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { onDone() }),
        modifier = Modifier.fillMaxWidth().height(56.dp).focusRequester(focusRequester),
        decorationBox = { inner ->
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .clip(shape)
                    .background(colors.bgSurface)
                    .border(2.dp, colors.brandSky, shape)
                    .padding(horizontal = 18.dp),
                contentAlignment = Alignment.CenterStart,
            ) {
                if (value.isEmpty()) Text(placeholder, style = RecentrTheme.typography.bodyMedium, color = colors.textTertiary)
                inner()
            }
        },
    )
}

/** Strict mode scope: Never / During sessions / Always (radio rows, saved on tap). */
@Composable
internal fun StrictModeSheet(selected: StrictScope, onSelect: (StrictScope) -> Unit, onDismissRequest: () -> Unit) {
    RecentrSheet(onDismissRequest = onDismissRequest) { controller ->
        SheetContent(topGap = 22.dp, horizontalAlignment = Alignment.Start) {
            Text(stringResource(R.string.strict_sheet_title), style = RecentrTheme.typography.title2, color = RecentrTheme.colors.textPrimary)
            Spacer(Modifier.height(4.dp))
            Text(stringResource(R.string.strict_sheet_subtitle), style = RecentrTheme.typography.bodyRegular, color = RecentrTheme.colors.textSecondary)
            Spacer(Modifier.height(18.dp))
            Column(Modifier.selectableGroup(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                listOf(
                    StrictScope.Off to R.string.strict_option_off,
                    StrictScope.Sessions to R.string.strict_option_sessions,
                    StrictScope.Always to R.string.strict_option_always,
                ).forEach { (scope, label) ->
                    OptionRow(
                        label = stringResource(label),
                        selected = scope == selected,
                        onClick = {
                            onSelect(scope)
                            controller.close()
                        },
                        role = Role.RadioButton,
                        radio = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
            Spacer(Modifier.height(14.dp))
        }
    }
}
