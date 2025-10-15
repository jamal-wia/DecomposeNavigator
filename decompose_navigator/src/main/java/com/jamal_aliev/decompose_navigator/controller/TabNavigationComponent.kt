package com.jamal_aliev.decompose_navigator.controller

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import com.arkivanov.decompose.router.stack.ChildStack
import com.jamal_aliev.decompose_navigator.RenderComponent
import com.jamal_aliev.decompose_navigator.config.DecomposeNavigationConfig

class TabNavigationComponent(
    override val typeId: String,
    override val id: Long,
    private val componentContext: ComponentContext,
    private val tabs: List<DecomposeNavigationConfig.TabNavigation.TabNavigationEntry>,
    private val initialConfig: DecomposeNavigationConfig.SwitchContainer = tabs.first().container,
    private val childFactory: (config: DecomposeNavigationConfig, ctx: ComponentContext) -> RenderComponent,
) : ComponentContext by componentContext, RenderComponent {

    private val innerSwitch = childFactory.invoke(
        DecomposeNavigationConfig.SwitchNavigation(initialConfig),
        componentContext
    ) as SwitchNavigationComponent

    @Composable
    override fun Render() {
        key(typeId, id) { RenderTabNavigationComponent() }
    }

    @Composable
    private fun RenderTabNavigationComponent() {
        val active: ChildStack<DecomposeNavigationConfig.SwitchContainer, RenderComponent>
                by innerSwitch.childStack.subscribeAsState()
        val activeIndex = tabs.indexOfFirst { it.container.id == active.active.configuration.id }
        Column(Modifier.fillMaxSize()) {
            Box(Modifier.weight(1f)) {
                key(innerSwitch.typeId, innerSwitch.id) {
                    innerSwitch.Render()
                }
            }

            NavigationBar(
                windowInsets = remember { WindowInsets() }
            ) {
                tabs.forEachIndexed { index, pair ->
                    key(index) {
                        NavigationBarItem(
                            selected = index == activeIndex,
                            onClick = {
                                innerSwitch.switchTo(pair.container)
                            },
                            label = { Text(pair.name) },
                            icon = {} // no icon in demo
                        )
                    }
                }
            }
        }
    }
}
