package io.github.jamal_wia.decomposenavigator.navigator.base

import io.github.jamal_wia.decomposenavigator.config.ScreenConfig

interface PopHandler {
    fun canPop(): Boolean
    fun pop(): ScreenConfig?
    fun popToRoot(): ScreenConfig?
    fun applyFallbackPop(pop: () -> ScreenConfig?)
}
