package io.github.jamal_wia.decomposenavigator.testing

import io.github.jamal_wia.decomposenavigator.config.ScreenConfig
import io.github.jamal_wia.decomposenavigator.controller.LineNavigationComponentController
import io.github.jamal_wia.decomposenavigator.navigator.impl.LineNavigator
import io.github.jamal_wia.decomposenavigator.navigator.impl.LineStackAnimation

/**
 * A real, in-memory [LineNavigator] — not a call recorder over a no-op. Every method mutates
 * [stack] the same way the Decompose-backed `DefaultLineNavigator` would mutate its `ChildStack`, so
 * a test can push/pop/replace through this fake and assert on the resulting [stack], not just on
 * which methods were called with which arguments (those are still recorded, in the `*Calls` lists,
 * for tests that need call-order or argument assertions instead).
 *
 * Never bound to a real [LineNavigationComponentController] — [navigationComponent] stays `null`
 * unless a test calls [bind] itself. Nothing here needs Decompose or Compose on the test classpath.
 *
 * @param initialStack the starting stack, bottom to top. Defaults to empty, matching a navigator
 * that has never been pushed to; most tests instead pass at least one config, since a real
 * `LineNavigationComponentController` never has an empty stack (see its `initialConfigs` check).
 */
class FakeLineNavigator(
    initialStack: List<ScreenConfig> = emptyList(),
) : LineNavigator() {

    override var navigationComponent: LineNavigationComponentController? = null

    private val backing: MutableList<ScreenConfig> = initialStack.toMutableList()

    /** Current stack, bottom to top. The last element is the active screen. */
    val stack: List<ScreenConfig> get() = backing.toList()

    val pushCalls: MutableList<ScreenConfig> = mutableListOf()
    val pushNewCalls: MutableList<ScreenConfig> = mutableListOf()
    val pushToFrontCalls: MutableList<ScreenConfig> = mutableListOf()
    val replaceCalls: MutableList<ScreenConfig> = mutableListOf()
    val replaceAllCalls: MutableList<ScreenConfig> = mutableListOf()
    val bringToFrontCalls: MutableList<ScreenConfig> = mutableListOf()
    val popToCalls: MutableList<(ScreenConfig) -> Boolean> = mutableListOf()

    /** `(oldStack, newStack)` recorded for every [navigate] call, in order. */
    val navigateCalls: MutableList<Pair<List<ScreenConfig>, List<ScreenConfig>>> = mutableListOf()

    override fun push(config: ScreenConfig, animation: LineStackAnimation?) {
        checkNotReleased()
        pushCalls += config
        backing.add(config)
    }

    override fun pushNew(config: ScreenConfig, animation: LineStackAnimation?) {
        checkNotReleased()
        pushNewCalls += config
        if (backing.lastOrNull() != config) backing.add(config)
    }

    override fun pushToFront(config: ScreenConfig, animation: LineStackAnimation?) {
        checkNotReleased()
        pushToFrontCalls += config
        backing.remove(config)
        backing.add(config)
    }

    override fun replace(config: ScreenConfig, animation: LineStackAnimation?) {
        checkNotReleased()
        replaceCalls += config
        if (backing.isNotEmpty()) backing[backing.lastIndex] = config else backing.add(config)
    }

    override fun replaceAll(config: ScreenConfig, animation: LineStackAnimation?) {
        checkNotReleased()
        replaceAllCalls += config
        backing.clear()
        backing.add(config)
    }

    override fun popToFirst(animation: LineStackAnimation?) {
        checkNotReleased()
        while (backing.size > 1) backing.removeAt(backing.lastIndex)
    }

    override fun bringToFront(config: ScreenConfig, animation: LineStackAnimation?) {
        checkNotReleased()
        bringToFrontCalls += config
        backing.remove(config)
        backing.add(config)
    }

    override fun stackSize(): Int = backing.size

    override fun popTo(predicate: (ScreenConfig) -> Boolean): Boolean {
        checkNotReleased()
        popToCalls += predicate
        val index: Int = backing.indexOfLast(predicate)
        if (index == -1) return false
        while (backing.lastIndex > index) backing.removeAt(backing.lastIndex)
        return true
    }

    override fun withAnimation(animation: LineStackAnimation?) {
        checkNotReleased()
        this.animation = animation
    }

    override fun withoutAnimation() {
        checkNotReleased()
        animation = null
    }

    override fun canPop(): Boolean = backing.size > 1

    override fun pop(): ScreenConfig? {
        checkNotReleased()
        if (backing.size <= 1) return fallbackPop?.invoke()
        return backing.removeAt(backing.lastIndex)
    }

    override fun popToRoot(): ScreenConfig? {
        checkNotReleased()
        while (backing.size > 1) backing.removeAt(backing.lastIndex)
        return backing.firstOrNull()
    }

    override fun applyFallbackPop(pop: () -> ScreenConfig?) {
        checkNotReleased()
        fallbackPop = pop
    }

    override fun navigate(
        transformer: (stack: List<ScreenConfig>) -> List<ScreenConfig>,
        onComplete: (newStack: List<ScreenConfig>, oldStack: List<ScreenConfig>) -> Unit,
    ) {
        checkNotReleased()
        val old: List<ScreenConfig> = backing.toList()
        val new: List<ScreenConfig> = transformer(old)
        backing.clear()
        backing.addAll(new)
        navigateCalls += old to new
        onComplete(new, old)
    }

    override fun bind(navigationComponent: LineNavigationComponentController) {
        defaultBind(navigationComponent)
    }
}
