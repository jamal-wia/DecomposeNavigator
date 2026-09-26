package io.github.jamal_wia.decomposenavigator.navigator

import io.github.jamal_wia.decomposenavigator.navigator.base.Navigator
import io.github.jamal_wia.decomposenavigator.logging.Logger
import io.github.jamal_wia.decomposenavigator.logging.NoopLogger
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainCoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * Recreation-safe holder for the navigator of a screen's Composition.
 *
 * ## The problem it solves
 *
 * Decompose screen components are **retained** across Activity recreation (rotation, theme/locale
 * change, multi-window resize), but the navigator is created in — and `release()`d with — the
 * **Composition**. A component that caches the navigator and calls it later (after a network call,
 * a delay, or any suspension point) can end up invoking the *released* navigator of the previous
 * Composition, which throws `IllegalStateException: <Navigator> is released`.
 *
 * ## How it works
 *
 * [publish] is called from `Render()` on every recomposition, so after a recreation the released
 * navigator is immediately overwritten with the fresh, live one. [navigate]/[navigateAwaiting] then
 * wait (briefly) for a non-released navigator and invoke it **on the main thread**. Because
 * `release()` also runs on the main thread (during Composition disposal), the non-released check and
 * the navigation call run back-to-back on the same thread with no suspension between them — so a
 * release cannot interleave: the navigator that passed the check is still live when it is called.
 * If no live navigator appears within [waitTimeout], the navigation is dropped instead of crashing.
 *
 * Pass [main] as the app's main dispatcher; when it is a [MainCoroutineDispatcher] its `immediate`
 * variant is used, so navigation triggered while already on the main thread runs inline (no extra
 * frame hop) for the common case where a live navigator is already published.
 *
 * @param scope the owning component's lifecycle-scoped [CoroutineScope]; [navigate] launches on it.
 */
class LiveNavigator<N : Navigator<*>>(
    private val scope: CoroutineScope,
    main: CoroutineDispatcher,
    private val waitTimeout: Duration = DEFAULT_WAIT_TIMEOUT,
    private val logger: Logger = NoopLogger,
) {

    private val mainImmediate: CoroutineDispatcher =
        (main as? MainCoroutineDispatcher)?.immediate ?: main

    private val navigatorHolderState = MutableStateFlow<N?>(null)

    /**
     * The navigator currently bound to the live Composition, or `null`. Read this only for
     * synchronous, non-mutating queries; perform actual navigation through [navigate].
     */
    val value: N? get() = navigatorHolderState.value

    /** Publishes the Composition's current navigator. Call from `Render()` every recomposition. */
    fun publish(navigator: N?) {
        navigatorHolderState.value = navigator
    }

    /**
     * Fire-and-forget navigation: runs [action] against a live (non-released) navigator on the main
     * thread, launching on the component scope. Use this for the vast majority of call sites,
     * including those previously written as a synchronous `navigator?.push(...)`.
     */
    fun navigate(action: (N) -> Unit) {
        scope.launch(mainImmediate) { runWhenLive(action) }
    }

    /**
     * Suspending variant for callers already inside a coroutine that need to run a **suspend**
     * action on the navigator (e.g. `SwitchNavigator.switchToFirstAndAwaitReady`) and/or sequence
     * work after it completes.
     */
    suspend fun navigateAwaiting(action: suspend (N) -> Unit) {
        withContext(mainImmediate) { runWhenLive(action) }
    }

    private suspend fun runWhenLive(action: suspend (N) -> Unit) {
        val ran: Unit? = withTimeoutOrNull(waitTimeout) {
            val activeNavigator: N = navigatorHolderState.first { it != null && !it.isReleased }!!
            action(activeNavigator)
        }
        if (ran == null) {
            logger.d { "LiveNavigator: dropped navigation — no live navigator within $waitTimeout" }
        }
    }

    companion object {
        val DEFAULT_WAIT_TIMEOUT: Duration = 5.seconds
    }
}
