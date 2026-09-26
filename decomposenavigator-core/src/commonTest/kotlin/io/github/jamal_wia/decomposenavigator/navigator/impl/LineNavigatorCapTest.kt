package io.github.jamal_wia.decomposenavigator.navigator.impl

import io.github.jamal_wia.decomposenavigator.config.ScreenConfig
import io.github.jamal_wia.decomposenavigator.controller.LineNavigationComponentController
import kotlin.test.Test
import kotlin.test.assertEquals

private data object ScreenA : ScreenConfig
private data object ScreenB : ScreenConfig
private data object ScreenC : ScreenConfig

/** Minimal in-memory [LineNavigator] double for exercising [pushCapped] alone. */
private class CappingLineNavigator(initial: List<ScreenConfig>) : LineNavigator() {
    override var navigationComponent: LineNavigationComponentController? = null
    val stack: MutableList<ScreenConfig> = initial.toMutableList()

    override fun push(config: ScreenConfig, animation: LineStackAnimation?) {
        stack.add(config)
    }

    override fun replace(config: ScreenConfig, animation: LineStackAnimation?) {
        stack[stack.lastIndex] = config
    }

    override fun stackSize(): Int = stack.size

    override fun bind(navigationComponent: LineNavigationComponentController) = Unit
    override fun pushNew(config: ScreenConfig, animation: LineStackAnimation?) = Unit
    override fun pushToFront(config: ScreenConfig, animation: LineStackAnimation?) = Unit
    override fun replaceAll(config: ScreenConfig, animation: LineStackAnimation?) = Unit
    override fun popToFirst(animation: LineStackAnimation?) = Unit
    override fun bringToFront(config: ScreenConfig, animation: LineStackAnimation?) = Unit
    override fun popTo(predicate: (ScreenConfig) -> Boolean): Boolean = false
    override fun withAnimation(animation: LineStackAnimation?) = Unit
    override fun withoutAnimation() = Unit
    override fun canPop(): Boolean = stack.size > 1
    override fun pop(): ScreenConfig? = null
    override fun popToRoot(): ScreenConfig? = null
    override fun applyFallbackPop(pop: () -> ScreenConfig?) = Unit
    override fun navigate(
        transformer: (stack: List<ScreenConfig>) -> List<ScreenConfig>,
        onComplete: (newStack: List<ScreenConfig>, oldStack: List<ScreenConfig>) -> Unit,
    ) = Unit
}

class LineNavigatorCapTest {

    @Test
    fun pushCapped_belowCap_pushesNormally() {
        val navigator = CappingLineNavigator(listOf(ScreenA))

        navigator.pushCapped(ScreenB, maxDepth = 3)

        assertEquals(listOf(ScreenA, ScreenB), navigator.stack)
    }

    @Test
    fun pushCapped_atCap_replacesTopInsteadOfGrowing() {
        val navigator = CappingLineNavigator(listOf(ScreenA, ScreenB))

        navigator.pushCapped(ScreenC, maxDepth = 2)

        assertEquals(listOf(ScreenA, ScreenC), navigator.stack)
    }

    @Test
    fun pushCapped_pastCap_stillReplacesRatherThanGrowingFurther() {
        val navigator = CappingLineNavigator(listOf(ScreenA, ScreenB, ScreenC))

        navigator.pushCapped(ScreenA, maxDepth = 2)

        assertEquals(3, navigator.stack.size, "replace does not shrink an already-over-cap stack")
        assertEquals(ScreenA, navigator.stack.last())
    }
}
