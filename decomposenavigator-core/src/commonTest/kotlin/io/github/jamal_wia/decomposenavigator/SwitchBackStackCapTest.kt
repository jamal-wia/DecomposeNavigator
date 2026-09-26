package io.github.jamal_wia.decomposenavigator

import androidx.compose.runtime.Composable
import io.github.jamal_wia.decomposenavigator.config.NavigationScreenConfig
import io.github.jamal_wia.decomposenavigator.config.NavigationScreenConfig.SwitchScreenConfigContainer
import io.github.jamal_wia.decomposenavigator.config.ScreenConfig
import io.github.jamal_wia.decomposenavigator.controller.SwitchNavigationComponentController
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

@Serializable
private data class SwitchCapTestScreenConfig(val tag: String) : ScreenConfig

/** Stub for every child [SwitchNavigationComponentController.childStack] builds — it receives the
 * [SwitchScreenConfigContainer] itself (the switch's own stack element type), not the
 * [SwitchCapTestScreenConfig] wrapped inside it, so the factory below must accept either. */
private class SwitchCapTestScreenComponent(
    @Suppress("unused") private val config: ScreenConfig,
    context: ComponentContext,
) : RenderComponent, ComponentContext by context {
    @Composable
    override fun Render() = Unit
}

/**
 * [NavigationScreenConfig.SwitchScreen.maxBackStackSize] exists solely so the admin app's tab
 * shell (`adminTabShellConfig` in `tahfeezAdminApp`) can cap how many distinct tabs the
 * switch-history back button walks through. `null` — the default, and what every other caller
 * (tahfeezApp's own tab bar) leaves it at — must keep today's unbounded history untouched.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SwitchBackStackCapTest {

    @BeforeTest
    fun installMainDispatcher() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterTest
    fun resetMainDispatcher() {
        Dispatchers.resetMain()
    }

    private fun context(): ComponentContext = DefaultComponentContext(LifecycleRegistry())

    private fun containerFor(tag: String): SwitchScreenConfigContainer =
        SwitchScreenConfigContainer(config = SwitchCapTestScreenConfig(tag))

    private fun controller(
        initial: SwitchScreenConfigContainer,
        maxBackStackSize: Int?,
    ): SwitchNavigationComponentController = SwitchNavigationComponentController(
        hostConfig = NavigationScreenConfig.SwitchScreen(
            initialConfig = initial,
            maxBackStackSize = maxBackStackSize,
        ),
        componentContext = context(),
        childFactory = { config, ctx -> SwitchCapTestScreenComponent(config, ctx) },
        json = Json,
    )

    @Test
    fun `back-switch history is unbounded when no cap is set`() {
        val tabA = containerFor("a")
        val tabB = containerFor("b")
        val tabC = containerFor("c")
        val tabD = containerFor("d")
        val controller = controller(initial = tabA, maxBackStackSize = null)

        controller.switchTo(tabB)
        controller.switchTo(tabC)
        controller.switchTo(tabD)

        assertEquals(listOf(tabA, tabB, tabC, tabD), controller.backStack.value)
    }

    @Test
    fun `switching within the cap keeps every visited tab`() {
        val tabA = containerFor("a")
        val tabB = containerFor("b")
        val tabC = containerFor("c")
        val controller = controller(initial = tabA, maxBackStackSize = 3)

        controller.switchTo(tabB)
        controller.switchTo(tabC)

        assertEquals(listOf(tabA, tabB, tabC), controller.backStack.value)
    }

    @Test
    fun `switching past the cap drops the oldest tab instead of growing further`() {
        val tabA = containerFor("a")
        val tabB = containerFor("b")
        val tabC = containerFor("c")
        val tabD = containerFor("d")
        val controller = controller(initial = tabA, maxBackStackSize = 3)

        controller.switchTo(tabB)
        controller.switchTo(tabC)
        controller.switchTo(tabD)

        assertEquals(listOf(tabB, tabC, tabD), controller.backStack.value)
    }

    @Test
    fun `re-visiting a tab already in the capped history moves it to the end without growing`() {
        val tabA = containerFor("a")
        val tabB = containerFor("b")
        val tabC = containerFor("c")
        val controller = controller(initial = tabA, maxBackStackSize = 3)
        controller.switchTo(tabB)
        controller.switchTo(tabC)

        controller.switchTo(tabA)

        assertEquals(listOf(tabB, tabC, tabA), controller.backStack.value)
    }

    @Test
    fun `opening nine tabs in a row still leaves only three steps of switch history`() {
        val tabs = (1..9).map { containerFor("tab-$it") }
        val controller = controller(initial = tabs.first(), maxBackStackSize = 3)

        tabs.drop(1).forEach { controller.switchTo(it) }

        assertEquals(tabs.takeLast(3), controller.backStack.value)
    }
}
