package io.github.jamal_wia.decomposenavigator.testing

import io.github.jamal_wia.decomposenavigator.config.ScreenConfig
import io.github.jamal_wia.decomposenavigator.deeplink.DeepLinkNavigator

/**
 * A [DeepLinkNavigator] test double: [showScreen] records the config and makes it [activeScreenConfig],
 * [returnToFirstScreen] is counted rather than acted on (there is no stack here to clear).
 *
 * A `DeepLinkHandler` under test never needs Decompose or Compose on the classpath — this is the
 * whole reason [DeepLinkNavigator] is a separate seam from `LineNavigationComponentController`.
 *
 * @param initial the config the reader is on when the fake is created.
 */
class FakeDeepLinkNavigator(initial: ScreenConfig) : DeepLinkNavigator {

    private var active: ScreenConfig = initial

    override val activeScreenConfig: ScreenConfig get() = active

    /** Every config passed to [showScreen], in order. */
    val shown: MutableList<ScreenConfig> = mutableListOf()

    /** How many times [returnToFirstScreen] was called. */
    var returnToFirstScreenCallCount: Int = 0
        private set

    override fun showScreen(config: ScreenConfig) {
        shown += config
        active = config
    }

    override fun returnToFirstScreen() {
        returnToFirstScreenCallCount++
    }
}
