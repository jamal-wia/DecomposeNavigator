package io.github.jamal_wia.decomposenavigator.testing

import io.github.jamal_wia.decomposenavigator.config.NavigationScreenConfig.SwitchScreenConfigContainer
import io.github.jamal_wia.decomposenavigator.config.ScreenConfig
import io.github.jamal_wia.decomposenavigator.controller.SwitchNavigationComponentController
import io.github.jamal_wia.decomposenavigator.navigator.impl.SwitchNavigator
import io.github.jamal_wia.decomposenavigator.navigator.impl.SwitchStackAnimation

/**
 * A real, in-memory [SwitchNavigator]. [backStack] mirrors what
 * `SwitchNavigationComponentController` tracks — the currently active container is always
 * `backStack.last()` — so a test can drive tab/switch navigation and assert on the resulting back
 * stack, not just on recorded call arguments.
 *
 * Never bound to a real [SwitchNavigationComponentController] — [navigationComponent] stays `null`
 * unless a test calls [bind] itself.
 *
 * @param initial the container active when the fake is created. A real switch always has exactly
 * one active container, so this is required rather than defaulted to empty.
 */
class FakeSwitchNavigator(
    initial: SwitchScreenConfigContainer,
) : SwitchNavigator() {

    override var navigationComponent: SwitchNavigationComponentController? = null

    private val backing: MutableList<SwitchScreenConfigContainer> = mutableListOf(initial)

    /** Back stack, bottom to top. [activeConfig] is always `backStack.last()`. */
    val backStack: List<SwitchScreenConfigContainer> get() = backing.toList()

    override val activeConfig: SwitchScreenConfigContainer? get() = backing.lastOrNull()

    val switchToCalls: MutableList<SwitchScreenConfigContainer> = mutableListOf()

    override fun switchTo(config: SwitchScreenConfigContainer, animation: SwitchStackAnimation?) {
        checkNotReleased()
        switchToCalls += config
        backing.remove(config)
        backing.add(config)
    }

    override fun switchToFirst(
        createIfAbsent: () -> SwitchScreenConfigContainer?,
        animation: SwitchStackAnimation?,
        predicate: (SwitchScreenConfigContainer) -> Boolean,
    ): Boolean {
        checkNotReleased()
        val target: SwitchScreenConfigContainer =
            backing.firstOrNull(predicate) ?: createIfAbsent() ?: return false
        switchTo(target, animation)
        return true
    }

    override suspend fun switchToFirstAndAwaitReady(
        createIfAbsent: () -> SwitchScreenConfigContainer?,
        animation: SwitchStackAnimation?,
        predicate: (SwitchScreenConfigContainer) -> Boolean,
    ): Boolean = switchToFirst(createIfAbsent, animation, predicate)

    override fun withAnimation(animation: SwitchStackAnimation?) {
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

    override fun bind(navigationComponent: SwitchNavigationComponentController) {
        defaultBind(navigationComponent)
    }
}
