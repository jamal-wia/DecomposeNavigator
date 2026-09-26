<!-- Favicon / badges: fill in the Maven Central badge URL once the first module is released. -->

# DecomposeNavigator

A wrapper over [Decompose](https://github.com/arkivanov/Decompose) that provides a declarative,
serializable navigation layer on top of `ChildStack` for Kotlin Multiplatform (Android, iOS, and JVM
/ Desktop). It hides Decompose's low-level plumbing — manual `KSerializer` registration, hand-written
`ChildStack`/`StackNavigation` wiring, `BackHandler` setup, component factories — behind a DSL screen
registry, a navigator tree reachable through Composition Locals, and process-death-safe state
persistence.

[![License: MIT](https://img.shields.io/badge/license-MIT-blue.svg)](LICENSE)
![Kotlin Multiplatform](https://img.shields.io/badge/kotlin-multiplatform-7F52FF)
![Android](https://img.shields.io/badge/platform-android-3DDC84)
![iOS](https://img.shields.io/badge/platform-ios-black)
![Desktop](https://img.shields.io/badge/platform-desktop-4285F4)

Extracted from [TahfeezAI](https://github.com/TahfeezAI-Company)'s internal
`core/decompose-navigator` module, where it has powered a Quran-recitation-training app's navigation
— including nested tab stacks, deep links that must survive an entire login flow, and Activity
recreation on rotation — across many releases.

## Why this exists

| Decompose problem                                          | What this library does instead                                                                  |
|--------------------------------------------------------------|-------------------------------------------------------------------------------------------------|
| Registering `KSerializer` for each screen config by hand    | `screen(::MyComponent)` registers the factory and serializer together                           |
| Every container knowing about all its possible children     | `ScreenConfigRegistry` centralizes component creation from one DSL block                        |
| Navigator access via DI or constructor threading            | `LocalNavigator` / `LocalLineNavigator` / `LocalSwitchNavigator` via Composition Locals          |
| Custom slide/fade animation fighting Decompose's caching     | `SlideAnimationMarker` — updates direction without replacing the animation object Decompose caches |
| Navigating before the first render crashes                  | A `pendingActions` queue buffers calls until the navigator is bound                              |
| A screen component outliving its Composition after rotation | `LiveNavigator` — republished every recomposition, drops (never crashes) a call with no live target |
| A deep link tapped before the app is ready to navigate      | `DeepLinkBus` holds the latest link indefinitely; nothing times it out                           |

See [`docs/01-architecture.md`](docs/01-architecture.md) for the principles this library follows (no
bundled DI framework, no hardcoded consumer identifiers, no user-facing text, why Compose is a
first-class dependency here) and [`docs/decomposenavigator-core/01-overview.md`](docs/decomposenavigator-core/01-overview.md)
for what the module does and does not do.

## Modules

| Artifact                        | What it solves                                                                                  | Depends on | Docs |
|----------------------------------|---------------------------------------------------------------------------------------------------|------------|------|
| `decomposenavigator-bom`         | Pins every artifact below to one version — import this first                                     | —          | [install](#installation) |
| `decomposenavigator-core`        | The library itself: screen registry, navigator tree, controllers, deep links, animations, persistence | —          | [docs](docs/decomposenavigator-core/01-overview.md) |
| `decomposenavigator-testing`     | Real, in-memory `FakeLineNavigator` / `FakeSwitchNavigator` / `FakeDeepLinkNavigator` / `RecordingNavigator` test doubles, for `testImplementation` | `core`     | [docs](docs/decomposenavigator-core/06-testing.md) |

All three publish Android, iOS (`iosArm64`, `iosSimulatorArm64`), and JVM (Desktop).

## Installation

Add the BOM to align every module on one version, then pull in what you use:

```kotlin
dependencies {
    implementation(platform("io.github.jamal-wia:decomposenavigator-bom:<version>"))

    implementation("io.github.jamal-wia:decomposenavigator-core")
    testImplementation("io.github.jamal-wia:decomposenavigator-testing")
}
```

## Quick start

```kotlin
// 1. Describe a screen with a config.
@Serializable
data class ArticleScreenConfig(val articleId: String) : ScreenConfig

// 2. A screen component that renders it.
class ArticleScreenComponent(
    private val config: ArticleScreenConfig,
    context: ComponentContext,
) : RenderComponent, ComponentContext by context {
    @Composable
    override fun Render() {
        ArticleScreen(articleId = config.articleId)
    }
}

// 3. Register it once, at app startup.
val registry = ScreenConfigRegistry {
    screen(::ArticleScreenComponent)
    // ...every other screen
}

val rootComponent = registry.createComponent(
    NavigationScreenConfig.LineNavigation(initialConfigs = listOf(HomeScreenConfig), id = 1L),
    componentContext,
)

// 4. Navigate from any Composable inside that stack.
val navigator = LocalLineNavigator
navigator?.push(ArticleScreenConfig(articleId = "123"))
```

See [`docs/decomposenavigator-core/02-getting-started.md`](docs/decomposenavigator-core/02-getting-started.md)
for a complete walk-through including tabs, deep links, and process-death-safe state.

## Documentation

- [`docs/README.md`](docs/README.md) — full documentation index and recommended reading order.
- [`docs/01-architecture.md`](docs/01-architecture.md) — design principles.
- [`docs/decomposenavigator-core/`](docs/decomposenavigator-core/01-overview.md) — overview, getting
  started, the full guide (controllers, navigators, deep links, animations, state persistence,
  `LiveNavigator`), API reference, platform notes, and testing.

## License

MIT — see [`LICENSE`](LICENSE).
