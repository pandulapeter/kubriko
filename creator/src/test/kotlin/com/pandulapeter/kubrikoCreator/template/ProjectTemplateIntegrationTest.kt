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

import com.pandulapeter.kubrikoCreator.actors.BlockDefinition
import com.pandulapeter.kubrikoCreator.actors.BlockKind
import com.pandulapeter.kubrikoCreator.actors.ConditionKind
import com.pandulapeter.kubrikoCreator.actors.EveryBlockTypes
import com.pandulapeter.kubrikoCreator.actors.PauseAction
import com.pandulapeter.kubrikoCreator.actors.store.BlockTypeStore
import com.pandulapeter.kubrikoCreator.foundation.CreatorConfiguration
import com.pandulapeter.kubrikoCreator.gradle.CompilationMode
import com.pandulapeter.kubrikoCreator.gradle.GradleProjectConnection
import com.pandulapeter.kubrikoCreator.project.KubrikoBuildFiles
import com.pandulapeter.kubrikoCreator.project.ProjectInspector
import com.pandulapeter.kubrikoCreator.scenes.ProjectClasspath
import com.pandulapeter.kubrikoCreator.session.SessionInitScript
import kotlinx.coroutines.runBlocking
import org.junit.Assume.assumeTrue
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO
import kotlin.io.path.createTempDirectory
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Generates every template for real, adds an Actor type and two Manager types that use every Event and every Block
 * (and each other's Properties), and compiles it all for every target. It downloads Gradle and every dependency, so it
 * only runs when the `KUBRIKO_CREATOR_INTEGRATION` environment variable is `true`. Against unreleased modules, also set
 * `KUBRIKO_CREATOR_VERSION` to the version published to Maven Local (see creator/CLAUDE.md).
 */
class ProjectTemplateIntegrationTest {

    @Test
    fun everyTemplateGeneratesAProjectThatCompilesForEveryTarget() {
        assumeTrue(System.getenv("KUBRIKO_CREATOR_INTEGRATION") == "true")
        val bundled = CreatorConfiguration.load()
        val configuration = System.getenv("KUBRIKO_CREATOR_VERSION")?.let { bundled.copy(kubrikoVersion = it, useMavenLocal = true) } ?: bundled
        ProjectTemplate.entries.forEach { template ->
            val parentDirectory = createTempDirectory().toFile()
            try {
                val directory = ProjectGenerator(configuration).create(
                    NewProjectRequest(name = "Integration Game", packageName = "com.example.integration", parentDirectory = parentDirectory, template = template),
                )
                assertTrue(directory.resolve("gradlew").canExecute(), "${template.id}: gradlew")
                val buildFile = directory.resolve(KubrikoBuildFiles.BUILD_FILE)
                assertFalse("libs.kubriko.collision" in buildFile.readText(), "${template.id}: a plugin nothing uses")
                GradleProjectConnection(directory).use { connection ->
                    assertEquals(":", runBlocking { ProjectInspector.desktopModulePathOf(connection) }, template.id)
                    assertCompiles(connection, directory, "${template.id} as created")
                    BlockTypeStore.open(directory)?.let { store ->
                        directory.resolve("src/commonMain/composeResources/drawable/every-block.png").apply { parentFile.mkdirs() }.let { image ->
                            ImageIO.write(BufferedImage(8, 8, BufferedImage.TYPE_INT_ARGB), "png", image)
                        }
                        EveryBlockTypes.all.forEach(store::save)
                        store.regenerateAll()
                        KubrikoBuildFiles(directory, configuration.kubrikoVersion, configuration.useMavenLocal).sync()
                        listOf("audioPlayback", "collision", "keyboardInput", "sprites").forEach { assertContains(buildFile.readText(), "implementation(libs.kubriko.$it)") }
                        assertCompiles(connection, directory, "${template.id} with every Block")
                        assertRuntimeClasspathResolves(connection, "${template.id} with every Block")
                        assertCompilerWarmsUp(connection, directory, "${template.id} with every Block")
                    }
                }
            } finally {
                parentDirectory.deleteRecursively()
            }
        }
    }

    @Test
    fun theIntegrationProjectUsesEveryBlockAndCondition() {
        val blocks = EveryBlockTypes.all.flatMap { it.events }.flatMap { it.blocks }
        assertEquals(BlockKind.entries.toSet(), blocks.mapTo(HashSet(), BlockKind::of))
        assertEquals(ConditionKind.entries.toSet(), blocks.filterIsInstance<BlockDefinition.If>().mapTo(HashSet()) { ConditionKind.of(it.condition) })
        assertEquals(PauseAction.entries.toSet(), blocks.filterIsInstance<BlockDefinition.PauseGame>().mapTo(HashSet()) { it.action })
    }

    private fun assertCompiles(connection: GradleProjectConnection, directory: File, description: String) {
        listOf("desktop" to "compileKotlinDesktop", "wasmJs" to "compileKotlinWasmJs").forEach { (target, task) ->
            val output = StringBuilder()
            val isSuccessful = runBlocking {
                connection.run(taskPath = task, arguments = emptyList(), onEvent = { output.appendLine(it.toString()) })
            }
            assertTrue(isSuccessful, "$description doesn't compile for $target:\n$output")
            val classes = directory.resolve("build/classes/kotlin/$target/main")
            assertTrue(classes.walkTopDown().any { it.isFile }, "$description: nothing was compiled for $target")
        }
    }

    /**
     * Resolves the desktop runtime classpath the way Kubriko Creator does before playing, which compiling alone doesn't:
     * a library missing from the project's repositories only shows up here.
     */
    private fun assertRuntimeClasspathResolves(connection: GradleProjectConnection, description: String) {
        val classpathFile = File.createTempFile("kubriko-creator-classpath", ".txt")
        try {
            val output = StringBuilder()
            val isSuccessful = runBlocking {
                connection.run(
                    taskPath = ":${SessionInitScript.CLASSPATH_TASK_NAME}",
                    arguments = SessionInitScript.gradleArguments(
                        mapOf("kubrikoCreatorMode" to "classpath", "kubrikoCreatorClasspathFile" to classpathFile.absolutePath),
                        CompilationMode(isCommonCodeIncremental = true, isClean = false),
                    ),
                    onEvent = { output.appendLine(it.toString()) },
                )
            }
            assertTrue(isSuccessful, "$description: the desktop runtime classpath doesn't resolve:\n$output")
            val classpath = assertNotNull(ProjectClasspath.parse(classpathFile.readText()), "$description: the classpath can't be read")
            val launch = assertNotNull(classpath.launch, "$description: the classpath says nothing about launching the game")
            assertTrue(launch.classpath.any { "creator-session" in it.name }, "$description: the game would be launched without the session library")
            assertTrue("org.jetbrains.kotlin:kotlin-stdlib" in classpath.libraries, "$description: the classpath doesn't say which Kotlin it resolved")
            assertTrue(classpath.libraries.keys.any { it.startsWith("org.jetbrains.compose.") }, "$description: the classpath doesn't say which Compose it resolved")
        } finally {
            classpathFile.delete()
        }
    }

    /**
     * Runs the init script's warm-up as Kubriko Creator does after opening a project, which has to compile its file
     * next to the project's code.
     */
    private fun assertCompilerWarmsUp(connection: GradleProjectConnection, directory: File, description: String) {
        val output = StringBuilder()
        val isSuccessful = runBlocking {
            connection.run(
                taskPath = ":${SessionInitScript.WARM_UP_TASK_NAME}",
                arguments = SessionInitScript.gradleArguments(
                    mapOf("kubrikoCreatorMode" to "warmUp"),
                    CompilationMode(isCommonCodeIncremental = true, isClean = false),
                ),
                onEvent = { output.appendLine(it.toString()) },
            )
        }
        assertTrue(isSuccessful, "$description: the compiler doesn't warm up:\n$output")
        val classes = directory.resolve("build/classes/kotlin/desktop/kubrikoCreatorWarmUp")
        assertTrue(classes.walkTopDown().any { it.name == "KubrikoCreatorWarmUp.class" }, "$description: the warm-up compiled nothing")
    }
}
