package io.github.jamal_wia.decomposenavigator.testing

import io.github.jamal_wia.decomposenavigator.controller.base.NavigationComponentController
import io.github.jamal_wia.decomposenavigator.config.NavigationScreenConfig
import io.github.jamal_wia.decomposenavigator.config.ScreenConfig
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertSame
import kotlin.test.assertTrue

private class FakeHostConfig(override val typeId: String = "fake", override val id: Long = 1L) :
    NavigationScreenConfig()

private class FakeComponentController(override val hostConfig: FakeHostConfig) :
    NavigationComponentController<FakeHostConfig>() {
    override val activeScreenConfig get() = error("not needed for this test")
    override fun navigate(
        transformer: (stack: List<ScreenConfig>) -> List<ScreenConfig>,
        onComplete: (newStack: List<ScreenConfig>, oldStack: List<ScreenConfig>) -> Unit,
    ) = error("not needed for this test")
}

class RecordingNavigatorTest {

    @Test
    fun bind_incrementsCountAndFlushesPendingActions() {
        val navigator = RecordingNavigator()
        var ran = false

        navigator.runOrEnqueue { ran = true }
        assertFalse(ran, "no component bound yet — action must be queued, not run")

        val component = FakeComponentController(FakeHostConfig())
        navigator.bind(component)

        assertTrue(ran, "bind() must flush pending actions")
        assertEquals(1, navigator.bindCallCount)
        assertSame(component, navigator.boundComponent)
    }

    @Test
    fun link_wiresParentAndChild() {
        val parent = RecordingNavigator()
        val child = link(parent, RecordingNavigator())

        assertSame(parent, child.parent)
        assertTrue(parent.children.contains(child))
    }

    @Test
    fun release_detachesFromParentAndChildren() {
        val parent = RecordingNavigator()
        val child = link(parent, RecordingNavigator())

        child.release()

        assertTrue(child.isReleased)
        assertFalse(parent.children.contains(child))
    }
}
