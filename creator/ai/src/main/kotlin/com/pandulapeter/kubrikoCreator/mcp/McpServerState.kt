/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.mcp

/**
 * Whether AI clients can reach Kubriko Creator.
 */
public sealed interface McpServerState {

    /**
     * The server isn't running.
     */
    public data object Off : McpServerState

    /**
     * The server is starting.
     */
    public data object Starting : McpServerState

    /**
     * The server takes calls on [port].
     */
    public data class Running(
        val port: Int,
    ) : McpServerState

    /**
     * The server couldn't start on [port], which is usually taken by another program.
     */
    public data class Failed(
        val port: Int,
    ) : McpServerState
}
