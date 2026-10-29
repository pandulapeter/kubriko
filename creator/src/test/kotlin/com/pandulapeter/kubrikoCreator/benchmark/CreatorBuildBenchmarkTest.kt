/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.benchmark

import com.pandulapeter.kubrikoCreator.CreatorConfiguration
import com.pandulapeter.kubrikoCreator.actors.ActorTypeDefinition
import com.pandulapeter.kubrikoCreator.actors.BlockDefinition
import com.pandulapeter.kubrikoCreator.actors.BlockTypeStore
import com.pandulapeter.kubrikoCreator.actors.PropertyDefinition
import com.pandulapeter.kubrikoCreator.gradle.GradleProjectConnection
import com.pandulapeter.kubrikoCreator.project.KubrikoBuildFiles
import com.pandulapeter.kubrikoCreator.scenes.ClassLoaderEntryPointLoader
import com.pandulapeter.kubrikoCreator.scenes.ProjectClasspath
import com.pandulapeter.kubrikoCreator.scenes.SceneDocument
import com.pandulapeter.kubrikoCreator.scenes.SceneStore
import com.pandulapeter.kubrikoCreator.scenes.SceneView
import com.pandulapeter.kubrikoCreator.session.SessionInitScript
import com.pandulapeter.kubrikoCreator.template.NewProjectRequest
import com.pandulapeter.kubrikoCreator.template.ProjectGenerator
import kotlinx.coroutines.runBlocking
import java.io.File
import kotlin.io.path.createTempDirectory
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Builds [BenchmarkProject] for real, through the Tooling API and the init script's classpath task as the Scene editor
 * does, and times a cold build, a warm one with nothing changed, the changes an edit loop makes, and loading the new
 * classes into a 1,000-Actor Scene. Each build also reports which generated files the change touched and which classes
 * Kotlin compiled again, which shows whether a change to one type recompiles only that type.
 *
 * Builds run with Creator's own arguments plus `--no-build-cache`: every run makes the same changes, so a build cache
 * (which `~/.gradle/gradle.properties` may turn on) would restore the outputs instead of compiling them.
 * `KUBRIKO_CREATOR_BENCHMARK_ARGUMENTS` adds more, separated by spaces, to compare a change to the build before making it.
 *
 * Only runs with `KUBRIKO_CREATOR_BENCHMARK=true` and, against unreleased libraries, `KUBRIKO_CREATOR_VERSION` (as
 * [com.pandulapeter.kubrikoCreator.template.ProjectTemplateIntegrationTest] does), and prints its numbers rather than
 * asserting them. "Cold" means a project that was never built: the Gradle and Kotlin daemons may already be running,
 * and the first run after they start is several times slower throughout.
 */
class CreatorBuildBenchmarkTest {

    @Test
    fun timeBuildingAGameMakerSizedProject() {
        if (System.getenv("KUBRIKO_CREATOR_BENCHMARK") != "true") return
        val bundled = CreatorConfiguration.load()
        val configuration = System.getenv("KUBRIKO_CREATOR_VERSION")?.let { bundled.copy(kubrikoVersion = it, useMavenLocal = true) } ?: bundled
        val parentDirectory = createTempDirectory().toFile()
        try {
            val directory = BenchmarkProject.create(parentDirectory, ProjectGenerator(configuration, startVersionControl = { _, _ -> }))
            val store = checkNotNull(BlockTypeStore.open(directory))
            store.regenerateAll()
            KubrikoBuildFiles(directory, configuration.kubrikoVersion, configuration.useMavenLocal).sync()
            GradleProjectConnection(directory).use { connection -> measure(BuildProbe(connection, directory, store)) }
        } finally {
            parentDirectory.deleteRecursively()
        }
    }

    private fun measure(probe: BuildProbe) {
        val leaf = BenchmarkProject.actorTypeName(LEAF_INDEX)
        val base = BenchmarkProject.actorTypeName(BASE_INDEX)
        probe.build("A cold build (a project never built)")
        probe.build("A warm build with nothing changed")
        probe.build("A Block added to a leaf Actor type") { probe.changeActorType(leaf) { it.withBlock(BlockDefinition.Log("Leaf")) } }
        probe.build("A Property added to a leaf Actor type") { probe.changeActorType(leaf) { it.withProperty("leafSpeed") } }
        probe.build("A Block added to the base of a family of ten") { probe.changeActorType(base) { it.withBlock(BlockDefinition.Log("Base")) } }
        probe.build("A Property added to the base of a family of ten") { probe.changeActorType(base) { it.withProperty("baseSpeed") } }
        probe.build("One Actor moved in a Scene") { probe.moveActor(BenchmarkProject.sceneName(LOADED_SCENE_SIZE), actorId = "a7") }
        probe.loadScene(BenchmarkProject.sceneName(LOADED_SCENE_SIZE))
    }

    private fun ActorTypeDefinition.withBlock(block: BlockDefinition) = copy(
        events = events.mapIndexed { index, event -> if (index == 1) event.copy(blocks = event.blocks + block) else event },
    )

    private fun ActorTypeDefinition.withProperty(name: String) = copy(properties = properties + PropertyDefinition.Number(name))

