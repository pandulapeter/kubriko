/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.creatorSession.implementation

import com.pandulapeter.kubriko.creatorSession.protocol.SessionCommand
import com.pandulapeter.kubriko.creatorSession.protocol.SessionMessage
import com.pandulapeter.kubriko.creatorSession.protocol.SessionProtocol
import kotlinx.serialization.SerializationException
import java.io.BufferedWriter
import java.io.IOException
import java.io.OutputStreamWriter
import java.net.InetAddress
import java.net.Socket

/**
 * The session's line-based connection to Kubriko Creator. A connection that was never opened, or has been lost,
 * silently drops messages: the game keeps running either way.
 */
internal class SessionConnection private constructor(
    private val socket: Socket?,
) : AutoCloseable {
    private val writer = socket?.let { BufferedWriter(OutputStreamWriter(it.getOutputStream(), Charsets.UTF_8)) }
    private var isBroken = false

    @Synchronized
    fun send(message: SessionMessage) {
        if (writer == null || isBroken) return
        try {
            writer.write(SessionProtocol.encode(message))
            writer.newLine()
            writer.flush()
        } catch (_: Exception) {
            isBroken = true
        }
    }

    /**
     * Reads the commands Kubriko Creator sends, on a background thread, until the connection ends. Lines that aren't
     * commands this version knows are skipped.
     */
    fun listen(onCommand: (SessionCommand) -> Unit) {
        val reader = socket?.getInputStream()?.bufferedReader(Charsets.UTF_8) ?: return
        Thread({
            while (true) {
                val line = try {
                    reader.readLine()
                } catch (_: IOException) {
                    null
                } ?: break
                val command = try {
                    SessionProtocol.decodeCommand(line)
                } catch (_: SerializationException) {
                    null
                } catch (_: IllegalArgumentException) {
                    null
                }
                command?.let(onCommand)
            }
        }, "kubriko-creator-connection").apply { isDaemon = true }.start()
    }

    override fun close() {
        socket?.close()
    }

    companion object {

        fun open(port: Int?) = SessionConnection(port?.let { Socket(InetAddress.getLoopbackAddress(), it) })
    }
}
