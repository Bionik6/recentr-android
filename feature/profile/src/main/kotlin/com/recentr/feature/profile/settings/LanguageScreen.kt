package com.recentr.feature.profile.settings

import android.app.LocaleManager
import android.content.Context
import android.os.Build
import android.os.LocaleList
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.recentr.core.designsystem.component.OptionRow
import com.recentr.core.model.AppLanguage
import com.recentr.feature.profile.R
import com.recentr.feature.profile.ui.SettingsPage

/** 70 · Settings — Language: one radio row per language. */
@Composable
internal fun LanguageScreen(
    selected: AppLanguage?,
    onBack: () -> Unit,
    onSelect: (AppLanguage) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val current = selected ?: systemLanguage()
    SettingsPage(title = stringResource(R.string.language_title), onBack = onBack, modifier = modifier) {
        Column(Modifier.selectableGroup(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            AppLanguage.entries.forEach { language ->
                OptionRow(
                    label = language.endonym,
                    selected = language == current,
                    onClick = {
                        if (language != current) {
                            onSelect(language)
                            applyAppLanguage(context, language)
                        }
                    },
                    role = Role.RadioButton,
                    radio = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

/**
 * Per-app language (Android 13+): the system recreates the activity in [language]. Earlier
 * versions keep the choice for when translations ship; the app follows the system language.
 */
internal fun applyAppLanguage(context: Context, language: AppLanguage) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        context.getSystemService(LocaleManager::class.java)?.applicationLocales = LocaleList.forLanguageTags(language.tag)
    }
}
