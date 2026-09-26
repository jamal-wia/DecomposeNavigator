package io.github.jamal_wia.decomposenavigator

import androidx.compose.runtime.Composable
import io.github.jamal_wia.decomposenavigator.config.NavigationScreenConfig
import io.github.jamal_wia.decomposenavigator.config.ScreenConfig
import io.github.jamal_wia.decomposenavigator.controller.LineNavigationComponentController
import io.github.jamal_wia.decomposenavigator.navigator.impl.LineNavigator
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
private data class StackSizeTestScreenConfig(val tag: String) : ScreenConfig

private class StackSizeTestScreenComponent(
    @Suppress("unused") private val config: StackSizeTestScreenConfig,
    context: ComponentContext,
) : RenderComponent, ComponentContext by context {
    @Composable
    override fun Render() = Unit
}

/**
 * [LineNavigator.stackSize] exists solely so a caller (the admin app's `pushCapped`, see
 * `tahfeezAdminApp`) can decide whether pushing would grow a stack past a depth it wants to cap.
 * A test main dispatcher is installed because the controller marshals every mutation onto
 * `Dispatchers.Main.immediate`, mirroring [RegistryAndTabNavigationTest]'s setup.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class LineNavigatorStackSizeTest {

    @BeforeTest
    fun installMainDispatcher() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterTest
    fun resetMainDispatcher() {
        Dispatchers.resetMain()
    }

    private fun context(): ComponentContext = DefaultComponentContext(LifecycleRegistry())

    private fun controller(): LineNavigationComponentController = LineNavigationComponentController(
        hostConfig = NavigationScreenConfig.LineNavigation(
            initialConfigs = listOf(StackSizeTestScreenConfig("root")),
        ),
        componentContext = context(),
        childFactory = { config, ctx -> StackSizeTestScreenComponent(config as StackSizeTestScreenConfig, ctx) },
        json = Json,
    )

    @Test
    fun `stackSize is 0 before the navigator is bound to a controller`() {
        assertEquals(0, LineNavigator.DefaultLineNavigator().stackSize())
    }

    @Test
    fun `stackSize is 1 for a freshly built single-screen stack`() {
        val navigator = LineNavigator.DefaultLineNavigator()
        navigator.bind(controller())

        assertEquals(1, navigator.stackSize())
    }

    @Test
    fun `stackSize grows by one on each push`() {
        val navigator = LineNavigator.DefaultLineNavigator()
        navigator.bind(controller())

        navigator.push(StackSizeTestScreenConfig("second"))
        assertEquals(2, navigator.stackSize())

        navigator.push(StackSizeTestScreenConfig("third"))
        assertEquals(3, navigator.stackSize())
    }

    @Test
    fun `stackSize shrinks back down after a pop`() {
        val navigator = LineNavigator.DefaultLineNavigator()
        navigator.bind(controller())
        navigator.push(StackSizeTestScreenConfig("second"))

        navigator.pop()

        assertEquals(1, navigator.stackSize())
    }

    @Test
    fun `stackSize stays put when replace swaps the top screen`() {
        val navigator = LineNavigator.DefaultLineNavigator()
        navigator.bind(controller())
        navigator.push(StackSizeTestScreenConfig("second"))

        navigator.replace(StackSizeTestScreenConfig("second-replaced"))

        assertEquals(2, navigator.stackSize())
    }
}
