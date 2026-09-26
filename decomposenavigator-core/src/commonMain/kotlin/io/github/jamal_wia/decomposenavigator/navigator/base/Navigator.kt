package io.github.jamal_wia.decomposenavigator.navigator.base

import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import io.github.jamal_wia.decomposenavigator.config.ScreenConfig
import io.github.jamal_wia.decomposenavigator.controller.base.NavigationComponentController
import io.github.jamal_wia.decomposenavigator.navigator.impl.LineNavigator
import io.github.jamal_wia.decomposenavigator.navigator.impl.SwitchNavigator
import io.github.jamal_wia.decomposenavigator.logging.Logger
import io.github.jamal_wia.decomposenavigator.logging.NoopLogger

abstract class Navigator<T : NavigationComponentController<*>> {

    /**
     * Diagnostic sink for this navigator's lifecycle/navigation events. Defaults to [NoopLogger]
     * (silent) — an app wires a real [Logger] by overriding this in its own navigator
     * implementation, or via whichever construction seam it uses (no DI framework is imposed).
     */
    protected open val logger: Logger = NoopLogger

    protected abstract var navigationComponent: T?

    /**
     * Whether [release] has been called on this navigator. A released navigator throws from every
     * navigation method (see [checkNotReleased]); callers deferring navigation across an Activity
     * recreation must re-check this on the **main thread** immediately before navigating, because
     * `release()` runs on the main thread during Composition disposal.
     */
    var isReleased: Boolean = false
        protected set

    // Underscore-prefixed names follow the standard Kotlin backing-property
    // idiom: mutable `_parent`/`_children` are exposed only as read-only
    // `parent`/`children`. Detekt's VariableNaming flags any leading underscore.
    @Suppress("VariableNaming")
    protected open var _parent: Navigator<*>? = null
    val parent: Navigator<*>? get() = _parent

    open fun setParent(parent: Navigator<*>) {
        check(!isReleased) { "Navigator is released" }
        _parent = parent
    }

    open fun removeParent(parent: Navigator<*>) {
        if (parent == _parent) {
            _parent = null
        }
    }

    @Suppress("VariableNaming")
    protected open val _children: MutableList<Navigator<*>> = mutableListOf()
    val children: List<Navigator<*>> get() = _children

    open fun addChild(child: Navigator<*>) {
        check(!isReleased) { "Navigator is released" }
        _children.add(child)
    }

    open fun removeChild(child: Navigator<*>): Boolean {
        return _children.remove(child)
    }

    /**
     * Releases this navigator: cleans up tree references (parent/children),
     * clears component binding, fallback pop, and pending actions.
     * Subclasses should call `super.release()` and then clean up their own state.
     */
    open fun release() {
        logger.d { "release: host=${navigationComponent?.hostConfig?.let { it::class.simpleName }}" }
        if (!isReleased) {
            isReleased = true
            while (_children.isNotEmpty()) {
                _children.removeAt(_children.lastIndex)
                    .removeParent(this)
            }
            _parent?.removeChild(this)
            _parent = null
        }
        navigationComponent = null
        fallbackPop = null
        pendingActions.clear()
    }

    abstract fun bind(navigationComponent: T)

    // ── Shared boilerplate for default navigator implementations ──

    protected var fallbackPop: (() -> ScreenConfig?)? = null
    protected val pendingActions: MutableList<() -> Unit> = mutableListOf()

    protected fun requireComponent(): T {
        return checkNotNull(navigationComponent) {
            "${this::class.simpleName} is not bound to a NavigationComponentController. " +
                    "Did you forget to call bind()?"
        }
    }

    protected fun withComponentOrEnqueue(action: (T) -> Unit) {
        val component: T? = navigationComponent
        if (component != null) {
            action(component)
        } else {
            pendingActions.add { action(requireComponent()) }
        }
    }

    protected fun checkNotReleased() {
        check(!isReleased) { "${this::class.simpleName} is released" }
    }

    /**
     * Default bind implementation: stores the component and executes pending actions.
     */
    protected fun defaultBind(navigationComponent: T) {
        check(!isReleased) { "${this::class.simpleName} is released" }
        logger.d { "bind: host=${navigationComponent.hostConfig::class.simpleName}" }
        this.navigationComponent = navigationComponent
        if (pendingActions.isNotEmpty()) {
            val actions: List<() -> Unit> = pendingActions.toList()
            pendingActions.clear()
            logger.d { "bind: executing ${actions.size} pending action(s)" }
            actions.forEach { it() }
        }
    }

    // ── Tree traversal ──

    fun findAncestorBy(predicate: (Navigator<*>) -> Boolean): Navigator<*>? {
        var parent: Navigator<*>? = this.parent
        while (parent != null && !predicate(parent)) {
            parent = parent.parent
        }
        return parent
    }

    /**
     * Returns the grandparent (parent's parent) if it matches [predicate], or `null` otherwise.
     */
    fun findGrandParentBy(predicate: (Navigator<*>) -> Boolean): Navigator<*>? {
        val grandParent: Navigator<*> = this.parent?.parent ?: return null
        return if (predicate(grandParent)) grandParent else null
    }

    fun findCousinBy(predicate: (Navigator<*>) -> Boolean): Navigator<*>? {
        val parent: Navigator<*> = this.parent ?: return null
        val grandParent: Navigator<*> = parent.parent ?: return null
        val siblings: List<Navigator<*>> = grandParent.children
        var sIndex = 0
        while (sIndex < siblings.size) {
            val sibling: Navigator<*> = siblings[sIndex++]
            if (sibling === parent) continue
            val cousins: List<Navigator<*>> = sibling.children
            var cIndex = 0
            while (cIndex < cousins.size) {
                val cousin: Navigator<*> = cousins[cIndex++]
                if (predicate(cousin)) return cousin
            }
        }
        return null
    }

