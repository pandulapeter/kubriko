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

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.runInterruptible
import java.io.IOException
import java.nio.channels.Channels
import java.nio.channels.Pipe
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Launches [FakeTerminalChannel]s, which a test writes the command's output to and reads what was typed from.
 *
 * @param isFailing Whether launching fails, as for a program that doesn't exist.
 */
internal class FakeTerminalLauncher(
    var isFailing: Boolean = false,
) : AgentLauncher {
    val commands = CopyOnWriteArrayList<AgentCommand>()
    val channels = CopyOnWriteArrayList<FakeTerminalChannel>()

    override fun launch(command: AgentCommand): AgentChannel {
        commands += command
        if (isFailing) throw IOException("No such program: ${command.program}")
        return FakeTerminalChannel().also { channels += it }
    }
}

internal class FakeTerminalChannel : AgentChannel {
    private val output = Pipe.open()
    private val errorOutput = Pipe.open()
    private val input = Pipe.open()
    private val exit = CompletableDeferred<Int>()
    override val fromAgent = Channels.newInputStream(output.source())!!
    override val errors = Channels.newInputStream(errorOutput.source())!!
    override val toAgent = Channels.newOutputStream(input.sink())!!
    private val outputWriter = Channels.newOutputStream(output.sink()).writer()
    private val inputReader = Channels.newInputStream(input.source()).bufferedReader()
    val isStopped get() = exit.isCompleted

    fun write(text: String) {
        outputWriter.write(text)
        outputWriter.flush()
    }

    suspend fun readTypedLine(): String? = runInterruptible { inputReader.readLine() }

    fun exit(exitCode: Int) {
        if (exit.isCompleted) return
        runCatching { outputWriter.close() }
        runCatching { errorOutput.sink().close() }
        exit.complete(exitCode)
    }

    override suspend fun awaitExit() = exit.await()

    override fun stop() = exit(STOPPED_EXIT_CODE)

    companion object {
        const val STOPPED_EXIT_CODE = 143
    }
}
