package com.jamal_aliev.decompose_navigator.navigator

import androidx.compose.runtime.compositionLocalOf
import com.jamal_aliev.decompose_navigator.NavigationComponent

abstract class Navigator {

    protected abstract var navigationComponent: NavigationComponent?

    abstract var parent: Navigator?
    abstract val children: List<Navigator>

    abstract fun addChild(child: Navigator)
    abstract fun removeChild(child: Navigator)

    abstract fun bind(navigationComponent: NavigationComponent)
    abstract fun unbind()

    fun findAncestorBy(predicate: (Navigator) -> Boolean): Navigator? {
        var parent: Navigator? = this.parent
        while (parent != null && !predicate(parent)) {
            parent = parent.parent
        }
        return parent
    }

    fun findGrandParentBy(predicate: (Navigator) -> Boolean): Navigator? {
        val grandParent = this.parent?.parent ?: return null
        var candidate: Navigator? = grandParent
        while (candidate != null) {
            if (predicate(candidate)) return candidate
            candidate = candidate.parent
        }
        return null
    }

    fun findCousinBy(predicate: (Navigator) -> Boolean): Navigator? {
        val parent = this.parent ?: return null
        val grandParent = parent.parent ?: return null
        val siblings = grandParent.children
        var sIndex = 0
        while (sIndex < siblings.size) {
            val sibling = siblings[sIndex++]
            if (sibling === parent) continue
            val cousins = sibling.children
            var cIndex = 0
            while (cIndex < cousins.size) {
                val cousin = cousins[cIndex++]
                if (predicate(cousin)) return cousin
            }
        }
        return null
    }

    fun findSiblingBy(predicate: (Navigator) -> Boolean): Navigator? {
        val parent = this.parent ?: return null
        val siblings = parent.children
        var index = 0
        while (index < siblings.size) {
            val sibling = siblings[index++]
            if (sibling === this) continue
            if (predicate(sibling)) return sibling
        }
        return null
    }

    fun findDescendantBy(predicate: (Navigator) -> Boolean): Navigator? {
        val queue: ArrayDeque<Navigator> = ArrayDeque(initialCapacity = children.size)
        queue.addAll(children)
        while (queue.isNotEmpty()) {
            val current = queue.removeFirst()
            if (predicate(current)) return current
            queue.addAll(current.children)
        }
        return null
    }
}

val LocalNavigator = compositionLocalOf<Navigator?> {
    null
}
