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

import com.pandulapeter.kubrikoCreator.mcp.clients.ProcessRunner
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException

/**
 * Works out how the agent chosen is started: through the user's `npx` (or the one of [node], Creator's own Node.js) or
 * `uvx`, as a program [installer] downloads, or as the user's own command line.
 */
internal class AgentCommands(
    private val runner: ProcessRunner,
    private val installer: BinaryAgentInstaller?,
    private val home: String,
    private val ioDispatcher: CoroutineDispatcher,
    private val node: NodeRuntime? = null,
) {
    private val _download = MutableStateFlow<AgentDownload?>(null)

    /**
     * The agent's archive being downloaded on its first start, if one is.
     */
    val download = _download.asStateFlow()

    /**
     * The user's `npx`, if their shell finds it, otherwise the one of the Node.js Creator downloaded, if it did.
     */
    suspend fun npx() = withContext(ioDispatcher) { runner.locate(NPX)?.let(::File) ?: node?.installedNpx() }

    /**
     * Whether Creator can download a Node.js of its own.
     */
    val canInstallNode get() = node != null

    /**
     * Downloads a Node.js of Creator's own, showing its progress as [download].
     *
     * @throws IOException If it couldn't be downloaded.
     */
    suspend fun installNode() {
        val runtime = node ?: return
        try {
            withContext(ioDispatcher) { runtime.install { downloaded, total -> _download.value = AgentDownload(NODE_NAME, downloaded, total) } }
        } finally {
            _download.value = null
        }
    }

    /**
     * What starts [choice], with [environment] added to what it is started with.
     *
     * @throws NodeMissingException If it needs `npx`, and there is none.
     * @throws UvMissingException If it needs `uvx`, and there is none.
     * @throws AgentStartException If its archive couldn't be downloaded, or its command line names no program.
     */
    suspend fun of(choice: AgentChoice, npx: File?, environment: Map<String, String>): AgentCommand = when (choice) {
        is AgentChoice.Registry -> when (val distribution = choice.agent.distribution) {
            is AgentDistribution.Npx -> RegistryAgent.npxCommand(distribution, npx ?: throw NodeMissingException(), environment)
            is AgentDistribution.Uvx -> RegistryAgent.uvxCommand(distribution, withContext(ioDispatcher) { runner.locate(UVX) }?.let(::File) ?: throw UvMissingException())
                .let { it.copy(environment = it.environment + environment) }
            is AgentDistribution.Binary -> binaryCommand(choice.agent, distribution).let { it.copy(environment = it.environment + environment) }
        }
        is AgentChoice.Custom -> withContext(ioDispatcher) { CustomAgentCommand.of(choice.commandLine, home, runner::locate) }
            ?.let { it.copy(environment = it.environment + environment) }
            ?: throw AgentStartException(exitCode = null, errorOutput = emptyList())
    }

    /**
     * Runs the program of [agent]'s archive, downloaded first unless it already is, with its folder first on the `PATH`.
     */
    private suspend fun binaryCommand(agent: RegistryAgent, distribution: AgentDistribution.Binary): AgentCommand {
        val program = try {
            withContext(ioDispatcher) { installer?.install(agent, distribution) { downloaded, total -> _download.value = AgentDownload(agent.name, downloaded, total) } }
        } catch (exception: IOException) {
            throw AgentStartException(exitCode = null, errorOutput = listOfNotNull(exception.message), cause = exception)
        } finally {
            _download.value = null
        } ?: throw AgentStartException(exitCode = null, errorOutput = emptyList())
        return AgentCommand(
            program = program.path,
            arguments = distribution.arguments,
            environment = RegistryAgent.withFolderOnPath(program) + distribution.environment,
        )
    }

    private companion object {
        const val NPX = "npx"
        const val UVX = "uvx"
        const val NODE_NAME = "Node.js"
    }
}
