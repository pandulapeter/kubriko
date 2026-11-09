/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.gradle

import org.gradle.tooling.BuildCancelledException
import org.gradle.tooling.BuildException
import org.gradle.tooling.GradleConnectionException
import org.gradle.tooling.GradleConnector
import org.gradle.tooling.events.OperationType
import org.gradle.tooling.events.problems.LineInFileLocation
import org.gradle.tooling.events.problems.Severity
import org.gradle.tooling.events.problems.SingleProblemEvent
import java.io.File

/**
 * A long-lived connection to a project's Gradle build, through the Gradle Tooling API, made on first use, so that
 * creating one costs nothing.
 */
public class GradleProjectConnection(
    internal val rootDirectory: File,
) : ProjectBuild {
    private val lazyConnection = lazy { GradleConnector.newConnector().forProjectDirectory(rootDirectory).connect() }
    private val connection by lazyConnection

    internal suspend fun <T> model(type: Class<T>): T {
        val cancellation = GradleConnector.newCancellationTokenSource()
        val outcome = ToolingOutcome<T>(cancel = cancellation::cancel)
        connection.model(type).withCancellationToken(cancellation.token()).get(outcome)
        return outcome.await()
    }

    override suspend fun run(
        taskPath: String,
        arguments: List<String>,
        onEvent: (GradleBuildEvent) -> Unit,
    ): Boolean {
        val cancellation = GradleConnector.newCancellationTokenSource()
        // Until Gradle stops a cancelled build, its daemon is busy, and the next build would start another one.
        val outcome = ToolingOutcome<Void?>(cancel = cancellation::cancel)
        connection.newBuild()
            .forTasks(taskPath)
            .withArguments(arguments)
            .withCancellationToken(cancellation.token())
            .setStandardOutput(LineOutputStream { onEvent(GradleBuildEvent.Output(it, isError = false)) })
            .setStandardError(LineOutputStream { onEvent(GradleBuildEvent.Output(it, isError = true)) })
            .addProgressListener({ event ->
                if (event is SingleProblemEvent) {
                    event.toBuildEvent()?.let(onEvent)
                }
            }, setOf(OperationType.PROBLEMS))
            .run(outcome)
        return try {
            outcome.await()
            true
        } catch (failure: GradleConnectionException) {
            // A failed build has printed why, but Gradle failing to start (offline, an unusable JDK) prints nothing.
            if (failure !is BuildException && failure !is BuildCancelledException) {
                val cause = generateSequence<Throwable>(failure) { it.cause }.last()
                onEvent(GradleBuildEvent.Output(cause.message ?: cause.toString(), isError = true))
            }
            false
        }
    }

    override fun close() {
        if (lazyConnection.isInitialized()) connection.close()
    }

    private fun SingleProblemEvent.toBuildEvent(): GradleBuildEvent.Problem? {
        if (problem.definition.severity != Severity.ERROR) return null
        val location = problem.originLocations.filterIsInstance<LineInFileLocation>().firstOrNull()
        return GradleBuildEvent.Problem(
            message = problemMessageOf(
                details = problem.details?.details,
                contextualLabel = problem.contextualLabel?.contextualLabel,
                displayName = problem.definition.id.displayName,
            ),
            file = location?.path?.let(::File),
            line = location?.line,
            column = location?.column,
        )
    }
}
