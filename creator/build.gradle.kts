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
    // Every published module a project could use, so that a project's classes loaded next to Creator's share them
    // (scenes/ClassLoaderEntryPointLoader.kt).
    implementation(projects.plugins.audioPlayback)
    implementation(projects.plugins.collision)
    implementation(projects.plugins.gamepadInput)
    implementation(projects.plugins.keyboardInput)
    implementation(projects.plugins.particles)
    implementation(projects.plugins.persistence)
    implementation(projects.plugins.physics)
    implementation(projects.plugins.pointerInput)
    implementation(projects.plugins.serialization)
    implementation(projects.plugins.shaders)
    implementation(projects.plugins.sprites)
    implementation(projects.tools.creatorApi)
    implementation(projects.tools.creatorSession)
    implementation(projects.tools.sceneEditor)
    implementation(projects.tools.uiComponents)
    implementation(compose.desktop.currentOs)
    implementation(libs.compose.resources)
    implementation(libs.gradle.toolingApi)
    implementation(libs.jbr.api)
    implementation(libs.jgit)
    implementation(libs.kotlinx.coroutines.desktop)
    implementation(libs.kotlinx.serialization)
    testImplementation(libs.kotlin.test)
    testImplementation(libs.kotlinx.coroutines.test)
}

/**
 * The versions Kubriko Creator writes into the projects it creates: the ones this repository builds Kubriko with. With
 * `creator.localKubriko`, projects use the libraries just published to Maven Local instead (see creator/CLAUDE.md).
 */
val generateCreatorProperties = tasks.register("generateCreatorProperties") {
    val outputDirectory = layout.buildDirectory.dir("generated/creatorProperties")
    val properties = mapOf(
        "kubrikoVersion" to rootProject.version.toString(),
        "kotlinVersion" to libs.versions.kotlin.get(),
        "composeVersion" to libs.versions.compose.multiplatform.get(),
        "gradleVersion" to libs.versions.gradle.toolingApi.get(),
        "useMavenLocal" to (findProperty("creator.localKubriko") == "true").toString(),
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
        // The JetBrains Runtime is what lets the content be laid out under a title bar that still behaves as one (ExtendedTitleBar.kt).
        javaHome = javaToolchains.launcherFor {
            languageVersion.set(JavaLanguageVersion.of(21))
            vendor.set(JvmVendorSpec.JETBRAINS)
        }.get().metadata.installationPath.asFile.absolutePath
        if (System.getProperty("os.name").orEmpty().lowercase().contains("mac")) {
            jvmArgs += "--add-exports=java.desktop/com.apple.eawt=ALL-UNNAMED"
        }
        nativeDistributions {
            packageName = "Kubriko Creator"
            packageVersion = "1.0.0"
        }
    }
}
