package io.github.jamal_wia.decomposenavigator

import io.github.jamal_wia.decomposenavigator.controller.base.NavigationComponentController
import io.github.jamal_wia.decomposenavigator.navigator.base.Navigator
import io.github.jamal_wia.decomposenavigator.navigator.base.resolveEnclosingLineNavigator
import io.github.jamal_wia.decomposenavigator.navigator.impl.LineNavigator
import kotlin.test.Test
import kotlin.test.assertNull
import kotlin.test.assertSame

/** Non-line navigator used to stand in for a switch/container node in the tree. */
private class NonLineNavigator : Navigator<NavigationComponentController<*>>() {
    override var navigationComponent: NavigationComponentController<*>? = null
    override fun bind(navigationComponent: NavigationComponentController<*>) = defaultBind(navigationComponent)
}

/**
 * Tests the resolution behind [io.github.jamal_wia.decomposenavigator.navigator.base.LocalEnclosingLineNavigator]:
 * the nearest [LineNavigator] at or above a given node. This is the helper that replaced the
 * repeated `LocalNavigator.current?.findAncestorBy { it is LineNavigator } as? LineNavigator`
 * across the screens.
 */
class EnclosingLineNavigatorTest {

    @Test
    fun `returns the current navigator when it is a line navigator`() {
        val line: LineNavigator = LineNavigator.DefaultLineNavigator()

        assertSame(line, resolveEnclosingLineNavigator(line))
    }

    @Test
    fun `walks up to the enclosing line navigator for a non-line current`() {
        val rootLine: LineNavigator = LineNavigator.DefaultLineNavigator()
        val switchLike = NonLineNavigator().also {
            it.setParent(rootLine)
            rootLine.addChild(it)
        }
        val leaf = NonLineNavigator().also {
            it.setParent(switchLike)
            switchLike.addChild(it)
        }

        // A screen hosted under a tab/switch resolves to the enclosing stack, not its parent switch.
        assertSame(rootLine, resolveEnclosingLineNavigator(leaf))
        assertSame(rootLine, resolveEnclosingLineNavigator(switchLike))
    }

    @Test
    fun `returns null when there is no line navigator at or above`() {
        val root = NonLineNavigator()
        val child = NonLineNavigator().also {
            it.setParent(root)
            root.addChild(it)
        }

        assertNull(resolveEnclosingLineNavigator(child))
        assertNull(resolveEnclosingLineNavigator(null))
    }
}
