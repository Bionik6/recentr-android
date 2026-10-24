package com.recentr.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

/** The routines onboarding sets up (docs/RULES.md › Starter routines). */
class StarterRoutineTest {

    private val zone = ZoneId.of("Europe/Paris")

    /** [day] Oct 2026 at [hour]:[minutes]; Monday 19 Oct by default, Saturday 24, Sunday 25. */
    private fun at(hour: Int, minutes: Int = 0, day: Int = 19): Long =
        LocalDate.of(2026, 10, day).atTime(hour, minutes).atZone(zone).toInstant().toEpochMilli()

    private val instagram = UsageApp("com.instagram.android", "Instagram", AppCategory.Social)
    private val tiktok = UsageApp("com.zhiliaoapp.musically", "TikTok", AppCategory.Social)
    private val youtube = UsageApp("com.google.android.youtube", "YouTube", AppCategory.Entertainment)
    private val maps = UsageApp("com.google.android.apps.maps", "Maps", AppCategory.Other)
    private val messages = UsageApp("com.google.android.apps.messaging", "Messages", AppCategory.Social)

    private fun StarterRoutine.blocksAt(epochMillis: Long) = rule("id", name, listOf(instagram)).isEnforcing(epochMillis, zone)

    @Test
    fun `they block the social apps installed`() {
        assertEquals(listOf(instagram, tiktok), StarterRoutine.apps(listOf(instagram, maps, tiktok, youtube)))
        assertEquals(emptyList<UsageApp>(), StarterRoutine.apps(listOf(maps, youtube)))
    }

    /** Google Messages says it's social: texts and sign-in codes stay at hand all the same. */
    @Test
    fun `they leave the default SMS app out`() {
        val installed = listOf(instagram, maps, messages, tiktok)
        assertEquals(listOf(instagram, tiktok), StarterRoutine.apps(installed, smsApp = messages.packageName))
        assertEquals(listOf(instagram, messages, tiktok), StarterRoutine.apps(installed, smsApp = null))
        assertEquals(emptyList<UsageApp>(), StarterRoutine.apps(listOf(messages, maps), smsApp = messages.packageName))
    }

    @Test
    fun `each is a routine with a 10-second wait, never strict`() {
        StarterRoutine.entries.forEach { routine ->
            val rule = routine.rule("id", "Name", listOf(instagram, tiktok))
            assertEquals(RuleType.Bedtime, rule.type)
            assertEquals(listOf(instagram, tiktok), rule.apps)
            assertFalse(rule.strictMode)
            assertEquals(UnlockMethod.Wait, rule.unlockMethod)
            assertEquals(10, rule.waitSeconds)
            assertTrue(rule.enabled)
            assertEquals(null, rule.reminderMinutesBefore)
        }
    }

    @Test
    fun `working time blocks on weekdays from 9 to 5`() {
        assertEquals(RuleSchedule(9 * 60, 17 * 60, setOf(1, 2, 3, 4, 5)), StarterRoutine.WorkingTime.schedule)
        assertEquals(RuleSticker.Bolt, StarterRoutine.WorkingTime.sticker)
        assertFalse(StarterRoutine.WorkingTime.blocksAt(at(8, 59)))
        assertTrue(StarterRoutine.WorkingTime.blocksAt(at(9)))
        assertTrue(StarterRoutine.WorkingTime.blocksAt(at(16, 59, day = 23)))
        assertFalse(StarterRoutine.WorkingTime.blocksAt(at(17)))
        assertFalse(StarterRoutine.WorkingTime.blocksAt(at(10, day = 24)))
    }

    @Test
    fun `night pause blocks every night from 11 PM to 7 AM`() {
        assertEquals(RuleSchedule(23 * 60, 7 * 60, (1..7).toSet()), StarterRoutine.NightPause.schedule)
        assertEquals(RuleSticker.Moon, StarterRoutine.NightPause.sticker)
        assertFalse(StarterRoutine.NightPause.blocksAt(at(22, 59)))
        assertTrue(StarterRoutine.NightPause.blocksAt(at(23)))
        assertTrue(StarterRoutine.NightPause.blocksAt(at(6, 59, day = 25)))
        assertFalse(StarterRoutine.NightPause.blocksAt(at(7, day = 25)))
    }

    @Test
    fun `weekend zen blocks on weekend mornings from 9 to noon`() {
        assertEquals(RuleSchedule(9 * 60, 12 * 60, setOf(6, 7)), StarterRoutine.WeekendZen.schedule)
        assertEquals(RuleSticker.Leaf, StarterRoutine.WeekendZen.sticker)
        assertTrue(StarterRoutine.WeekendZen.blocksAt(at(9, day = 24)))
        assertTrue(StarterRoutine.WeekendZen.blocksAt(at(11, 59, day = 25)))
        assertFalse(StarterRoutine.WeekendZen.blocksAt(at(12, day = 25)))
        assertFalse(StarterRoutine.WeekendZen.blocksAt(at(10)))
    }
}
