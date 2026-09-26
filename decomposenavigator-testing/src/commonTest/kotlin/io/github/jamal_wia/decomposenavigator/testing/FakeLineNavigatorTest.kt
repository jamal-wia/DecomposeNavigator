package io.github.jamal_wia.decomposenavigator.testing

import io.github.jamal_wia.decomposenavigator.config.ScreenConfig
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

private data object ScreenA : ScreenConfig
private data object ScreenB : ScreenConfig
private data object ScreenC : ScreenConfig

class FakeLineNavigatorTest {

    @Test
    fun push_appendsToStackAndRecordsCall() {
        val navigator = FakeLineNavigator(initialStack = listOf(ScreenA))

        navigator.push(ScreenB)

        assertEquals(listOf(ScreenA, ScreenB), navigator.stack)
        assertEquals(listOf<ScreenConfig>(ScreenB), navigator.pushCalls)
    }

    @Test
    fun pushNew_isNoOpWhenTopAlreadyEqual() {
        val navigator = FakeLineNavigator(initialStack = listOf(ScreenA))

        navigator.pushNew(ScreenA)

        assertEquals(listOf(ScreenA), navigator.stack)
        assertEquals(listOf<ScreenConfig>(ScreenA), navigator.pushNewCalls, "call is still recorded even as a no-op")
    }

    @Test
    fun replaceAll_clearsStackDownToOneScreen() {
        val navigator = FakeLineNavigator(initialStack = listOf(ScreenA, ScreenB))

        navigator.replaceAll(ScreenC)

        assertEquals(listOf(ScreenC), navigator.stack)
    }

    @Test
    fun pop_returnsTopAndShrinksStack() {
        val navigator = FakeLineNavigator(initialStack = listOf(ScreenA, ScreenB))

        val popped: ScreenConfig? = navigator.pop()

        assertEquals(ScreenB, popped)
        assertEquals(listOf(ScreenA), navigator.stack)
    }

    @Test
    fun pop_onSingleScreenStack_fallsBackInsteadOfPopping() {
        val navigator = FakeLineNavigator(initialStack = listOf(ScreenA))
        var fallbackCalls = 0
        navigator.applyFallbackPop {
            fallbackCalls++
            null
        }

        val popped: ScreenConfig? = navigator.pop()

        assertNull(popped)
        assertEquals(1, fallbackCalls)
        assertEquals(listOf(ScreenA), navigator.stack, "fallback must not itself mutate the stack")
    }

    @Test
    fun canPop_reflectsStackSize() {
        assertFalse(FakeLineNavigator(initialStack = listOf(ScreenA)).canPop())
        assertTrue(FakeLineNavigator(initialStack = listOf(ScreenA, ScreenB)).canPop())
    }

    @Test
    fun popTo_dropsEverythingAboveTheMatch() {
        val navigator = FakeLineNavigator(initialStack = listOf(ScreenA, ScreenB, ScreenC))

        val found: Boolean = navigator.popTo { it == ScreenA }

        assertTrue(found)
        assertEquals(listOf(ScreenA), navigator.stack)
    }

    @Test
    fun popTo_withNoMatch_leavesStackUnchanged() {
        val navigator = FakeLineNavigator(initialStack = listOf(ScreenA, ScreenB))

        val found: Boolean = navigator.popTo { it == ScreenC }

        assertFalse(found)
        assertEquals(listOf(ScreenA, ScreenB), navigator.stack)
    }

    @Test
    fun navigate_appliesTransformerAndReportsOldAndNewStack() {
        val navigator = FakeLineNavigator(initialStack = listOf(ScreenA))
        var reportedNew: List<ScreenConfig>? = null
        var reportedOld: List<ScreenConfig>? = null

        navigator.navigate(
            transformer = { it + ScreenB },
            onComplete = { newStack, oldStack ->
                reportedNew = newStack
                reportedOld = oldStack
            },
        )

        assertEquals(listOf(ScreenA, ScreenB), navigator.stack)
        assertEquals(listOf(ScreenA, ScreenB), reportedNew)
        assertEquals(listOf(ScreenA), reportedOld)
    }

    @Test
    fun releasedNavigator_throwsOnFurtherNavigation() {
        val navigator = FakeLineNavigator(initialStack = listOf(ScreenA))

        navigator.release()

        assertTrue(navigator.isReleased)
        try {
            navigator.push(ScreenB)
            error("push after release must throw")
        } catch (expected: IllegalStateException) {
            // expected — matches every real Navigator's checkNotReleased() contract
        }
    }
}
