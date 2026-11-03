/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.workspace

import com.pandulapeter.kubrikoCreator.actors.store.UnsavedWrites
import com.pandulapeter.kubrikoCreator.console.ConsoleEntry
import com.pandulapeter.kubrikoCreator.gradle.GradleBuildEvent
import com.pandulapeter.kubrikoCreator.gradle.IncrementalCompilationPolicy
import com.pandulapeter.kubrikoCreator.gradle.ProjectBuild
import com.pandulapeter.kubrikoCreator.gradle.ProjectInspectionException
import com.pandulapeter.kubrikoCreator.project.ProjectProblem
import com.pandulapeter.kubrikoCreator.session.SessionInitScript
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Runs Kubriko Creator's init script tasks in the project's desktop module, reporting how they went in the console.
 *
 * @param awaitWrites Returns once every change made so far is written, which a build waits for, or writing them failed; what
 *   isn't on disk then, which the build goes ahead without, or null if everything is.
 */
internal class CreatorTaskRunner(
    private val rootDirectory: File,
    private val build: ProjectBuild,
    private val desktopModulePath: DesktopModuleLookup,
    private val incrementalCompilation: IncrementalCompilationPolicy,
    private val console: ConsoleLog,
    private val awaitWrites: suspend () -> UnsavedWrites?,
    private val currentTimeMillis: () -> Long,
) {
    /**
     * Keeps builds of the classpath from overlapping each other or the start of a session, since a build that starts
     * while another one runs needs a Gradle daemon of its own.
     */
    private val classpathBuildLock = Mutex()

    /**
     * Runs one of the init script's tasks in the project's desktop module with Kubriko Creator's [properties], reporting
     * compiler errors (or, when the build reports none, the end of its output) in the console.
     *
     * @return Whether the build succeeded.
     */
    suspend fun run(taskName: String, properties: Map<String, String>): Boolean {
        console.append(ConsoleEntry.Status(WorkspaceStatus.BUILDING))
        awaitWrites()?.let { unsaved ->
            console.append(
                when (val subjects = unsaved.subjects) {
                    null -> ConsoleEntry.Status(WorkspaceStatus.BUILDING_WITH_UNWRITTEN_GENERATED_CODE, isError = true, arguments = listOf(unsaved.reason))
                    else -> ConsoleEntry.Status(WorkspaceStatus.BUILDING_WITH_UNSAVED_TYPES, isError = true, arguments = listOf(subjects, unsaved.reason))
                },
            )
        }
        val modulePath = try {
            desktopModulePath.await()
        } catch (exception: ProjectInspectionException) {
            console.append(ConsoleEntry.Status(exception.reason, isError = true))
            return false
        } catch (exception: CancellationException) {
            throw exception
        } catch (_: Exception) {
            console.append(ConsoleEntry.Status(ProjectProblem.NOT_A_GRADLE_PROJECT, isError = true))
            return false
        }
        var startTime = 0L
        val outputTail = ArrayDeque<String>()
        val hasReportedProblems = AtomicBoolean(false)
        suspend fun runBuild() = incrementalCompilation.build { mode ->
            startTime = currentTimeMillis()
            build.run(
                taskPath = taskPathOf(modulePath, taskName),
                arguments = SessionInitScript.gradleArguments(properties, mode),
                onEvent = { event ->
                    when (event) {
                        is GradleBuildEvent.Output -> synchronized(outputTail) {
                            outputTail.addLast(event.line)
                            if (outputTail.size > OUTPUT_TAIL_SIZE) outputTail.removeFirst()
                        }
                        is GradleBuildEvent.Problem -> {
                            hasReportedProblems.set(true)
                            console.append(event.toConsoleEntry())
                        }
                    }
                },
            )
        }
        val isSuccessful = if (taskName != SessionInitScript.SESSION_TASK_NAME) {
            classpathBuildLock.withLock { runBuild() }
        } else {
            // A session's build lasts as long as the game runs, so it only waits for the classpath build before it.
            classpathBuildLock.withLock {}
            runBuild()
        }
        if (!isSuccessful) {
            console.append(ConsoleEntry.Status(WorkspaceStatus.BUILD_FAILED, isError = true))
            if (!hasReportedProblems.get()) synchronized(outputTail) { outputTail.toList() }.forEach { console.append(ConsoleEntry.BuildOutput(it)) }
        }
        if (isSuccessful && taskName == SessionInitScript.CLASSPATH_TASK_NAME) {
            console.append(ConsoleEntry.Status(WorkspaceStatus.BUILT, arguments = listOf(secondsSince(startTime))))
        }
        return isSuccessful
    }

    /**
     * Compiles one generated file next to the project's code, without a word in the console, so that Kotlin's compile
     * daemon runs and is warm before the first change: the build at opening starts it only when something needed
     * compiling, and a cold one makes the first change take seconds longer.
     */
    suspend fun warmUpCompiler() {
        val modulePath = try {
            desktopModulePath.await()
        } catch (exception: CancellationException) {
            throw exception
        } catch (_: Exception) {
            return
        }
        classpathBuildLock.withLock {
            build.run(
                taskPath = taskPathOf(modulePath, SessionInitScript.WARM_UP_TASK_NAME),
                arguments = SessionInitScript.gradleArguments(mapOf("kubrikoCreatorMode" to "warmUp"), incrementalCompilation.incrementalMode()),
                onEvent = {},
            )
        }
    }

    private fun taskPathOf(modulePath: String, taskName: String) = if (modulePath == ":") ":$taskName" else "$modulePath:$taskName"

    private fun GradleBuildEvent.Problem.toConsoleEntry() = ConsoleEntry.Problem(
        message = message,
        location = file?.let { file ->
            listOfNotNull(file.relativeToOrSelf(rootDirectory).invariantSeparatorsPath, line, column).joinToString(":")
        },
    )

    private fun secondsSince(startTime: Long) = ((currentTimeMillis() - startTime) / 100 / 10f).toString()

    private companion object {
        const val OUTPUT_TAIL_SIZE = 30
    }
}
