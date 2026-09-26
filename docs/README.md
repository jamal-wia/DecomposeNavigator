# Documentation index

This is a learning path, not a reference dump. Read in order the first time; come back to individual
pages afterward as you need them.

## Read first, once

1. [`01-architecture.md`](01-architecture.md) — the principles this library follows, and why: no
   bundled DI, no hardcoded consumer identifiers, no user-facing text, why Compose and Desktop are
   first-class here, and the `explicitApi()` / ABI / semver policy.

## Then

`decomposenavigator-core` is the one module with a documentation set; `decomposenavigator-testing`'s
doubles are documented in that same set's `06-testing.md` rather than a folder of their own —
`decomposenavigator-bom` has nothing beyond the [installation](../README.md#installation) snippet.

| File | Purpose |
|---|---|
| [`decomposenavigator-core/01-overview.md`](decomposenavigator-core/01-overview.md) | What the module solves, and what it explicitly does **not** do |
| [`decomposenavigator-core/02-getting-started.md`](decomposenavigator-core/02-getting-started.md) | A minimal working example, five minutes to a compiling result |
| [`decomposenavigator-core/03-guide.md`](decomposenavigator-core/03-guide.md) | Every concept in depth: controllers, navigators, deep links, animations, state persistence, `LiveNavigator` |
| [`decomposenavigator-core/04-api-reference.md`](decomposenavigator-core/04-api-reference.md) | Every public symbol: signature, contract |
| [`decomposenavigator-core/05-platform-notes.md`](decomposenavigator-core/05-platform-notes.md) | Android vs. iOS vs. Desktop behavior, threading rules |
| [`decomposenavigator-core/06-testing.md`](decomposenavigator-core/06-testing.md) | The `decomposenavigator-testing` doubles, and how to use them |

Start with [`01-overview.md`](decomposenavigator-core/01-overview.md) — if what you need doesn't fit
its stated scope, check its "What this is not" section for a pointer elsewhere.
