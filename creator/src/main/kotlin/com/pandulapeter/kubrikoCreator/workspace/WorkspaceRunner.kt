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

import com.pandulapeter.kubriko.creatorSession.protocol.SessionMode
import com.pandulapeter.kubrikoCreator.commands.ProjectRunner
import com.pandulapeter.kubrikoCreator.session.SessionInitScript
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Builds and plays the project for the AI's action tools.
 */
internal class WorkspaceRunner(
    private val sessions: SessionController,
    private val runTask: suspend (taskName: String, properties: Map<String, String>) -> Boolean,
    private val console: ConsoleLog,
    private val ioDispatcher: CoroutineDispatcher,
) : ProjectRunner {

    override suspend fun build() = recordConsole {
        val classpathFile = withContext(ioDispatcher) { File.createTempFile("kubriko-creator-classpath", ".txt") }
        try {
            runTask(
                SessionInitScript.CLASSPATH_TASK_NAME,
                mapOf("kubrikoCreatorMode" to "classpath", "kubrikoCreatorClasspathFile" to classpathFile.absolutePath),
            )
        } finally {
            withContext(ioDispatcher) { classpathFile.delete() }
        }
    }

    override suspend fun play(durationInMilliseconds: Long) = recordConsole {
        sessions.job?.let { previous ->
            sessions.stop()
            previous.join()
        }
        sessions.start(SessionMode.PLAY, scene = null)
        val session = checkNotNull(sessions.job)
        val hasStarted = merge(
            sessions.state.filterIsInstance<SessionState.Running>().map { true },
            flow {
                session.join()
                emit(false)
            },
        ).first()
        if (hasStarted) {
            delay(durationInMilliseconds)
            sessions.stop()
            session.join()
        }
        hasStarted
    }

    private suspend fun recordConsole(action: suspend () -> Boolean) = console.record(action).let { (isSuccessful, entries) ->
        ProjectRunner.RunResult(isSuccessful, entries)
    }
}
