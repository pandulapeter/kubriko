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
 * An agent of the [ACP Registry](https://agentclientprotocol.com/registry).
 *
 * @param id The registry's id of the agent.
 * @param isTested Whether the agent was checked in a real session with Kubriko Creator; every other one runs on the
 *   fallbacks alone ([AgentProfile.fallback]).
 */
internal data class RegistryAgent(
    val id: String,
    val name: String,
    val version: String,
    val distribution: AgentDistribution,
    val isTested: Boolean = false,
) {

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
            distribution = AgentDistribution.Npx("@agentclientprotocol/claude-agent-acp@0.99.0"),
            isTested = true,
        )

        /**
         * Runs [distribution] through [npx], with the folder it is in (which holds Node.js) first on the `PATH`, since an
         * app started from the Finder lacks the user's shell's `PATH`.
         *
         * @param environment More variables for the agent, such as the configuration folder it should use.
         */
        fun npxCommand(distribution: AgentDistribution.Npx, npx: File, environment: Map<String, String> = emptyMap()) = AgentCommand(
            program = npx.path,
            arguments = listOf("--yes", distribution.packageName) + distribution.arguments,
            environment = withFolderOnPath(npx) + distribution.environment + environment,
        )

        /**
         * Runs [distribution] through [uvx], with its folder first on the `PATH`.
         */
        fun uvxCommand(distribution: AgentDistribution.Uvx, uvx: File) = AgentCommand(
            program = uvx.path,
            arguments = listOf(distribution.packageName) + distribution.arguments,
            environment = withFolderOnPath(uvx) + distribution.environment,
        )

        /**
         * The `PATH` with [program]'s folder first, so that what it starts in turn is found next to it.
         */
        fun withFolderOnPath(program: File) = mapOf(PATH to listOfNotNull(program.parent, System.getenv(PATH)).joinToString(File.pathSeparator))
    }
}
