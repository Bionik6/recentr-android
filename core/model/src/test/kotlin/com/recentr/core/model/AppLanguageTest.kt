package com.recentr.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.Locale

/** The languages Recentr is translated into, and which one a stored or system tag stands for. */
class AppLanguageTest {

    @Test
    fun `Recentr speaks twenty languages, in the order of their English names`() {
        assertEquals(
            listOf(
                "ar", "bg", "zh-Hans", "cs", "en", "fr", "de", "he", "hi", "id",
                "it", "ja", "ko", "pl", "pt-BR", "ru", "es", "sv", "tr", "uk",
            ),
            AppLanguage.entries.map { it.tag },
        )
        assertEquals(AppLanguage.entries.sortedBy { it.name }, AppLanguage.entries)
    }

    @Test
    fun `every tag reads back as its language`() {
        AppLanguage.entries.forEach { assertEquals(it, AppLanguage.fromTag(it.tag)) }
    }

    @Test
    fun `a region doesn't change the language`() {
        assertEquals(AppLanguage.English, AppLanguage.fromTag("en-GB"))
        assertEquals(AppLanguage.French, AppLanguage.fromTag("fr-CA"))
        assertEquals(AppLanguage.Spanish, AppLanguage.fromTag("es-MX"))
        // Earlier versions kept Portuguese as "pt".
        assertEquals(AppLanguage.Portuguese, AppLanguage.fromTag("pt"))
        assertEquals(AppLanguage.Portuguese, AppLanguage.forLocale(Locale.forLanguageTag("pt-PT")))
    }

    @Test
    fun `Hebrew and Indonesian are found by their former codes too`() {
        assertEquals(AppLanguage.Hebrew, AppLanguage.fromTag("iw"))
        assertEquals(AppLanguage.Indonesian, AppLanguage.fromTag("in"))
        assertEquals(AppLanguage.Hebrew, AppLanguage.fromTag("iw-IL"))
        assertEquals(AppLanguage.Indonesian, AppLanguage.fromTag("in-ID"))
    }

    @Test
    fun `Chinese is only translated in simplified characters`() {
        assertEquals(AppLanguage.Chinese, AppLanguage.fromTag("zh"))
        assertEquals(AppLanguage.Chinese, AppLanguage.fromTag("zh-CN"))
        assertEquals(AppLanguage.Chinese, AppLanguage.fromTag("zh-Hans-SG"))
        assertNull(AppLanguage.fromTag("zh-TW"))
        assertNull(AppLanguage.fromTag("zh-Hant"))
        assertNull(AppLanguage.fromTag("zh-HK"))
    }

    @Test
    fun `an untranslated language or no tag at all follows the system`() {
        // Vietnamese was offered before translations shipped.
        assertNull(AppLanguage.fromTag("vi"))
        assertNull(AppLanguage.fromTag("nl"))
        assertNull(AppLanguage.fromTag(""))
        assertNull(AppLanguage.fromTag(null))
    }
}
