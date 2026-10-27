/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
plugins {
    alias(libs.plugins.compose)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.kotlin.desktop)
    alias(libs.plugins.kotlin.serialization)
}

dependencies {
    implementation(projects.tools.creatorSession)
    implementation(projects.tools.uiComponents)
    implementation(compose.desktop.currentOs)
    implementation(libs.compose.resources)
    implementation(libs.gradle.toolingApi)
    implementation(libs.kotlinx.coroutines.desktop)
    implementation(libs.kotlinx.serialization)
    testImplementation(libs.kotlin.test)
    testImplementation(libs.kotlinx.coroutines.test)
}

/**
 * The versions Kubriko Creator writes into the projects it creates: the ones this repository builds Kubriko with.
 */
val generateCreatorProperties = tasks.register("generateCreatorProperties") {
    val outputDirectory = layout.buildDirectory.dir("generated/creatorProperties")
    val properties = mapOf(
        "kubrikoVersion" to rootProject.version.toString(),
        "kotlinVersion" to libs.versions.kotlin.get(),
        "composeVersion" to libs.versions.compose.multiplatform.get(),
        "gradleVersion" to libs.versions.gradle.toolingApi.get(),
    )
    inputs.properties(properties)
    outputs.dir(outputDirectory)
    doLast {
        outputDirectory.get().file("creator.properties").asFile.writeText(properties.entries.joinToString("\n") { "${it.key}=${it.value}" })
    }
}

sourceSets.main {
    resources.srcDir(generateCreatorProperties)
}

compose.desktop {
    application {
        mainClass = "com.pandulapeter.kubrikoCreator.KubrikoCreatorAppKt"
        // For trying Creator against unreleased Kubriko modules published to Maven Local (see creator/DESIGN.md).
        providers.gradleProperty("creator.kubrikoVersion").orNull?.let { jvmArgs += "-Dkubriko.creator.kubrikoVersion=$it" }
        providers.gradleProperty("creator.useMavenLocal").orNull?.let { jvmArgs += "-Dkubriko.creator.useMavenLocal=$it" }
        nativeDistributions {
            packageName = "Kubriko Creator"
            packageVersion = "1.0.0"
        }
    }
}
