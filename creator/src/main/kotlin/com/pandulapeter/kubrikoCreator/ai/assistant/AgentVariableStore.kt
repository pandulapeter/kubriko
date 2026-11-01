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

/**
 * Keeps the environment variables agents are given for signing in (an agent's own API key) in the operating system's
 * secret store, one entry per agent and variable. Every call blocks.
 */
internal class AgentVariableStore(
    private val secrets: SecretStore,
) {

    /**
     * The values kept for the variables [names] of the agent [agentId], leaving out those that are gone.
     */
    fun read(agentId: String, names: List<String>): Map<String, String> = names.mapNotNull { name -> secrets.read(keyOf(agentId, name))?.let { name to it } }.toMap()

    /**
     * Keeps [values] for the agent [agentId].
     *
     * @return Whether every value was kept.
     */
    fun write(agentId: String, values: Map<String, String>) = values.all { (name, value) -> secrets.write(keyOf(agentId, name), value) }

    private fun keyOf(agentId: String, name: String) = "agent-variable:$agentId:$name"
}
