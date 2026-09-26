# Architecture

Design principles this library follows, and why.

## No DI framework dependency

No module depends on Koin, Hilt, Kodein, or any other DI library. `ScreenConfigRegistry` is built
from a plain DSL block (`ScreenConfigRegistry { screen(::MyComponent) }`), not resolved from a
container. The one seam that looks like DI at a glance and isn't is diagnostic logging: every class
that logs takes a `logging.Logger` constructor parameter defaulting to `NoopLogger` (silent), and
`ScreenConfigRegistry.Builder.logger` is a plain settable property — a consuming app wires its own
`Logger` implementation into whatever DI it already uses, this library never asks it to install one.
`Logger` is vendored inside this module rather than a dependency on the sibling `kmptoolkit-logging`
library — see that package's own KDoc for why (in short: no `jvm` target there yet).

## No hardcoded consumer-facing identifier

`StateKeeper` keys are derived from each `NavigationScreenConfig`'s own `typeId`/`id`, never a fixed
string a consumer might collide with. See the root config `id` note in
[`02-getting-started.md`](decomposenavigator-core/02-getting-started.md) for the one place this
requires a deliberate, fixed value from the consumer rather than the library's own default.

## No user-facing text

The library surfaces `ScreenConfig`s and typed navigator state, never a string meant for display.
Localization and copy are entirely the consuming app's job.

## Compose is a first-class dependency here, not an opt-in extra

Unlike a general-purpose toolkit where most modules stay plain Kotlin and only a few opt into
Compose, this library's whole premise is Decompose + Compose Multiplatform integration:
`RenderComponent.Render()` is `@Composable`, `LocalNavigator` is a `CompositionLocal`, and the stack
animation types come from `decompose-compose`. `decomposenavigator-core` depends on
`compose.runtime` / `compose.foundation` / `compose.ui` as `api`, not `implementation` — a consumer
referencing `LocalNavigator` needs those types on its own compile classpath, and a plain
`implementation` dependency would not provide that.

## Desktop is a default target, not a narrow exception

Every module publishes Android, iOS, and JVM (Desktop). The types this library publishes — a
`ScreenConfig`, the navigator tree, `LineNavigator` / `SwitchNavigator`, `LiveNavigator`, the
deep-link plumbing — are exactly the kind of type a consumer puts in code shared between phone and
desktop UI: a navigation stack has no reason to differ by platform. The whole module was already
`commonMain`-only with zero `expect`/`actual` when it was extracted, and both Decompose and Essenty
support the JVM target natively, so the target costs nothing to add and nothing to maintain.

## `explicitApi()` — not yet enabled

`decomposenavigator-core` was mechanically ported from a large existing codebase with no explicit
visibility or return-type annotations anywhere. Turning on Kotlin's strict explicit-API mode today
would fail the build on every public declaration at once, rather than catch a real API-boundary
mistake as intended. Adopting it — the annotation retrofit that requires — is tracked as a
pre-`1.0.0` task in `CHANGELOG.md`. ABI validation (`checkKotlinAbi` / `updateKotlinAbi`) is already
active regardless, since it dumps whatever is publicly visible under Kotlin's default rules.

## Public API and semver

- ABI validation runs on every build. A green `api/` dump diff is a record of what changed, not
  permission to change it however you like.
- Semver: patch = no public API change; minor = additive public API only; major = any breaking
  change. Before `1.0.0`, a breaking change in a minor bump is allowed but is called out explicitly
  under its own `Breaking` heading in `CHANGELOG.md`. After `1.0.0`, a breaking change requires a
  major version, full stop.

## Where this code came from

This library was extracted from [TahfeezAI](https://github.com/TahfeezAI-Company)'s internal
`core/decompose-navigator` module — a production Kotlin Multiplatform app (Android + iOS) with
nested tab navigation, deep links that must survive an entire login flow, and Activity recreation on
every rotation. Two pieces of code were pulled in from elsewhere in that same app because they were
already fully generic despite living outside the original module:

- `LineNavigator.pushCapped` — generalized from an app-local helper that capped one admin screen's
  stack depth; `SwitchNavigator`/`TabNavigation` already had the equivalent (`maxBackStackSize`), this
  is the same idea for a plain push-based stack.
- `event.ResultEvent<T>` — a per-tag, non-replaying result channel used to hand a value to a screen
  only once it has actually subscribed, pairing with `ReadinessAwaitable` and
  `SwitchNavigator.switchToFirstAndAwaitReady` for an "open a tab, then deliver it a result" flow.
