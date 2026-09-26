package io.github.jamal_wia.decomposenavigator.controller

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import io.github.jamal_wia.decomposenavigator.RenderComponent
import io.github.jamal_wia.decomposenavigator.ReturnsToFirstScreen
import io.github.jamal_wia.decomposenavigator.config.NavigationScreenConfig
import io.github.jamal_wia.decomposenavigator.config.NavigationScreenConfig.SwitchScreenConfigContainer
import io.github.jamal_wia.decomposenavigator.config.ScreenConfig
import io.github.jamal_wia.decomposenavigator.controller.base.NavigationComponentController
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.childContext
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import com.arkivanov.decompose.router.stack.ChildStack
/**
 * A navigation controller that manages multiple tabs.
 * Internally delegates to a [SwitchNavigationComponentController].
 *
 * @param tabBarContent Custom composable for the tab chrome. Receives the list of tabs, active
 *   tab index, a callback to select a tab, and the tab content itself as a composable slot — so
 *   the chrome can wrap or overlay the content (e.g. a drawer) rather than only append after it.
 *   If `null`, the content renders full-size with no chrome.
 */
class TabNavigationComponentController(
    override val hostConfig: NavigationScreenConfig.TabNavigation,
    private val componentContext: ComponentContext,
    private val childFactory: (config: ScreenConfig, ctx: ComponentContext) -> RenderComponent,
    private val tabBarContent: (@Composable (
        tabs: List<NavigationScreenConfig.TabNavigation.TabNavigationEntry>,
        activeIndex: Int,
        onTabSelected: (SwitchScreenConfigContainer) -> Unit,
        content: @Composable () -> Unit,
    ) -> Unit)? = null,
) : NavigationComponentController<NavigationScreenConfig.TabNavigation>(),
    RenderComponent,
    ReturnsToFirstScreen,
    ComponentContext by componentContext {

    private val tabs: List<NavigationScreenConfig.TabNavigation.TabNavigationEntry> get() = hostConfig.tabs
    private val initialConfig: SwitchScreenConfigContainer get() = hostConfig.initialConfig

    val innerSwitch: SwitchNavigationComponentController = run {
        val component: RenderComponent = childFactory.invoke(
            NavigationScreenConfig.SwitchScreen(
                initialConfig = initialConfig,
                id = id,
                maxBackStackSize = hostConfig.maxBackStackSize,
            ), childContext(key = "$typeId$id")
        )
        return@run checkNotNull(
            value = component as? SwitchNavigationComponentController
        ) {
            "TabNavigationComponentController expects childFactory to produce " +
                    "SwitchNavigationComponentController for SwitchScreen config, " +
                    "but got: ${component::class.simpleName}"
        }
    }

    override val activeScreenConfig: ScreenConfig
        get() = innerSwitch.activeScreenConfig

    /** No stack of its own; the tab standing in the switch may have one. */
    override fun returnToFirstScreen() {
        innerSwitch.returnToFirstScreen()
    }

    override fun navigate(
        transformer: (stack: List<ScreenConfig>) -> List<ScreenConfig>,
        onComplete: (newStack: List<ScreenConfig>, oldStack: List<ScreenConfig>) -> Unit
    ) {
        innerSwitch.navigate(transformer, onComplete)
    }

    @Composable
    override fun Render() {
        key(typeId, id) { RenderTabNavigationComponent() }
    }

    @Composable
    private fun RenderTabNavigationComponent() {
        val active: ChildStack<SwitchScreenConfigContainer, RenderComponent>
                by innerSwitch.childStack.subscribeAsState()
        val activeIndex: Int = tabs.indexOfFirst { it.container.id == active.active.configuration.id }
        val content: @Composable () -> Unit = {
            key(innerSwitch.typeId, innerSwitch.id) {
                innerSwitch.Render()
            }
        }
        val chrome: (@Composable (
            tabs: List<NavigationScreenConfig.TabNavigation.TabNavigationEntry>,
            activeIndex: Int,
            onTabSelected: (SwitchScreenConfigContainer) -> Unit,
            content: @Composable () -> Unit,
        ) -> Unit)? = tabBarContent

        if (chrome != null) {
            chrome(tabs, activeIndex, { container -> innerSwitch.switchTo(container) }, content)
        } else {
            Column(Modifier.fillMaxSize()) {
                Box(Modifier.weight(1f)) { content() }
            }
        }
    }
}
