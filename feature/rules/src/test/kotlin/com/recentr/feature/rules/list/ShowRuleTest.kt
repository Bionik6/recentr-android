package com.recentr.feature.rules.list

import com.recentr.core.data.RulesRepository
import com.recentr.core.model.AppCategory
import com.recentr.core.model.Rule
import com.recentr.core.model.RuleType
import com.recentr.core.model.StarterRoutine
import com.recentr.core.model.UsageApp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

/** Home's next rule tile shows its rule on Rules: the list opens its group and scrolls to its card. */
@OptIn(ExperimentalCoroutinesApi::class)
class ShowRuleTest {

    private val instagram = UsageApp("com.instagram.android", "Instagram", AppCategory.Social)
    private val workingTime = StarterRoutine.WorkingTime.rule("work", "Working Time", listOf(instagram))
    private val nightPause = StarterRoutine.NightPause.rule("night", "Night Pause", listOf(instagram))
    private val weekendZen = StarterRoutine.WeekendZen.rule("zen", "Weekend Zen", listOf(instagram))
    private val socialTime = Rule("limit", RuleType.Limit, "Social time", apps = listOf(instagram), limitMinutes = 30)

    private val dispatcher = UnconfinedTestDispatcher()
    private val rules = RulesRepository(file = null)

    private fun TestScope.viewModel(): RulesViewModel {
        val vm = RulesViewModel(rules, MutableStateFlow(true))
        backgroundScope.launch { vm.state.collect {} }
        return vm
    }

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `showing a rule leaves the templates and opens its group only`() = runTest(dispatcher) {
        listOf(workingTime, nightPause, weekendZen, socialTime).forEach(rules::create)
        val vm = viewModel()
        vm.showTemplates(true)
        vm.toggleGroup(RuleGroup.Routines)
        vm.toggleGroup(RuleGroup.Limits)

        vm.showRule("night")

        val state = vm.state.value
        assertFalse(state.showTemplates)
        assertEquals(setOf(RuleGroup.Limits), state.collapsed)
        assertEquals("night", state.ruleToShow)
        val index = state.indexOfRule("night")!!
        assertEquals("night", (state.listItems[index] as RulesListItem.Card).rule.id)
    }

    @Test
    fun `the list scrolls to it once`() = runTest(dispatcher) {
        rules.create(nightPause)
        val vm = viewModel()

        vm.showRule("night")
        vm.ruleShown()

        assertNull(vm.state.value.ruleToShow)
    }

    @Test
    fun `a rule since deleted shows the rules as they are`() = runTest(dispatcher) {
        rules.create(nightPause)
        val vm = viewModel()
        vm.showTemplates(true)
        vm.toggleGroup(RuleGroup.Routines)

        vm.showRule("gone")

        val state = vm.state.value
        assertFalse(state.showTemplates)
        assertEquals(setOf(RuleGroup.Routines), state.collapsed)
        assertNull(state.ruleToShow)
    }

    /** The rows of the list, as it draws them: the cards of the open groups only. */
    @Test
    fun `a rule's card is found among the rows`() {
        val state = RulesUiState(
            sections = listOf(
                RuleSection(RuleGroup.Sessions, emptyList()),
                RuleSection(RuleGroup.Limits, listOf(socialTime)),
                RuleSection(RuleGroup.Routines, listOf(workingTime, nightPause, weekendZen)),
            ),
            blockingOn = false,
        )

        assertEquals(
            listOf(
                "header", "blocking-off",
                "group-Sessions", "add-Sessions",
                "group-Limits", "limit",
                "group-Routines", "work", "night", "zen", "add-Routines",
                "templates-header", "templates-row",
            ),
            state.listItems.map { it.key },
        )
        assertEquals(8, state.indexOfRule("night"))
        assertEquals(5, state.indexOfRule("limit"))
        assertNull(state.indexOfRule("gone"))

        val collapsed = state.copy(collapsed = setOf(RuleGroup.Routines), blockingOn = true)
        assertNull(collapsed.indexOfRule("night"))
        assertEquals(4, collapsed.indexOfRule("limit"))
    }
}
