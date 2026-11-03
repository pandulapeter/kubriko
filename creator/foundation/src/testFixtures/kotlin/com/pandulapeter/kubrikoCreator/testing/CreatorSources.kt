/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.testing

import java.io.File

/**
 * The `src/main/kotlin` folder of the app and of every Kubriko Creator module, for tests that read the sources.
 *
 * @throws IllegalStateException When the test doesn't run through Gradle, or no such folder exists.
 */
public fun creatorSourceRoots(): List<File> = creatorRoots("src/main/kotlin")

/**
 * The `src/test/kotlin` and `src/testFixtures/kotlin` folders of the app and of every Kubriko Creator module.
 *
 * @throws IllegalStateException When the test doesn't run through Gradle, or no such folder exists.
 */
public fun creatorTestRoots(): List<File> = creatorRoots("src/test/kotlin", "src/testFixtures/kotlin")

/**
 * The `creator` folder: the app's, which holds every module's.
 */
public fun creatorDirectory(): File = File(
    checkNotNull(System.getProperty(CREATOR_DIRECTORY_PROPERTY)) { "Run through Gradle, which sets $CREATOR_DIRECTORY_PROPERTY." },
)

private fun creatorRoots(vararg paths: String): List<File> {
    val creator = creatorDirectory()
    val modules = creator.listFiles().orEmpty().filter { File(it, "build.gradle.kts").isFile }.sortedBy { it.name }
    return (listOf(creator) + modules)
        .flatMap { module -> paths.map { File(module, it) } }
        .filter { it.isDirectory }
        .also { check(it.isNotEmpty()) { "No sources under ${creator.absolutePath}" } }
}

private const val CREATOR_DIRECTORY_PROPERTY = "kubrikoCreator.directory"
