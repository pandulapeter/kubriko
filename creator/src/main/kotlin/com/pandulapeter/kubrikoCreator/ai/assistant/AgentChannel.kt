/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.ai.assistant

import java.io.InputStream
import java.io.OutputStream

/**
 * A running agent's standard streams and its exit.
 */
internal interface AgentChannel {

    /**
     * What the agent writes to its standard output: newline-delimited JSON-RPC.
     */
    val fromAgent: InputStream

    /**
     * The agent's standard input.
     */
    val toAgent: OutputStream

    /**
     * The agent's standard error, which never carries the protocol.
     */
    val errors: InputStream

    /**
     * Suspends until the agent has exited.
     *
     * @return Its exit code.
     */
    suspend fun awaitExit(): Int

    /**
     * Ends the agent, forcibly if it doesn't end on its own soon. Blocks until it is gone.
     */
    fun stop()
}

/**
 * Starts agents.
 */
internal fun interface AgentLauncher {

    /**
     * @throws java.io.IOException If the agent can't be started.
     */
    fun launch(command: AgentCommand): AgentChannel
}
