# Guide

## `ScreenConfigRegistry`

The central configuration point, built once via DSL:

```kotlin
val ScreenConfigRegistryHolder = ScreenConfigRegistry {
    // Diagnostic logging (optional, silent by default) — see 05-platform-notes.md.
    logger = myAppLogger

    // Additional SerializersModules, for non-ScreenConfig types embedded in a config.
    serializerModule {
        polymorphic(TabBarMapper::class) {
            subclass(MainTabsMapper::class, MainTabsMapper.serializer())
        }
    }

    // A custom controller for a NavigationScreenConfig.
    navigationController<TabBarNavigationScreenConfig> { config, ctx, childFactory, json ->
        TabBarNavigationComponentController(config, ctx, childFactory, json)
    }

    // Screen registration (factory + serializer, both from one constructor reference).
    screen(::SplashScreenComponent)
    screen(::HomeScreenComponent)
}
```

**Controller selection**, in `createComponent`:

1. A custom controller registered for this `NavigationScreenConfig` type, if any.
2. Otherwise, a built-in controller (`LineNavigationComponentController`,
   `SwitchNavigationComponentController`, `SwitchContainerComponentController`,
   `TabNavigationComponentController`).
3. For a plain `ScreenConfig`, the registered factory — `IllegalArgumentException` if none matches.

| Builder method | Purpose |
|---|---|
| `screen(::Component)` | Registers factory + `KSerializer` via `reified` type |
| `navigationController<T> { ... }` | Custom controller + `KSerializer` for a `NavigationScreenConfig` |
| `serializerModule { ... }` | Adds a `SerializersModule` (for third-party types embedded in a config) |
| `register<T>(factory)` | Factory only, no auto-serializer |
| `registerSerializer<T>(serializer)` | Serializer only |
| `registerWithSerializer(serializer, factory)` | Factory + serializer explicitly |
| `registerNavigationController<T>(factory)` | Custom controller only, no serializer |
| `registerNavigationControllerWithSerializer(...)` | Custom controller + serializer explicitly |
| `logger` (property) | `kmptoolkit.logging.Logger` passed to every built-in controller this registry creates |

## `NavigationScreenConfig`

An abstract base for configs describing a **navigation container**, not an individual screen. Each
subclass carries `typeId: String` and `id: Long` for stable identification across recreation.

| Subclass | Purpose |
|---|---|
| `LineNavigation(initialConfigs)` | A classic back stack. |
| `SwitchScreen(initialConfig)` | Screen switching, with its own back stack. |
| `SwitchScreenConfigContainer(config)` | A transparent wrapper — a "slot" for a tab. |
| `TabNavigation(initialConfig, tabs)` | Multiple tabs with a customizable tab bar. |

## The navigator tree

`Navigator<T>` forms a parent/child tree. `LineNavigationComponentController.Render()` calls
`rememberNavigator(...)`, which creates a `LineNavigator` and adds it as a child of
`LocalNavigator.current`:

```
RootLineNavigator (app root)
    └── SwitchNavigator (tab bar)
            ├── LineNavigator (tab A)
            └── LineNavigator (tab B)
                    └── LineNavigator (nested stack inside tab B)
```

Tree traversal, all defined on `Navigator`:

| Method | Description |
|---|---|
| `findAncestorBy(predicate)` | Search upward |
| `findGrandParentBy(predicate)` | The grandparent, if it matches |
| `findSiblingBy(predicate)` | Search among siblings |
| `findCousinBy(predicate)` | Children of the parent's siblings |
| `findDescendantBy(predicate)` | BFS downward |

## Navigation controllers

**`LineNavigationComponentController`** — a classic back stack, created for `LineNavigation`.

