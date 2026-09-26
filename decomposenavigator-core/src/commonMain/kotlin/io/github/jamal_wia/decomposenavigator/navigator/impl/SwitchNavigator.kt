package io.github.jamal_wia.decomposenavigator.navigator.impl

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import io.github.jamal_wia.decomposenavigator.ReadinessAwaitable
import io.github.jamal_wia.decomposenavigator.RenderComponent
import io.github.jamal_wia.decomposenavigator.config.NavigationScreenConfig.SwitchScreenConfigContainer
import io.github.jamal_wia.decomposenavigator.config.ScreenConfig
import io.github.jamal_wia.decomposenavigator.controller.SwitchNavigationComponentController
import io.github.jamal_wia.decomposenavigator.emptyStackAnimation
import io.github.jamal_wia.decomposenavigator.navigator.base.Navigator
import io.github.jamal_wia.decomposenavigator.navigator.base.PopHandler
import com.arkivanov.decompose.extensions.compose.stack.animation.StackAnimation
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

typealias SwitchStackAnimation = StackAnimation<SwitchScreenConfigContainer, RenderComponent>

abstract class SwitchNavigator : Navigator<SwitchNavigationComponentController>(),
    PopHandler {

    abstract val activeConfig: SwitchScreenConfigContainer?

    abstract fun switchTo(
        config: SwitchScreenConfigContainer,
        animation: SwitchStackAnimation? = null
    )

    /**
     * Switches to the first container in the current back stack whose wrapped config matches
     * [predicate], reusing that existing container (its id) so no duplicate is created.
     *
     * If nothing matches, [createIfAbsent] is invoked and, when it returns a non-null container,
     * that one is opened instead — just like [switchTo], which opens a container even when it was
     * never in the stack. It is a lambda so the container is only resolved when actually needed;
     * return the caller's canonical container (e.g. the registered tab) so the opened screen keeps
     * its identity.
     *
     * Returns `true` if a match was found (or [createIfAbsent] produced a container that was
     * opened), `false` otherwise (not bound, or no match and [createIfAbsent] returned `null`).
     * With the default `{ null }` the behaviour is the original "open only if already in the stack".
     */
    abstract fun switchToFirst(
        createIfAbsent: () -> SwitchScreenConfigContainer? = { null },
        animation: SwitchStackAnimation? = null,
        predicate: (SwitchScreenConfigContainer) -> Boolean,
    ): Boolean

    /**
     * Suspend variant of [switchToFirst] that additionally awaits the opened component's readiness
     * when it implements [ReadinessAwaitable]. Returns only once the target has been switched to and
     * (if applicable) become ready — so the caller can safely perform a follow-up action the screen
     * depends on (e.g. emit an event it must already be subscribed to) without racing its setup.
     * Same resolution and return semantics as [switchToFirst].
     */
    abstract suspend fun switchToFirstAndAwaitReady(
        createIfAbsent: () -> SwitchScreenConfigContainer? = { null },
        animation: SwitchStackAnimation? = null,
        predicate: (SwitchScreenConfigContainer) -> Boolean,
    ): Boolean

    open var animation: SwitchStackAnimation? by mutableStateOf(null)

    abstract fun withAnimation(animation: SwitchStackAnimation?)
    abstract fun withoutAnimation()

    class DefaultSwitchNavigator : SwitchNavigator() {

        override var navigationComponent: SwitchNavigationComponentController? = null

        override val activeConfig: SwitchScreenConfigContainer?
            get() = navigationComponent?.activeScreenConfig as? SwitchScreenConfigContainer

        override fun withAnimation(animation: SwitchStackAnimation?) {
            checkNotReleased()
            this@DefaultSwitchNavigator.animation = animation
        }

        override fun withoutAnimation() {
            checkNotReleased()
            animation = emptyStackAnimation()
        }

        override fun switchTo(
            config: SwitchScreenConfigContainer,
            animation: SwitchStackAnimation?
        ) {
            checkNotReleased()
            logger.d { "switchTo: ${config.config::class.simpleName}" }
            if (animation != null) this.animation = animation
            withComponentOrEnqueue { it.switchTo(switchConfig = config) }
        }

        override fun switchToFirst(
            createIfAbsent: () -> SwitchScreenConfigContainer?,
            animation: SwitchStackAnimation?,
            predicate: (SwitchScreenConfigContainer) -> Boolean,
        ): Boolean {
            checkNotReleased()
            val component: SwitchNavigationComponentController =
                navigationComponent ?: return false
            val target: SwitchScreenConfigContainer =
                component.backStack.value.firstOrNull(predicate) ?: createIfAbsent() ?: return false
            switchTo(config = target, animation = animation)
            return true
        }

        override suspend fun switchToFirstAndAwaitReady(
            createIfAbsent: () -> SwitchScreenConfigContainer?,
            animation: SwitchStackAnimation?,
            predicate: (SwitchScreenConfigContainer) -> Boolean,
        ): Boolean {
            checkNotReleased()
            val component: SwitchNavigationComponentController =
                navigationComponent ?: return false
            val target: SwitchScreenConfigContainer =
                component.backStack.value.firstOrNull(predicate) ?: createIfAbsent() ?: return false
            if (animation != null) this.animation = animation
            // Await the controller actually applying the switch (it marshals onto the main thread),
            // so the target child is created before we read it.
            suspendCancellableCoroutine { cont ->
                component.switchTo(switchConfig = target) { cont.resume(Unit) }
            }
            // Then await the opened screen's own async readiness, if it reports it.
            (component.childStack.value.active.instance as? ReadinessAwaitable)?.awaitReady()
            return true
        }

        override fun canPop(): Boolean {
            checkNotReleased()
            return navigationComponent?.let { it.backStack.value.size > 1 } == true
        }

        override fun pop(): ScreenConfig? {
            checkNotReleased()
            val component: SwitchNavigationComponentController? = navigationComponent
            if (component == null || component.backStack.value.size <= 1) {
                logger.d { "pop: backStack size <= 1, trying fallback" }
                return fallbackPop?.invoke()
            }
            val popped: SwitchScreenConfigContainer? = component.popBackStack()
            logger.d { "pop: ${popped?.config?.let { it::class.simpleName }}" }
            return popped
        }

        override fun popToRoot(): ScreenConfig? {
            checkNotReleased()
            val component: SwitchNavigationComponentController = requireComponent()
            val backStackSize: Int = component.backStack.value.size
            if (backStackSize <= 1) return component.backStack.value.firstOrNull()
            logger.d { "popToRoot: clearing ${backStackSize - 1} entries" }
            return component.popBackStackToRoot()
        }

        override fun applyFallbackPop(pop: () -> ScreenConfig?) {
            checkNotReleased()
            fallbackPop = pop
        }

        override fun bind(navigationComponent: SwitchNavigationComponentController) {
            defaultBind(navigationComponent)
        }

        override fun release() {
            super.release()
            animation = null
        }
    }
}
