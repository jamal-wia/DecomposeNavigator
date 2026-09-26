plugins {
    `java-platform`
    id("decomposenavigator.publish")
}

// BOM declares versions for the rest of the suite. Constraints reference the same
// `decomposenavigator.version` property as the modules they pin, so the BOM and the artifacts it
// pins cannot drift — bumping `decomposenavigator.version` in gradle.properties updates every
// constraint in lockstep.
dependencies {
    constraints {
        api("${project.group}:decomposenavigator-core:${project.version}")
        api("${project.group}:decomposenavigator-testing:${project.version}")
    }
}

decomposenavigatorPublish {
    pomName.set("DecomposeNavigator BOM — Bill of Materials")
    pomDescription.set(
        "Bill of Materials (BOM) for DecomposeNavigator. Importing this platform via " +
            "implementation(platform(\"io.github.jamal-wia:decomposenavigator-bom:<version>\")) " +
            "lets consumers declare decomposenavigator-core and decomposenavigator-testing without " +
            "specifying versions individually — the BOM keeps them aligned."
    )
}
