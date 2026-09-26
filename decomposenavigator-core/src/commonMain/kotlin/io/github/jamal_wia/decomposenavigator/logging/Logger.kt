package io.github.jamal_wia.decomposenavigator.logging

/**
 * A minimal, dependency-free logging seam — intentionally shaped to match
 * `io.github.jamal_wia.decomposenavigator.logging.Logger` (same author, sibling library) so that swapping
 * to a real dependency on it later is a mechanical rename, not a redesign.
 *
 * **Why this is vendored instead of depending on `kmptoolkit-logging` directly:** that library
 * currently publishes Android and iOS only, no `jvm` target — see its own
 * `docs/01-architecture.md` § "Desktop targets". This module targets JVM (Desktop) on every
 * artifact (see this repository's `docs/01-architecture.md`), and `Logger` appears in public
 * `commonMain` API (constructor parameters on `Navigator`, `LiveNavigator`, `DeepLinkRouter`,
 * `ScreenConfigRegistry.Builder.logger`, and `StackStateSerializer`'s functions), so it must resolve
 * for every target this module compiles — including one `kmptoolkit-logging` does not yet publish.
 *
 * Once `kmptoolkit-logging` adds a `jvm` target, this file and its usages should be deleted in
 * favor of a real dependency on that library — see the `[Unreleased]` entry in `CHANGELOG.md`.
 */
interface Logger {

    /** Emits [message], evaluated only if the implementation actually logs it. */
    fun d(message: () -> String)

    /** Emits [message] — and, if not `null`, [throwable] — at error severity. */
    fun e(throwable: Throwable? = null, message: () -> String)
}

/**
 * A [Logger] that discards everything and never evaluates a message lambda. The default for every
 * optional `logger` parameter in this module — silent unless a consumer wires a real [Logger] in.
 */
object NoopLogger : Logger {
    override fun d(message: () -> String) = Unit
    override fun e(throwable: Throwable?, message: () -> String) = Unit
}
