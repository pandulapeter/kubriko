/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.session

import com.pandulapeter.kubriko.creatorSession.protocol.SessionCommand
import com.pandulapeter.kubriko.creatorSession.protocol.SessionMessage
import kotlinx.coroutines.flow.Flow

/**
 * Where session processes connect to, and the messages they send.
 */
public interface SessionChannel : AutoCloseable {

    /**
     * The localhost port session processes are told to connect to.
     */
    public val port: Int

    /**
     * The messages of the current session.
     */
    public val messages: Flow<SessionMessage>

    /**
     * Starts waiting for the session of a game being launched: the first connection that says Hello after this becomes
     * the session, and connections after it are closed, until the next launch.
     */
    public fun expectSession()

    /**
     * Sends [command] to the session of the current launch.
     *
     * @return Whether there was a session to send it to.
     */
    public fun send(command: SessionCommand): Boolean
}
