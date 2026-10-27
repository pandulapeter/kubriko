/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.template

import com.pandulapeter.kubrikoCreator.CreatorConfiguration
import com.pandulapeter.kubrikoCreator.gradle.GradleProjectConnection
import com.pandulapeter.kubrikoCreator.project.ProjectInspector
import kotlinx.coroutines.runBlocking
import kotlin.io.path.createTempDirectory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Generates every template for real and compiles it for every target. It downloads Gradle and every dependency, so it
 * only runs when the `KUBRIKO_CREATOR_INTEGRATION` environment variable is `true`. Against unreleased modules, also set
 * `KUBRIKO_CREATOR_VERSION` to the version published to Maven Local (see creator/DESIGN.md).
 */
class ProjectTemplateIntegrationTest {

    @Test
    fun everyTemplateGeneratesAProjectThatCompilesForEveryTarget() {
        if (System.getenv("KUBRIKO_CREATOR_INTEGRATION") != "true") return
        val bundled = CreatorConfiguration.load()
        val configuration = System.getenv("KUBRIKO_CREATOR_VERSION")?.let { bundled.copy(kubrikoVersion = it, useMavenLocal = true) } ?: bundled
        ProjectTemplate.entries.forEach { template ->
            val parentDirectory = createTempDirectory().toFile()
            try {
                val directory = ProjectGenerator(configuration).create(
                    NewProjectRequest(name = "Integration Game", packageName = "com.example.integration", parentDirectory = parentDirectory, template = template),
                )
                assertTrue(directory.resolve("gradlew").canExecute(), "${template.id}: gradlew")
                GradleProjectConnection(directory).use { connection ->
                    assertEquals(":", ProjectInspector.inspect(connection).desktopModulePath, template.id)
                    listOf("desktop" to "compileKotlinDesktop", "wasmJs" to "compileKotlinWasmJs").forEach { (target, task) ->
                        val output = StringBuilder()
                        val isSuccessful = runBlocking {
                            connection.run(taskPath = task, arguments = emptyList(), onEvent = { output.appendLine(it.toString()) })
                        }
                        assertTrue(isSuccessful, "${template.id} doesn't compile for $target:\n$output")
                        val classes = directory.resolve("build/classes/kotlin/$target/main")
                        assertTrue(classes.walkTopDown().any { it.isFile }, "${template.id}: nothing was compiled for $target")
                    }
                }
            } finally {
                parentDirectory.deleteRecursively()
            }
        }
    }
}
