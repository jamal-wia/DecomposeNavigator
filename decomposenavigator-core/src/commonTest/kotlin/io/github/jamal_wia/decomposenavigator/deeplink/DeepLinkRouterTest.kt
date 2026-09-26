package io.github.jamal_wia.decomposenavigator.deeplink

import io.github.jamal_wia.decomposenavigator.config.ScreenConfig
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest

/**
 * Routing contract of [DeepLinkRouter]: exactly one handler claims a link, readiness is awaited rather
 * than dropped, a newer tap cancels an older one wherever it is, and no failure takes down routing.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class DeepLinkRouterTest {

    private data class TestLink(val target: String)

    private object BootConfig : ScreenConfig
    private object ReadyConfig : ScreenConfig
    private data class PushedConfig(val of: String) : ScreenConfig

    /** Records pushes and reflects them in [activeScreenConfig], like the real controller does. */
    private class FakeNavigator(
        override var activeScreenConfig: ScreenConfig = ReadyConfig,
    ) : DeepLinkNavigator {
        val pushed = mutableListOf<ScreenConfig>()

        override fun showScreen(config: ScreenConfig) {
            pushed += config
            activeScreenConfig = config
        }

        override fun returnToFirstScreen() {
            activeScreenConfig = ReadyConfig
        }
    }

    private class RecordingHandler(
        private val claims: (TestLink) -> Boolean = { true },
        private val onHandle: suspend (TestLink) -> Unit = {},
    ) : DeepLinkHandler<TestLink> {
        val handled = mutableListOf<TestLink>()
        val completed = mutableListOf<TestLink>()

        override fun canHandle(link: TestLink): Boolean = claims(link)

        override suspend fun handle(link: TestLink, navigator: DeepLinkNavigator) {
            handled += link
            onHandle(link)
            navigator.showScreen(PushedConfig(link.target))
            completed += link
        }
    }

    /** Readiness that is already satisfied — the router proceeds straight to the handler. */
    private val alwaysReady: suspend (TestLink) -> Unit = {}

    @Test
    fun `a pending link goes to the handler that claims it`() = runTest {
        val bus = DeepLinkBus<TestLink>()
        val chat = RecordingHandler(claims = { it.target == "chat" })
        val notifications = RecordingHandler(claims = { it.target == "notifications" })
        val navigator = FakeNavigator()
        val router = DeepLinkRouter(bus = bus, handlers = listOf(chat, notifications))
        backgroundScope.launch { router.route(navigator, awaitReady = alwaysReady) }

        bus.publish(TestLink("notifications"))
        runCurrent()

        assertEquals(listOf(TestLink("notifications")), notifications.handled)
        assertTrue(chat.handled.isEmpty())
        assertEquals(listOf<ScreenConfig>(PushedConfig("notifications")), navigator.pushed)
    }

    @Test
    fun `only the first matching handler routes the link`() = runTest {
        val bus = DeepLinkBus<TestLink>()
        val first = RecordingHandler()
        val second = RecordingHandler()
        val navigator = FakeNavigator()
        val router = DeepLinkRouter(bus = bus, handlers = listOf(first, second))
        backgroundScope.launch { router.route(navigator, awaitReady = alwaysReady) }

        bus.publish(TestLink("chat"))
        runCurrent()

        assertEquals(listOf(TestLink("chat")), first.handled)
        assertTrue(second.handled.isEmpty(), "a claimed link must not reach a second handler")
        assertEquals(1, navigator.pushed.size, "one tap must not push two screens")
    }

    @Test
    fun `a handled link is consumed`() = runTest {
        val bus = DeepLinkBus<TestLink>()
        val router = DeepLinkRouter(bus = bus, handlers = listOf(RecordingHandler()))
        backgroundScope.launch { router.route(FakeNavigator(), awaitReady = alwaysReady) }

        bus.publish(TestLink("chat"))
        runCurrent()

        assertNull(bus.pending.value)
    }

    @Test
    fun `a link no handler claims stays pending`() = runTest {
        val bus = DeepLinkBus<TestLink>()
        val chat = RecordingHandler(claims = { it.target == "chat" })
        val router = DeepLinkRouter(bus = bus, handlers = listOf(chat))
        backgroundScope.launch { router.route(FakeNavigator(), awaitReady = alwaysReady) }

        val tabTarget = TestLink("tasks-tab")
        bus.publish(tabTarget)
        runCurrent()

        assertTrue(chat.handled.isEmpty())
        assertEquals(tabTarget, bus.pending.value)

        // The router was listening all along — a link it does claim is handled — so the two checks above
        // are about the unclaimed link, not about a router that never started collecting.
        bus.publish(TestLink("chat"))
        runCurrent()
        assertEquals(listOf(TestLink("chat")), chat.handled)
    }

    @Test
    fun `a link tapped before the app is ready is applied once ready`() = runTest {
        val bus = DeepLinkBus<TestLink>()
        val handler = RecordingHandler()
        val ready = CompletableDeferred<Unit>()
        val router = DeepLinkRouter(bus = bus, handlers = listOf(handler))
        backgroundScope.launch { router.route(FakeNavigator(), awaitReady = { ready.await() }) }

        bus.publish(TestLink("chat"))
        advanceTimeBy(WAIT_MS)
        runCurrent()
        assertTrue(handler.handled.isEmpty(), "must not navigate while the app is still booting")

        ready.complete(Unit)
        runCurrent()

        assertEquals(listOf(TestLink("chat")), handler.handled)
        assertNull(bus.pending.value)
    }

    @Test
    fun `a newer tap supersedes one still waiting for readiness`() = runTest {
        val bus = DeepLinkBus<TestLink>()
        val handler = RecordingHandler()
        val ready = CompletableDeferred<Unit>()
        val router = DeepLinkRouter(bus = bus, handlers = listOf(handler))
        backgroundScope.launch { router.route(FakeNavigator(), awaitReady = { ready.await() }) }

        bus.publish(TestLink("chat"))
        advanceTimeBy(WAIT_MS)
        runCurrent()
        bus.publish(TestLink("quiz"))
        runCurrent()

        ready.complete(Unit)
        runCurrent()

        assertEquals(listOf(TestLink("quiz")), handler.handled, "the superseded link must be dropped")
    }

    @Test
    fun `a newer tap cancels a handler already navigating`() = runTest {
        val bus = DeepLinkBus<TestLink>()
        val firstStepDone = CompletableDeferred<Unit>()
        // A multi-step handler: it suspends between steps, exactly where a newer tap must cut it off.
        val handler = RecordingHandler(onHandle = { link ->
            if (link.target == "chat") firstStepDone.await()
        })
        val navigator = FakeNavigator()
        val router = DeepLinkRouter(bus = bus, handlers = listOf(handler))
        backgroundScope.launch { router.route(navigator, awaitReady = alwaysReady) }

        bus.publish(TestLink("chat"))
        runCurrent()
        assertEquals(listOf(TestLink("chat")), handler.handled, "the first handler must have started")

        bus.publish(TestLink("quiz"))
        runCurrent()
        firstStepDone.complete(Unit)
        runCurrent()

        assertEquals(
            listOf(TestLink("quiz")),
            handler.completed,
            "the superseded handler must be cancelled mid-navigation, not allowed to finish",
        )
        assertEquals(listOf<ScreenConfig>(PushedConfig("quiz")), navigator.pushed)
    }

    @Test
    fun `a failing handler does not stop routing later links`() = runTest {
        val bus = DeepLinkBus<TestLink>()
        val handler = RecordingHandler(onHandle = { if (it.target == "boom") error("handler failed") })
        val router = DeepLinkRouter(bus = bus, handlers = listOf(handler))
        backgroundScope.launch { router.route(FakeNavigator(), awaitReady = alwaysReady) }

        bus.publish(TestLink("boom"))
        runCurrent()
        bus.publish(TestLink("chat"))
        runCurrent()

        assertEquals(listOf(TestLink("boom"), TestLink("chat")), handler.handled)
    }

    @Test
    fun `a failing readiness wait does not stop routing later links`() = runTest {
        val bus = DeepLinkBus<TestLink>()
        val handler = RecordingHandler()
        var failNext = true
        val router = DeepLinkRouter(bus = bus, handlers = listOf(handler))
        backgroundScope.launch {
            router.route(FakeNavigator(), awaitReady = {
                if (failNext) error("readiness gate blew up")
            })
        }

        bus.publish(TestLink("boom"))
        runCurrent()
        assertTrue(handler.handled.isEmpty())

        failNext = false
        bus.publish(TestLink("chat"))
        runCurrent()

        assertEquals(listOf(TestLink("chat")), handler.handled, "routing must survive a failed gate")
    }

    @Test
    fun `a link whose routing failed is left pending`() = runTest {
        val bus = DeepLinkBus<TestLink>()
        val handler = RecordingHandler(onHandle = { error("handler failed") })
        val router = DeepLinkRouter(bus = bus, handlers = listOf(handler))
        backgroundScope.launch { router.route(FakeNavigator(), awaitReady = alwaysReady) }

        val link = TestLink("boom")
        bus.publish(link)
        runCurrent()

        assertEquals(listOf(link), handler.handled, "the handler was reached — the routing did fail, not skip")
        assertEquals(link, bus.pending.value, "a failed tap must not be silently swallowed")
    }

    private companion object {
        /** Comfortably past any interval a readiness wait might use. */
        const val WAIT_MS = 1_000L
    }
}
