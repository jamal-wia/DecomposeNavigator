package io.github.jamal_wia.decomposenavigator.deeplink

/**
 * Routes one kind of link to its screen. Each feature contributes its own handler — registered in that
 * feature's DI module and handed to [DeepLinkRouter] — so adding a new deep-link target means adding a
 * handler in the owning feature, never editing the router.
 *
 * The router only calls [handle] once the app reports itself ready (see [DeepLinkRouter.route]), and
 * consumes the link afterwards; a handler just performs the navigation.
 */
interface DeepLinkHandler<L : Any> {

    /** True if this handler owns [link]. At most one handler should claim a given link. */
    fun canHandle(link: L): Boolean

    /**
     * Navigates [navigator] to [link]'s target. Invoked on the main dispatcher.
     *
     * **Cancellable at every suspension point.** A newer link supersedes this one, and the router
     * cancels this call when that happens — a multi-step handler can be stopped half-way through. Keep
     * it idempotent and avoid leaving the stack in a state a partial run would break; do the
     * navigation in one step where possible, and never treat "handle returned" as guaranteed.
     */
    suspend fun handle(link: L, navigator: DeepLinkNavigator)
}
