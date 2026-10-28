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

import com.pandulapeter.kubrikoCreator.actors.BlockDefinition
import com.pandulapeter.kubrikoCreator.actors.BlockTypeStore
import com.pandulapeter.kubrikoCreator.commands.Invocation
import com.pandulapeter.kubrikoCreator.commands.actorTypes.AddBlock
import com.pandulapeter.kubrikoCreator.commands.scenes.MoveActor
import com.pandulapeter.kubrikoCreator.project.CreatorProject
import com.pandulapeter.kubrikoCreator.project.KubrikoBuildFiles
import com.pandulapeter.kubrikoCreator.project.ProjectWatcher
import com.pandulapeter.kubrikoCreator.project.ResourceGroup
import com.pandulapeter.kubrikoCreator.project.ResourceScanner
import com.pandulapeter.kubrikoCreator.workspace.FakeProjectBuild
import com.pandulapeter.kubrikoCreator.workspace.FakeSessionChannel
import com.pandulapeter.kubrikoCreator.workspace.WorkspaceController
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import java.io.File
import java.util.concurrent.Executors
import kotlin.io.path.createTempDirectory
import kotlin.test.Test

/**
 * Times what Kubriko Creator does with a GameMaker-sized project ([BenchmarkProject]), without Gradle: opening it, changing
 * an Actor type, moving an Actor in Scenes of each size, undoing, and a pass of the file watcher. Each step reports the
 * time the main thread was busy with it, and the time until it was written. Only runs with
 * `KUBRIKO_CREATOR_BENCHMARK=true`, and prints its numbers rather than asserting them, so every change to the data flow can
 * be compared against the last run.
 */
class CreatorBenchmarkTest {

    @Test
    fun timeAGameMakerSizedProject() {
        if (System.getenv("KUBRIKO_CREATOR_BENCHMARK") != "true") return
        val parentDirectory = createTempDirectory().toFile()
        val executor = Executors.newSingleThreadExecutor { Thread(it, "benchmark-main") }
        val main = executor.asCoroutineDispatcher()
        val scope = CoroutineScope(main + SupervisorJob())
        try {
            val directory = BenchmarkProject.create(parentDirectory)
            runBlocking { withTimeout(TIMEOUT_IN_MILLISECONDS) { measure(directory, scope, main) } }
        } finally {
            scope.cancel()
            executor.shutdown()
            parentDirectory.deleteRecursively()
        }
    }

    private suspend fun measure(directory: File, scope: CoroutineScope, main: CoroutineDispatcher) {
        val store = checkNotNull(BlockTypeStore.open(directory))
        var mainTime = 0L
        var totalStart = System.nanoTime()
        val controller = withContext(main) {
            timed({ mainTime = it }) {
                WorkspaceController(
                    project = CreatorProject(rootDirectory = directory, name = "Benchmark"),
                    build = FakeProjectBuild(),
                    sessionChannel = FakeSessionChannel(),
                    parentScope = scope,
                    ioDispatcher = Dispatchers.IO,
                    scanResources = { ResourceScanner.scan(it, hiddenDirectory = store.generatedPath) },
                    blockTypeRepository = store,
                    syncBuildFiles = KubrikoBuildFiles(directory, "1.0.0", useMavenLocal = false)::sync,
                    generatedPath = store.generatedPath,
                )
            }
        }
        val blockTypes = checkNotNull(controller.blockTypes)
        blockTypes.types.first { it.size == BenchmarkProject.ACTOR_TYPE_COUNT + BenchmarkProject.MANAGER_TYPE_COUNT }
        controller.resources.first { it[ResourceGroup.SCENES].orEmpty().size > BenchmarkProject.SCENE_SIZES.size }
        blockTypes.awaitWrites()
        report("Open the project (types loaded, resources scanned, code generated)", mainTime, System.nanoTime() - totalStart)

        val type = BenchmarkProject.actorTypeName(42)
        totalStart = System.nanoTime()
        mainTime = withContext(main) { timed { controller.commands.dispatch(Invocation(AddBlock, AddBlock.Input(type, eventIndex = 1, block = BlockDefinition.Stop))) } }
        awaitHistorySize(controller, 1)
        blockTypes.awaitWrites()
        report("Add a Block to an Actor type (until its code is written)", mainTime, System.nanoTime() - totalStart)

        totalStart = System.nanoTime()
        mainTime = 0L
        repeat(TYPING_COUNT) { index ->
            mainTime += withContext(main) {
                timed { controller.commands.dispatch(Invocation(AddBlock, AddBlock.Input(type, eventIndex = 1, block = BlockDefinition.Log("Typed $index")))) }
            }
        }
        awaitHistorySize(controller, 1 + TYPING_COUNT)
        blockTypes.awaitWrites()
        report("$TYPING_COUNT quick changes to one Actor type, as typing makes (until written)", mainTime, System.nanoTime() - totalStart)

        var historySize = 1 + TYPING_COUNT
        BenchmarkProject.SCENE_SIZES.forEach { size ->
            totalStart = System.nanoTime()
            mainTime = withContext(main) {
                timed { controller.commands.dispatch(Invocation(MoveActor, MoveActor.Input(BenchmarkProject.sceneName(size), actorId = "a7", x = 1f, y = 2f))) }
            }
            awaitHistorySize(controller, ++historySize)
            blockTypes.awaitWrites()
            report("Move an Actor in a Scene of $size Actors (until written)", mainTime, System.nanoTime() - totalStart)
        }

        totalStart = System.nanoTime()
        mainTime = withContext(main) { timed { controller.commands.undo() } }
        awaitHistorySize(controller, historySize - 1)
        blockTypes.awaitWrites()
        report("Undo the move in the largest Scene (until written)", mainTime, System.nanoTime() - totalStart)

        totalStart = System.nanoTime()
        ProjectWatcher.fingerprint(directory, store.generatedPath)
        report("One pass of the file watcher", 0L, System.nanoTime() - totalStart)
        withContext(main) { controller.close() }.join()
    }

    private suspend fun awaitHistorySize(controller: WorkspaceController, size: Int) {
        controller.commands.history.first { it.undoable.size == size }
    }

    private inline fun <T> timed(onTime: (Long) -> Unit, block: () -> T): T {
        val start = System.nanoTime()
        return block().also { onTime(System.nanoTime() - start) }
    }

    private inline fun timed(block: () -> Unit): Long {
        val start = System.nanoTime()
        block()
        return System.nanoTime() - start
    }

    private fun report(step: String, mainThreadNanos: Long, totalNanos: Long) {
        println("BENCHMARK %-80s main thread %8.1f ms, total %8.1f ms".format(step, mainThreadNanos / 1e6, totalNanos / 1e6))
    }

    private companion object {
        const val TYPING_COUNT = 10
        const val TIMEOUT_IN_MILLISECONDS = 15 * 60 * 1000L
    }
}
