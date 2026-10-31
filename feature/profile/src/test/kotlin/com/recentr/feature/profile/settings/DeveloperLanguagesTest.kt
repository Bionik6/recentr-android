package com.recentr.feature.profile.settings

import com.recentr.core.model.AppLanguage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Developer › Language: its rows, the row of the per-app locales, and what Settings › Language keeps. */
class DeveloperLanguagesTest {

    @Test
    fun `the system's language comes first, then every translation, then the pseudo-locales`() {
        assertEquals(listOf(null) + AppLanguage.entries.map { it.tag } + listOf("en-XA", "ar-XB"), DeveloperLanguages.tags)
    }

    @Test
    fun `no per-app locale selects the system's language`() {
        assertNull(DeveloperLanguages.selected(""))
    }

    @Test
    fun `a per-app locale selects its language`() {
        assertEquals("fr", DeveloperLanguages.selected("fr"))
        assertEquals("pt-BR", DeveloperLanguages.selected("pt-BR"))
        assertEquals("fr", DeveloperLanguages.selected("fr,de"))
        // Android may hand Hebrew back by its former code.
        assertEquals("he", DeveloperLanguages.selected("iw"))
        assertEquals("en-XA", DeveloperLanguages.selected("en-XA"))
        assertEquals("ar-XB", DeveloperLanguages.selected("ar-XB"))
    }

    @Test
    fun `Settings › Language keeps the language, and nothing for the system or a pseudo-locale`() {
        assertEquals(AppLanguage.Portuguese, DeveloperLanguages.stored("pt-BR"))
        assertEquals(AppLanguage.Arabic, DeveloperLanguages.stored("ar"))
        assertNull(DeveloperLanguages.stored(null))
        assertNull(DeveloperLanguages.stored("en-XA"))
        assertNull(DeveloperLanguages.stored("ar-XB"))
    }
}
