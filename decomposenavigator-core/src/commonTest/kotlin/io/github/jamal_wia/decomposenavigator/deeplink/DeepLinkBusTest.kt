package io.github.jamal_wia.decomposenavigator.deeplink

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/** Verifies the retain/consume semantics of [DeepLinkBus] over an arbitrary link type. */
class DeepLinkBusTest {

    private data class TestLink(val target: String)

    @Test
    fun `publish exposes the link as pending`() {
        val bus = DeepLinkBus<TestLink>()
        val link = TestLink("chat/1")

        bus.publish(link)

        assertEquals(link, bus.pending.value)
    }

    @Test
    fun `consume clears the pending link`() {
        val bus = DeepLinkBus<TestLink>()
        val link = TestLink("chat/1")
        bus.publish(link)

        bus.consume(link)

        assertNull(bus.pending.value)
    }

    @Test
    fun `consume does not clear a newer link`() {
        val bus = DeepLinkBus<TestLink>()
        bus.publish(TestLink("chat/1"))
        bus.publish(TestLink("chat/2"))

        bus.consume(TestLink("chat/1"))

        assertEquals(TestLink("chat/2"), bus.pending.value)
    }
}
