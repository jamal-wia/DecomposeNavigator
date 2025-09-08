package com.jamal_aliev.decompose_navigator.controller

import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import com.arkivanov.decompose.ComponentContext
import com.jamal_aliev.decompose_navigator.RenderComponent
import com.jamal_aliev.decompose_navigator.config.DecomposeNavigationConfig
import com.jamal_aliev.decompose_navigator.NavigationComponent

class SwitchContainerComponent(
    override val typeId: String,
    override val id: Long,
    val ctx: ComponentContext,
    val config: DecomposeNavigationConfig,
    val childFactory: (config: DecomposeNavigationConfig, ctx: ComponentContext) -> RenderComponent,
) : ComponentContext by ctx, RenderComponent, NavigationComponent {

    val child: RenderComponent = childFactory.invoke(config, ctx)

    override val activeConfig: DecomposeNavigationConfig
        get() = config

    @Composable
    override fun Render() {
        key(typeId, id) { RenderSwitchContainerComponent() }
    }

    @Composable
    private fun RenderSwitchContainerComponent() {
        key(child.typeId, child.id) { child.Render() }
    }
}
