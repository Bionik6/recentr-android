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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.recentr.core.designsystem.component.OptionRow
import com.recentr.core.designsystem.component.OptionTrailing
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
                            applyAppLocales(context, language.tag)
                        }
                    },
                    trailing = OptionTrailing.Radio,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

/**
 * Settings › Developer › Language (debug builds, Android 13+): switches Recentr's language right
 * away, back to the system's, or to a pseudo-locale that shows untranslated text (en-XA) and
 * right-to-left layouts (ar-XB).
 */
@Composable
internal fun DeveloperLanguageScreen(
    onBack: () -> Unit,
    onSelect: (AppLanguage?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var current by remember { mutableStateOf(DeveloperLanguages.selected(appLocales(context))) }
    SettingsPage(title = stringResource(R.string.language_title), onBack = onBack, modifier = modifier) {
        Column(Modifier.selectableGroup(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            DeveloperLanguages.tags.forEach { tag ->
                OptionRow(
                    label = developerLanguageLabel(tag),
                    selected = tag == current,
                    onClick = {
                        if (tag != current) {
                            current = tag
                            onSelect(DeveloperLanguages.stored(tag))
                            applyAppLocales(context, tag.orEmpty())
                        }
                    },
                    trailing = OptionTrailing.Radio,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

/** What Developer › Language offers, by language tag: null follows the system. */
internal object DeveloperLanguages {
    /** Accented English and right-to-left Arabic, built into debug builds (`isPseudoLocalesEnabled`). */
    val pseudoLocales = listOf("en-XA", "ar-XB")

    val tags: List<String?> = listOf(null) + AppLanguage.entries.map { it.tag } + pseudoLocales

    /** The row of [appLocales], the per-app locales as Android keeps them ("" follows the system). */
    fun selected(appLocales: String): String? {
        val first = appLocales.substringBefore(',').ifEmpty { return null }
        return if (first in pseudoLocales) first else AppLanguage.fromTag(first)?.tag ?: first
    }

    /** What Settings › Language keeps for [tag]: nothing for the system's language or a pseudo-locale. */
    fun stored(tag: String?): AppLanguage? = if (tag == null || tag in pseudoLocales) null else AppLanguage.fromTag(tag)
}

/** The label of a Developer › Language row, and of its value on Settings. */
@Composable
internal fun developerLanguageLabel(tag: String?): String = when (tag) {
    null -> stringResource(R.string.developer_language_system)
    "en-XA" -> stringResource(R.string.developer_language_accented)
    "ar-XB" -> stringResource(R.string.developer_language_rtl)
    else -> AppLanguage.fromTag(tag)?.endonym ?: tag
}

/** The per-app locales, as a tag list ("" when Recentr follows the system, or before Android 13). */
internal fun appLocales(context: Context): String =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        context.getSystemService(LocaleManager::class.java)?.applicationLocales?.toLanguageTags().orEmpty()
    } else {
        ""
    }

/**
 * Per-app language (Android 13+): the system recreates the activity in [tags], or in the system
 * language when empty. Earlier versions keep the choice; the app follows the system language.
 */
internal fun applyAppLocales(context: Context, tags: String) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        context.getSystemService(LocaleManager::class.java)?.applicationLocales = LocaleList.forLanguageTags(tags)
    }
}
