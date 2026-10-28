/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.creatorSession

import com.pandulapeter.kubriko.creatorSession.implementation.EditSession
import com.pandulapeter.kubriko.creatorSession.implementation.LogForwarder
import com.pandulapeter.kubriko.creatorSession.implementation.PlayCommandHandler
import com.pandulapeter.kubriko.creatorSession.implementation.PlaySession
import com.pandulapeter.kubriko.creatorSession.implementation.SessionArguments
import com.pandulapeter.kubriko.creatorSession.implementation.SessionConnection
import com.pandulapeter.kubriko.creatorSession.protocol.SessionMessage
import com.pandulapeter.kubriko.creatorSession.protocol.SessionProtocol
import kotlin.system.exitProcess

/**
 * The entry point of a session process, started by Kubriko Creator from a project's own desktop classpath.
 *
 * Kubriko Creator adds this module to that classpath itself, at the project's Kubriko version; projects never declare
 * it. The arguments are `edit <scene folder> [scene file]` or `play <main class>`.
 */
fun main(args: Array<String>) {
    val connection = SessionConnection.open(System.getProperty(SessionProtocol.PORT_PROPERTY)?.toIntOrNull())
    val failure = try {
        val arguments = SessionArguments.parse(args.toList())
        connection.send(SessionMessage.Hello(protocolVersion = SessionProtocol.VERSION, mode = arguments.mode))
        LogForwarder(connection::send).start()
        when (arguments) {
            is SessionArguments.Edit -> EditSession.run(arguments)
            is SessionArguments.Play -> {
                connection.listen(
                    onCommand = PlayCommandHandler(connection::send)::handle,
                    onUnknownCommand = { id -> connection.send(SessionMessage.CommandFailed(id, UNKNOWN_COMMAND_MESSAGE)) },
                )
                PlaySession.run(arguments)
            }
        }
        null
    } catch (exception: SessionFailure) {
        exception.message
    }
    if (failure != null) {
        connection.send(SessionMessage.Failure(failure))
        System.err.println(failure)
    }
    connection.close()
    if (failure != null) exitProcess(1)
}

private const val UNKNOWN_COMMAND_MESSAGE = "The game's Kubriko version is too old for this command; a newer one adds it."
