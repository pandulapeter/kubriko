/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.session

import com.pandulapeter.kubrikoCreator.project.ProjectOutput
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runInterruptible
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.file.Files
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

/**
 * Plays a project's game in a process Kubriko Creator starts itself ([GameLaunch]), rather than through Gradle, whose
 * daemon would be busy for as long as the game runs.
 *
 * The game runs from a copy of the project's class files, made in [snapshotDirectory] and deleted once it ends: the
 * Scene editor builds the project again while the game plays, and a class the game loads later must be the version it
 * started with. Resources are read from where they are. A game started with Compose Hot Reload runs from Hot Reload's own
 * copy instead, which its reloads update.
 *
 * @param projectDirectory The project's folder: the classpath entries in it are the ones a build changes.
 */
internal class GameProcess(
    projectDirectory: File,
    private val snapshotDirectory: File,
) {
    private val projectDirectory = projectDirectory.canonicalFile

    /**
     * How the game ended.
     *
     * @param output The last lines the game printed.
     */
    data class Exit(
        val exitCode: Int,
        val output: List<String>,
    ) {
        val isSuccessful get() = exitCode == 0
    }

    /**
     * Plays [gameLaunch] with [systemProperties] until the game ends. Cancelling the calling coroutine stops the game, and
     * returns once it is gone.
     */
    suspend fun run(gameLaunch: GameLaunch, systemProperties: Map<String, String>): Exit = withContext(Dispatchers.IO) {
        val snapshot = Files.createTempDirectory(snapshotDirectory.apply { mkdirs() }.toPath(), "game").toFile()
        try {
            val classpath = if (gameLaunch.reloadTaskName == null) snapshotOf(gameLaunch.classpath, snapshot) else gameLaunch.classpath
            val classpathArgumentFile = File(snapshot, CLASSPATH_ARGUMENT_FILE_NAME).apply { writeText(GameLaunch.classpathArguments(classpath)) }
            val process = ProcessBuilder(gameLaunch.command(classpathArgumentFile, systemProperties))
                .directory(gameLaunch.workingDirectory)
                .redirectErrorStream(true)
                .start()
            LiveGameProcesses.add(process)
            try {
                val output = ArrayDeque<String>()
                coroutineScope {
                    launch {
                        process.inputStream.bufferedReader().useLines { lines ->
                            lines.forEach { line ->
                                output.addLast(line)
                                if (output.size > OUTPUT_TAIL_SIZE) output.removeFirst()
                            }
                        }
                    }
                    try {
                        runInterruptible { process.waitFor() }
                    } catch (exception: CancellationException) {
                        withContext(NonCancellable) { stop(process) }
                        throw exception
                    }
                }
                Exit(process.exitValue(), output.toList())
            } finally {
                LiveGameProcesses.remove(process)
            }
        } finally {
            snapshot.deleteRecursively()
        }
    }

    /**
     * [classpath] with the project's own entries copied into [snapshot]: a folder's class files go to a folder of their
     * own placed before it (so that its resources are still found), and a file is replaced by its copy.
     */
    internal fun snapshotOf(classpath: List<File>, snapshot: File) = classpath.flatMapIndexed { index, entry ->
        when {
            !entry.exists() || !ProjectOutput.isOwnUnderCanonicalRoot(entry, projectDirectory) -> listOf(entry)
            entry.isDirectory -> {
                val copy = File(snapshot, index.toString())
                entry.walkTopDown().filter { it.isFile && it.extension == "class" }.forEach { it.copyTo(File(copy, it.relativeTo(entry).path)) }
                listOf(copy, entry)
            }
            else -> listOf(entry.copyTo(File(File(snapshot, index.toString()), entry.name)))
        }
    }

    /**
     * Stops [process] and every process it started, which Hot Reload and a Gradle-run session leave behind otherwise,
     * since destroying a process doesn't reach its children. Takes at most [STOP_TIMEOUT_IN_MILLISECONDS] however many
     * processes there are, since they are all waited for against one deadline.
     */
    private fun stop(process: Process) {
        val processes = listOf(process.toHandle()) + process.descendants().toList()
        processes.forEach(ProcessHandle::destroy)
        if (!processes.awaitExit()) {
            processes.forEach(ProcessHandle::destroyForcibly)
            processes.awaitExit()
        }
    }

    private fun List<ProcessHandle>.awaitExit(): Boolean {
        val deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(STOP_WAIT_IN_MILLISECONDS)
        return all { handle ->
            try {
                handle.onExit().get(maxOf(deadline - System.nanoTime(), 0L), TimeUnit.NANOSECONDS)
                true
            } catch (_: TimeoutException) {
                false
            }
        }
    }

    companion object {
        private const val OUTPUT_TAIL_SIZE = 30
        private const val STOP_WAIT_IN_MILLISECONDS = 3_000L
        private const val CLASSPATH_ARGUMENT_FILE_NAME = "classpath.args"

        /**
         * How long stopping the game may take at most: a wait for it and every process it started to end, and another
         * after killing those that didn't.
         */
        const val STOP_TIMEOUT_IN_MILLISECONDS = 2 * STOP_WAIT_IN_MILLISECONDS
    }
}