| Method | Description |
|---|---|
| `push(config)` | Adds a screen to the top |
| `pushNew(config)` | Adds only if it doesn't equal the current top |
| `pop()` | Removes the top screen |
| `replaceCurrent(config)` | Replaces the top screen |
| `replaceAll(config)` | Clears the stack, leaving only `config` |
| `popToFirst()` | Returns to the first screen |
| `bringToFront(config)` | Moves an existing screen to the top |
| `popTo(predicate)` | Returns to the first matching screen |
| `canPop()` | Whether there is anything to pop |

State is preserved via `StackStateSerializer` and Decompose's `StateKeeper` (survives Android process
death).

**`SwitchNavigationComponentController`** — screen switching, `ChildStack` always holding exactly one
`SwitchScreenConfigContainer`. Maintains its own `backStack` separate from Decompose, persisted via
`StateKeeper`, with `popBackStack()`/`popBackStackToRoot()` for atomic manipulation. `navigate()`
validates every config the transformer returns is a `SwitchScreenConfigContainer`.

**`SwitchContainerComponentController`** — a transparent wrapper; delegates `navigate()` if the child
is itself a `NavigationComponentController`. Used inside `TabNavigation`/`SwitchScreen` as a slot.

**`TabNavigationComponentController`** — renders content and delegates the tab bar to a
`tabBarContent` composable slot; `null` renders no bar. Internally creates a
`SwitchNavigationComponentController`, exposed as `innerSwitch` for custom controllers that wrap it.

## `LineNavigator`

Obtained via `LocalLineNavigator` or `LocalNavigator.current as? LineNavigator`.

| Method | Description |
|---|---|
| `push(config, animation?)` | Push with optional animation |
| `pushNew(config, animation?)` | Push only if it doesn't equal the current top |
| `replace(config, animation?)` | Replace the current screen |
| `replaceAll(config, animation?)` | Reset the stack to a single screen |
| `popToFirst(animation?)` | Return to the first screen |
| `bringToFront(config, animation?)` | Move to the top |
| `popTo(predicate)` | Pop back to the first matching screen |
| `withAnimation(animation?)` / `withoutAnimation()` | Set/clear the animation without navigating |
| `pop()` | Go back, or `fallbackPop` if the stack is empty |
| `canPop()` / `popToRoot()` | Query / return to the first screen |
| `LineNavigator.pushCapped(config, maxDepth)` | Push, or replace the top once `stackSize() >= maxDepth` — a bounded-depth stack without hand-rolling the check |

**Pending actions**: `push()` called before `bind()` is queued and runs immediately after `bind()`.
**Atomic pop**: `pop()` uses `navigate()` internally to read-and-modify the stack atomically.

## `SwitchNavigator`

Obtained via `LocalSwitchNavigator` or `LocalNavigator.current as? SwitchNavigator`.

| Method | Description |
|---|---|
| `switchTo(container)` | Switch to the given container |
| `switchToFirst(createIfAbsent, predicate)` | Switch to the first matching container in the back stack, or one `createIfAbsent` supplies |
| `switchToFirstAndAwaitReady(...)` | Suspend variant that also awaits the opened component's `ReadinessAwaitable`, if it implements one |
| `pop()` / `popToRoot()` | Delegate to the controller's atomic back-stack manipulation |

## Deep links

A deep link arrives from somewhere with no navigator — a notification tap, a URL open — often
*before* the app can act on it. `deeplink/` is the hand-off: the platform callback publishes,
navigation happens once the app says it's ready. The link type is the app's own — everything here is
generic over `L : Any`; the module carries links, it never inspects them.

| Type | Role |
|---|---|
| `DeepLinkBus<L>` | Holds the pending link as a `StateFlow`. `publish`/`consume`. Never dropped for arriving early. |
| `DeepLinkRouter<L>` | Collects the bus, hands each link to the handler that claims it, once ready. |
| `DeepLinkHandler<L>` | One feature's "I own this link" plus the navigation it performs. |
| `DeepLinkNavigator` | The slice of root navigation a handler needs: read the active config, push a new one. |
| `DecomposeDeepLinkNavigator` | `DeepLinkNavigator` backed by `LineNavigationComponentController`. |

