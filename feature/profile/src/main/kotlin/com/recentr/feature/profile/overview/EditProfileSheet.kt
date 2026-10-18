package com.recentr.feature.profile.overview

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.recentr.core.designsystem.component.RecentrAvatar
import com.recentr.core.designsystem.component.RecentrButton
import com.recentr.core.designsystem.component.RecentrSheet
import com.recentr.core.designsystem.component.RecentrTextField
import com.recentr.core.designsystem.component.SettingsSectionLabel
import com.recentr.core.designsystem.icon.RecentrIcon
import com.recentr.core.designsystem.icon.RecentrIconImage
import com.recentr.core.designsystem.theme.RecentrTheme
import com.recentr.core.model.AvatarColor
import com.recentr.feature.profile.R
import com.recentr.feature.profile.ui.SheetContent
import com.recentr.feature.profile.ui.style

/** 62 · Profile — Edit: avatar (tap to change its colour), name, email, "Save changes". */
@Composable
internal fun EditProfileSheet(
    name: String,
    email: String,
    avatar: AvatarColor,
    onSave: (name: String, email: String, avatar: AvatarColor) -> Unit,
    onDismissRequest: () -> Unit,
) {
    var nameValue by rememberSaveable { mutableStateOf(name) }
    var emailValue by rememberSaveable { mutableStateOf(email) }
    var avatarValue by rememberSaveable { mutableStateOf(avatar) }
    val focusManager = LocalFocusManager.current
    RecentrSheet(onDismissRequest = onDismissRequest) { controller ->
        SheetContent(topGap = 18.dp) {
            Text(stringResource(R.string.edit_title), style = RecentrTheme.typography.title1, color = RecentrTheme.colors.textPrimary)
            Spacer(Modifier.height(18.dp))
            val changeColour = stringResource(R.string.edit_avatar_colour)
            // Figma "Avatar edit": 96dp avatar, 32dp black pencil badge at (66, 66) overflowing it.
            Box(Modifier.size(96.dp)) {
                Box(
                    Modifier
                        .size(96.dp)
                        // Unclipped so the avatar keeps its shadow; the ripple stays round.
                        .clickable(interactionSource = null, indication = ripple(bounded = false, radius = 48.dp), role = Role.Button) {
                            avatarValue = avatarValue.next()
                        }
                        .semantics { contentDescription = changeColour },
                ) {
                    val initial = nameValue.trim().firstOrNull()?.uppercase() ?: "?"
                    RecentrAvatar(initial = initial, style = avatarValue.style, size = 96.dp)
                }
                Box(
                    Modifier
                        .offset(x = 66.dp, y = 66.dp)
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(RecentrTheme.colors.actionPrimary),
                    contentAlignment = Alignment.Center,
                ) {
                    RecentrIconImage(RecentrIcon.Pencil, tint = RecentrTheme.colors.textOnAction, size = 16.08.dp)
                }
            }
            Spacer(Modifier.height(22.dp))
            SettingsSectionLabel(stringResource(R.string.edit_name), Modifier.fillMaxWidth())
            Spacer(Modifier.height(6.dp))
            RecentrTextField(
                value = nameValue,
                onValueChange = { nameValue = it.take(30) },
                placeholder = stringResource(R.string.edit_name_placeholder),
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(14.dp))
            SettingsSectionLabel(stringResource(R.string.edit_email), Modifier.fillMaxWidth())
            Spacer(Modifier.height(6.dp))
            RecentrTextField(
                value = emailValue,
                onValueChange = { emailValue = it.trim() },
                placeholder = stringResource(R.string.edit_email_placeholder),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(210.dp))
            RecentrButton(
                text = stringResource(R.string.edit_save),
                onClick = {
                    onSave(nameValue, emailValue, avatarValue)
                    controller.close()
                },
                enabled = nameValue.isNotBlank(),
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
