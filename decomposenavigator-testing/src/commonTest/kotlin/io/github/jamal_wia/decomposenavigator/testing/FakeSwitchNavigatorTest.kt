package io.github.jamal_wia.decomposenavigator.testing

import io.github.jamal_wia.decomposenavigator.config.NavigationScreenConfig.SwitchScreenConfigContainer
import io.github.jamal_wia.decomposenavigator.config.ScreenConfig
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

private data object HomeTab : ScreenConfig
private data object TasksTab : ScreenConfig
private data object SettingsTab : ScreenConfig

class FakeSwitchNavigatorTest {

    private val home = SwitchScreenConfigContainer(HomeTab)
    private val tasks = SwitchScreenConfigContainer(TasksTab)
    private val settings = SwitchScreenConfigContainer(SettingsTab)

    @Test
    fun switchTo_movesTargetToTopOfBackStack() {
        val navigator = FakeSwitchNavigator(initial = home)

        navigator.switchTo(tasks)

        assertEquals(tasks, navigator.activeConfig)
        assertEquals(listOf(home, tasks), navigator.backStack)
        assertEquals(listOf(tasks), navigator.switchToCalls)
    }

    @Test
    fun switchTo_sameContainerTwice_doesNotDuplicateInBackStack() {
        val navigator = FakeSwitchNavigator(initial = home)

        navigator.switchTo(tasks)
        navigator.switchTo(home)

        assertEquals(listOf(tasks, home), navigator.backStack)
    }

    @Test
    fun switchToFirst_opensExistingMatchWithoutCreating() {
        val navigator = FakeSwitchNavigator(initial = home)
        navigator.switchTo(tasks)
        navigator.switchTo(home)
        var createCalled = false

        val found: Boolean = navigator.switchToFirst(
            createIfAbsent = { createCalled = true; null },
            predicate = { it == tasks },
        )

        assertTrue(found)
        assertFalse(createCalled)
        assertEquals(tasks, navigator.activeConfig)
    }

    @Test
    fun switchToFirst_withNoMatchAndNoFallback_returnsFalse() {
        val navigator = FakeSwitchNavigator(initial = home)

        val found: Boolean = navigator.switchToFirst(predicate = { it == tasks })

        assertFalse(found)
        assertEquals(home, navigator.activeConfig)
    }

    @Test
    fun switchToFirstAndAwaitReady_behavesLikeSwitchToFirst() = runTest {
        val navigator = FakeSwitchNavigator(initial = home)

        val found: Boolean = navigator.switchToFirstAndAwaitReady(
            createIfAbsent = { tasks },
            predicate = { it == tasks },
        )

        assertTrue(found)
        assertEquals(tasks, navigator.activeConfig)
    }

    @Test
    fun pop_onSingleEntryBackStack_fallsBack() {
        val navigator = FakeSwitchNavigator(initial = home)
        var fallbackCalls = 0
        navigator.applyFallbackPop { fallbackCalls++; null }

        val popped: ScreenConfig? = navigator.pop()

        assertEquals(null, popped)
        assertEquals(1, fallbackCalls)
    }

    @Test
    fun pop_withHistory_returnsToThePreviousContainer() {
        val navigator = FakeSwitchNavigator(initial = home)
        navigator.switchTo(tasks)

        val popped: ScreenConfig? = navigator.pop()

        assertEquals(tasks, popped)
        assertEquals(home, navigator.activeConfig)
        assertFalse(navigator.canPop())
    }

    @Test
    fun popToRoot_clearsEverythingButTheFirstEntry() {
        val navigator = FakeSwitchNavigator(initial = home)
        navigator.switchTo(tasks)
        navigator.switchTo(settings)

        val root: ScreenConfig? = navigator.popToRoot()

        assertEquals(home, root)
        assertEquals(listOf(home), navigator.backStack)
    }

    @Test
    fun popToRoot_afterRevisitingTheFirstTab_rootIsWhicheverEntryWasNeverRevisited() {
        // Revisiting `home` moves it to the top of the back stack (most-recently-used), the same
        // way DefaultSwitchNavigator.switchTo mutates its own backStack — so the container that
        // ends up at the bottom is `tasks`, the one that was never switched back to, not the
        // original initial container. This mirrors SwitchNavigationComponentController.switchTo's
        // real remove-then-append semantics, not a fake-only quirk.
        val navigator = FakeSwitchNavigator(initial = home)
        navigator.switchTo(tasks)
        navigator.switchTo(home)

        val root: ScreenConfig? = navigator.popToRoot()

        assertEquals(tasks, root)
        assertEquals(listOf(tasks), navigator.backStack)
    }
}