**One link at a time**: the bus holds only the latest; the router collects with `collectLatest`, so a
newer tap supersedes whatever the previous one was still waiting on — a handler must be
cancellation-safe.

**Readiness is the caller's**: `route(navigator, awaitReady)` takes a suspend function returning once
the app can be navigated. This library knows nothing about auth or which screens count as "still
booting" — the app shell owns that and simply suspends.

**Nothing takes routing down**: a throw from `awaitReady` or a handler is logged and the link stays
pending, rather than escaping the collector.

```kotlin
// App-side, once: non-generic subtypes pinned to this app's link type (generics are erased in
// service-locator keys, so a bare DeepLinkBus<A> and DeepLinkBus<B> collide as the same DI key).
class AppDeepLinkBus : DeepLinkBus<DeepLink>()
interface AppDeepLinkHandler : DeepLinkHandler<DeepLink>
class AppDeepLinkRouter(bus: AppDeepLinkBus, handlers: List<AppDeepLinkHandler>) :
    DeepLinkRouter<DeepLink>(bus, handlers)

// Platform callback — no navigator needed.
deepLinkBus.publish(link)

// App shell — runs for the app's lifetime.
deepLinkRouter.route(
    navigator = DecomposeDeepLinkNavigator(rootController),
    awaitReady = {
        while (!authTokenStorage.isLoggedIn() || rootController.activeScreenConfig is SplashScreenConfig) {
            delay(POLL_MS)
        }
    },
)
```

## Result delivery: `event.ResultEvent<T>`

`ReadinessAwaitable`/`switchToFirstAndAwaitReady` get you to a screen that has finished its own
setup; they say nothing about delivering it a *value* once it's there. `ResultEvent<T>` is a per-tag,
non-replaying result channel for exactly that: a producer must not emit before the target has
actually subscribed, or the value is silently lost (it never replays).

```kotlin
object RequestResult : ResultEvent<PickedItem>()

// Consumer screen, once ready:
LaunchedEffect(Unit) {
    RequestResult.flow(tagObserver = screenConfig).collect { item -> /* ... */ }
}

// Producer, after switching to that tab and awaiting its readiness:
switchNavigator.switchToFirstAndAwaitReady(predicate = { it.config == PickerScreenConfig })
RequestResult.emit(targetTagObserver = PickerScreenConfig, value = pickedItem)
```

## Animations

All animation functions are generic (`<C : Any, T : Any>`).

```kotlin
fadeStackAnimation<ScreenConfig, RenderComponent>()               // crossfade — tabs/switch
slideStackAnimation<ScreenConfig, RenderComponent>(SlideDirection.FORWARD)
pushStackAnimation<ScreenConfig, RenderComponent>()                // fixed slide forward
popStackAnimation<ScreenConfig, RenderComponent>()                 // fixed slide backward
emptyStackAnimation<ScreenConfig, RenderComponent>()               // no animation (singleton)
```

**`CoveringScreen`** — the standard slide fades both screens, which flashes the container's
background between two screens drawn over a photo. Marking a config with `CoveringScreen` makes the
navigator's standard animation let it rise from the bottom (and sink back on pop) while the screen
beneath holds still, fully drawn:

```kotlin
@Serializable
data class CityPickerScreenConfig(val leavesTheApp: Boolean = false) : ScreenConfig, CoveringScreen

navigator.navigate { it.push(CityPickerScreenConfig()) } // rises; the screen beneath stays put
```

**`SlideAnimationMarker`** solves Decompose's animation-object caching: `slideStackAnimation(direction)`
returns a marker that is never rendered — `LineNavigator.applyAnimation()` reads it and updates a
`MutableState<SlideDirection>` in place, so the underlying `StackAnimation` object never changes and
Decompose's per-child cache stays valid.

```kotlin
// Correct
navigator.replace(SurahScreenConfig(5), slideStackAnimation(SlideDirection.FORWARD))

// Wrong — replaces the animation object, breaking Decompose's cache
navigator.withAnimation(pushStackAnimation())
navigator.replace(SurahScreenConfig(5))
```

