package com.jamal_aliev.decompose_navigator.config

import com.jamal_aliev.decompose_navigator.randomId
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.Polymorphic
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.PolymorphicModuleBuilder
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.serializer

@Polymorphic
interface DecomposeNavigationConfig {

    val typeId: String
    val id: Long

    @Serializable
    @SerialName("LineNavigation")
    data class LineNavigation(
        val initialConfigs: List<@Polymorphic DecomposeNavigationConfig>,
        override val typeId: String = LineNavigation::class.simpleName.orEmpty(),
        override val id: Long = randomId()
    ) : DecomposeNavigationConfig

    @Serializable
    @SerialName("SwitchNavigation")
    data class SwitchNavigation(
        val initialConfig: SwitchContainer,
        override val typeId: String = SwitchNavigation::class.simpleName.orEmpty(),
        override val id: Long = randomId()
    ) : DecomposeNavigationConfig

    @Serializable
    @SerialName("SwitchContainer")
    data class SwitchContainer(
        val contentConfig: @Polymorphic DecomposeNavigationConfig,
        override val typeId: String = SwitchContainer::class.simpleName.orEmpty(),
        override val id: Long = randomId()
    ) : DecomposeNavigationConfig

    @Serializable
    @SerialName("TabNavigation")
    data class TabNavigation(
        val initialConfig: SwitchContainer,
        val tabs: List<TabNavigationEntry>,
        override val typeId: String = TabNavigation::class.simpleName.orEmpty(),
        override val id: Long = randomId()
    ) : DecomposeNavigationConfig {

        @Serializable
        data class TabNavigationEntry(
            val name: String,
            val container: SwitchContainer
        )
    }

}

var extraSubClasses: (PolymorphicModuleBuilder<DecomposeNavigationConfig>) -> Unit = {
}

@OptIn(InternalSerializationApi::class)
val decomposeNavigationConfigSerializersModule
    get() = SerializersModule {
        polymorphic(DecomposeNavigationConfig::class) {
            subclass(
                DecomposeNavigationConfig.LineNavigation::class,
                DecomposeNavigationConfig.LineNavigation::class.serializer()
            )
            subclass(
                DecomposeNavigationConfig.SwitchNavigation::class,
                DecomposeNavigationConfig.SwitchNavigation::class.serializer()
            )
            subclass(
                DecomposeNavigationConfig.SwitchContainer::class,
                DecomposeNavigationConfig.SwitchContainer::class.serializer()
            )
            subclass(
                DecomposeNavigationConfig.TabNavigation::class,
                DecomposeNavigationConfig.TabNavigation::class.serializer()
            )
            extraSubClasses(this)
        }
    }
