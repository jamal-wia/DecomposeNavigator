# Changelog

All notable changes to this project are documented here. Format loosely follows
[Keep a Changelog](https://keepachangelog.com/en/1.1.0/).

## [1.0.0] - 2026-09-26

### Added

- Initial extraction of `decomposenavigator-core` from TahfeezAI's internal
  `core/decompose-navigator` module: `ScreenConfigRegistry`, the `Navigator` tree, `LineNavigator` /
  `SwitchNavigator`, all four navigation controllers, deep-link plumbing (`DeepLinkBus` /
  `DeepLinkRouter` / `DeepLinkHandler` / `DeepLinkNavigator`), `LiveNavigator`, stack animations
  (including `CoveringScreen` / `SlideAnimationMarker`), `StackStateSerializer`, and `PersistedSlice`.
- `decomposenavigator-testing`: real, in-memory `FakeLineNavigator`, `FakeSwitchNavigator`,
  `FakeDeepLinkNavigator`, and `RecordingNavigator` test doubles.
- `decomposenavigator-bom`.
- `LineNavigator.pushCapped(config, maxDepth)` — extracted and generalized from an
  admin-app-local `pushCapped` helper in TahfeezAI that only worked for its own hardcoded depth.
  `SwitchNavigator`/`TabNavigation` already had an equivalent cap (`maxBackStackSize`); this is the
  same idea for a plain push-based stack.
- `event.ResultEvent<T>` — extracted from TahfeezAI's `core/result_event` (there, entirely
  app-agnostic already: a per-tag, non-replaying result channel used to hand a value to a screen only
  once it has actually subscribed, pairing with `ReadinessAwaitable` /
  `SwitchNavigator.switchToFirstAndAwaitReady` for the "open a tab, then deliver it a result" flow).
- Desktop (`jvm()`) target on every module, in addition to Android and iOS — the ported codebase was
  already 100% `commonMain` with zero `expect`/`actual`, and Decompose/Essenty both support the JVM
  target natively.

### Changed

- Replaced the internal `com.app.tahfeez.core.logger.console.ConsoleLoggable` mixin (TahfeezAI-only,
  could not ship in a public library) with a small vendored `Logger`/`NoopLogger` seam
  (`io.github.jamal_wia.decomposenavigator.logging`), shaped to match the sibling library
  `kmptoolkit-logging`'s own `Logger` type so a future switch is a mechanical rename. It is vendored
  rather than an actual dependency because `kmptoolkit-logging` does not yet publish a `jvm` target
  and this module targets JVM on every artifact — see that package's KDoc and
  `docs/01-architecture.md`. Every affected class now takes a `logger: Logger = NoopLogger`
  constructor parameter (or, for `ScreenConfigRegistry`, a `Builder.logger` property) instead of
  mixing in a logging interface — silent by default, opt-in for an app that wants visibility.
  `StackStateSerializer`'s error-path logging is threaded through
  `LineNavigationComponentController` / `SwitchNavigationComponentController`'s own `logger`
  parameter rather than defaulting silently on its own.

### Known gaps (tracked, not silent)

- **The logging seam is vendored, not a real dependency on `kmptoolkit-logging`.** Discovered while
  wiring up the JVM (Desktop) target: `kmptoolkit-logging:1.8.0` has no `jvm` variant, so depending
  on it broke every JVM compile in this module. If `kmptoolkit-logging` adds a `jvm` target later,
  delete `io.github.jamal_wia.decomposenavigator.logging` and switch the (identically-shaped) imports
  — a mechanical change, not a redesign.
- **`explicitApi()` is not yet enabled**, and `1.0.0` ships without it. `decomposenavigator-core` was
  ported from a codebase with no explicit visibility/return-type annotations; turning on strict mode
  today would fail on every public declaration at once. Adopting it is still tracked as follow-up
  work, not abandoned — writing out the modifiers a symbol already has by default is not itself an
  ABI break, so it can land in a `1.x` release rather than forcing a `2.0.0`.
- **No `:sample` app yet** (KMPToolkit and Paginator both ship one). `settings.gradle.kts` has the
  module commented out rather than a broken reference.
- The original module's README documents the `deeplink/` subsystem in detail but its own "Threading
  and limitations" section still claimed no deep-linking support — a leftover from before that
  subsystem was added. Fixed in this repository's own README; worth the same fix back in TahfeezAI.
