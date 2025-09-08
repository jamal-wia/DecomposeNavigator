package com.jamal_aliev.decompose_navigator

import com.arkivanov.decompose.ComponentContext
import com.jamal_aliev.decompose_navigator.config.DecomposeNavigationConfig
import com.jamal_aliev.decompose_navigator.controller.LineNavigationComponent
import com.jamal_aliev.decompose_navigator.controller.SwitchContainerComponent
import com.jamal_aliev.decompose_navigator.controller.SwitchNavigationComponent
import com.jamal_aliev.decompose_navigator.controller.TabNavigationComponent

class ScreenConfigFactory(
    extraScreenConfigFactory: (
        config: DecomposeNavigationConfig,
        ctx: ComponentContext
    ) -> RenderComponent
) {

    val screenConfigFactory: (
        config: DecomposeNavigationConfig,
        ctx: ComponentContext
    ) -> RenderComponent = { config: DecomposeNavigationConfig,
                             ctx: ComponentContext ->
        when (config) {

            is DecomposeNavigationConfig.LineNavigation -> {
                LineNavigationComponent(
                    typeId = config.typeId,
                    id = config.id,
                    componentContext = ctx,
                    initialConfigs = config.initialConfigs,
                    childFactory = screenConfigFactory
                )
            }

            is DecomposeNavigationConfig.SwitchNavigation -> {
                SwitchNavigationComponent(
                    typeId = config.typeId,
                    id = config.id,
                    componentContext = ctx,
                    initialConfig = config.initialConfig,
                    childFactory = screenConfigFactory,
                )
            }

            is DecomposeNavigationConfig.SwitchContainer -> {
                SwitchContainerComponent(
                    typeId = config.typeId,
                    id = config.id,
                    ctx = ctx,
                    config = config.contentConfig,
                    childFactory = screenConfigFactory
                )
            }

            is DecomposeNavigationConfig.TabNavigation -> {
                TabNavigationComponent(
                    typeId = config.typeId,
                    id = config.id,
                    componentContext = ctx,
                    tabs = config.tabs,
                    initialConfig = config.initialConfig,
                    childFactory = screenConfigFactory,
                )
            }

            else -> {
                extraScreenConfigFactory.invoke(config, ctx)
            }
        }
    }

}