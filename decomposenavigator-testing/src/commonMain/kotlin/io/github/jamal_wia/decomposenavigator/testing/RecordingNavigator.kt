package io.github.jamal_wia.decomposenavigator.testing

import io.github.jamal_wia.decomposenavigator.controller.base.NavigationComponentController
import io.github.jamal_wia.decomposenavigator.navigator.base.Navigator

/**
 * Minimal concrete [Navigator], for testing code that walks the navigator tree
 * ([Navigator.findAncestorBy], [Navigator.findSiblingBy], [Navigator.findCousinBy],
 * [Navigator.findDescendantBy], [Navigator.findGrandParentBy]) or exercises [Navigator.release] /
 * [Navigator.bind] / [Navigator.setParent] / [Navigator.addChild] without needing a real
 * `LineNavigator` or `SwitchNavigator`.
 *
 * [bindCallCount] and [boundComponent] record what [bind] was called with, for tests asserting on
 * the pending-actions-flush behavior [Navigator.defaultBind] provides.
 */
class RecordingNavigator : Navigator<NavigationComponentController<*>>() {

    override var navigationComponent: NavigationComponentController<*>? = null

    var bindCallCount: Int = 0
        private set

    val boundComponent: NavigationComponentController<*>? get() = navigationComponent

    override fun bind(navigationComponent: NavigationComponentController<*>) {
        bindCallCount++
        defaultBind(navigationComponent)
    }

    /** Runs [action] through the fake's [withComponentOrEnqueue] seam, for pending-action tests. */
    fun runOrEnqueue(action: (NavigationComponentController<*>) -> Unit) {
        withComponentOrEnqueue(action)
    }
}

/** Wires [child] under [parent] the same way `rememberNavigator` does in production. */
fun link(parent: RecordingNavigator, child: RecordingNavigator): RecordingNavigator {
    child.setParent(parent)
    parent.addChild(child)
    return child
}
