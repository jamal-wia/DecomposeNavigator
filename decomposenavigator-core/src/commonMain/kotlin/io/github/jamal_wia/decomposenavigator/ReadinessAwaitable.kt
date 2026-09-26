package io.github.jamal_wia.decomposenavigator

/**
 * A [RenderComponent] whose readiness is reached asynchronously (e.g. it sets up flow subscriptions
 * in a launched coroutine) and that can report when it has become ready.
 *
 * It lets a caller act on the component deterministically instead of racing its setup. In
 * particular, `SwitchNavigator.switchToFirstAndAwaitReady` returns only once the opened screen is
 * ready, so a follow-up action — such as emitting an event the screen must already be subscribed to
 * — is guaranteed to be observed.
 */
interface ReadinessAwaitable {

    /** Suspends until the component is ready; returns immediately if it already is. */
    suspend fun awaitReady()
}
