package io.github.jamal_wia.decomposenavigator.deeplink

import io.github.jamal_wia.decomposenavigator.config.ScreenConfig
import io.github.jamal_wia.decomposenavigator.controller.LineNavigationComponentController

/**
 * The slice of root navigation a [DeepLinkHandler] needs: read the screen currently on top, put one
 * on top, or clear back to the first. A thin seam over the Decompose controller so [DeepLinkRouter]
 * and the handlers are unit-testable with a fake — no Decompose/Compose required in tests.
 */
interface DeepLinkNavigator {
    /** The config currently on top of the root stack. */
    val activeScreenConfig: ScreenConfig

    /**
     * Puts [config] on top of the root stack, moving it there if the stack already holds it.
     *
     * Deliberately not called `pushNew`: the navigation library's method of that name de-duplicates
     * against the top of the stack alone, and this one does it against the whole stack. A deep link
     * is the one caller that can ask for a screen the reader already has further down — they opened
     * the prayer schedule, walked on to the Mushaf from it, left the app, and tapped the widget.
     * Decompose rejects a stack holding one configuration twice, and raises that from its own
     * main-thread scope, where neither [DeepLinkRouter] nor the handler is there to catch it.
     *
     * Most handlers guard the already-active case themselves, so that costs no needless navigation.
     * The prayer widget's does not, and does not need to: it clears the navigation before it shows
     * anything, which leaves nothing for a duplicate to be a duplicate of.
     */
    fun showScreen(config: ScreenConfig)

    /**
     * Drops everything above the first screen of the root stack, leaving the reader on it.
     *
     * For a link whose answer is a screen already at the bottom of the stack rather than one to put
     * on top — the guest's tabs, whose Home *is* the day's prayer times. Nothing to push there, only
     * things in the way to clear.
     */
    fun returnToFirstScreen()
}

/** [DeepLinkNavigator] backed by the app's root [LineNavigationComponentController]. */
class DecomposeDeepLinkNavigator(
    private val controller: LineNavigationComponentController,
) : DeepLinkNavigator {

    override val activeScreenConfig: ScreenConfig
        get() = controller.activeScreenConfig

    // pushToFront, not the controller's pushNew: that one de-duplicates against the top alone, which
    // is weaker than what this seam promises.
    override fun showScreen(config: ScreenConfig) {
        controller.pushToFront(config)
    }

    // Not popToFirst: that clears the app's own stack and stops, leaving a screen pushed inside a tab
    // standing. returnToFirstScreen walks the whole tree down from here — see ReturnsToFirstScreen.
    override fun returnToFirstScreen() {
        controller.returnToFirstScreen()
    }
}
