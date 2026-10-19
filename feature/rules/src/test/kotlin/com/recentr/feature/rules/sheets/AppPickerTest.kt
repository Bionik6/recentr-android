package com.recentr.feature.rules.sheets

import com.recentr.core.model.AppCategory
import com.recentr.core.model.UsageApp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** "Choose apps": categories, their boxes, "All apps & categories" and the search. */
class AppPickerTest {

    private fun app(name: String, category: AppCategory) = UsageApp("app.$name", name, category)

    private val instagram = app("Instagram", AppCategory.Social)
    private val snapchat = app("Snapchat", AppCategory.Social)
    private val youTube = app("YouTube", AppCategory.Entertainment)
    private val roblox = app("Roblox", AppCategory.Games)
    private val chrome = app("Chrome", AppCategory.Other)
    private val apps = listOf(chrome, instagram, roblox, snapchat, youTube)

    private fun names(vararg apps: UsageApp) = apps.map { it.packageName }.toSet()

    @Test
    fun `categories come Social first, without the empty ones`() {
        val groups = AppPicker.groups(apps)
        assertEquals(listOf(AppCategory.Social, AppCategory.Games, AppCategory.Entertainment, AppCategory.Other), groups.map { it.first })
        assertEquals(listOf(instagram, snapchat), groups.first().second)
    }

    @Test
    fun `a category's box picks all its apps, then none`() {
        val social = listOf(instagram, snapchat)
        val picked = AppPicker.toggle(social, names(youTube))
        assertEquals(names(youTube, instagram, snapchat), picked)
        assertEquals(Picked.All, AppPicker.picked(social, picked))
        val unpicked = AppPicker.toggle(social, picked)
        assertEquals(names(youTube), unpicked)
        assertEquals(Picked.None, AppPicker.picked(social, unpicked))
    }

    @Test
    fun `a partly picked category is completed by its box`() {
        val social = listOf(instagram, snapchat)
        assertEquals(Picked.Some, AppPicker.picked(social, names(instagram)))
        assertEquals(names(instagram, snapchat), AppPicker.toggle(social, names(instagram)))
    }

    @Test
    fun `all apps and categories picks every app, then none`() {
        val all = AppPicker.toggle(apps, names(roblox))
        assertEquals(apps.map { it.packageName }.toSet(), all)
        assertEquals(Picked.All, AppPicker.picked(apps, all))
        assertTrue(AppPicker.toggle(apps, all).isEmpty())
    }

    @Test
    fun `the search matches names in any case, across categories`() {
        assertEquals(listOf(snapchat), AppPicker.search(apps, "  snap "))
        assertEquals(listOf(chrome, roblox), AppPicker.search(apps, "RO"))
        assertTrue(AppPicker.search(apps, "   ").isEmpty())
    }

    @Test
    fun `the picked apps keep the picker's order`() {
        assertEquals(listOf(instagram, youTube), AppPicker.pickedApps(apps, names(youTube, instagram)))
    }
}
