package io.github.jamal_wia.decomposenavigator

import io.github.jamal_wia.decomposenavigator.controller.base.NavigationComponentController
import io.github.jamal_wia.decomposenavigator.navigator.base.Navigator
import io.github.jamal_wia.decomposenavigator.navigator.base.resolveEnclosingLineNavigator
import io.github.jamal_wia.decomposenavigator.navigator.base.resolveEnclosingSwitchNavigator
import io.github.jamal_wia.decomposenavigator.navigator.base.resolveOutermostLineNavigator
import io.github.jamal_wia.decomposenavigator.navigator.impl.LineNavigator
import io.github.jamal_wia.decomposenavigator.navigator.impl.SwitchNavigator
import kotlin.test.Test
import kotlin.test.assertNull
import kotlin.test.assertSame

/**
 * A tab that holds a stack of its own puts two line navigators above a screen instead of one, and
 * from there the two questions a screen can ask stop having the same answer: which stack is mine,
 * and which stack is the app's. These cover the tree that shape produces — app stack, tab switch,
 * tab stack, screen.
 */
class OutermostLineNavigatorTest {

    private class Tree {
        val appStack: LineNavigator = LineNavigator.DefaultLineNavigator()
        val tabSwitch: SwitchNavigator = SwitchNavigator.DefaultSwitchNavigator().also {
            it.setParent(appStack)
            appStack.addChild(it)
        }
        val tabStack: LineNavigator = LineNavigator.DefaultLineNavigator().also {
            it.setParent(tabSwitch)
            tabSwitch.addChild(it)
        }

        /**
         * A third stack under the tab's own. Two levels would not tell the walk apart from a search
         * for the first matching ancestor — both answer the same thing there — so the tree has to be
         * deep enough that "nearest" and "outermost" are three different navigators.
         */
        val innerStack: LineNavigator = LineNavigator.DefaultLineNavigator().also {
            it.setParent(tabStack)
            tabStack.addChild(it)
        }
    }

    @Test
    fun `a screen in a tab's own stack reaches the app's stack for the outermost`() {
        val tree = Tree()

        assertSame(tree.appStack, resolveOutermostLineNavigator(tree.tabStack))
    }

    @Test
    fun `the outermost is the last stack up the tree and not the first one found`() {
        val tree = Tree()

        // From three stacks deep the answer must still be the app's own. Asking for the closest
        // ancestor would answer the tab's stack here and the two would never be told apart.
        assertSame(tree.appStack, resolveOutermostLineNavigator(tree.innerStack))
        assertSame(tree.innerStack, resolveEnclosingLineNavigator(tree.innerStack))
    }

    @Test
    fun `the same screen reaches its own tab's stack for the nearest`() {
        val tree = Tree()

        // The two must differ, or a screen could not choose between opening inside its tab and
        // opening over the whole app.
        assertSame(tree.tabStack, resolveEnclosingLineNavigator(tree.tabStack))
    }

    @Test
    fun `the tab switch is still found from inside a tab's stack`() {
        val tree = Tree()

        // Switching tabs is asked for from a screen that now has a stack between it and the switch;
        // answering null here would leave a tap on another tab doing nothing at all.
        assertSame(tree.tabSwitch, resolveEnclosingSwitchNavigator(tree.tabStack))
    }

    @Test
    fun `with only one stack above it the nearest and the outermost are the same`() {
        val onlyStack: LineNavigator = LineNavigator.DefaultLineNavigator()

        assertSame(onlyStack, resolveOutermostLineNavigator(onlyStack))
        assertSame(onlyStack, resolveEnclosingLineNavigator(onlyStack))
    }

    @Test
    fun `a tree with no stack at all answers nothing rather than guessing`() {
        val lone = LoneNavigator()

        assertNull(resolveOutermostLineNavigator(lone))
        assertNull(resolveOutermostLineNavigator(null))
    }

    private class LoneNavigator : Navigator<NavigationComponentController<*>>() {
        override var navigationComponent: NavigationComponentController<*>? = null
        override fun bind(navigationComponent: NavigationComponentController<*>) =
            defaultBind(navigationComponent)
    }
}
