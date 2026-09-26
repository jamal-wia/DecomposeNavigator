package io.github.jamal_wia.decomposenavigator.navigator.impl

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import io.github.jamal_wia.decomposenavigator.RenderComponent
import io.github.jamal_wia.decomposenavigator.SlideAnimationMarker
import io.github.jamal_wia.decomposenavigator.SlideDirection
import io.github.jamal_wia.decomposenavigator.config.ScreenConfig
import io.github.jamal_wia.decomposenavigator.controller.LineNavigationComponentController
import io.github.jamal_wia.decomposenavigator.emptyStackAnimation
import io.github.jamal_wia.decomposenavigator.navigator.base.NavigateHandler
import io.github.jamal_wia.decomposenavigator.navigator.base.Navigator
import io.github.jamal_wia.decomposenavigator.navigator.base.PopHandler
import io.github.jamal_wia.decomposenavigator.slideStackAnimation
import com.arkivanov.decompose.extensions.compose.stack.animation.StackAnimation

typealias LineStackAnimation = StackAnimation<ScreenConfig, RenderComponent>

abstract class LineNavigator : Navigator<LineNavigationComponentController>(),
    PopHandler,
    NavigateHandler {

    open var animation: LineStackAnimation? by mutableStateOf(null)

    protected val slideDirection: MutableState<SlideDirection> =
        mutableStateOf(SlideDirection.FORWARD)

    abstract fun push(config: ScreenConfig, animation: LineStackAnimation? = null)
    abstract fun pushNew(config: ScreenConfig, animation: LineStackAnimation? = null)

    /**
     * Puts [config] on top, moving the copy already in the stack rather than adding a second one.
     *
     * [pushNew] compares against the top alone, which is enough for a double tap but not for a screen
     * the reader can reach twice by different routes — the city picker, opened from Home and again
     * from the prayer schedule that was pushed over it. Two equal configurations in one stack is what
     * Decompose rejects with "Configurations must be unique", from a scope that does not catch it.
     */
    abstract fun pushToFront(config: ScreenConfig, animation: LineStackAnimation? = null)
    abstract fun replace(config: ScreenConfig, animation: LineStackAnimation? = null)
    abstract fun replaceAll(config: ScreenConfig, animation: LineStackAnimation? = null)
    abstract fun popToFirst(animation: LineStackAnimation? = null)
    abstract fun bringToFront(config: ScreenConfig, animation: LineStackAnimation? = null)

    /**
     * Number of screens currently on this stack (the active screen plus its back stack), `0` if
     * this navigator is not yet bound to a component. Lets a caller decide, before pushing, whether
     * doing so would grow the stack past a depth it wants to cap.
     */
    abstract fun stackSize(): Int

    /**
     * Pops back to the last screen in the stack that matches [predicate].
     * Searches from the end of the back stack (including the active screen).
     * Returns `true` if a matching screen was found and the stack was trimmed,
     * `false` if no screen matched (stack unchanged).
     */
    abstract fun popTo(predicate: (ScreenConfig) -> Boolean): Boolean

    abstract fun withAnimation(animation: LineStackAnimation?)
    abstract fun withoutAnimation()

    /**
     * If [animation] is a [SlideAnimationMarker], updates [slideDirection] and keeps the
     * current [StackAnimation] object intact (avoiding Decompose's per-child caching issue).
     * Otherwise, sets [animation] directly.
     */
    protected fun applyAnimation(animation: LineStackAnimation?) {
        if (animation == null) return
        val marker = animation as? SlideAnimationMarker
        if (marker != null) {
            slideDirection.value = marker.direction
        } else {
            this.animation = animation
        }
    }

    class DefaultLineNavigator : LineNavigator() {

        override var navigationComponent: LineNavigationComponentController? = null

        init {
            animation = slideStackAnimation(slideDirection)
        }

        override fun withAnimation(animation: LineStackAnimation?) {
            checkNotReleased()
            applyAnimation(animation)
        }

        override fun withoutAnimation() {
            checkNotReleased()
            animation = emptyStackAnimation()
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
            withComponentOrEnqueue { it.navigate(transformer, onComplete) }
        }

        override fun push(config: ScreenConfig, animation: LineStackAnimation?) {
            checkNotReleased()
            logger.d { "push: ${config::class.simpleName}" }
            applyAnimation(animation)
            withComponentOrEnqueue { it.push(config = config) }
        }

        override fun pushNew(config: ScreenConfig, animation: LineStackAnimation?) {
            checkNotReleased()
            logger.d { "pushNew: ${config::class.simpleName}" }
            applyAnimation(animation)
            withComponentOrEnqueue { it.pushNew(config = config) }
        }

        override fun pushToFront(config: ScreenConfig, animation: LineStackAnimation?) {
            checkNotReleased()
            logger.d { "pushToFront: ${config::class.simpleName}" }
            applyAnimation(animation)
            withComponentOrEnqueue { it.pushToFront(config = config) }
        }

        override fun replace(config: ScreenConfig, animation: LineStackAnimation?) {
            checkNotReleased()
            logger.d { "replace: ${config::class.simpleName}" }
            applyAnimation(animation)
            withComponentOrEnqueue { it.replaceCurrent(config) }
        }

        override fun replaceAll(config: ScreenConfig, animation: LineStackAnimation?) {
            checkNotReleased()
            logger.d { "replaceAll: ${config::class.simpleName}" }
            applyAnimation(animation)
            withComponentOrEnqueue { it.replaceAll(config = config) }
        }

        override fun bringToFront(config: ScreenConfig, animation: LineStackAnimation?) {
            checkNotReleased()
            logger.d { "bringToFront: ${config::class.simpleName}" }
            applyAnimation(animation)
            withComponentOrEnqueue { it.bringToFront(config = config) }
        }

        override fun stackSize(): Int {
            checkNotReleased()
            val component: LineNavigationComponentController? = navigationComponent
            return if (component == null) 0 else component.childStack.value.backStack.size + 1
        }

        /**
         * Pops back to the last screen in the stack matching [predicate].
         * Returns `true` if a matching screen was found and navigation occurred,
         * `false` if no match was found (stack unchanged).
         */
        override fun popTo(predicate: (ScreenConfig) -> Boolean): Boolean {
            checkNotReleased()
            val component: LineNavigationComponentController = requireComponent()
            val result: Boolean = component.popTo(predicate)
            logger.d { "popTo: ${if (result) "found" else "no match"}" }
            return result
        }

        override fun canPop(): Boolean {
            checkNotReleased()
            return navigationComponent?.canPop() == true
        }

        override fun pop(): ScreenConfig? {
            checkNotReleased()
            val component: LineNavigationComponentController? = navigationComponent
            if (component == null || !component.canPop()) {
                logger.d { "pop: cannot pop, trying fallback" }
                return fallbackPop?.invoke()
            }
            // Snapshot the active config BEFORE navigating: navigation is now marshalled
            // onto the main thread and may run after this method returns, so we cannot
            // rely on the transformer's side effect to capture the popped config.
            val poppedConfig: ScreenConfig = component.activeScreenConfig
            component.navigate(
                transformer = { stack -> if (stack.size > 1) stack.dropLast(1) else stack },
                onComplete = { _, _ -> }
            )
            logger.d { "pop: ${poppedConfig::class.simpleName}" }
            return poppedConfig
        }

        override fun popToRoot(): ScreenConfig {
            checkNotReleased()
            logger.d { "popToRoot" }
            val component: LineNavigationComponentController = requireComponent()
            component.popToFirst()
            return component.childStack.value.active.configuration
        }

        override fun popToFirst(animation: LineStackAnimation?) {
            checkNotReleased()
            logger.d { "popToFirst" }
            applyAnimation(animation)
            requireComponent().popToFirst()
        }

        override fun bind(navigationComponent: LineNavigationComponentController) {
            defaultBind(navigationComponent)
        }

        override fun release() {
            super.release()
            animation = null
        }
    }
}
