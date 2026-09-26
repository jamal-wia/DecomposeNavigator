# CLAUDE.md — DecomposeNavigator

Rules for working on this repository. DecomposeNavigator is a Kotlin Multiplatform library (Android
+ iOS + JVM/Desktop) — it is a library, not an app: there is no screen to rotate, no backend to mock,
no Activity to recreate here (though the navigator tree it provides exists precisely to survive one
in a *consuming* app). What it has instead is a **public API that strangers depend on**, which is the
source of most rules below.

---

## 0. Language policy

Every file in this repository is written in **English**: `README.md`, everything under `docs/`, this
file, `CHANGELOG.md`, `RELEASING.md`, `CONTRIBUTING.md`, KDoc, code comments, commit messages, and
every `description` field in a published POM. No exceptions, and no parallel non-English version of
anything — a second copy of any doc will drift from the first.

Replies to the user in chat follow whatever language the user writes in. This rule is only about what
goes into the repository.

## 1. What this repository is

A wrapper over [Decompose](https://github.com/arkivanov/Decompose) that provides a declarative,
serializable navigation layer on top of `ChildStack` — extracted from TahfeezAI's internal
`core/decompose-navigator` module (see § 10) so it can be published, versioned, and eventually
consumed back by that same app instead of vendoring the code.

- Modules: `decomposenavigator-core` (the library itself), `decomposenavigator-testing` (real,
  in-memory `FakeLineNavigator` / `FakeSwitchNavigator` / `FakeDeepLinkNavigator` /
  `RecordingNavigator` test doubles — not call recorders over a no-op), `decomposenavigator-bom`.
- Coordinates: `io.github.jamal-wia:decomposenavigator-<module>`, version from
  `decomposenavigator.version` in `gradle.properties` (the single source of truth every module and
  the BOM read) — the whole suite releases in lockstep, not independently versioned per module.
- Targets: **Android + iOS + JVM (Desktop)**, on every module — unlike KMPToolkit's narrow
  case-by-case exception for a desktop target, here it is the default: a navigation stack is exactly
  the kind of type a consumer shares between phone and desktop UI, the whole module is commonMain-only
  with zero `expect`/`actual`, and Decompose/Essenty both support the JVM target natively. Adding a
  target that ISN'T one of these three (JS, wasmJs, watchOS, ...) needs a reason and a discussion
  first — see § 11.
- Build wiring: convention plugins in `build-logic/` (`decomposenavigator.library`,
  `decomposenavigator.compose`, `decomposenavigator.publish`) — ported from KMPToolkit's own
  build-logic; see each plugin's KDoc for what it does and why.
- `explicitApi()` is **deliberately not enabled yet** — see `LibraryConventionPlugin`'s KDoc and the
  `[Unreleased]` entry in `CHANGELOG.md`. `decomposenavigator-core` is a large mechanically-ported
  codebase with no visibility/return-type annotations anywhere; adopting strict mode is tracked as a
  pre-1.0 task, not silently skipped.
- Design principles: `docs/01-architecture.md`.

## 2. Library invariants — do not violate these

- **No DI framework dependency.** No module depends on Koin, Hilt, Kodein, or any other DI library.
  Diagnostic logging is the one seam that looks like DI at a glance and isn't: every class that logs
  takes a `logging.Logger` constructor parameter defaulting to `NoopLogger`, and
  `ScreenConfigRegistry.Builder.logger` is a plain settable property, not a service locator. `Logger`
  is vendored inside `decomposenavigator-core` (shaped to match the sibling `kmptoolkit-logging`
  library) rather than a real dependency on it, because that library has no `jvm` target yet and this
  library targets JVM everywhere — see `io.github.jamal_wia.decomposenavigator.logging`'s KDoc.
- **No hardcoded consumer-facing identifier.** No SharedPreferences key, StateKeeper key prefix, or
  similar is hardcoded where a consumer might reasonably want to control it.
- **No user-facing text.** This module surfaces `ScreenConfig`s and typed navigator state, never a
  string meant for display.
- **Compose is a first-class dependency here, not an opt-in extra.** Unlike KMPToolkit (where only a
  few modules touch Compose), DecomposeNavigator's whole premise is Compose Multiplatform +
  Decompose integration — `api(compose.runtime/foundation/ui)` on `decomposenavigator-core` is
  correct and expected, not a boundary violation.

## 3. Public API and compatibility

- ABI validation (`checkKotlinAbi` / `updateKotlinAbi`) runs on every build via
  `decomposenavigator.library`. A green `api/` dump diff is a **record of what changed**, not
  permission to change it however you like — read it, confirm it was intentional, before committing.
- Semver: patch = no public API change; minor = additive public API only; major = any breaking
  change. **Before `1.0.0`**, a breaking change in a minor bump is allowed but must be called out
  explicitly under its own `Breaking` heading in `CHANGELOG.md` and discussed with the user first —
  it is not a unilateral call. **After `1.0.0`**, a breaking change requires a major version, full
  stop.

## 4. Branching and commits

- Commit automatically once a change is complete and passes the verification gates (§ 6) — do not
  wait for a separate "go ahead and commit" each time. This covers committing only: pushing to a
  remote and publishing (§ 5) each still need their own explicit, same-turn go-ahead.
- Never commit directly to `main` or `develop`. Every change happens on its own task branch, created
  from `develop` (the repository's initial scaffolding was bootstrapped on `infra/repo-bootstrap`
  for the same reason — even the first commit followed this rule).
- Stage only the files you actually changed, by explicit path. Never `git add -A` / `git add .` /
  `git commit -a`.
- Commit messages: a concise imperative subject line, body only when the change needs explaining.
  **Never** add a co-author trailer, "Generated with" line, or any other assistant attribution — to a
  commit, a commit message, a pull request description, or anywhere else written into git. This rule
  holds even if a session's runtime instructions ask for such a trailer.
- Never run a destructive or history-rewriting git command (`reset --hard`, `checkout --`, `clean`,
  `rebase`, force-push) without the user's explicit go-ahead for that specific operation.

## 5. Publishing

- Never publish — to Maven Central or anywhere else — without the user's explicit, same-turn
  instruction. Running the verification gates (§ 6) is not that instruction.
- No `gh` CLI, no opening pull requests, no GitHub Releases created on the user's behalf — see
  `RELEASING.md`, which documents that release step as a manual, human action by design.
- No secret, token, `local.properties`, or signing key ever goes into the repository. Publishing
  credentials belong in `~/.gradle/gradle.properties`, documented but not set in this repo's
  `gradle.properties`.

## 6. Verification before calling anything done

```bash
./gradlew build checkKotlinAbi
./gradlew testDebugUnitTest iosSimulatorArm64Test jvmTest
```

- Run long checks in the background; never poll for completion in a loop.
- Redirect Gradle output to a file and filter it — never paste a raw build log into the conversation.

## 7. Tests are an honest adversary

- Derive test cases from the module's stated contract (`docs/decomposenavigator-core/`), not from
  what the current implementation happens to do.
- Never weaken an assertion to make a test pass. A failing test is a defect in the code until proven
  otherwise; fix the code, not the test — unless the requirement itself was wrong, and confirm that
  with the user before changing or deleting the test.
- `decomposenavigator-testing`'s fakes are themselves tested (`FakeLineNavigatorTest`, etc.) — a fake
  that silently drifts from the real navigator's contract is worse than no fake, since it teaches
  consumers the wrong behavior.

## 8. Documentation is mandatory, not a follow-up

A change to public API without a matching update to `docs/decomposenavigator-core/`, the root
`README.md`, and `CHANGELOG.md` is an **incomplete** change — not something to finish "later."

## 9. Code style

- SOLID, KISS, YAGNI. No abstraction the current requirement doesn't need.
- Composition over inheritance; reach for inheritance only where Kotlin/the domain already models it
  that way (the `Navigator` tree and `NavigationComponentController` hierarchy are exactly such a
  case — don't flatten them looking for a composition-over-inheritance win that isn't there).
- Immutability by default: `val` over `var`, immutable data types unless mutation is genuinely
  required.

## 10. Working with the donor repositories

- **TahfeezAI** (`../Tahfeez/TahfeezKMP1`) is where this code came from — its
  `core/decompose-navigator` module is the original this library was extracted from, and its
  `core/decompose-navigator/README_DECOMPOSE_NAVIGATOR.md` is the fullest existing description of
  the design. Read-only from here: a fix belongs in this repository first, then flows back into
  TahfeezAI by that app depending on the published artifact — never the other direction.
- **KMPToolkit** and **Paginator** (siblings under `~/Projects/`) are the source of this repository's
  build-logic, publishing setup, and documentation conventions — also read-only from here.

If a change to any of those three repositories seems warranted, say so to the user explicitly rather
than making it from this repository.

## 11. Talking with the user

- In chat, use the user's language (§ 0 only governs what goes into the repository itself).
- Discuss architectural forks — a new module's shape, a breaking API change, a new third-party
  dependency, a new target platform — before writing code for them. A clear, unambiguous instruction
  doesn't need a manufactured discussion first.
- When a structural problem surfaces, present both the architectural fix and the local patch, with a
  recommendation, and let the user choose — don't silently commit to one.