## State serialization

`StackStateSerializer` saves/restores the stack via `Json`. Errors are logged (via the `logger`
passed through the owning controller) rather than silently swallowed; on failure, a fallback keeps
the app running. Type discriminator in JSON: `"type"`.

### Screen-level state: `persistedSlice`

The stack is one thing; a screen's own small UI-state slice is another. The `ComponentContext`
extension `persistedSlice` uses the same `StateKeeper` mechanism to survive process death:

```kotlin
private val restored: SavedSignIn? = persistedSlice(
    key = "sign_in_saved",
    serializer = SavedSignIn.serializer(),
) {
    // Lazily re-reads current state, only at real save time.
    (_state.value as? SignInScreenState.Content)
        ?.let { SavedSignIn(email = it.email, passwordVisible = it.passwordVisible) }
        ?: SavedSignIn()
}
```

Rules: persist a small `@Serializable` slice, never the whole screen state, never a fetched
list/map (the saved-state Bundle has a roughly 1&nbsp;MB limit), never a secret (the Bundle is
written to disk). `snapshot` must return a valid slice even before real content has loaded. A failed
restore returns `null`, exactly like nothing was ever saved.

## `LiveNavigator` — recreation-safe deferred navigation

A screen component is retained across Activity recreation, but the navigator lives with the
Composition. Caching the raw navigator and calling it after a suspension point can hit an already
**released** navigator and throw. `LiveNavigator<N : Navigator<*>>` is the shared holder:

```kotlin
private val navigator = LiveNavigator<LineNavigator>(scope, dispatchers.main)

@Composable
override fun Render() {
    navigator.publish(LocalEnclosingLineNavigator) // republished every recomposition
}

// anywhere — event handler or background coroutine
navigator.navigate { it.push(SomeScreenConfig) }
```

- `publish(navigator)` — call from `Render()` every recomposition.
- `navigate { }` — fire-and-forget; waits briefly for a non-released navigator, runs on the main
  thread. Drops the navigation (never crashes) if none appears within the timeout (default 5s).
- `navigateAwaiting { }` — suspending variant, for a suspend action or sequencing follow-up work.

## Composition Locals

| Local | Type | Purpose |
|---|---|---|
| `LocalNavigator` | `Navigator<*>?` | Current navigator of any type |
| `LocalLineNavigator` | `LineNavigator?` | `LocalNavigator.current as? LineNavigator` |
| `LocalSwitchNavigator` | `SwitchNavigator?` | Nearest `SwitchNavigator` at or above the current node |
| `LocalEnclosingLineNavigator` | `LineNavigator?` | Nearest `LineNavigator` at or above the current node |
| `LocalOutermostLineNavigator` | `LineNavigator?` | The outermost `LineNavigator` — the app's own stack, above any tab bar |

`requireLineNavigator()` / `requireSwitchNavigator()` throw if the type doesn't match.

Prefer `LocalEnclosingLineNavigator` from a screen hosted inside a tab: the current navigator there
is a `SwitchNavigator`, but `push`/`replace` belong to the surrounding stack.

## Navigator lifecycle

```
rememberNavigator() in Render()
    ├── remember { navigatorFactory() }       → Navigator created
    ├── parent?.addChild(navigator); navigator.setParent(parent)
    ├── DisposableEffect(navigator, component) { navigator.bind(component); flush pending actions }
    └── DisposableEffect(navigator) { onDispose { navigator.release() } }
```

`release()`: marks `isReleased = true`, recursively detaches children, detaches from its own parent,
nullifies `navigationComponent`/`fallbackPop`, clears pending actions. Every navigator method throws
`IllegalStateException` after release.

## Threading

All navigator methods must be called from the main thread — `pendingActions` and internal state are
not thread-safe by design, matching Compose/Decompose's own threading model. Route background-thread
navigation through `LiveNavigator`.
