/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.creatorSession.protocol

import kotlinx.serialization.json.Json

/**
 * Encoding of the [SessionMessage]s exchanged between a session process and Kubriko Creator.
 *
 * The session connects to Kubriko Creator over TCP on localhost, on the port passed in the [PORT_PROPERTY] system
 * property, and writes one encoded message per line.
 */
object SessionProtocol {

    /**
     * The protocol version this module speaks. Raised whenever a change would make an older Kubriko Creator misread
     * the messages.
     */
    const val VERSION = 1

    /**
     * The system property that holds the localhost port Kubriko Creator listens on. Without it, the session runs
     * without a connection.
     */
    const val PORT_PROPERTY = "kubriko.creator.port"

    private val json = Json {
        ignoreUnknownKeys = true
    }

    /**
     * Encodes [message] as a single line of JSON, without the line break.
     */
    fun encode(message: SessionMessage) = json.encodeToString(SessionMessage.serializer(), message)

    /**
     * Decodes a single line written by [encode].
     */
    fun decode(line: String) = json.decodeFromString(SessionMessage.serializer(), line)
}
