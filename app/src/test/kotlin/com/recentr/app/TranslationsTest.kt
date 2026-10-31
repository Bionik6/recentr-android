package com.recentr.app

import com.recentr.core.model.AppLanguage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.w3c.dom.Element
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

/**
 * The translations of every module: each language has every string, with the same placeholders,
 * and the plural forms of its language. Settings › Language and Android's per-app language
 * settings offer the same languages.
 */
class TranslationsTest {

    /** The repository: unit tests run in the module's folder. */
    private val root = File(System.getProperty("user.dir")).parentFile

    /** The resource folder of each translation. Android keeps Hebrew and Indonesian under their former codes. */
    private val folders = mapOf(
        "ar" to "values-ar",
        "bg" to "values-bg",
        "zh-Hans" to "values-b+zh+Hans",
        "cs" to "values-cs",
        "fr" to "values-fr",
        "de" to "values-de",
        "he" to "values-iw",
        "hi" to "values-hi",
        "id" to "values-in",
        "it" to "values-it",
        "ja" to "values-ja",
        "ko" to "values-ko",
        "pl" to "values-pl",
        "pt-BR" to "values-pt-rBR",
        "ru" to "values-ru",
        "es" to "values-es",
        "sv" to "values-sv",
        "tr" to "values-tr",
        "uk" to "values-uk",
    )

    /** The CLDR plural categories of each language, which its `<plurals>` must all have. */
    private val pluralCategories = mapOf(
        "ar" to setOf("zero", "one", "two", "few", "many", "other"),
        "bg" to setOf("one", "other"),
        "zh-Hans" to setOf("other"),
        "cs" to setOf("one", "few", "many", "other"),
        "fr" to setOf("one", "many", "other"),
        "de" to setOf("one", "other"),
        "he" to setOf("one", "two", "other"),
        "hi" to setOf("one", "other"),
        "id" to setOf("other"),
        "it" to setOf("one", "many", "other"),
        "ja" to setOf("other"),
        "ko" to setOf("other"),
        "pl" to setOf("one", "few", "many", "other"),
        "pt-BR" to setOf("one", "many", "other"),
        "ru" to setOf("one", "few", "many", "other"),
        "es" to setOf("one", "many", "other"),
        "sv" to setOf("one", "other"),
        "tr" to setOf("one", "other"),
        "uk" to setOf("one", "few", "many", "other"),
    )

    private val placeholder = Regex("""%(\d+\$)?[-#+ 0,(]*\d*(\.\d+)?[sdfxXc]""")

    private class Strings(
        val strings: Map<String, String>,
        val plurals: Map<String, Map<String, String>>,
        /** Weekdays, months, level names. */
        val arrays: Map<String, List<String>>,
    )

    /** Every module's English strings (`values/strings.xml`), by module folder. */
    private val modules: Map<File, Strings> by lazy {
        root.walkTopDown()
            .onEnter { it.name != "build" && !it.name.startsWith(".") }
            .filter { it.name == "strings.xml" && it.parentFile.name == "values" }
            .associate { it.parentFile.parentFile to read(it) }
    }

    private fun read(file: File): Strings {
        val document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(file)
        val strings = mutableMapOf<String, String>()
        val plurals = mutableMapOf<String, Map<String, String>>()
        val arrays = mutableMapOf<String, List<String>>()
        val nodes = document.documentElement.childNodes
        for (i in 0 until nodes.length) {
            val element = nodes.item(i) as? Element ?: continue
            if (element.getAttribute("translatable") == "false") continue
            val name = element.getAttribute("name")
            when (element.tagName) {
                "string" -> strings[name] = element.textContent
                "plurals" -> {
                    val items = element.getElementsByTagName("item")
                    plurals[name] = (0 until items.length).map { items.item(it) as Element }
                        .associate { it.getAttribute("quantity") to it.textContent }
                }
                "string-array" -> {
                    val items = element.getElementsByTagName("item")
                    arrays[name] = (0 until items.length).map { items.item(it).textContent }
                }
            }
        }
        return Strings(strings, plurals, arrays)
    }

