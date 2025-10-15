package com.jamal_aliev.decompose_navigator

import com.arkivanov.decompose.ComponentContext
import com.jamal_aliev.decompose_navigator.config.DecomposeNavigationConfig
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("ColorConfig")
data class ColorConfig(
    val colorHex: String,
    override val typeId: String = ColorConfig::class.simpleName.orEmpty(),
    override val id: Long = typeId.hashCode().toLong(),
) : DecomposeNavigationConfig


val ScreenConfigFactory = ScreenConfigFactory(
    extraScreenConfigFactory = { config: DecomposeNavigationConfig,
                                 ctx: ComponentContext ->
        when (config) {
            is ColorConfig -> {
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
        initialConfigs = listOf(element = ColorConfig("#000000")),
    ),
)

val switchContainerDecomposeNavigationConfig2 = DecomposeNavigationConfig.SwitchContainer(
    contentConfig = DecomposeNavigationConfig.LineNavigation(
        initialConfigs = listOf(ColorConfig("#FFFFFF"))
    ),
)

val tabNavigationConfig = DecomposeNavigationConfig.TabNavigation(
    initialConfig = switchContainerDecomposeNavigationConfig1,
    tabs = listOf(
        DecomposeNavigationConfig.TabNavigation.TabNavigationEntry(
            "tab-1",
            switchContainerDecomposeNavigationConfig1
        ),
        DecomposeNavigationConfig.TabNavigation.TabNavigationEntry(
            "tab-2",
            switchContainerDecomposeNavigationConfig2
        ),
    )
)

val rootLineNavigationConfig = DecomposeNavigationConfig.LineNavigation(
    initialConfigs = listOf(tabNavigationConfig)
)


val testContentConfig1 = DecomposeNavigationConfig.LineNavigation(
    initialConfigs = listOf(ColorConfig("#FFFFFF")),
    id = 10
)
