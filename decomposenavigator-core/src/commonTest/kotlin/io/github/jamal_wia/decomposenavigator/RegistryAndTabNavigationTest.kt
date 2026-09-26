package io.github.jamal_wia.decomposenavigator

import androidx.compose.runtime.Composable
import io.github.jamal_wia.decomposenavigator.config.NavigationScreenConfig
import io.github.jamal_wia.decomposenavigator.config.NavigationScreenConfig.SwitchScreenConfigContainer
import io.github.jamal_wia.decomposenavigator.config.ScreenConfig
import io.github.jamal_wia.decomposenavigator.controller.LineNavigationComponentController
import io.github.jamal_wia.decomposenavigator.controller.SwitchContainerComponentController
import io.github.jamal_wia.decomposenavigator.controller.SwitchNavigationComponentController
import io.github.jamal_wia.decomposenavigator.controller.TabNavigationComponentController
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.serialization.Serializable
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

@Serializable
private data class TabTestScreenConfig(val tag: String) : ScreenConfig

private class TabTestScreenComponent(
    @Suppress("unused") private val config: TabTestScreenConfig,
    context: ComponentContext,
) : RenderComponent, ComponentContext by context {
    @Composable
    override fun Render() = Unit
}

@Serializable
private data object UnregisteredScreenConfig : ScreenConfig

/**
 * Construction-level tests for [ScreenConfigRegistry.createComponent] and the built-in
 * [TabNavigationComponentController]. They exercise only synchronous construction and reads — no
 * navigate()/switchTo(). Constructing a controller still touches `Dispatchers.Main` (the controllers
 * marshal their mutations onto it), and there is no platform main dispatcher in a plain unit test, so
 * a test dispatcher is installed for the duration of the class.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class RegistryAndTabNavigationTest {

    @BeforeTest
    fun installMainDispatcher() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterTest
    fun resetMainDispatcher() {
        Dispatchers.resetMain()
    }

    private val registry = ScreenConfigRegistry {
        screen(::TabTestScreenComponent)
    }

    private fun context(): ComponentContext = DefaultComponentContext(LifecycleRegistry())

    private val homeContainer = SwitchScreenConfigContainer(config = TabTestScreenConfig("home"))
    private val tasksContainer = SwitchScreenConfigContainer(config = TabTestScreenConfig("tasks"))

    @Test
    fun `createComponent builds the built-in TabNavigationComponentController`() {
        val tabConfig = NavigationScreenConfig.TabNavigation(
            initialConfig = tasksContainer,
            tabs = listOf(
                NavigationScreenConfig.TabNavigation.TabNavigationEntry("home", homeContainer),
                NavigationScreenConfig.TabNavigation.TabNavigationEntry("tasks", tasksContainer),
            ),
        )

        val component: RenderComponent = registry.createComponent(tabConfig, context())

        assertTrue(component is TabNavigationComponentController)
        assertTrue(component.innerSwitch is SwitchNavigationComponentController)
    }

    @Test
    fun `built-in tab navigation exposes the initial tab as the active screen`() {
        val tabConfig = NavigationScreenConfig.TabNavigation(
            initialConfig = tasksContainer,
            tabs = listOf(
                NavigationScreenConfig.TabNavigation.TabNavigationEntry("home", homeContainer),
                NavigationScreenConfig.TabNavigation.TabNavigationEntry("tasks", tasksContainer),
            ),
        )

        val component = registry.createComponent(tabConfig, context()) as TabNavigationComponentController
        val active = component.activeScreenConfig as SwitchScreenConfigContainer

        assertEquals(TabTestScreenConfig("tasks"), active.config)
    }

    @Test
    fun `createComponent maps each NavigationScreenConfig to its built-in controller`() {
        val line = registry.createComponent(
            NavigationScreenConfig.LineNavigation(initialConfigs = listOf(TabTestScreenConfig("a"))),
            context(),
        )
        val switch = registry.createComponent(
            NavigationScreenConfig.SwitchScreen(initialConfig = homeContainer),
            context(),
        )
        val container = registry.createComponent(homeContainer, context())

        assertTrue(line is LineNavigationComponentController)
        assertTrue(switch is SwitchNavigationComponentController)
        assertTrue(container is SwitchContainerComponentController)
    }

    @Test
    fun `createComponent throws for an unregistered screen config`() {
        assertFailsWith<IllegalArgumentException> {
            registry.createComponent(UnregisteredScreenConfig, context())
        }
    }

    @Test
    fun `a controller built with a custom chrome slot still resolves the active screen`() {
        val tabConfig = NavigationScreenConfig.TabNavigation(
            initialConfig = tasksContainer,
            tabs = listOf(
                NavigationScreenConfig.TabNavigation.TabNavigationEntry("home", homeContainer),
                NavigationScreenConfig.TabNavigation.TabNavigationEntry("tasks", tasksContainer),
            ),
        )

        // The chrome slot now also receives the tab content as a composable — this is never invoked
        // here (these tests are construction-only, no Render()), just proves the 4-arg shape still
        // wires through `childFactory`/`registry.createComponent` without breaking construction.
        val component = TabNavigationComponentController(
            hostConfig = tabConfig,
            componentContext = context(),
            childFactory = registry::createComponent,
            tabBarContent = { _, _, _, _ -> },
        )
        val active = component.activeScreenConfig as SwitchScreenConfigContainer

        assertEquals(TabTestScreenConfig("tasks"), active.config)
    }
}
