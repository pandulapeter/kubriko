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

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * A message the session process sends to Kubriko Creator, one JSON object per line (see [SessionProtocol]).
 *
 * This is the wire format between the two processes, not an API for game code.
 */
@Serializable
sealed interface SessionMessage {

    /**
     * The first message of every session.
     *
     * @param protocolVersion The [SessionProtocol.VERSION] the session speaks, which tells Kubriko Creator which commands it takes.
     * @param mode The mode the session was started in.
     */
    @Serializable
    @SerialName("hello")
    data class Hello(
        val protocolVersion: Int,
        val mode: SessionMode,
    ) : SessionMessage

    /**
     * A [com.pandulapeter.kubriko.logger.Logger] entry from the session process.
     *
     * @param id The entry's unique ID, so that Kubriko Creator can discard duplicates.
     * @param message The logged message.
     * @param details Optional details of the entry.
     * @param source The name of the component that logged the entry, if any.
     * @param timestamp The time of logging, in milliseconds since the epoch.
     * @param importance The name of the entry's [com.pandulapeter.kubriko.logger.Logger.Importance].
     */
    @Serializable
    @SerialName("log")
    data class Log(
        val id: String,
        val message: String,
        val details: String?,
        val source: String?,
        val timestamp: Long,
        val importance: String,
    ) : SessionMessage

    /**
     * The session could not do what it was started for, and is about to exit.
     *
     * @param message A human-readable description of the problem, in English.
     */
    @Serializable
    @SerialName("failure")
    data class Failure(
        val message: String,
    ) : SessionMessage

    /**
     * The [SessionCommand] with [id] was carried out. Added in protocol version 2.
     */
    @Serializable
    @SerialName("commandDone")
    data class CommandDone(
        val id: Int,
    ) : SessionMessage

    /**
     * The answer to a [SessionCommand.CaptureScreen]. Added in protocol version 2.
     *
     * @param png The captured image, PNG-encoded, in Base64.
     */
    @Serializable
    @SerialName("screenshot")
    data class Screenshot(
        val id: Int,
        val png: String,
    ) : SessionMessage

    /**
     * The [SessionCommand] with [id] couldn't be carried out. Added in protocol version 2.
     *
     * @param message A human-readable description of the problem, in English.
     */
    @Serializable
    @SerialName("commandFailed")
    data class CommandFailed(
        val id: Int,
        val message: String,
    ) : SessionMessage
}
