package com.jamal_aliev.decompose_navigator.navigator.impl

import com.jamal_aliev.decompose_navigator.config.DecomposeNavigationConfig
import com.jamal_aliev.decompose_navigator.controller.LineNavigationComponent
import com.jamal_aliev.decompose_navigator.NavigationComponent
import com.jamal_aliev.decompose_navigator.navigator.Navigator

abstract class LineNavigator : Navigator() {

    abstract fun push(config: DecomposeNavigationConfig)
    abstract fun pushNew(config: DecomposeNavigationConfig)
    abstract fun pop(onComplete: (Boolean) -> Unit = {})
    abstract fun replace(config: DecomposeNavigationConfig)
    abstract fun replaceAll(config: DecomposeNavigationConfig)
    abstract fun popToFirst()
    abstract fun bringToFront(config: DecomposeNavigationConfig)

    class LineNavigatorImpl(
        override var parent: Navigator?,
        override val children: List<Navigator> = mutableListOf()
    ) : LineNavigator() {

        override var navigationComponent: NavigationComponent? = null

        override fun push(config: DecomposeNavigationConfig) {
            (navigationComponent as? LineNavigationComponent)?.push(config)
        }

        override fun pushNew(config: DecomposeNavigationConfig) {
            (navigationComponent as? LineNavigationComponent)?.pushNew(config)
        }

        override fun pop(onComplete: (Boolean) -> Unit) {
            (navigationComponent as? LineNavigationComponent)?.pop(onComplete)
        }

        override fun replace(config: DecomposeNavigationConfig) {
            (navigationComponent as? LineNavigationComponent)?.replaceCurrent(config)
        }

        override fun replaceAll(config: DecomposeNavigationConfig) {
            (navigationComponent as? LineNavigationComponent)?.replaceAll(config)
        }

        override fun popToFirst() {
            (navigationComponent as? LineNavigationComponent)?.popToFirst()
        }

        override fun bringToFront(config: DecomposeNavigationConfig) {
            (navigationComponent as? LineNavigationComponent)?.bringToFront(config)
        }

        override fun addChild(child: Navigator) {
            (children as MutableList).add(child)
        }

        override fun removeChild(child: Navigator) {
            (children as MutableList).remove(child)
        }

        override fun bind(navigationComponent: NavigationComponent) {
            this.navigationComponent = navigationComponent as LineNavigationComponent
        }

        override fun unbind() {
            this.navigationComponent = null
            this.parent = null
            for (child in this.children) child.unbind()
        }
    }
}
