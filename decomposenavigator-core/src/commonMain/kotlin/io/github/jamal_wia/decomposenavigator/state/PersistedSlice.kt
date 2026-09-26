package io.github.jamal_wia.decomposenavigator.state

import com.arkivanov.decompose.ComponentContext
import kotlinx.serialization.KSerializer

/**
 * Registers a small serialized slice of this component's own UI state with its
 * [com.arkivanov.essenty.statekeeper.StateKeeper], so it **survives process death** (and
 * "Don't keep activities" / low-memory background kills) on Android, and returns whatever was
 * restored (or `null` on first launch, or if restoring failed).
 *
 * This is the screen-level counterpart of the navigation-stack persistence already done in
 * `SwitchNavigationComponentController` / `StackStateSerializer`: same mechanism
 * ([com.arkivanov.essenty.statekeeper.StateKeeper.consume] at construction,
 * [com.arkivanov.essenty.statekeeper.StateKeeper.register] for save).
 *
 * It is **distinct** from Decompose `retainedComponent` / `InstanceKeeper`, which retain live
 * instances across a *configuration change* but are gone after *process death*. Because the whole
 * component tree is already retained across config changes, the only thing this helper adds is
 * surviving process death — so reach for it only for state that is worth restoring after the OS
 * has killed and restored the app.
 *
 * Platform note: on iOS the root component is built without a SavedState-backed StateKeeper, so
 * `consume` returns `null` and `register` is a harmless no-op — this helper is **inert (safe) on
 * iOS** and behaves exactly like not calling it at all.
 *
 * ### Single source of truth — no second value to keep in sync
 *
 * Unlike a naive "parallel `MutableValue` the caller must remember to update on every change"
 * design, [snapshot] is a lazy projection: it is invoked only when the StateKeeper actually
 * serializes (Android `onSaveInstanceState`, or a test's explicit save/reload), and it should read
 * straight from the caller's OWN state (typically `_state.value`). There is nothing else to
 * update — a screen wires this up once at construction and every subsequent state change is
 * automatically reflected the next time the OS saves, because [snapshot] always re-reads the
 * live value rather than a stale copy. This removes an entire class of bugs where a new setter (or
 * a new field on an existing one) forgets to also write the persisted copy.
 *
 * Usage rules (keep what is persisted small and safe):
 * - Persist a dedicated, small `@Serializable` slice — user input or chosen view state
 *   (selected tab, query, toggles, small selections). Never the full screen state, never fetched
 *   lists/maps (they reload from their source; the saved-state Bundle has a ~1 MB limit), never
 *   secrets (passwords, OTP/reset codes, tokens — the Bundle is written to disk).
 * - [key] must be unique within the component. Registering the same key twice throws; a screen
 *   that needs two values uses two keys. A plain constant string is safe across instances because
 *   each component owns its own StateKeeper subtree — do not add instance/id suffixes.
 * - [snapshot] must always return a valid, non-null slice — including before the screen's real
 *   content has loaded (e.g. while `_state` is still `Loading`). Represent "nothing to persist
 *   yet" with a slice whose fields default to their initial/empty values, not by throwing.
 *
 * A failed restore (e.g. an incompatible slice schema written by a previous app version) returns
 * `null`, exactly like "nothing was ever saved", instead of crashing the screen on cold start —
 * mirroring the `runCatching` fallback discipline in `StackStateSerializer`.
 *
 * @param key stable, component-unique key used by the StateKeeper.
 * @param serializer serializer for the persisted slice; used both to save and to restore.
 * @param snapshot lazily produces the CURRENT slice to persist, read from the caller's own state.
 *   Invoked only at real save time, never eagerly and never on a timer.
 * @return the restored slice, or `null` when nothing was saved yet or restoring failed.
 */
fun <S : Any> ComponentContext.persistedSlice(
    key: String,
    serializer: KSerializer<S>,
    snapshot: () -> S,
): S? {
    val restored: S? = runCatching {
        stateKeeper.consume(key = key, strategy = serializer)
    }.getOrNull()
    stateKeeper.register(key = key, strategy = serializer, supplier = snapshot)
    return restored
}
