# Testing

`decomposenavigator-testing` ships test doubles as **real, in-memory implementations** — not call
recorders sitting on top of a no-op. Each one mutates its own state the way the Decompose-backed
production navigator would, so a test can drive a full navigation flow and assert on the result, not
just on which method was called with which argument (those are still recorded separately, for tests
that need it).

```kotlin
testImplementation("io.github.jamal-wia:decomposenavigator-testing")
```

## `FakeLineNavigator`

```kotlin
val navigator = FakeLineNavigator(initialStack = listOf(HomeScreenConfig))

navigator.push(ArticleScreenConfig("42"))
assertEquals(listOf(HomeScreenConfig, ArticleScreenConfig("42")), navigator.stack)

navigator.pop()
assertEquals(listOf(HomeScreenConfig), navigator.stack)
```

`stack` reflects every push/pop/replace/replaceAll/popTo/bringToFront call. `pushCalls`,
`pushNewCalls`, `pushToFrontCalls`, `replaceCalls`, `replaceAllCalls`, `bringToFrontCalls`,
`popToCalls`, and `navigateCalls` record arguments in call order, for tests that need to assert on
*what was asked for* rather than only the end state. `applyFallbackPop` and `checkNotReleased`-guarded
methods behave exactly like `DefaultLineNavigator`.

## `FakeSwitchNavigator`

```kotlin
val home = SwitchScreenConfigContainer(HomeScreenConfig)
val tasks = SwitchScreenConfigContainer(TasksScreenConfig)
val navigator = FakeSwitchNavigator(initial = home)

navigator.switchTo(tasks)
assertEquals(tasks, navigator.activeConfig)
assertEquals(listOf(home, tasks), navigator.backStack)

navigator.pop()
assertEquals(home, navigator.activeConfig)
```

`switchToFirst`/`switchToFirstAndAwaitReady` search `backStack` for a match before falling back to
`createIfAbsent`, matching `DefaultSwitchNavigator`'s own resolution order.

## `FakeDeepLinkNavigator`

For testing a `DeepLinkHandler` without Decompose or Compose on the test classpath:

```kotlin
val navigator = FakeDeepLinkNavigator(initial = HomeScreenConfig)

myHandler.handle(DeepLink.Article(id = "42"), navigator)

assertEquals(listOf(ArticleScreenConfig("42")), navigator.shown)
assertEquals(ArticleScreenConfig("42"), navigator.activeScreenConfig)
```

## `RecordingNavigator`

A minimal concrete `Navigator`, for testing code that walks the navigator tree
(`findAncestorBy`, `findSiblingBy`, `findCousinBy`, `findDescendantBy`, `findGrandParentBy`) or
exercises `release`/`bind`/`setParent`/`addChild` without needing a real `LineNavigator` or
`SwitchNavigator`:

```kotlin
val root = RecordingNavigator()
val child = link(root, RecordingNavigator())

child.release()
assertTrue(child.isReleased)
assertFalse(root.children.contains(child))
```

`runOrEnqueue` exposes the protected `withComponentOrEnqueue` seam, for testing pending-action
behavior directly.

## The doubles are themselves tested

`FakeLineNavigatorTest`, `FakeSwitchNavigatorTest`, `FakeDeepLinkNavigatorTest`, and
`RecordingNavigatorTest` in this module's own `commonTest` verify every double against the real
navigator's contract — a fake that silently drifts from production behavior would be worse than no
fake, teaching consumers the wrong thing.
