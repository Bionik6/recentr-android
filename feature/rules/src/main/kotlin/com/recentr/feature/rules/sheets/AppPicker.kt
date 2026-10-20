package com.recentr.feature.rules.sheets

import com.recentr.core.model.AppCategory
import com.recentr.core.model.UsageApp

/** How much of a group of apps (a category, or every app) is picked. */
internal enum class Picked { None, Some, All }

/**
 * The selection logic of "Choose apps", like iOS' app picker: apps grouped by category (Social
 * first), a category's box picks or unpicks all of its apps, "All apps & categories" every app.
 * Picks are package names.
 */
internal object AppPicker {

    /** Social first, as on iOS, then the categories people block most. */
    val CategoryOrder = listOf(
        AppCategory.Social,
        AppCategory.Games,
        AppCategory.Entertainment,
        AppCategory.Music,
        AppCategory.Productivity,
        AppCategory.Other,
    )

    /** [apps] by category in [CategoryOrder], without the empty ones. */
    fun groups(apps: List<UsageApp>): List<Pair<AppCategory, List<UsageApp>>> =
        CategoryOrder.map { category -> category to apps.filter { it.category == category } }.filter { it.second.isNotEmpty() }

    fun picked(apps: List<UsageApp>, picks: Set<String>): Picked {
        val count = apps.count { it.packageName in picks }
        return when {
            count == 0 -> Picked.None
            count == apps.size -> Picked.All
            else -> Picked.Some
        }
    }

    /** Tapping a group's box: all of [apps] picked, or none of them once they all were. */
    fun toggle(apps: List<UsageApp>, picks: Set<String>): Set<String> {
        val names = apps.map { it.packageName }.toSet()
        return if (picked(apps, picks) == Picked.All) picks - names else picks + names
    }

    /** The apps whose name holds [query] (any case), or none for a blank query. */
    fun search(apps: List<UsageApp>, query: String): List<UsageApp> {
        val text = query.trim()
        return if (text.isEmpty()) emptyList() else apps.filter { it.label.contains(text, ignoreCase = true) }
    }

    /** The picked apps, in the picker's order. */
    fun pickedApps(apps: List<UsageApp>, picks: Set<String>): List<UsageApp> = apps.filter { it.packageName in picks }
}
