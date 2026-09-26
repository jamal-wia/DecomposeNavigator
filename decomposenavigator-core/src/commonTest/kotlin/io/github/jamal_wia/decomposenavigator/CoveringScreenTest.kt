package io.github.jamal_wia.decomposenavigator

import io.github.jamal_wia.decomposenavigator.config.ScreenConfig
import kotlinx.serialization.Serializable
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@Serializable
private data object PlainScreen : ScreenConfig

@Serializable
private data object OtherPlainScreen : ScreenConfig

@Serializable
private data object SheetLikeScreen : ScreenConfig, CoveringScreen

class CoveringScreenTest {

    @Test
    fun `two plain screens slide`() {
        assertFalse(isCoverTransition(PlainScreen, OtherPlainScreen))
    }

    @Test
    fun `the first screen of a stack has nothing to cover`() {
        assertFalse(isCoverTransition(PlainScreen, null))
    }

    @Test
    fun `a covering screen rising over a plain one is a cover`() {
        assertTrue(isCoverTransition(SheetLikeScreen, PlainScreen))
    }

    @Test
    fun `the plain screen beneath a cover is part of the cover too`() {
        // The screen beneath must hold still, so the transition is a cover from its side as well —
        // otherwise it would slide and fade away under a cover that expects it to stay.
        assertTrue(isCoverTransition(PlainScreen, SheetLikeScreen))
    }
}
