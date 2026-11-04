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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Runs Kubriko Creator's init script tasks in the game's module, reporting how they went in the console.
 *
 * @param awaitWrites Returns once every change made so far is written, which a build waits for, or writing them failed; what
 *   isn't on disk then, which the build goes ahead without, or null if everything is.
 */
internal class CreatorTaskRunner(
    private val rootDirectory: File,
    private val build: ProjectBuild,
    private val gameModule: GameModuleChoice,
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
    private val _runningBuilds = MutableStateFlow(0)

    /**
     * How many builds the console reports are running or waiting for another, so the user can tell the game is being
     * built: neither the silent warm-up nor a play session's build, which lasts as long as the game does, counts.
     */
    val runningBuilds: StateFlow<Int> = _runningBuilds.asStateFlow()

    /**
     * Runs one of the init script's tasks in the game's module with Kubriko Creator's [properties], reporting
     * compiler errors (or, when the build reports none, the end of its output) in the console.
     *
     * @return Whether the build succeeded.
     */
    suspend fun run(taskName: String, properties: Map<String, String>): Boolean = runBuild(
        properties = properties,
        isSession = taskName == SessionInitScript.SESSION_TASK_NAME,
        reportsTime = taskName == SessionInitScript.CLASSPATH_TASK_NAME,
    ) { modulePath -> taskPathOf(modulePath, taskName) }

    /**
     * Runs the task at [taskPath] (in any of the project's modules), as [run] does.
     */
    suspend fun runAt(taskPath: String): Boolean = runBuild(properties = emptyMap(), isSession = false, reportsTime = false) { taskPath }

    /**
     * Runs a build Kubriko Creator doesn't run through Gradle itself (Xcode's, whose build phase runs Gradle), as [run]
     * runs its own: once the changes made so far are written, and never alongside another build of the classpath.
     */
    suspend fun <T> runOutside(build: suspend () -> T): T = counted {
        startBuild()
        classpathBuildLock.withLock { build() }
    }

    private suspend fun <T> counted(block: suspend () -> T): T {
        _runningBuilds.update { it + 1 }
        try {
            return block()
        } finally {
            _runningBuilds.update { it - 1 }
        }
    }

    private suspend fun startBuild() {
        console.append(ConsoleEntry.Status(WorkspaceStatus.BUILDING))
        awaitWrites()?.let { unsaved ->
            console.append(
                when (val subjects = unsaved.subjects) {
                    null -> ConsoleEntry.Status(WorkspaceStatus.BUILDING_WITH_UNWRITTEN_GENERATED_CODE, isError = true, arguments = listOf(unsaved.reason))
                    else -> ConsoleEntry.Status(WorkspaceStatus.BUILDING_WITH_UNSAVED_TYPES, isError = true, arguments = listOf(subjects, unsaved.reason))
                },
            )
        }
    }

    private suspend fun runBuild(properties: Map<String, String>, isSession: Boolean, reportsTime: Boolean, taskPathIn: (modulePath: String) -> String): Boolean =
        if (isSession) buildWith(properties, isSession = true, reportsTime, taskPathIn) else counted { buildWith(properties, isSession = false, reportsTime, taskPathIn) }

    private suspend fun buildWith(properties: Map<String, String>, isSession: Boolean, reportsTime: Boolean, taskPathIn: (modulePath: String) -> String): Boolean {
        startBuild()
        val modulePath = try {
            gameModule.await()
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
        val outputTail = OutputTail()
        val hasReportedProblems = AtomicBoolean(false)
        suspend fun runBuild() = incrementalCompilation.build { mode ->
            startTime = currentTimeMillis()
            build.run(
                taskPath = taskPathIn(modulePath),
                arguments = SessionInitScript.gradleArguments(properties, mode),
                onEvent = { event ->
                    when (event) {
                        is GradleBuildEvent.Output -> outputTail.add(event.line)
                        is GradleBuildEvent.Problem -> {
                            hasReportedProblems.set(true)
                            console.append(event.toConsoleEntry())
                        }
                    }
                },
            )
        }
        val isSuccessful = if (!isSession) {
            classpathBuildLock.withLock { runBuild() }
        } else {
            // A session's build lasts as long as the game runs, so it only waits for the classpath build before it.
            classpathBuildLock.withLock {}
            runBuild()
        }
        if (!isSuccessful) {
            console.append(ConsoleEntry.Status(WorkspaceStatus.BUILD_FAILED, isError = true))
            if (!hasReportedProblems.get()) outputTail.toList().forEach { console.append(ConsoleEntry.BuildOutput(it)) }
        }
        if (isSuccessful && reportsTime) {
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
            gameModule.await()
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
}
