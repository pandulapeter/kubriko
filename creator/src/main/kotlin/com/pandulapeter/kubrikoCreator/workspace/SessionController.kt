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

import com.pandulapeter.kubriko.creatorSession.protocol.SessionMessage
import com.pandulapeter.kubriko.creatorSession.protocol.SessionMode
import com.pandulapeter.kubriko.creatorSession.protocol.SessionProtocol
import com.pandulapeter.kubrikoCreator.project.ProjectResource
import com.pandulapeter.kubrikoCreator.session.SessionChannel
import com.pandulapeter.kubrikoCreator.session.SessionInitScript
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kubriko.creator.generated.resources.Res
import kubriko.creator.generated.resources.status_session_ended
import kubriko.creator.generated.resources.status_session_started
import kubriko.creator.generated.resources.status_session_stopped
import kubriko.creator.generated.resources.status_unsupported_protocol

/**
 * The session process that plays the game or edits a Scene in a window of its own: starting and stopping it, and what
 * it reports.
 */
internal class SessionController(
    private val channel: SessionChannel,
    private val runTask: suspend (taskName: String, properties: Map<String, String>) -> Boolean,
    private val console: ConsoleLog,
    private val scope: CoroutineScope,
) : AutoCloseable {
    private val _state = MutableStateFlow<SessionState>(SessionState.Idle)
    val state = _state.asStateFlow()

    /**
     * The coroutine that runs the current session, which completes when it ends.
     */
    var job: Job? = null
        private set

    init {
        scope.launch {
            channel.messages.collect(::onMessage)
        }
    }

    fun start(mode: SessionMode, scene: ProjectResource?) {
        job?.cancel()
        job = scope.launch {
            _state.value = SessionState.Starting(mode)
            try {
                val isSuccessful = runTask(
                    SessionInitScript.SESSION_TASK_NAME,
                    buildMap {
                        put("kubrikoCreatorPort", channel.port.toString())
                        when (mode) {
                            SessionMode.PLAY -> put("kubrikoCreatorMode", "play")
                            SessionMode.EDIT -> {
                                put("kubrikoCreatorMode", "edit")
                                scene?.let {
                                    put("kubrikoCreatorSceneFolder", it.file.parentFile.absolutePath)
                                    put("kubrikoCreatorSceneFile", it.name)
                                }
                            }
                        }
                    },
                )
                if (isSuccessful) console.append(ConsoleEntry.Status(Res.string.status_session_ended))
            } finally {
                _state.value = SessionState.Idle
            }
        }
    }

    fun stop() {
        if (job?.isActive == true) {
            job?.cancel()
            console.append(ConsoleEntry.Status(Res.string.status_session_stopped))
        }
    }

    private fun onMessage(message: SessionMessage) = when (message) {
        is SessionMessage.Hello -> if (message.protocolVersion == SessionProtocol.VERSION) {
            _state.value = SessionState.Running(message.mode)
            console.append(ConsoleEntry.Status(Res.string.status_session_started))
        } else {
            console.append(ConsoleEntry.Status(Res.string.status_unsupported_protocol, isError = true))
            stop()
        }
        is SessionMessage.Log -> console.append(
            ConsoleEntry.Log(
                message = message.message,
                source = message.source,
                isImportant = message.importance == "HIGH",
            ),
        )
        is SessionMessage.Failure -> console.append(ConsoleEntry.SessionFailure(message.message))
        is SessionMessage.CommandDone, is SessionMessage.Screenshot, is SessionMessage.CommandFailed -> Unit
    }

    override fun close() {
        job?.cancel()
        channel.close()
    }
}
