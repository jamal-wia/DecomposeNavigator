package com.jamal_aliev.decompose_navigator.config

import com.jamal_aliev.decompose_navigator.randomId

interface DecomposeNavigationConfig {

    val typeId: String
    val id: Long

    data class LineNavigation(
        val initialConfigs: List<DecomposeNavigationConfig>,
        override val typeId: String = LineNavigation::class.simpleName.orEmpty(),
        override val id: Long = randomId()
    ) : DecomposeNavigationConfig

    data class SwitchNavigation(
        val initialConfig: SwitchContainer,
        override val typeId: String = SwitchNavigation::class.simpleName.orEmpty(),
        override val id: Long = randomId()
    ) : DecomposeNavigationConfig

    data class SwitchContainer(
        val contentConfig: DecomposeNavigationConfig,
        override val typeId: String = SwitchContainer::class.simpleName.orEmpty(),
        override val id: Long = randomId()
    ) : DecomposeNavigationConfig

    data class TabNavigation(
        val initialConfig: SwitchContainer,
        val tabs: List<Pair<String, SwitchContainer>>,
        override val typeId: String = TabNavigation::class.simpleName.orEmpty(),
        override val id: Long = randomId()
    ) : DecomposeNavigationConfig

}
