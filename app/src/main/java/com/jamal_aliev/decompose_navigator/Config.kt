package com.jamal_aliev.decompose_navigator

import com.arkivanov.decompose.ComponentContext
import com.jamal_aliev.decompose_navigator.config.DecomposeNavigationConfig

sealed interface Config : DecomposeNavigationConfig {

    data class Color(
        val colorHex: String,
        override val typeId: String = Color::class.simpleName.orEmpty(),
        override val id: Long = typeId.hashCode().toLong(),
    ) : Config

}

val ScreenConfigFactory = ScreenConfigFactory(
    extraScreenConfigFactory = { config: DecomposeNavigationConfig,
                                 ctx: ComponentContext ->
        when (config) {
            is Config.Color -> {
                ColorScreenComponent(
                    colorHex = config.colorHex,
                )
            }

            else -> TODO()
        }
    }
)

val switchContainerDecomposeNavigationConfig1 = DecomposeNavigationConfig.SwitchContainer(
    contentConfig = DecomposeNavigationConfig.LineNavigation(
        initialConfigs = listOf(element = Config.Color("#000000")),
    ),
)

val switchContainerDecomposeNavigationConfig2 = DecomposeNavigationConfig.SwitchContainer(
    contentConfig = DecomposeNavigationConfig.LineNavigation(
        initialConfigs = listOf(Config.Color("#FFFFFF"))
    ),
)

val tabNavigationConfig = DecomposeNavigationConfig.TabNavigation(
    initialConfig = switchContainerDecomposeNavigationConfig1,
    tabs = listOf(
        "tab-1" to switchContainerDecomposeNavigationConfig1,
        "tab-2" to switchContainerDecomposeNavigationConfig2
    )
)

val rootLineNavigationConfig = DecomposeNavigationConfig.LineNavigation(
    initialConfigs = listOf(tabNavigationConfig)
)

