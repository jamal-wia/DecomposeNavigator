package io.github.jamal_wia.decomposenavigator.testing

import io.github.jamal_wia.decomposenavigator.config.ScreenConfig
import kotlin.test.Test
import kotlin.test.assertEquals

private data object Home : ScreenConfig
private data object ArticleScreen : ScreenConfig

class FakeDeepLinkNavigatorTest {

    @Test
    fun showScreen_recordsAndBecomesActive() {
        val navigator = FakeDeepLinkNavigator(initial = Home)

        navigator.showScreen(ArticleScreen)

        assertEquals(ArticleScreen, navigator.activeScreenConfig)
        assertEquals(listOf<ScreenConfig>(ArticleScreen), navigator.shown)
    }

    @Test
    fun returnToFirstScreen_isCountedButDoesNotChangeActiveScreen() {
        val navigator = FakeDeepLinkNavigator(initial = Home)
        navigator.showScreen(ArticleScreen)

        navigator.returnToFirstScreen()
        navigator.returnToFirstScreen()

        assertEquals(2, navigator.returnToFirstScreenCallCount)
        assertEquals(ArticleScreen, navigator.activeScreenConfig)
    }
}
