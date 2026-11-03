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
public sealed interface AgentChoice {
    /**
     * What the settings keep the choice as: the registry agent's ID, or the custom command's.
     */
    public val id: String

    /**
     * What the panel calls the agent: the registry's name, or the custom command's program.
     */
    public val name: String

    /**
     * One of the ACP Registry's agents.
     */
    public data class Registry(
        val agent: RegistryAgent,
    ) : AgentChoice {
        override val id: String get() = agent.id
        override val name: String get() = agent.name
    }

    /**
     * @param commandLine What starts the agent, as the user typed it: variables to set, the program, and its arguments.
     */
    public data class Custom(
        val commandLine: String,
    ) : AgentChoice {
        override val id: String get() = ID
        override val name: String get() = CustomAgentCommand.programOf(commandLine).let { File(it).name }.ifEmpty { commandLine }

        internal companion object {
            internal const val ID = "custom"
        }
    }

    /**
     * Whether the agent is Claude Agent.
     */
    public val isClaude: Boolean get() = this is Registry && agent.id == RegistryAgent.claude.id

    /**
     * Whether the agent was checked in a real session with Kubriko Creator ([RegistryAgent.isTested]).
     */
    public val isTested: Boolean get() = this is Registry && agent.isTested

    /**
     * What the agent gets beyond the fallbacks: only Claude Agent has more.
     */
    public val profile: AgentProfile get() = if (isClaude) AgentProfile.claude else AgentProfile.fallback

    public companion object {

        /**
         * The agent [settings] remember, out of [agents]: Claude Agent if it isn't among them.
         */
        internal fun of(settings: AssistantSettings, agents: List<RegistryAgent>): AgentChoice = when (val id = settings.agentId) {
            Custom.ID -> settings.customAgentCommand?.let(::Custom)
            else -> agents.firstOrNull { it.id == id }?.let(::Registry)
        } ?: Registry(RegistryAgent.claude)
    }
}
