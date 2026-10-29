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

import com.agentclientprotocol.agent.Agent
import com.agentclientprotocol.protocol.Protocol
import com.agentclientprotocol.transport.StdioTransport
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.flow
import java.io.IOException
import java.nio.channels.Channels
import java.nio.channels.Pipe
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Launches [FakeAgent]s in this process, talking to Kubriko Creator over pipes as a real agent's process would.
 *
 * @param failingLaunches How many launches fail before one succeeds.
 */
internal class FakeAgentLauncher(
    private val fakeAgent: () -> FakeAgent = { FakeAgent() },
    var failingLaunches: Int = 0,
) : AgentLauncher {
    val commands = CopyOnWriteArrayList<AgentCommand>()
    val channels = CopyOnWriteArrayList<FakeAgentChannel>()
    val agents = CopyOnWriteArrayList<FakeAgent>()

    override fun launch(command: AgentCommand): AgentChannel {
        commands += command
        if (failingLaunches > 0) {
            failingLaunches--
            throw IOException("No such program: ${command.program}")
        }
        val agent = fakeAgent()
        return FakeAgentChannel(agent).also {
            agents += agent
            channels += it
        }
    }

    fun close() = channels.forEach { it.stop() }
}

internal class FakeAgentChannel(
    private val agent: FakeAgent,
) : AgentChannel {
    private val toCreator = Pipe.open()
    private val fromCreator = Pipe.open()
    private val errorPipe = Pipe.open()
    private val exit = CompletableDeferred<Int>()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    override val fromAgent = Channels.newInputStream(toCreator.source())!!
    override val toAgent = Channels.newOutputStream(fromCreator.sink())!!
    override val errors = Channels.newInputStream(errorPipe.source())!!
    private val agentWriter = Channels.newOutputStream(toCreator.sink()).bufferedWriter()
    private val errorWriter = Channels.newOutputStream(errorPipe.sink()).bufferedWriter()
    val isStopped get() = exit.isCompleted

    init {
        val reader = Channels.newInputStream(fromCreator.source()).bufferedReader()
        val transport = StdioTransport(
            parentScope = scope,
            ioDispatcher = Dispatchers.IO,
            input = flow { while (true) emit(reader.readLine() ?: break) },
            output = { line ->
                synchronized(agentWriter) {
                    agentWriter.write(line)
                    agentWriter.newLine()
                    agentWriter.flush()
                }
            },
        )
        val protocol = Protocol(scope, transport)
        Agent(protocol, agent.support(this))
        protocol.start()
        if (agent.exitsAtStart != null) crash(agent.exitsAtStart)
    }

    fun writeError(line: String) = synchronized(errorWriter) {
        errorWriter.write(line)
        errorWriter.newLine()
        errorWriter.flush()
    }

    /**
     * Ends the agent as a crashing process would: its streams close and it exits with [exitCode].
     */
    fun crash(exitCode: Int) {
        if (exit.isCompleted) return
        scope.cancel()
        runCatching { toCreator.sink().close() }
        runCatching { errorPipe.sink().close() }
        runCatching { fromCreator.source().close() }
        exit.complete(exitCode)
    }

    override suspend fun awaitExit() = exit.await()

    override fun stop() = crash(STOPPED_EXIT_CODE)

    companion object {
        const val STOPPED_EXIT_CODE = 143
    }
}
