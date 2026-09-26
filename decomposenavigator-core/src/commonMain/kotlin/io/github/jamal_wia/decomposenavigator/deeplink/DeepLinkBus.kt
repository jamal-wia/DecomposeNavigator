package io.github.jamal_wia.decomposenavigator.deeplink

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * App-scope holder for a pending deep link — the hand-off point between a platform callback that has
 * no navigator (a notification tap arriving from the OS) and the app code that can act on it.
 *
 * It is a [StateFlow] (state, not a one-shot event) so a link tapped during cold start — or while the
 * user is logged out — is **retained** until the app can act on it, instead of being dropped. It holds
 * only the latest link: a newer tap supersedes an unhandled one. Deliberately unbounded: unlike
 * [io.github.jamal_wia.decomposenavigator.navigator.LiveNavigator], which waits briefly for a live
 * navigator and then drops the navigation, a link must survive an arbitrary wait — a whole login flow,
 * if that is what stands between the tap and a screen that can show it.
 *
 * Observed by [DeepLinkRouter] for the targets reachable from the root stack, and directly by any
 * component whose target is reachable only through a navigator it owns (e.g. a bottom-tab switch);
 * such a component claims the link itself and calls [consume].
 *
 * [L] is the app's own link type; this module never inspects it, it only carries it. Keeping it opaque
 * is what lets the plumbing live here while the link vocabulary (and its handlers) stay in the app.
 *
 * Open so a consumer can pin the link type with a non-generic subclass — generics are erased in
 * service-locator keys, so a `DeepLinkBus<A>` and a `DeepLinkBus<B>` are the same DI key.
 */
open class DeepLinkBus<L : Any> {
    private val _pending = MutableStateFlow<L?>(null)
    val pending: StateFlow<L?> = _pending.asStateFlow()

    fun publish(link: L) {
        _pending.value = link
    }

    /** Clears [link] if it is still the pending one (a newer publish wins). */
    fun consume(link: L) {
        _pending.compareAndSet(link, null)
    }
}
