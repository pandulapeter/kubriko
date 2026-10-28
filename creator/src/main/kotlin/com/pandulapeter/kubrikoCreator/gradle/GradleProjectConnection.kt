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

import kotlinx.coroutines.suspendCancellableCoroutine
import org.gradle.tooling.GradleConnectionException
import org.gradle.tooling.GradleConnector
import org.gradle.tooling.ResultHandler
import org.gradle.tooling.events.OperationType
import org.gradle.tooling.events.problems.LineInFileLocation
import org.gradle.tooling.events.problems.Severity
import org.gradle.tooling.events.problems.SingleProblemEvent
import java.io.File
import kotlin.coroutines.resume

/**
 * A long-lived connection to a project's Gradle build, through the Gradle Tooling API, made on first use, so that
 * creating one costs nothing.
 */
internal class GradleProjectConnection(
    val rootDirectory: File,
) : ProjectBuild {
    private val lazyConnection = lazy { GradleConnector.newConnector().forProjectDirectory(rootDirectory).connect() }
    private val connection by lazyConnection

    fun <T> model(type: Class<T>): T = connection.getModel(type)

    override suspend fun run(
        taskPath: String,
        arguments: List<String>,
        onEvent: (GradleBuildEvent) -> Unit,
    ): Boolean {
        val cancellation = GradleConnector.newCancellationTokenSource()
        return suspendCancellableCoroutine { continuation ->
            continuation.invokeOnCancellation { cancellation.cancel() }
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
                .run(object : ResultHandler<Void?> {
                    override fun onComplete(result: Void?) = continuation.resume(true)

                    override fun onFailure(failure: GradleConnectionException) = continuation.resume(false)
                })
        }
    }

    override fun close() {
        if (lazyConnection.isInitialized()) connection.close()
    }

    private fun SingleProblemEvent.toBuildEvent(): GradleBuildEvent.Problem? {
        if (problem.definition.severity != Severity.ERROR) return null
        val location = problem.originLocations.filterIsInstance<LineInFileLocation>().firstOrNull()
        return GradleBuildEvent.Problem(
            message = problem.contextualLabel?.contextualLabel ?: problem.definition.id.displayName,
            file = location?.path?.let(::File),
            line = location?.line,
            column = location?.column,
        )
    }
}
