package io.github.jamal_wia.decomposenavigator

import io.github.jamal_wia.decomposenavigator.controller.base.NavigationComponentController
import io.github.jamal_wia.decomposenavigator.navigator.base.Navigator
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

/**
 * Minimal concrete [Navigator] used only to exercise the tree-traversal and
 * lifecycle logic that lives in the abstract base class.
 */
private class TestNavigator : Navigator<NavigationComponentController<*>>() {
    override var navigationComponent: NavigationComponentController<*>? = null
    override fun bind(navigationComponent: NavigationComponentController<*>) {
        defaultBind(navigationComponent)
    }
}

/** Wires [child] under [parent] the same way `rememberNavigator` does. */
private fun link(parent: TestNavigator, child: TestNavigator): TestNavigator {
    child.setParent(parent)
    parent.addChild(child)
    return child
}

/**
 * Characterization tests for the [Navigator] tree: traversal helpers and `release()`.
 * The tree built here mirrors the real shape (root → switch → line → nested line).
 *
 *            root
 *           /    \
 *          a      b
 *        /  \      \
 *      a1    a2     b1
 */
class NavigatorTreeTest {

    private val root = TestNavigator()
    private val a = link(root, TestNavigator())
    private val b = link(root, TestNavigator())
    private val a1 = link(a, TestNavigator())
    private val a2 = link(a, TestNavigator())
    private val b1 = link(b, TestNavigator())

    @Test
    fun `findAncestorBy walks up to a matching ancestor`() {
        assertSame(root, a1.findAncestorBy { it === root })
        assertSame(a, a1.findAncestorBy { it === a })
    }

    @Test
    fun `findAncestorBy returns null when no ancestor matches`() {
        assertNull(a1.findAncestorBy { false })
    }

    @Test
    fun `findGrandParentBy returns the grandparent only when it matches`() {
        assertSame(root, a1.findGrandParentBy { it === root })
        assertNull(a1.findGrandParentBy { it === a })
        assertNull(root.findGrandParentBy { true })
    }

    @Test
    fun `findSiblingBy finds a sibling but never self`() {
        assertSame(a2, a1.findSiblingBy { it === a2 })
        assertNull(a1.findSiblingBy { it === a1 })
    }

    @Test
    fun `findCousinBy finds children of the parent's siblings`() {
        assertSame(b1, a1.findCousinBy { it === b1 })
        assertNull(a1.findCousinBy { it === a2 })
    }

    @Test
    fun `findDescendantBy does a breadth-first search downward`() {
        assertSame(b1, root.findDescendantBy { it === b1 })
        assertSame(a2, root.findDescendantBy { it === a2 })
        assertNull(a1.findDescendantBy { true })
    }

    @Test
    fun `release detaches the navigator from its parent and children`() {
        a.release()

        assertFalse(root.children.contains(a))
        assertNull(a1.parent)
        assertNull(a2.parent)
        assertTrue(a.children.isEmpty())
    }

    @Test
    fun `using a released navigator throws`() {
        a1.release()
        assertFailsWith<IllegalStateException> { a1.addChild(TestNavigator()) }
        assertFailsWith<IllegalStateException> { a1.setParent(root) }
    }
}
