# Contributing

Thanks for considering a contribution to DecomposeNavigator. This file covers the mechanics; see
`.claude/CLAUDE.md` for the full set of repository rules (language policy, library invariants, API
compatibility, branching, testing, documentation).

## Before you start

- **Open an issue for anything beyond a small fix** — a new target platform, a public API change, a
  new dependency — so the design can be discussed before code is written. See
  `docs/01-architecture.md` for the principles this library follows (no DI framework, no hardcoded
  consumer identifiers, no user-facing text, Compose as a first-class dependency).
- **Read `docs/decomposenavigator-core/01-overview.md`'s "What this is not" section** before adding a
  feature — if it doesn't fit the module's stated scope, say so rather than bending the module to fit.

## Making a change

1. Branch from `develop` (never commit directly to `main` or `develop`).
2. Make the change. Every behavioral change needs a matching update to `docs/decomposenavigator-core/`
   (see `.claude/CLAUDE.md` § Documentation is mandatory).
3. Add tests. See `.claude/CLAUDE.md` § Tests — cases are derived from requirements, not from what
   the implementation happens to do; boundary and platform-specific cases are not optional.
4. Run the checks locally before opening a pull request:
   ```bash
   ./gradlew build checkKotlinAbi
   ./gradlew testDebugUnitTest iosSimulatorArm64Test jvmTest
   ```
5. If your change adds or changes public API, run `./gradlew updateKotlinAbi` and commit the updated
   `api/` dump alongside the code change — don't hand-edit the dump file.
6. Add an entry under `## [Unreleased]` in `CHANGELOG.md`.

## Code style

SOLID / KISS / YAGNI, composition over inheritance, immutability by default — see `.claude/CLAUDE.md`
§ Code style for the full list.

## Language

Everything in the repository — code, comments, KDoc, commit messages, documentation — is in English.
See `.claude/CLAUDE.md` § Language policy.

## Reporting issues

Please include: the artifact and version, target platform (Android API level / iOS version / JVM
version), minimal repro, and expected vs. actual behavior.
