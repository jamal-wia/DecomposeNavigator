import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("decomposenavigator.library")
    id("decomposenavigator.compose")
    id("decomposenavigator.publish")
}

decomposenavigatorPublish {
    pomName.set("DecomposeNavigator Testing")
    pomDescription.set(
        "Test doubles for decomposenavigator-core: FakeLineNavigator and FakeSwitchNavigator are " +
            "real in-memory implementations (not call recorders over a no-op) that mutate their " +
            "stack the way the Decompose-backed navigators do, so a test can push/pop/switch " +
            "through them and assert on the result. FakeDeepLinkNavigator doubles the DeepLinkNavigator " +
            "seam for testing DeepLinkHandler implementations without Decompose or Compose on the " +
            "classpath. RecordingNavigator is a minimal concrete Navigator for exercising the " +
            "navigator-tree traversal and lifecycle logic directly. Pick this module when writing " +
            "unit tests for code that navigates."
    )
}

android {
    namespace = "io.github.jamal_wia.decomposenavigator.testing"
}

kotlin {
    iosArm64()
    iosSimulatorArm64()

    // Mirrors decomposenavigator-core's own target set — a fake is only useful where the thing it
    // fakes is published.
    jvm {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
    }

    sourceSets {
        commonMain.dependencies {
            api(project(":decomposenavigator-core"))
        }
        commonTest.dependencies {
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