    /**
     * Builds the project's classpath and reports each build, and loads the classes it built.
     */
    private class BuildProbe(
        private val connection: GradleProjectConnection,
        private val directory: File,
        private val store: BlockTypeStore,
    ) {
        private val classpathFile = File(directory, "build/kubriko-creator-benchmark-classpath.txt")
        private val classesDirectory = File(directory, "build/classes/kotlin/desktop/main")
        private val generatedDirectory = File(directory, store.generatedPath)
        private val extraArguments = System.getenv("KUBRIKO_CREATOR_BENCHMARK_ARGUMENTS")?.split(' ')?.filter { it.isNotBlank() }.orEmpty()

        fun changeActorType(name: String, change: (ActorTypeDefinition) -> ActorTypeDefinition) {
            store.save(change(store.load().filterIsInstance<ActorTypeDefinition>().single { it.name == name }))
            store.regenerateAll()
        }

        /**
         * Moves the Actor [actorId] of the Scene [sceneName], and regenerates the code as Creator does after every Scene
         * change (for the Scenes' music).
         */
        fun moveActor(sceneName: String, actorId: String) {
            val file = sceneFile(sceneName)
            val actors = SceneDocument.parse(file.readText()).map { entry ->
                if (entry.id == actorId) entry.copy(state = checkNotNull(SceneDocument.withPosition(entry.state, x = 1f, y = 2f))) else entry
            }
            file.writeText(SceneDocument.format(actors))
            store.regenerateAll()
        }

        /**
         * Times [change] (regenerating the code), then the build, and reports both with the generated files the change
         * touched and the classes the build compiled again.
         */
        fun build(step: String, change: () -> Unit = {}) {
            val generatedBefore = snapshot(generatedDirectory) { it.readText() }
            val changeStart = System.nanoTime()
            change()
            val changeNanos = System.nanoTime() - changeStart
            val touched = changedNames(generatedBefore, snapshot(generatedDirectory) { it.readText() })
            val classesBefore = snapshot(classesDirectory) { it.lastModified() to it.length() }
            val output = StringBuilder()
            val buildStart = System.nanoTime()
            val isSuccessful = runBlocking {
                connection.run(
                    taskPath = ":${SessionInitScript.CLASSPATH_TASK_NAME}",
                    arguments = SessionInitScript.gradleArguments(
                        mapOf("kubrikoCreatorMode" to "classpath", "kubrikoCreatorClasspathFile" to classpathFile.absolutePath),
                    ) + "--no-build-cache" + extraArguments,
                    onEvent = { output.appendLine(it.toString()) },
                )
            }
            val buildNanos = System.nanoTime() - buildStart
            assertTrue(isSuccessful, "$step: the build failed:\n$output")
            val compiled = changedNames(classesBefore, snapshot(classesDirectory) { it.lastModified() to it.length() })
                .map { it.removeSuffix(".class").substringBefore('$') }
                .distinct()
            report(step, "change %8.1f ms, build %8.1f ms".format(changeNanos / 1e6, buildNanos / 1e6))
            report("", "generated files touched: ${touched.summary()}")
            report("", "classes compiled again: ${compiled.summary()}")
        }

        /**
         * Times loading the last build's classes, and creating the Scene [sceneName]'s Actors from them as the Scene
         * editor does.
         */
        fun loadScene(sceneName: String) {
            val classpath = checkNotNull(ProjectClasspath.parse(classpathFile.readText()))
            val actors = SceneDocument.parse(sceneFile(sceneName).readText())
            var start = System.nanoTime()
            val loaded = ClassLoaderEntryPointLoader.load(classpath)
            report("Load the built classes (Kubriko ${classpath.kubrikoVersion})", "%8.1f ms".format((System.nanoTime() - start) / 1e6))
            try {
                start = System.nanoTime()
                val view = SceneView(loaded.entryPoint)
                runBlocking { view.load(actors) }
                report("Create a Scene of ${actors.size} Actors from them", "%8.1f ms (${view.actors.count { it != null }} created)".format((System.nanoTime() - start) / 1e6))
                view.dispose()
            } finally {
                loaded.close()
            }
        }

        private fun sceneFile(sceneName: String) = File(directory, "${SceneStore.DEFAULT_SCENE_DIRECTORY}/$sceneName.json")

        private fun <T> snapshot(root: File, read: (File) -> T) = root.walkTopDown()
            .filter { it.isFile }
            .associate { it.relativeTo(root).invariantSeparatorsPath to read(it) }

        private fun <T> changedNames(before: Map<String, T>, after: Map<String, T>) = (before.keys + after.keys).filter { before[it] != after[it] }.sorted()

        private fun List<String>.summary() = when {
            isEmpty() -> "none"
            size <= SUMMARY_SIZE -> "$size (${joinToString()})"
            else -> "$size (${take(SUMMARY_SIZE).joinToString()}, ...)"
        }

        private fun report(step: String, numbers: String) = println("BENCHMARK %-60s %s".format(step, numbers))
    }

    private companion object {
        const val LEAF_INDEX = 42
        const val BASE_INDEX = 40
        const val LOADED_SCENE_SIZE = 1_000
        const val SUMMARY_SIZE = 8
    }
}
