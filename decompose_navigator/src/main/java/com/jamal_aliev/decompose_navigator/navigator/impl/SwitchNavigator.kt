package com.jamal_aliev.decompose_navigator.navigator.impl

import com.jamal_aliev.decompose_navigator.config.DecomposeNavigationConfig
import com.jamal_aliev.decompose_navigator.controller.SwitchNavigationComponent
import com.jamal_aliev.decompose_navigator.NavigationComponent
import com.jamal_aliev.decompose_navigator.navigator.Navigator

abstract class SwitchNavigator : Navigator() {

    abstract fun switchTo(config: DecomposeNavigationConfig.SwitchContainer)
    abstract val activeConfig: DecomposeNavigationConfig.SwitchContainer

    class SwitchNavigatorImpl(
        override var parent: Navigator?,
        override val children: List<Navigator> = mutableListOf(),
    ) : SwitchNavigator() {


        override var navigationComponent: NavigationComponent? = null
        private val switchNavigationComponent: SwitchNavigationComponent
            get() = navigationComponent as SwitchNavigationComponent

        override val activeConfig: DecomposeNavigationConfig.SwitchContainer
            get() = navigationComponent?.activeConfig as DecomposeNavigationConfig.SwitchContainer

        override fun switchTo(config: DecomposeNavigationConfig.SwitchContainer) {
            switchNavigationComponent.switchTo(config)
        }

        override fun addChild(child: Navigator) {
            (children as MutableList).add(child)
        }

        override fun removeChild(child: Navigator) {
            (children as MutableList).remove(child)
        }

        override fun bind(navigationComponent: NavigationComponent) {
            this.navigationComponent = navigationComponent as SwitchNavigationComponent
        }

        override fun unbind() {
            navigationComponent = null
            parent = null
            for (child in children) {
                child.unbind()
            }
        }
    }
}
