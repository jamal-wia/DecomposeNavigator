// Standalone build, included via `pluginManagement { includeBuild("build-logic") }` in the root
// settings.gradle.kts — a *composite build* rather than `buildSrc`: a change to one convention
// plugin here invalidates only the modules that apply it, whereas buildSrc is an implicit
// dependency of every project and would invalidate the whole build on every edit.
dependencyResolutionManagement {
    repositories {
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
    // The one version catalog, shared with the main build — conventions must never pin their own
    // versions, or the catalog stops being the single source of truth.
    versionCatalogs {
        create("libs") {
            from(files("../gradle/libs.versions.toml"))
        }
    }
}

rootProject.name = "build-logic"
