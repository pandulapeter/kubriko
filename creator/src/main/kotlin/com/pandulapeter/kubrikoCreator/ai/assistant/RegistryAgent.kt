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

import java.io.File

/**
 * An agent of the [ACP Registry](https://agentclientprotocol.com/registry), distributed as an npm package that `npx` runs.
 *
 * @param id The registry's id of the agent.
 * @param arguments What the package's program is started with.
 */
internal data class RegistryAgent(
    val id: String,
    val name: String,
    val version: String,
    val npxPackage: String,
    val arguments: List<String> = emptyList(),
) {

    /**
     * Runs the agent through [npx], with the folder it is in (which holds Node.js) first on the `PATH`, since an app
     * started from the Finder lacks the user's shell's `PATH`.
     *
     * @param environment More variables for the agent, such as the configuration folder it should use.
     */
    fun command(npx: File, environment: Map<String, String> = emptyMap()) = AgentCommand(
        program = npx.path,
        arguments = listOf("--yes", npxPackage) + arguments,
        environment = mapOf(PATH to listOfNotNull(npx.parent, System.getenv(PATH)).joinToString(File.pathSeparator)) + environment,
    )

    companion object {
        private const val PATH = "PATH"

        /**
         * Claude Code, through the ACP adapter of Anthropic, Zed, and JetBrains, which runs Anthropic's own `claude` binary
         * signed in as the user is.
         */
        val claude = RegistryAgent(
            id = "claude-acp",
            name = "Claude Agent",
            version = "0.99.0",
            npxPackage = "@agentclientprotocol/claude-agent-acp@0.99.0",
        )
    }
}