    fun findSiblingBy(predicate: (Navigator<*>) -> Boolean): Navigator<*>? {
        val parent: Navigator<*> = this.parent ?: return null
        val siblings: List<Navigator<*>> = parent.children
        var index = 0
        while (index < siblings.size) {
            val sibling: Navigator<*> = siblings[index++]
            if (sibling === this) continue
            if (predicate(sibling)) return sibling
        }
        return null
    }

    fun findDescendantBy(predicate: (Navigator<*>) -> Boolean): Navigator<*>? {
        val queue: ArrayDeque<Navigator<*>> = ArrayDeque(initialCapacity = children.size)
        queue.addAll(children)
        while (queue.isNotEmpty()) {
            val current: Navigator<*> = queue.removeFirst()
            if (predicate(current)) return current
            queue.addAll(current.children)
        }
        return null
    }

    override fun toString(): String {
        return "${this::class.simpleName}(hostConfig=${navigationComponent?.hostConfig})"
    }
}

val LocalNavigator = compositionLocalOf<Navigator<*>?> {
    null
}

/**
 * Returns the current [LineNavigator] from the composition local, or `null` if
 * the current navigator is not a [LineNavigator].
 */
val LocalLineNavigator: LineNavigator?
    @Composable get() = LocalNavigator.current as? LineNavigator

/**
 * The nearest [SwitchNavigator] at or above the current position: the current navigator if it is
 * one, otherwise the closest ancestor that is, or `null`.
 *
 * It looks upwards rather than only at the current navigator because a tab may hold a stack of its
 * own. A screen sitting in such a stack has a [LineNavigator] beneath it, and asking only the
 * current navigator would answer `null` — and a null here fails quietly, an hour later, as a tab
 * that never switched.
 */
val LocalSwitchNavigator: SwitchNavigator?
    @Composable get() = resolveEnclosingSwitchNavigator(LocalNavigator.current)

/**
 * Resolves the nearest [SwitchNavigator] at or above [current]. Extracted from
 * [LocalSwitchNavigator] so the resolution can be unit-tested without a composition.
 */
internal fun resolveEnclosingSwitchNavigator(current: Navigator<*>?): SwitchNavigator? =
    current as? SwitchNavigator
        ?: current?.findAncestorBy { it is SwitchNavigator } as? SwitchNavigator

/**
 * The nearest [LineNavigator] at or above the current position in the navigator tree:
 * the current navigator if it is a [LineNavigator], otherwise the closest ancestor that is,
 * or `null` if there is none.
 *
 * Unlike [LocalLineNavigator] (which only matches the current navigator), this also resolves
 * the enclosing stack for screens hosted inside a tab/switch container — where the current
 * navigator is a [SwitchNavigator] but pushes belong to the surrounding [LineNavigator].
 */
val LocalEnclosingLineNavigator: LineNavigator?
    @Composable get() = resolveEnclosingLineNavigator(LocalNavigator.current)

/**
 * Resolves the nearest [LineNavigator] at or above [current]: [current] itself if it is a
 * [LineNavigator], otherwise its closest ancestor that is, or `null`. Extracted from
 * [LocalEnclosingLineNavigator] so the resolution can be unit-tested without a composition.
 */
internal fun resolveEnclosingLineNavigator(current: Navigator<*>?): LineNavigator? =
    current as? LineNavigator
        ?: current?.findAncestorBy { it is LineNavigator } as? LineNavigator

/**
 * The OUTERMOST [LineNavigator] above the current position — the app's own stack, the one the tab
 * bar itself sits in.
 *
 * Use it for a screen that must take the whole surface: a reader, a picker, a conversation. Pushed
 * on the nearest stack instead, such a screen opens inside its tab and the tab bar stays drawn over
 * it. [LocalEnclosingLineNavigator] remains the right choice for everything that belongs inside its
 * section and should keep the bar.
 */
val LocalOutermostLineNavigator: LineNavigator?
    @Composable get() = resolveOutermostLineNavigator(LocalNavigator.current)

/**
 * Resolves the outermost [LineNavigator] at or above [current] — the last one on the way up to the
 * root. Extracted from [LocalOutermostLineNavigator] so the walk can be unit-tested without a
 * composition.
 */
internal fun resolveOutermostLineNavigator(current: Navigator<*>?): LineNavigator? =
    generateSequence(current) { it.parent }
        .filterIsInstance<LineNavigator>()
        .lastOrNull()

/**
 * Returns the current [LineNavigator] from the composition local.
 * Throws [IllegalStateException] if the current navigator is not a [LineNavigator].
 */
@Composable
fun requireLineNavigator(): LineNavigator = checkNotNull(LocalNavigator.current as? LineNavigator) {
    "No LineNavigator found in composition. " +
            "Current navigator: ${LocalNavigator.current?.let { it::class.simpleName } ?: "null"}"
}

/**
 * Returns the current [SwitchNavigator] from the composition local.
 * Throws [IllegalStateException] if the current navigator is not a [SwitchNavigator].
 */
@Composable
fun requireSwitchNavigator(): SwitchNavigator = checkNotNull(LocalNavigator.current as? SwitchNavigator) {
    "No SwitchNavigator found in composition. " +
            "Current navigator: ${LocalNavigator.current?.let { it::class.simpleName } ?: "null"}"
}
