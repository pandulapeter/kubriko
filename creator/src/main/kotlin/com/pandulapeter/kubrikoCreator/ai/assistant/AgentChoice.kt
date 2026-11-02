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
 * The agent the assistant runs: one of the registry's, or a command line the user entered.
 */
internal sealed interface AgentChoice {
    val id: String

    /**
     * What the panel calls the agent: the registry's name, or the custom command's program.
     */
    val name: String

    data class Registry(
        val agent: RegistryAgent,
    ) : AgentChoice {
        override val id get() = agent.id
        override val name get() = agent.name
    }

    /**
     * @param commandLine What starts the agent, as the user typed it: variables to set, the program, and its arguments.
     */
    data class Custom(
        val commandLine: String,
    ) : AgentChoice {
        override val id get() = ID
        override val name get() = CustomAgentCommand.programOf(commandLine).let { File(it).name }.ifEmpty { commandLine }

        companion object {
            const val ID = "custom"
        }
    }

    val isClaude get() = this is Registry && agent.id == RegistryAgent.claude.id

    /**
     * Whether the agent was checked in a real session with Kubriko Creator ([RegistryAgent.isTested]).
     */
    val isTested get() = this is Registry && agent.isTested

    /**
     * What the agent gets beyond the fallbacks: only Claude Agent has more.
     */
    val profile get() = if (isClaude) AgentProfile.claude else AgentProfile.fallback

    companion object {

        /**
         * The agent [settings] remember, out of [agents]: Claude Agent if it isn't among them.
         */
        internal fun of(settings: AssistantSettings, agents: List<RegistryAgent>): AgentChoice = when (val id = settings.agentId) {
            Custom.ID -> settings.customAgentCommand?.let(::Custom)
            else -> agents.firstOrNull { it.id == id }?.let(::Registry)
        } ?: Registry(RegistryAgent.claude)
    }
}
