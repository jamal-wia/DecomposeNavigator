# API reference

Grouped by package. Method-level contracts are in [`03-guide.md`](03-guide.md); this is the index.

## `io.github.jamal_wia.decomposenavigator`

| Symbol | Kind | Purpose |
|---|---|---|
| `RenderComponent` | interface | `@Composable fun Render()` — every screen and container implements this |
| `ScreenConfigRegistry` | class | DSL screen registrar; `createComponent(config, ctx)` is the entry point |
| `StackStateSerializer` | internal object | Stack save/restore via `Json`, with error logging |
| `NavigationAnimations.kt` | functions | `fadeStackAnimation`, `slideStackAnimation`, `pushStackAnimation`, `popStackAnimation`, `emptyStackAnimation`, `CoveringScreen`, `SlideAnimationMarker` |
| `ReadinessAwaitable` | interface | `suspend fun awaitReady()` — a component whose setup finishes asynchronously |
| `ReturnsToFirstScreen` | interface | `fun returnToFirstScreen()` — walked down the tree by `LineNavigationComponentController` |
| `randomId` / `emptyStackAnimation()` | functions | `Util.kt` — id generation, a singleton no-op animation |

## `io.github.jamal_wia.decomposenavigator.config`

| Symbol | Kind | Purpose |
|---|---|---|
| `ScreenConfig` | `@Polymorphic` interface | Marker for an individual screen's config |
| `NavigationScreenConfig` | abstract class | Base for a navigation container's config: `LineNavigation`, `SwitchScreen`, `SwitchScreenConfigContainer`, `TabNavigation` |

## `io.github.jamal_wia.decomposenavigator.controller`

| Symbol | Kind | Purpose |
|---|---|---|
| `base.NavigationComponentController<T>` | abstract class | Base for all controllers; `hostConfig`, `activeScreenConfig`, `navigate()` |
| `LineNavigationComponentController` | class | Stack navigation controller |
| `SwitchNavigationComponentController` | class | Screen-switching controller, own back stack |
| `SwitchContainerComponentController` | class | Transparent wrapper over one child config |
| `TabNavigationComponentController` | class | Tab container with a customizable tab bar; `innerSwitch` |

## `io.github.jamal_wia.decomposenavigator.navigator`

| Symbol | Kind | Purpose |
|---|---|---|
| `base.Navigator<T>` | abstract class | Parent/child tree, shared lifecycle boilerplate, `logger` seam |
| `base.NavigateHandler` | interface | `navigate(transformer, onComplete)` |
| `base.PopHandler` | interface | `canPop` / `pop` / `popToRoot` / `applyFallbackPop` |
| `impl.LineNavigator` | abstract class | `push`, `replace`, `replaceAll`, `popToFirst`, `bringToFront`, `popTo`, `pushCapped` (extension) |
| `impl.SwitchNavigator` | abstract class | `switchTo`, `switchToFirst`, `switchToFirstAndAwaitReady` |
| `LiveNavigator<N>` | class | Recreation-safe navigator holder: `publish`, `navigate`, `navigateAwaiting` |
| `util.rememberNavigator` | `@Composable` function | Navigator factory with lifecycle binding |
| `base.LocalNavigator` / `LocalLineNavigator` / `LocalSwitchNavigator` / `LocalEnclosingLineNavigator` / `LocalOutermostLineNavigator` | `CompositionLocal` | See [`03-guide.md`](03-guide.md#composition-locals) |
| `base.requireLineNavigator()` / `requireSwitchNavigator()` | `@Composable` functions | Throwing variants of the locals above |

## `io.github.jamal_wia.decomposenavigator.deeplink`

| Symbol | Kind | Purpose |
|---|---|---|
| `DeepLinkBus<L>` | open class | Holds the pending link as a `StateFlow`; `publish`/`consume` |
| `DeepLinkRouter<L>` | open class | `route(navigator, awaitReady)` |
| `DeepLinkHandler<L>` | interface | `canHandle(link)` / `handle(link, navigator)` |
| `DeepLinkNavigator` | interface | `activeScreenConfig`, `showScreen`, `returnToFirstScreen` |
| `DecomposeDeepLinkNavigator` | class | `DeepLinkNavigator` backed by `LineNavigationComponentController` |

## `io.github.jamal_wia.decomposenavigator.event`

| Symbol | Kind | Purpose |
|---|---|---|
| `ResultEvent<T>` | abstract class | Per-tag, non-replaying result channel — `flow`, `emit`, `tryEmit` |

## `io.github.jamal_wia.decomposenavigator.state`

| Symbol | Kind | Purpose |
|---|---|---|
| `persistedSlice` | `ComponentContext` extension | Persists a small `@Serializable` UI-state slice across process death |
