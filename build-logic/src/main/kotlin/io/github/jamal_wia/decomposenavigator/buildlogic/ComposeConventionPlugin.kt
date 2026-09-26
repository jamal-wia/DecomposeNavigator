package io.github.jamal_wia.decomposenavigator.buildlogic

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.jetbrains.kotlin.compose.compiler.gradle.ComposeCompilerGradlePluginExtension

/**
 * `decomposenavigator.compose` — Compose Multiplatform, applied *alongside*
 * `decomposenavigator.library`, never alone.
 *
 * Deliberately adds **no** Compose dependencies (`compose.runtime`, `compose.foundation`,
 * `compose.ui`): different modules need different subsets, so those stay explicit per module (same
 * rationale as KMPToolkit's `ComposeConventionPlugin`, which this is ported from).
 */
class ComposeConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("org.jetbrains.compose")
            pluginManager.apply("org.jetbrains.kotlin.plugin.compose")

            extensions.configure<ComposeCompilerGradlePluginExtension> {
                // Read after a release build to see which composables are skippable/restartable
                // and which parameters are inferred unstable.
                reportsDestination.set(layout.buildDirectory.dir("compose_compiler"))
                metricsDestination.set(layout.buildDirectory.dir("compose_compiler"))
                includeSourceInformation.set(true)
            }
        }
    }
}
