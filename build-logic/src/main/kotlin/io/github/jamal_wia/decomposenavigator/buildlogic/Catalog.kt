package io.github.jamal_wia.decomposenavigator.buildlogic

import org.gradle.api.Project
import org.gradle.api.artifacts.MinimalExternalModuleDependency
import org.gradle.api.artifacts.VersionCatalog
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.api.provider.Provider
import org.gradle.kotlin.dsl.getByType

/**
 * Access to the single `gradle/libs.versions.toml` from inside a convention plugin. Convention
 * plugins must never hard-code a version — that would quietly fork the catalog, which is the one
 * thing the catalog exists to prevent.
 */
internal val Project.libs: VersionCatalog
    get() = extensions.getByType<VersionCatalogsExtension>().named("libs")

/** Fails loudly with the missing key rather than an opaque `NoSuchElementException` from `get()`. */
internal fun VersionCatalog.intVersion(alias: String): Int {
    val raw: String = findVersion(alias)
        .orElseThrow { IllegalStateException("Version '$alias' missing from gradle/libs.versions.toml") }
        .requiredVersion
    return raw.toIntOrNull()
        ?: error("Version '$alias' is '$raw', which is not an Int — expected an SDK level")
}

/** Same intent as [intVersion]: name the missing alias instead of failing anonymously. */
internal fun VersionCatalog.library(alias: String): Provider<MinimalExternalModuleDependency> =
    findLibrary(alias)
        .orElseThrow { IllegalStateException("Library '$alias' missing from gradle/libs.versions.toml") }