    /** Each module's translations into [language]. */
    private fun translations(language: String): Map<File, Strings> = modules.keys.associateWith { res ->
        val file = File(res, "${folders.getValue(language)}/strings.xml")
        assertTrue("${file.relativeTo(root)} is missing", file.exists())
        read(file)
    }

    /** The placeholders of [text]: a literal percent sign (`%%`) isn't one, nor what follows it ("50 %% de"). */
    private fun placeholders(text: String) = placeholder.findAll(text.replace("%%", "")).map { it.value }.sorted().toList()

    /** Runs [check] on every module and language, and fails with every problem found. */
    private fun forEachTranslation(check: (english: Strings, translated: Strings, language: String, problems: MutableList<String>) -> Unit) {
        val problems = mutableListOf<String>()
        folders.keys.forEach { language ->
            translations(language).forEach { (res, translated) ->
                val before = problems.size
                check(modules.getValue(res), translated, language, problems)
                for (i in before until problems.size) problems[i] = "${res.relativeTo(root)} [$language] ${problems[i]}"
            }
        }
        assertTrue(problems.joinToString("\n", prefix = "${problems.size} problem(s):\n"), problems.isEmpty())
    }

    @Test
    fun `Settings and Android offer every language Recentr is translated into`() {
        val config = DocumentBuilderFactory.newInstance().newDocumentBuilder()
            .parse(File(root, "app/src/main/res/xml/locales_config.xml"))
            .getElementsByTagName("locale")
        val offered = (0 until config.length).map { (config.item(it) as Element).getAttribute("android:name") }
        assertEquals(AppLanguage.entries.map { it.tag }, offered)
        assertEquals(AppLanguage.entries.map { it.tag }.toSet() - "en", folders.keys)
    }

    @Test
    fun `every string is translated into every language`() {
        assertTrue(modules.size >= 8)
        forEachTranslation { english, translated, _, problems ->
            (english.strings.keys - translated.strings.keys).forEach { problems += "missing string $it" }
            (translated.strings.keys - english.strings.keys).forEach { problems += "unknown string $it" }
            (english.plurals.keys - translated.plurals.keys).forEach { problems += "missing plurals $it" }
            (translated.plurals.keys - english.plurals.keys).forEach { problems += "unknown plurals $it" }
            english.arrays.forEach { (name, items) ->
                val translatedItems = translated.arrays[name]
                if (translatedItems?.size != items.size) problems += "array $name: ${translatedItems?.size ?: "missing"}, not ${items.size} items"
            }
            (translated.arrays.keys - english.arrays.keys).forEach { problems += "unknown array $it" }
            (
                translated.strings +
                    translated.plurals.flatMap { (name, items) -> items.map { "$name.${it.key}" to it.value } } +
                    translated.arrays.flatMap { (name, items) -> items.mapIndexed { i, item -> "$name[$i]" to item } }
                )
                .filter { it.value.isBlank() }
                .forEach { problems += "empty ${it.key}" }
        }
    }

    @Test
    fun `translations keep the placeholders`() {
        forEachTranslation { english, translated, _, problems ->
            english.strings.forEach { (name, text) ->
                val translation = translated.strings[name] ?: return@forEach
                if (placeholders(text) != placeholders(translation)) problems += "$name: \"$translation\""
            }
            english.plurals.forEach { (name, items) ->
                val expected = placeholders(items.getValue("other"))
                val count = placeholders(items.getValue("other")).firstOrNull { it.endsWith("d") }
                translated.plurals[name]?.forEach { (quantity, translation) ->
                    val found = placeholders(translation)
                    // Zero, one and two may spell the number out instead.
                    val spelledOut = quantity in setOf("zero", "one", "two") && count != null && (found + count).sorted() == expected
                    if (found != expected && !spelledOut) problems += "$name.$quantity: \"$translation\""
                }
            }
        }
    }

    @Test
    fun `plurals have every form of their language`() {
        forEachTranslation { _, translated, language, problems ->
            translated.plurals.forEach { (name, items) ->
                if (items.keys != pluralCategories.getValue(language)) problems += "$name: ${items.keys}"
            }
        }
    }
}
