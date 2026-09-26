import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("decomposenavigator.library")
    id("decomposenavigator.compose")
    id("decomposenavigator.publish")
    alias(libs.plugins.kotlin.serialization)
}

decomposenavigatorPublish {
    pomName.set("DecomposeNavigator Core")
    pomDescription.set(
        "A declarative, serializable navigation layer on top of Decompose's ChildStack: a DSL " +
            "screen registry (no manual KSerializer registration), a navigator tree reachable via " +
            "Composition Locals (LocalNavigator / LocalLineNavigator / LocalSwitchNavigator / " +
            "LocalEnclosingLineNavigator), line (stack) and switch (tab) navigation controllers, " +
            "process-death-safe stack and per-screen state persistence, a recreation-safe " +
            "LiveNavigator for navigating from a coroutine after an Activity recreation, deep-link " +
            "plumbing (DeepLinkBus / DeepLinkRouter / DeepLinkHandler) that never drops a link " +
            "tapped before the app is ready, and slide/fade/covering-screen stack animations that " +
            "survive Decompose's per-child animation caching. Pick this module if you want " +
            "Decompose's power without hand-writing StackNavigation, ChildStack, component " +
            "factories, BackHandler wiring, and serializer registration for every screen."
    )
}

android {
    namespace = "io.github.jamal_wia.decomposenavigator.core"
}

kotlin {
    iosArm64()
    iosSimulatorArm64()

    // Desktop. The types this module publishes — ScreenConfig, the navigator tree, LineNavigator /
    // SwitchNavigator, LiveNavigator, the deep-link plumbing — are exactly the kind a consumer puts
    // in code shared between phone and desktop: a navigation stack has no reason to differ by
    // platform. The whole module is already commonMain-only with zero expect/actual, so this target
    // costs nothing to add and nothing to maintain (same bar as KMPToolkit's own "Desktop targets"
    // policy in its docs/01-architecture.md — system bars, storage, language — this is the same
    // shape of exception, not a new one).
    jvm {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
    }

    sourceSets {
        commonMain.dependencies {
            // api, not implementation: these types are part of this module's public surface —
            // Navigator/LineNavigator/SwitchNavigator expose Compose types (CompositionLocal,
            // @Composable), controllers expose Decompose's ComponentContext/Value<ChildStack<...>>,
            // and configs are (de)serialized through a public Json. A consumer referencing any of
            // these needs the dependency on its own compile classpath, which a plain `implementation`
            // would not provide. The logging seam (`Logger`/`NoopLogger`) is vendored in this module
            // (io.github.jamal_wia.decomposenavigator.logging) rather than depending on the sibling
            // kmptoolkit-logging library — that library does not yet publish a `jvm` target, and this
            // module targets JVM on every artifact. See that package's KDoc.
            api(compose.runtime)
            api(compose.foundation)
            api(compose.ui)
            api(libs.decompose)
            api(libs.decompose.compose)
            api(libs.essenty.lifecycle)
            api(libs.essenty.lifecycle.coroutines)
            api(libs.kotlinx.coroutines.core)
            api(libs.kotlinx.serialization.json)
        }
        commonTest.dependencies {
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.kotlinx.coroutines.test)
        }
        val androidUnitTest by getting {
            dependencies {
                implementation(kotlin("test-junit"))
                implementation(libs.junit)
            }
        }
    }
}
