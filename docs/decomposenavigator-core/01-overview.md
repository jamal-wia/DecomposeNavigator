# Overview

`decomposenavigator-core` is a wrapper over [Decompose](https://github.com/arkivanov/Decompose) that
provides a declarative, serializable navigation layer on top of `ChildStack`, for Compose
Multiplatform (Android, iOS, JVM/Desktop).

## What it solves

- **Screen registration and serialization**: `screen(::MyComponent)` registers both the component
  factory and the `KSerializer` for its config in one call, instead of hand-writing both.
- **A navigator tree reachable from any screen**: `LocalNavigator` / `LocalLineNavigator` /
  `LocalSwitchNavigator` / `LocalEnclosingLineNavigator` Composition Locals, instead of threading a
  navigator through constructors or a DI container.
- **Stack, tab, and switch navigation containers**: `LineNavigation` (a classic back stack),
  `SwitchScreen` (screen switching with its own back stack), `TabNavigation` (multiple tabs with a
  customizable bar) — each with a corresponding controller created automatically.
- **Process-death-safe state**: the navigation stack (`StackStateSerializer`) and, separately, a
  small per-screen UI-state slice (`persistedSlice`) both survive Android process death via
  Decompose's `StateKeeper`.
- **Recreation-safe deferred navigation**: `LiveNavigator` lets a screen component navigate from a
  coroutine — after a network call, a `delay`, any suspension point — without risking a call on a
  navigator already released by an Activity recreation.
- **Deep links that never drop an early tap**: `DeepLinkBus` holds the latest link as unbounded
  state; a link tapped before login or during a cold boot is applied once `DeepLinkRouter.route`'s
  `awaitReady` says the app is ready, not discarded.
- **Custom animations that survive Decompose's per-child caching**: `SlideAnimationMarker` and
  `CoveringScreen` update direction/behavior via mutable state instead of replacing the cached
  `StackAnimation` object.

## What this is not

- **Not a routing library for URLs.** There is no URL parser and no route-string DSL. A
  `ScreenConfig` is a typed Kotlin value, not a path template; if you need URL-based routing, build
  it as a thin layer that maps a URL to a `ScreenConfig` and pushes it — `deeplink/` is exactly that
  seam, generic over your own link type.
- **Not a state-management library.** `Navigator` methods change *which* screen is shown; they are
  not a general application-state container. `persistedSlice` deliberately persists only a small
  serializable slice of one screen's own state, never a whole app state tree.
- **Not a replacement for Decompose.** This library assumes and requires Decompose underneath —
  `ComponentContext`, `ChildStack`, `StateKeeper` are all Decompose's own types, exposed rather than
  hidden, so you can reach for Decompose directly wherever this layer doesn't cover something.
- **Not tied to any particular DI framework, backend, or app shape.** Nothing here depends on Koin,
  Hilt, or any specific networking/auth stack — see [`01-architecture.md`](../01-architecture.md).

If what you need doesn't fit here, `docs/README.md` has no other module to redirect you to yet —
open an issue describing the gap.
