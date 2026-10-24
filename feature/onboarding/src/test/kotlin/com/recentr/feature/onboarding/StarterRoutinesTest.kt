package com.recentr.feature.onboarding

import com.recentr.core.data.RulesRepository
import com.recentr.core.model.AppCategory
import com.recentr.core.model.Rule
import com.recentr.core.model.RuleSchedule
import com.recentr.core.model.RuleSticker
import com.recentr.core.model.RuleType
import com.recentr.core.model.StarterRoutine
import com.recentr.core.model.UnlockMethod
import com.recentr.core.model.UsageApp
import com.recentr.core.model.UserProfile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Completing the onboarding sets up Working Time, Night Pause and Weekend Zen, blocking the social
 * apps installed (docs/RULES.md › Starter routines).
 */
@OptIn(ExperimentalCoroutinesApi::class)
class StarterRoutinesTest {

    private val instagram = UsageApp("com.instagram.android", "Instagram", AppCategory.Social)
    private val tiktok = UsageApp("com.zhiliaoapp.musically", "TikTok", AppCategory.Social)
    private val youtube = UsageApp("com.google.android.youtube", "YouTube", AppCategory.Entertainment)
    private val maps = UsageApp("com.google.android.apps.maps", "Maps", AppCategory.Other)
    private val messages = UsageApp("com.google.android.apps.messaging", "Messages", AppCategory.Social)

    private val dispatcher = UnconfinedTestDispatcher()
    private val profile = MutableStateFlow(UserProfile())
    private val rules = RulesRepository(file = null)
    private var installed = listOf(instagram, maps, tiktok, youtube)
    private var smsApp: String? = null

    private val names = mapOf(
        StarterRoutine.WorkingTime to "Working Time",
        StarterRoutine.NightPause to "Night Pause",
        StarterRoutine.WeekendZen to "Weekend Zen",
    )

    private fun viewModel() = OnboardingViewModel(
        profile = profile,
        updateProfile = { transform -> profile.update(transform) },
        rules = rules,
        installedApps = { installed },
        routineName = names::getValue,
        smsApp = { smsApp },
    )

    /** Completes the onboarding, and says whether it moved on. */
    private fun OnboardingViewModel.completed(): Boolean {
        var done = false
        complete { done = true }
        return done
    }

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `completing sets up the three routines, blocking the social apps`() = runTest(dispatcher) {
        assertTrue(viewModel().completed())

        assertTrue(profile.value.onboardingCompleted)
        val routines = rules.rules.value
        assertEquals(listOf("Working Time", "Night Pause", "Weekend Zen"), routines.map { it.name })
        assertEquals(
            listOf(
                RuleSchedule(9 * 60, 17 * 60, setOf(1, 2, 3, 4, 5)),
                RuleSchedule(23 * 60, 7 * 60, setOf(1, 2, 3, 4, 5, 6, 7)),
                RuleSchedule(9 * 60, 12 * 60, setOf(6, 7)),
            ),
            routines.map { it.schedule },
        )
        assertEquals(listOf(RuleSticker.Bolt, RuleSticker.Moon, RuleSticker.Leaf), routines.map { it.sticker })
        assertEquals(3, routines.map(Rule::id).toSet().size)
        routines.forEach { routine ->
            assertEquals(RuleType.Bedtime, routine.type)
            assertEquals(listOf(instagram, tiktok), routine.apps)
            assertTrue(routine.enabled)
        }
    }

    /** Google Messages says it's social, but as the SMS app it stays at hand: texts, sign-in codes. */
    @Test
    fun `the default SMS app isn't blocked`() = runTest(dispatcher) {
        installed = listOf(instagram, messages, tiktok)
        smsApp = messages.packageName

        viewModel().completed()

        assertEquals(3, rules.rules.value.size)
        rules.rules.value.forEach { routine -> assertEquals(listOf(instagram, tiktok), routine.apps) }
    }

    @Test
    fun `no routine is strict, and each unlocks after a 10-second wait`() = runTest(dispatcher) {
        viewModel().completed()

        val routines = rules.rules.value
        assertEquals(3, routines.size)
        routines.forEach { routine ->
            assertFalse(routine.strictMode)
            assertFalse(routine.isStrictLocked(System.currentTimeMillis()))
            assertEquals(UnlockMethod.Wait, routine.unlockMethod)
            assertEquals(10, routine.waitSeconds)
        }
    }

    @Test
    fun `without social apps the routines are set up all the same, for the user to fill`() = runTest(dispatcher) {
        installed = listOf(maps, youtube)

        viewModel().completed()

        assertEquals(listOf("Working Time", "Night Pause", "Weekend Zen"), rules.rules.value.map { it.name })
        assertTrue(rules.rules.value.all { it.apps.isEmpty() })
    }

    /** Back from logging out (or replaying the onboarding), the rules kept stay as they are. */
    @Test
    fun `someone with rules gets no starter routines`() = runTest(dispatcher) {
        val focus = Rule("focus", RuleType.Session, "Focus", apps = listOf(instagram), durationMinutes = 30)
        rules.create(focus)

        assertTrue(viewModel().completed())

        assertEquals(listOf("Focus"), rules.rules.value.map { it.name })
    }

    @Test
    fun `completing twice sets them up once`() = runTest(dispatcher) {
        viewModel().completed()
        viewModel().completed()

        assertEquals(3, rules.rules.value.size)
    }
}
