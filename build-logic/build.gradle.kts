plugins {
    `kotlin-dsl`
}

group = "io.github.jamal_wia.decomposenavigator.buildlogic"

dependencies {
    // compileOnly, not implementation: these plugins are applied to the *consuming* project, which
    // brings its own copy via the root build's classpath. Leaking them as `implementation` would
    // put two copies of AGP/KGP on the same classpath and fail with a duplicate-class error.
    compileOnly(libs.android.gradlePlugin)
    compileOnly(libs.kotlin.gradlePlugin)
    compileOnly(libs.kotlin.serialization.gradlePlugin)
    compileOnly(libs.compose.gradlePlugin)
    compileOnly(libs.composeCompiler.gradlePlugin)
    compileOnly(libs.mavenPublish.gradlePlugin)
}

// Plugin ids are `decomposenavigator.<capability>`, so a module's plugins block states what the
// module *is* (a published library, a Compose UI module) instead of restating target lists, SDK
// levels, and a ~150-line publishing block per module (same rationale as KMPToolkit's build-logic,
// which this is ported from).
gradlePlugin {
    plugins {
        register("library") {
            id = "decomposenavigator.library"
            implementationClass =
                "io.github.jamal_wia.decomposenavigator.buildlogic.LibraryConventionPlugin"
        }
        register("compose") {
            id = "decomposenavigator.compose"
            implementationClass =
                "io.github.jamal_wia.decomposenavigator.buildlogic.ComposeConventionPlugin"
        }
        register("publish") {
            id = "decomposenavigator.publish"
            implementationClass =
                "io.github.jamal_wia.decomposenavigator.buildlogic.PublishConventionPlugin"
        }
    }
}
