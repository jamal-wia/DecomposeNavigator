package io.github.jamal_wia.decomposenavigator.navigator.base

import io.github.jamal_wia.decomposenavigator.config.ScreenConfig

interface NavigateHandler {

    fun navigate(
        transformer: (
            stack: List<ScreenConfig>
        ) -> List<ScreenConfig>,

        onComplete: (
            newStack: List<ScreenConfig>,
            oldStack: List<ScreenConfig>
        ) -> Unit,
    )
}
