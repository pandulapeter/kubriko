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
import com.pandulapeter.kubriko.creatorSession.protocol.SessionMode
import com.pandulapeter.kubriko.creatorSession.protocol.SessionProtocol
import com.pandulapeter.kubrikoCreator.session.SessionChannel
import com.pandulapeter.kubrikoCreator.session.SessionInitScript
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import kubriko.creator.generated.resources.Res
import kubriko.creator.generated.resources.status_session_ended
import kubriko.creator.generated.resources.status_session_started
import kubriko.creator.generated.resources.status_session_stopped
import kubriko.creator.generated.resources.status_unsupported_protocol
import java.util.concurrent.atomic.AtomicInteger

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
    private var protocolVersion = SessionProtocol.VERSION
    private val commandIds = AtomicInteger()

    init {
        scope.launch {
            channel.messages.collect(::onMessage)
        }
    }

    fun play() {
        job?.cancel()
        job = scope.launch {
            _state.value = SessionState.Starting(SessionMode.PLAY)
            try {
                val isSuccessful = runTask(
                    SessionInitScript.SESSION_TASK_NAME,
                    mapOf("kubrikoCreatorPort" to channel.port.toString(), "kubrikoCreatorMode" to "play"),
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
        is SessionMessage.Hello -> if (message.protocolVersion >= MINIMUM_PROTOCOL_VERSION) {
            protocolVersion = minOf(message.protocolVersion, SessionProtocol.VERSION)
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

    companion object {
        private const val MINIMUM_PROTOCOL_VERSION = 1

        /**
         * The first protocol version whose sessions take [SessionCommand]s.
         */
        private const val COMMANDS_PROTOCOL_VERSION = 2

        /**
         * The first protocol version whose sessions take [SessionCommand.AwaitActorsDrawn].
         */
        const val AWAIT_ACTORS_DRAWN_PROTOCOL_VERSION = 3
    }

    /**
     * Sends the command [create] makes with a new ID to the running game, and waits for its answer. Sessions older than
     * [minimumProtocolVersion] don't take the command, and get [GameAnswer.Unsupported] instead.
     */
    suspend fun request(
        timeoutInMilliseconds: Long,
        minimumProtocolVersion: Int = COMMANDS_PROTOCOL_VERSION,
        create: (id: Int) -> SessionCommand,
    ): GameAnswer {
        if (_state.value != SessionState.Running(SessionMode.PLAY)) return GameAnswer.NotPlaying
        if (protocolVersion < maxOf(minimumProtocolVersion, COMMANDS_PROTOCOL_VERSION)) return GameAnswer.Unsupported
        val command = create(commandIds.incrementAndGet())
        return coroutineScope {
            val answer = async(start = CoroutineStart.UNDISPATCHED) { channel.messages.first { it.answers(command.id) } }
            if (!channel.send(command)) {
                answer.cancel()
                return@coroutineScope GameAnswer.NotPlaying
            }
            withTimeoutOrNull(timeoutInMilliseconds) { GameAnswer.Answered(answer.await()) } ?: GameAnswer.NoAnswer.also { answer.cancel() }
        }
    }

    private fun SessionMessage.answers(id: Int) = when (this) {
        is SessionMessage.CommandDone -> this.id == id
        is SessionMessage.Screenshot -> this.id == id
        is SessionMessage.CommandFailed -> this.id == id
        is SessionMessage.Hello, is SessionMessage.Log, is SessionMessage.Failure -> false
    }

    override fun close() {
        job?.cancel()
        channel.close()
    }
}
