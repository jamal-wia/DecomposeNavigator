package io.github.jamal_wia.decomposenavigator

import com.arkivanov.decompose.extensions.compose.stack.animation.Direction
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

@Serializable
private data object OtherSheetLikeScreen : ScreenConfig, CoveringScreen

/** The two children of a push or a replace: the arriving one, and the one it goes over. */
private val FORWARD_CHILDREN: List<Direction> = listOf(Direction.ENTER_FRONT, Direction.EXIT_BACK)

/** The two children of a pop: the leaving one, and the one it uncovers. */
private val BACKWARD_CHILDREN: List<Direction> = listOf(Direction.EXIT_FRONT, Direction.ENTER_BACK)

class CoveringScreenTest {

    @Test
    fun `two plain screens slide both ways`() {
        FORWARD_CHILDREN.forEach { direction ->
            assertFalse(isCoverTransition(direction, OtherPlainScreen, PlainScreen), "$direction")
        }
        BACKWARD_CHILDREN.forEach { direction ->
            assertFalse(isCoverTransition(direction, PlainScreen, OtherPlainScreen), "$direction")
        }
    }

    @Test
    fun `the first screen of a stack has nothing to cover`() {
        assertFalse(isCoverTransition(Direction.ENTER_FRONT, PlainScreen, null))
    }

    @Test
    fun `a covering screen rising over a plain one is a cover for both children`() {
        // The cover rises, and the screen it goes over holds still instead of sliding away.
        FORWARD_CHILDREN.forEach { direction ->
            assertTrue(isCoverTransition(direction, SheetLikeScreen, PlainScreen), "$direction")
        }
    }

    @Test
    fun `a covering screen sinking back off a plain one is a cover for both children`() {
        // The cover sinks, and the screen it uncovers holds still instead of sliding back in.
        BACKWARD_CHILDREN.forEach { direction ->
            assertTrue(isCoverTransition(direction, PlainScreen, SheetLikeScreen), "$direction")
        }
    }

    @Test
    fun `a plain screen pushed on top of a covering one slides`() {
        // The covering screen is beneath here: it is the plain screen that arrives, and it arrives
        // like any other plain screen, from the side and not from below. A replace of the covering
        // screen by a plain one is the same forward change, with the same answer.
        FORWARD_CHILDREN.forEach { direction ->
            assertFalse(isCoverTransition(direction, PlainScreen, SheetLikeScreen), "$direction")
        }
    }

    @Test
    fun `a plain screen popped back off a covering one slides`() {
        BACKWARD_CHILDREN.forEach { direction ->
            assertFalse(isCoverTransition(direction, SheetLikeScreen, PlainScreen), "$direction")
        }
    }

    @Test
    fun `a covering screen over another covering screen is a cover both ways`() {
        FORWARD_CHILDREN.forEach { direction ->
            assertTrue(isCoverTransition(direction, OtherSheetLikeScreen, SheetLikeScreen), "$direction")
        }
        BACKWARD_CHILDREN.forEach { direction ->
            assertTrue(isCoverTransition(direction, SheetLikeScreen, OtherSheetLikeScreen), "$direction")
        }
    }
}
