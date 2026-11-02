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

import com.pandulapeter.kubriko.creatorSession.implementation.LogForwarder
import com.pandulapeter.kubriko.creatorSession.implementation.PlayCommandHandler
import com.pandulapeter.kubriko.creatorSession.implementation.PlaySession
import com.pandulapeter.kubriko.creatorSession.implementation.SessionArguments
import com.pandulapeter.kubriko.creatorSession.implementation.SessionConnection
import com.pandulapeter.kubriko.creatorSession.protocol.SessionMessage
import com.pandulapeter.kubriko.creatorSession.protocol.SessionProtocol
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlin.system.exitProcess

/**
 * The entry point of a session process, started by Kubriko Creator from a project's own desktop classpath.
 *
 * Kubriko Creator adds this module to that classpath itself, at the project's Kubriko version; projects never declare
 * it. The arguments are `play <main class>`; `edit` is refused with a failure, since Kubriko Creator edits Scenes in its
 * own window.
 */
fun main(args: Array<String>) {
    val connection = SessionConnection.open(System.getProperty(SessionProtocol.PORT_PROPERTY)?.toIntOrNull())
    val failure = run(args.toList(), connection)
    connection.close()
    if (failure != null) exitProcess(1)
}

/**
 * Runs the session [arguments] describe over [connection]: says Hello, forwards the log in [logScope], and runs the
 * mode. A [SessionFailure] is sent to Kubriko Creator as a [SessionMessage.Failure] and printed to standard error.
 *
 * @return The failure's message, or null when the session ran.
 */
internal fun run(
    arguments: List<String>,
    connection: SessionConnection,
    logScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
): String? = try {
    val sessionArguments = SessionArguments.parse(arguments)
    connection.send(SessionMessage.Hello(protocolVersion = SessionProtocol.VERSION, mode = sessionArguments.mode))
    LogForwarder(connection::send, logScope).start()
    when (sessionArguments) {
        is SessionArguments.Edit -> throw SessionFailure(EDIT_MODE_REMOVED_MESSAGE)
        is SessionArguments.Play -> {
            connection.listen(
                onCommand = PlayCommandHandler(connection::send)::handle,
                onUnknownCommand = { id -> connection.send(SessionMessage.CommandFailed(id, UNKNOWN_COMMAND_MESSAGE)) },
            )
            PlaySession.run(sessionArguments)
        }
    }
    null
} catch (exception: SessionFailure) {
    connection.send(SessionMessage.Failure(exception.message))
    System.err.println(exception.message)
    exception.message
}

private const val UNKNOWN_COMMAND_MESSAGE = "The game's Kubriko version is too old for this command; a newer one adds it."
private const val EDIT_MODE_REMOVED_MESSAGE =
    "This Kubriko version's session no longer opens the Scene editor; Kubriko Creator edits Scenes in its own window."
