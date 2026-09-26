package io.github.jamal_wia.decomposenavigator.deeplink

import io.github.jamal_wia.decomposenavigator.logging.Logger
import io.github.jamal_wia.decomposenavigator.logging.NoopLogger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.collectLatest

/**
 * Observes a [DeepLinkBus] and routes each pending link to the [DeepLinkHandler] that claims it, once
 * the app is ready. A link tapped while logged out or during cold-start boot is retained by the bus
 * (a StateFlow) and applied when the app becomes ready — never dropped for being early.
 *
 * Readiness is supplied by the caller as [route]'s `awaitReady` rather than baked in, so this stays
 * app-agnostic: it knows nothing about auth, about which screens count as "still booting", or about
 * *how* to wait for them. The app shell owns that knowledge and simply suspends until it is ready —
 * on a flow, a channel, or a poll, whichever fits — while this router stays a plain suspension.
 *
 * A link is **consumed only when a handler claims it**. A link no handler owns is left pending on the
 * bus for another observer to route: some targets can only be reached from a navigator this router
 * doesn't hold (e.g. a bottom-tab switch owned by a screen component), so those observers collect the
 * bus directly and consume the link themselves.
 *
 * Open so a consumer can pin the link type with a non-generic subclass — generics are erased in
 * service-locator keys, so a `DeepLinkRouter<A>` and a `DeepLinkRouter<B>` are the same DI key.
 */
open class DeepLinkRouter<L : Any>(
    private val bus: DeepLinkBus<L>,
    private val handlers: List<DeepLinkHandler<L>>,
    private val logger: Logger = NoopLogger,
) {

    /**
     * Collects pending links and routes them for the app's lifetime; suspends indefinitely.
     *
     * [awaitReady] must suspend until the app can be navigated for the link it is given (e.g.
     * authenticated and past the boot screens) and return; it is awaited before every claimed link, so
     * a target is never pushed over a screen that isn't ready for it. It takes the link because
     * readiness is not the same for all of them — one may be openable to a reader another is not. It
     * may suspend arbitrarily long — a link is never dropped for arriving early — and is cancelled if
     * a newer link supersedes the one being waited on.
     */
    suspend fun route(navigator: DeepLinkNavigator, awaitReady: suspend (L) -> Unit) {
        // collectLatest, not collect: a newer tap must cancel whatever the previous one was still
        // waiting on (readiness, or the handler's own navigation). With a plain collect, a link
        // superseded mid-wait would still be routed first — landing the user on the older target.
        bus.pending.collectLatest { link: L? ->
            if (link == null) return@collectLatest
            // Only links we own are routed here; one we don't stays pending for its real owner.
            val handler: DeepLinkHandler<L> =
                handlers.firstOrNull { it.canHandle(link) } ?: return@collectLatest
            // Neither the readiness wait nor a handler may take down the collector: one failure would
            // otherwise kill routing for the rest of the session. Swallow and log; cancellation
            // bubbles, because that is a newer link superseding this one (or the scope shutting down).
            try {
                awaitReady(link)
                handler.handle(link, navigator)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                logger.e(e) { "Deep link routing failed for $link" }
                // Leave it pending: the failure may be transient, and dropping it silently loses the
                // user's tap. A newer tap supersedes it anyway.
                return@collectLatest
            }
            bus.consume(link)
        }
    }
}
