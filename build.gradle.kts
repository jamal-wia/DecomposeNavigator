// Top-level build file.
//
// Every plugin that a `decomposenavigator.*` convention plugin applies must appear here with
// `apply false`: build-logic declares them `compileOnly` (so two copies of AGP/KGP never land on
// one classpath), which compiles the conventions but does not put the plugins on the *consuming*
// build's classpath. This block is what does that. Omit one and the module fails at configuration
// time with "Plugin with id '...' not found".
plugins {
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.compose.multiplatform) apply false
    alias(libs.plugins.maven.publish) apply false
}
