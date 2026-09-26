# Getting started

## 1. Create a config

Every screen is described by a `@Serializable` value implementing `ScreenConfig` — a marker
interface. The config carries only what's needed to create the component.

```kotlin
@Serializable
object HomeScreenConfig : ScreenConfig

@Serializable
data class ArticleScreenConfig(val articleId: String) : ScreenConfig
```

`@Serializable` is required: configs are saved in Decompose's `StateKeeper` for restoration after
process death.

## 2. Create a screen component

```kotlin
class ArticleScreenComponent(
    private val config: ArticleScreenConfig,
    context: ComponentContext,
) : RenderComponent, ComponentContext by context {

    @Composable
    override fun Render() {
        ArticleScreen(articleId = config.articleId)
    }
}
```

## 3. Register every screen once

```kotlin
val ScreenConfigRegistryHolder = ScreenConfigRegistry {
    screen(::HomeScreenComponent)
    screen(::ArticleScreenComponent)
    // ...every other screen
}
```

## 4. Create the root component

```kotlin
// A fixed, deterministic id — see the note below.
val RootLineNavigationConfig = NavigationScreenConfig.LineNavigation(
    initialConfigs = listOf(HomeScreenConfig),
    id = 1L,
)

// Android — inside MainActivity, via Decompose's retainedComponent
val rootComponent = retainedComponent { ctx ->
    ScreenConfigRegistryHolder.createComponent(RootLineNavigationConfig, ctx)
}

// iOS
val rootComponent = ScreenConfigRegistryHolder.createComponent(
    RootLineNavigationConfig,
    DefaultComponentContext(LifecycleRegistry()),
)

// Desktop (JVM) — same shape as iOS: construct your own ComponentContext at the process root
val rootComponent = ScreenConfigRegistryHolder.createComponent(
    RootLineNavigationConfig,
    DefaultComponentContext(LifecycleRegistry()),
)
```

> **Give the root config a fixed `id`.** `id` defaults to a random value, and the controller keys its
> Decompose `childStack` saved state by `"$typeId$id"`. The root config is a top-level `val`
> reconstructed on every cold start, so a random id would change each process start, the saved key
> would never match, and the whole root stack would be silently dropped on process-death restore. A
> fixed id keeps the key stable. Nested configs are serialized inside the parent stack's own JSON, so
> their random ids round-trip correctly and need no fixing.

## 5. Render it

```kotlin
@Composable
fun App(rootComponent: RenderComponent) {
    MaterialTheme {
        rootComponent.Render()
    }
}
```

## 6. Navigate

```kotlin
@Composable
fun SomeScreen() {
    val navigator = LocalLineNavigator // LineNavigator?, null if not inside a LineNavigation

    Button(onClick = {
        navigator?.push(ArticleScreenConfig(articleId = "123"))
    }) {
        Text("Open article")
    }
}
```

That compiles and runs. For tabs, deep links, custom animations, and process-death-safe screen
state, see [`03-guide.md`](03-guide.md).
