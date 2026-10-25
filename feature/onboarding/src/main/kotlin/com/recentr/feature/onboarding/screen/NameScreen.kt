package com.recentr.feature.onboarding.screen

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentType
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.recentr.core.designsystem.component.RecentrTextField
import com.recentr.core.designsystem.motion.entrance
import com.recentr.core.designsystem.theme.RecentrTheme
import com.recentr.feature.onboarding.R
import com.recentr.feature.onboarding.component.ContinueButton
import com.recentr.feature.onboarding.component.HeaderProgress
import com.recentr.feature.onboarding.component.OnboardingHeading
import com.recentr.feature.onboarding.component.OnboardingPage
import kotlinx.coroutines.delay

/**
 * 09 · Onboarding — Name (Figma 2023:901). The iOS keyboard mock-up is the Android IME: the field
 * is focused on entry and the Continue button rides 14dp above the keyboard. The field asks for a
 * first name, so the autofill service can offer the person's own above the keys (iOS: the
 * `givenName` content type).
 */
@Composable
internal fun NameScreen(
    progress: HeaderProgress,
    name: String,
    isValid: Boolean,
    onNameChange: (String) -> Unit,
    onBack: () -> Unit,
    onContinue: () -> Unit,
) {
    val focusRequester = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    LaunchedEffect(Unit) {
        // The keyboard rises once the field has settled in (iOS waits 450ms too).
        delay(450)
        focusRequester.requestFocus()
    }
    val submit = {
        if (isValid) {
            keyboard?.hide()
            onContinue()
        }
    }
    OnboardingPage(
        progress = progress,
        onBack = {
            keyboard?.hide()
            onBack()
        },
        bottomBar = { ContinueButton(enabled = isValid, onClick = submit) },
    ) {
        Spacer(Modifier.height(20.dp))
        OnboardingHeading(title = stringResource(R.string.onb_name_title))
        Spacer(Modifier.height(37.dp))
        RecentrTextField(
            value = name,
            onValueChange = onNameChange,
            placeholder = stringResource(R.string.onb_name_placeholder),
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Words,
                autoCorrectEnabled = false,
                keyboardType = KeyboardType.Text,
                imeAction = ImeAction.Done,
            ),
            keyboardActions = KeyboardActions(onDone = { submit() }),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = RecentrTheme.spacing.screenGutter)
                .entrance(200)
                .focusRequester(focusRequester)
                .semantics { contentType = ContentType.PersonFirstName },
        )
    }
}

@Preview(widthDp = 402, heightDp = 874)
@Composable
private fun NameScreenPreview() {
    RecentrTheme {
        NameScreen(HeaderProgress(0.5f), name = "", isValid = false, onNameChange = {}, onBack = {}, onContinue = {})
    }
}
