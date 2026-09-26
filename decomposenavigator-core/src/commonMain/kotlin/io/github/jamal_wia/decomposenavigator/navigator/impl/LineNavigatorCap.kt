package io.github.jamal_wia.decomposenavigator.navigator.impl

import io.github.jamal_wia.decomposenavigator.config.ScreenConfig

/**
 * Pushes [config] on top of this stack, unless doing so would take [LineNavigator.stackSize] past
 * [maxDepth] — then replaces the current top instead, so the stack never grows past the cap.
 *
 * [SwitchNavigator]/`TabNavigation` already cap tab-switch history via
 * `NavigationScreenConfig.TabNavigation.maxBackStackSize`; this is the equivalent for a plain
 * push-based stack, for callers that want a bounded "back is never more than N taps" guarantee (a
 * drawer or tab whose own stack should not grow without limit) without hand-rolling the
 * push-or-replace check themselves.
 */
fun LineNavigator.pushCapped(
    config: ScreenConfig,
    maxDepth: Int,
    animation: LineStackAnimation? = null,
) {
    if (stackSize() >= maxDepth) {
        replace(config, animation)
    } else {
        push(config, animation)
    }
}
