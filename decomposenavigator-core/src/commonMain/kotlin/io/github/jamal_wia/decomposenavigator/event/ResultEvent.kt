package io.github.jamal_wia.decomposenavigator.event

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onSubscription
import kotlin.random.Random

/**
 * A per-tag, non-replaying result channel — the piece that completes the "open a screen, then hand
 * it a result" pattern [io.github.jamal_wia.decomposenavigator.ReadinessAwaitable] and
 * `SwitchNavigator.switchToFirstAndAwaitReady` only get halfway to: those get you to a screen that
 * has finished its own setup, but say nothing about *delivering a value* to it once it is there. A
 * cross-tab "request", answered by whichever tab is switched to and ready, is exactly the shape this
 * solves: the producer must not emit before the target has actually subscribed, or the value (this
 * class never replays) is silently lost.
 *
 * [tagObserver] identifies *which* subscriber a value is for — typically the requesting screen's own
 * identity (a config, a component instance) — so several independent screens can each observe their
 * own slice of one [ResultEvent] without seeing each other's values.
 */
abstract class ResultEvent<T>(
    val id: Long = Random.nextLong()
) {

    private val flows = hashMapOf<Any, MutableSharedFlow<T>>()

    /**
     * Stream of values for [tagObserver]. Does not replay: a value emitted while no one is
     * collecting is dropped, so the collector must be subscribed before the producer emits.
     *
     * [onSubscribed], if given, is invoked once this collector's subscription is actually
     * registered (via [onSubscription]) — before any value is delivered. Callers that must emit
     * only after a consumer is guaranteed to receive it can use this to await registration and
     * thereby avoid an emit-before-subscribe race — e.g. awaiting it right after
     * `switchToFirstAndAwaitReady` returns, before emitting the value that screen is waiting for.
     */
    fun flow(tagObserver: Any, onSubscribed: (() -> Unit)? = null): Flow<T> {
        val shared: SharedFlow<T> = flows.getOrPut(tagObserver) { MutableSharedFlow() }.asSharedFlow()
        val source: SharedFlow<T> =
            if (onSubscribed == null) shared else shared.onSubscription { onSubscribed() }
        return source.onCompletion { flows.remove(tagObserver) }
    }

    open suspend fun emit(value: T) {
        flows.keys.toList()
            .forEach { emit(it, value) }
    }

    open suspend fun emit(targetTagObserver: Any?, value: T) {
        if (targetTagObserver == null) return emit(value)
        flows[targetTagObserver]?.emit(value)
    }

    open suspend fun emit(excludeTagObservers: Set<Any?>, value: T) {
        flows.keys.toList()
            .filterNot { excludeTagObservers.contains(it) }
            .forEach { emit(it, value) }
    }

    open fun tryEmit(value: T): Boolean {
        var success = false
        flows.keys.toList()
            .forEach {
                if (tryEmit(it, value)) success = true
            }
        return success
    }

    open fun tryEmit(targetTagObserver: Any?, value: T): Boolean {
        if (targetTagObserver == null) return tryEmit(value)
        return flows[targetTagObserver]?.tryEmit(value) ?: false
    }

    open fun tryEmit(excludeTagObservers: Set<Any?>, value: T): Boolean {
        var success = false
        flows.keys.toList()
            .filterNot { excludeTagObservers.contains(it) }
            .forEach {
                if (tryEmit(it, value)) success = true
            }
        return success
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        if (other !is ResultEvent<*>) return false
        return this.id == other.id
    }
}
