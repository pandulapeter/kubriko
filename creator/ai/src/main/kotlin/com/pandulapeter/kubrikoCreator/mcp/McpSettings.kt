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

import com.pandulapeter.kubrikoCreator.mcp.clients.AiClient
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * How Kubriko Creator serves its tools to AI clients. Stored in the settings folder, so it survives restarts.
 *
 * @param token What AI clients send as a bearer token. Anyone who has it can change the open project.
 * @param connectedClients The [com.pandulapeter.kubrikoCreator.mcp.clients.AiClientSetup.id]s of the AI tools Kubriko
 *   Creator connected, which it connects again when the port or token changes.
 * @param clientCommands The programs to run for AI tools whose command isn't the usual one, by [AiClient.id], so that a
 *   tool no longer listed doesn't make the settings unreadable.
 */
@Serializable
public data class McpSettings(
    @SerialName("isEnabled") val isEnabled: Boolean = false,
    @SerialName("port") val port: Int = DEFAULT_PORT,
    @SerialName("token") val token: String,
    @SerialName("connectedClients") val connectedClients: Set<String> = emptySet(),
    @SerialName("clientCommands") val clientCommands: Map<String, String> = emptyMap(),
) {
    /**
     * Where AI clients reach the server.
     */
    public val url: String get() = "http://${McpToolServer.HOST}:$port${McpToolServer.PATH}"

    /**
     * The [url] with the token in it, for clients that can't send headers.
     */
    public val urlWithToken: String get() = "$url?${McpToolServer.TOKEN_PARAMETER}=$token"

    /**
     * What starts [client]'s program: the command the user set for it, or its default.
     */
    public fun commandOf(client: AiClient): String = clientCommands[client.id] ?: client.defaultCommand

    public companion object {
        internal const val DEFAULT_PORT = 7781
        /**
         * The ports the server can be set to.
         */
        public val portRange: IntRange = 1024..65535
    }
}
