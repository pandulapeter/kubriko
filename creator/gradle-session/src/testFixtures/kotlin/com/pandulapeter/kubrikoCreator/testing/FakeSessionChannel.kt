/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.testing

import com.pandulapeter.kubriko.creatorSession.protocol.SessionCommand
import com.pandulapeter.kubriko.creatorSession.protocol.SessionMessage
import com.pandulapeter.kubrikoCreator.session.SessionChannel
import kotlinx.coroutines.flow.MutableSharedFlow

/**
 * A [SessionChannel] whose session the test plays: it records what is sent, and answers each command as [answer] says.
 */
public class FakeSessionChannel : SessionChannel {
    override val port: Int = 4242
    override val messages: MutableSharedFlow<SessionMessage> = MutableSharedFlow(extraBufferCapacity = 16)

    /**
     * Whether the channel was closed.
     */
    public var isClosed: Boolean = false
        private set

    /**
     * Every command sent while a session was connected, in order.
     */
    public val sent: MutableList<SessionCommand> = mutableListOf()

    /**
     * How the fake session answers each command, or null to leave it unanswered.
     */
    public var answer: (SessionCommand) -> SessionMessage? = { SessionMessage.CommandDone(it.id) }

    /**
     * Whether a session is connected, which [send] needs.
     */
    public var isConnected: Boolean = true

    /**
     * How many times [expectSession] was called.
     */
    public var expectedSessions: Int = 0
        private set

    /**
     * Emits [message] as if the session had sent it.
     */
    public fun receive(message: SessionMessage): Unit = check(messages.tryEmit(message))

    override fun expectSession() {
        expectedSessions++
    }

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
