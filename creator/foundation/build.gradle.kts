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
    id("kubriko-creator-module")
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
        "androidGradlePluginVersion" to libs.versions.gradle.asProvider().get(),
        "androidCompileSdk" to libs.versions.android.compileSdk.get(),
        "androidMinSdk" to libs.versions.android.minSdk.get(),
        "androidxActivityVersion" to libs.versions.androidx.activity.get(),
        "useMavenLocal" to (gradle.startParameter.projectProperties["creator.localKubriko"] == "true").toString(),
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
