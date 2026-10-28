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

import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * Encoding of the [SessionMessage]s and [SessionCommand]s exchanged between a session process and Kubriko Creator.
 *
 * The session connects to Kubriko Creator over TCP on localhost, on the port passed in the [PORT_PROPERTY] system
 * property, and writes one encoded message per line. Kubriko Creator writes commands back on the same connection, one
 * per line.
 *
 * Sessions run at the game's own Kubriko version, so a session can be older or newer than Kubriko Creator. The protocol only
 * ever grows: messages and commands are added, never changed or removed, each side skips the lines it can't decode, and a
 * session answers a command it doesn't know with a [SessionMessage.CommandFailed]. Both sides use only what the lower of
 * their two [VERSION]s has.
 */
object SessionProtocol {

    /**
     * The protocol version this module speaks. Raised with every message or command added, so that it tells the other
     * side which ones exist.
     */
    const val VERSION = 3

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

    /**
     * Encodes [command] as a single line of JSON, without the line break.
     */
    fun encodeCommand(command: SessionCommand) = json.encodeToString(SessionCommand.serializer(), command)

    /**
     * Decodes a single line written by [encodeCommand].
     */
    fun decodeCommand(line: String) = json.decodeFromString(SessionCommand.serializer(), line)

    /**
     * The [SessionCommand.id] of a line written by [encodeCommand], also for commands [decodeCommand] doesn't know, or null.
     */
    internal fun decodeCommandId(line: String) = try {
        json.parseToJsonElement(line).jsonObject["id"]?.jsonPrimitive?.intOrNull
    } catch (_: SerializationException) {
        null
    } catch (_: IllegalArgumentException) {
        null
    }
}
