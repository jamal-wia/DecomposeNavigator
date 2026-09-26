package io.github.jamal_wia.decomposenavigator.controller.base

import io.github.jamal_wia.decomposenavigator.config.NavigationScreenConfig
import io.github.jamal_wia.decomposenavigator.config.ScreenConfig

abstract class NavigationComponentController<T : NavigationScreenConfig> {

    abstract val hostConfig: T

    val typeId: String get() = hostConfig.typeId
    val id: Long get() = hostConfig.id

    abstract val activeScreenConfig: ScreenConfig

    abstract fun navigate(
        transformer: (
            stack: List<ScreenConfig>
        ) -> List<ScreenConfig>,

        onComplete: (
            newStack: List<ScreenConfig>,
            oldStack: List<ScreenConfig>
        ) -> Unit,
    )

    override fun hashCode(): Int {
        val result: Int = hostConfig.hashCode()
        return result
    }

    override fun equals(other: Any?): Boolean {
        if (other !is NavigationComponentController<*>) return false
        val result: Boolean = this.hostConfig == other.hostConfig
        return result
    }
}
