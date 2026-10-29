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

import com.pandulapeter.kubriko.creatorSession.protocol.SessionCommand
import com.pandulapeter.kubriko.creatorSession.protocol.SessionMessage
import com.pandulapeter.kubrikoCreator.commands.GameStep
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
import java.util.Base64

/**
 * Builds and plays the project for the AI's action tools.
 */
internal class WorkspaceRunner(
    private val sessions: SessionController,
    private val startSession: (firstScene: String?) -> Unit = sessions::play,
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
        val hasStarted = startAndAwait()
        if (hasStarted) {
            delay(durationInMilliseconds)
            stopAndAwait()
        }
        hasStarted
    }

    override suspend fun startGame(firstScene: String?) = recordConsole {
        startAndAwait(firstScene).also { hasStarted ->
            if (hasStarted) {
                sessions.request(ANSWER_TIMEOUT, create = SessionCommand::BringToFront)
                // So that a screenshot taken right away shows the first Scene; a game that can't tell starts all the same.
                sessions.request(ACTORS_TIMEOUT + ANSWER_TIMEOUT, SessionController.AWAIT_ACTORS_DRAWN_PROTOCOL_VERSION) { id ->
                    SessionCommand.AwaitActorsDrawn(id, ACTORS_TIMEOUT)
                }
            }
        }
    }

    override suspend fun stopGame() = recordConsole { stopAndAwait() }

    override suspend fun pressKeys(keys: List<String>, durationInMilliseconds: Long) = step {
        sessions.request(durationInMilliseconds + ANSWER_TIMEOUT) { id -> SessionCommand.PressKeys(id, keys, durationInMilliseconds) }
    }

    override suspend fun wait(durationInMilliseconds: Long) = step { answer ->
        delay(durationInMilliseconds)
        answer
    }

    override suspend fun captureScreen() = step {
        sessions.request(ANSWER_TIMEOUT) { id -> SessionCommand.CaptureScreen(id, SCREENSHOT_WIDTH) }
    }

    /**
     * Brings the game to the front (Kubriko pauses games whose window isn't focused), does [action] with the answer
     * to that, and turns the final answer into a [GameStep] with the console lines of the time it took.
     */
    private suspend fun step(action: suspend (GameAnswer) -> GameAnswer): GameStep {
        val (answer, console) = this.console.record {
            when (val focused = sessions.request(ANSWER_TIMEOUT, create = SessionCommand::BringToFront)) {
                is GameAnswer.Answered -> if (focused.message is SessionMessage.CommandFailed) focused else action(focused)
                GameAnswer.NotPlaying, GameAnswer.Unsupported, GameAnswer.NoAnswer -> focused
            }
        }
        return when (answer) {
            is GameAnswer.Answered -> when (val message = answer.message) {
                is SessionMessage.CommandFailed -> GameStep.Failed(message.message)
                is SessionMessage.Screenshot -> GameStep.Done(console, png = Base64.getDecoder().decode(message.png))
                else -> GameStep.Done(console)
            }
            GameAnswer.NotPlaying -> GameStep.NotPlaying
            GameAnswer.Unsupported -> GameStep.Unsupported
            GameAnswer.NoAnswer -> GameStep.NoAnswer
        }
    }

    /**
     * Starts the game, replacing a session that was running, and waits until it is up or has ended.
     *
     * @return Whether it is up.
     */
    private suspend fun startAndAwait(firstScene: String? = null): Boolean {
        stopAndAwait()
        startSession(firstScene)
        val session = checkNotNull(sessions.job)
        return merge(
            sessions.state.filterIsInstance<SessionState.Running>().map { true },
            flow {
                session.join()
                emit(false)
            },
        ).first()
    }

    /**
     * @return Whether a session was running.
     */
    private suspend fun stopAndAwait(): Boolean {
        val session = sessions.job?.takeIf { it.isActive } ?: return false
        sessions.stop()
        session.join()
        return true
    }

    private suspend fun recordConsole(action: suspend () -> Boolean) = console.record(action).let { (isSuccessful, entries) ->
        ProjectRunner.RunResult(isSuccessful, entries)
    }

    private companion object {
        const val ANSWER_TIMEOUT = 10_000L
        const val ACTORS_TIMEOUT = 5_000L
        const val SCREENSHOT_WIDTH = 1280
    }
}
