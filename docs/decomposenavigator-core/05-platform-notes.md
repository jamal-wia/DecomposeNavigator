# Platform notes

## Android

- `LineNavigationComponentController`'s stack, `SwitchNavigationComponentController`'s back stack,
  and any `persistedSlice` all survive **process death** through Decompose's `StateKeeper`, which
  Android backs with `onSaveInstanceState`'s Bundle — subject to that Bundle's roughly 1&nbsp;MB
  limit (see [`03-guide.md`](03-guide.md#screen-level-state-persistedslice)).
- A plain configuration change (rotation, theme, locale, multi-window resize) is a different
  mechanism: if the host app retains its root component (e.g. Decompose's `retainedComponent` in
  `MainActivity`), the whole component tree — and with it every `Navigator` — survives without
  needing `StateKeeper` at all. `LiveNavigator` exists for the gap this still leaves: the
  **Composition** (and the navigator that lives with it) is *not* retained the same way, so a
  screen component holding a stale navigator reference across that recreation can call a released
  one.

## iOS

- No `StateKeeper`-backed process-death restoration path exists the way Android's does — the app
  process starting fresh is the iOS equivalent, and `DefaultComponentContext(LifecycleRegistry())`
  is constructed anew. `persistedSlice` is an inert no-op there (see its KDoc) since the root has no
  SavedState-backed `StateKeeper`.

## Desktop (JVM)

- No `StateKeeper`-backed persistence path either — a desktop process restart is a fresh process,
  same as iOS. If a desktop app needs cross-restart persistence, build it outside this library (e.g.
  your own settings/store) and reconstruct the initial `NavigationScreenConfig` from it at startup.
- No platform-specific code exists in this module at all — `decomposenavigator-core` is 100%
  `commonMain` with zero `expect`/`actual` — so the JVM target compiles the identical logic Android
  and iOS run. This is also why it costs nothing to keep publishing.

## Threading — every platform

All `Navigator` methods (`push`, `pop`, `switchTo`, etc.) must be called from the main thread.
`pendingActions` and the rest of `Navigator`'s internal state are not thread-safe by design, matching
how Compose and Decompose themselves operate. Code that navigates from a background dispatcher or
after a suspension point should go through `LiveNavigator`, which marshals onto the main dispatcher
it was constructed with.
