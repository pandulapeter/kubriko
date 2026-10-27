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
import com.pandulapeter.kubrikoCreator.session.SessionChannel
import kotlinx.coroutines.flow.MutableSharedFlow

internal class FakeSessionChannel : SessionChannel {
    override val port = 4242
    override val messages = MutableSharedFlow<SessionMessage>(extraBufferCapacity = 16)
    var isClosed = false
        private set

    val sent = mutableListOf<SessionCommand>()

    /**
     * How the fake session answers each command, or null to leave it unanswered.
     */
    var answer: (SessionCommand) -> SessionMessage? = { SessionMessage.CommandDone(it.id) }

    var isConnected = true

    fun receive(message: SessionMessage) = check(messages.tryEmit(message))

    override fun send(command: SessionCommand): Boolean {
        if (!isConnected) return false
        sent += command
        answer(command)?.let(::receive)
        return true
    }

    override fun close() {
        isClosed = true
    }
}
