/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
// Kubriko Creator's development run publishes the libraries to Maven Local under a version of their own, so that they never
// stand in for a real release in other projects (see creator/CLAUDE.md).
// Read from the start parameter, which settings.gradle.kts changes for :creator:run (findProperty doesn't see that).
version = project.findProperty("library.version").toString() + if (gradle.startParameter.projectProperties["creator.localKubriko"] == "true") "-creator" else ""

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.multiplatformLibrary) apply false
    alias(libs.plugins.codingfeline.buildkonfig) apply false
    alias(libs.plugins.compose) apply false
    alias(libs.plugins.compose.compiler) apply false
    alias(libs.plugins.kotlin.desktop) apply false
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.vanniktech.publish) apply false
    id("kubriko-style")
}

// What settings.gradle.kts publishes before Kubriko Creator's development run, from whichever folder Gradle was started in:
// the libraries of every target a game can be built for, iOS only on a Mac, which is the only host that can build it.
tasks.register("publishCreatorLibraries") {
    group = "publishing"
    description = "Publishes the libraries to Maven Local as Kubriko Creator's development run uses them."
    val iosPublications = if (System.getProperty("os.name").startsWith("Mac")) listOf("IosArm64", "IosSimulatorArm64") else emptyList()
    val publishTasks = (listOf("Desktop", "WasmJs", "Android", "KotlinMultiplatform") + iosPublications).mapTo(HashSet()) { "publish${it}PublicationToMavenLocal" }
    // By name, since asking for the tasks themselves would create every task of every project.
    dependsOn(provider { subprojects.flatMap { subproject -> subproject.tasks.names.filter { it in publishTasks }.map { "${subproject.path}:$it" } } })
}
