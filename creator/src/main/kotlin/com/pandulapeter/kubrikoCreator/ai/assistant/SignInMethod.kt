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

import com.agentclientprotocol.model.AuthMethod
import com.agentclientprotocol.model.AuthMethodId
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull

/**
 * A way of signing in that an agent offers, as the assistant panel shows it.
 *
 * @param terminal What runs in the panel's sign-in terminal, or null if the agent signs in by itself (`authenticate`).
 */
internal data class SignInMethod(
    val id: AuthMethodId,
    val name: String,
    val description: String? = null,
    val terminal: AgentCommand? = null,
) {
    companion object {

        /**
         * The methods of [methods] Kubriko Creator can run for the agent started with [agent]. A terminal method runs the
         * command of its `_meta["terminal-auth"]` where it names one (Claude Agent's does), and otherwise the agent's own
         * program with the method's arguments; either way with the agent's environment, so that it signs in to the same
         * configuration. Methods that take variables are left out, since the panel doesn't ask for them yet.
         */
        fun of(methods: List<AuthMethod>, agent: AgentCommand): List<SignInMethod> = methods.mapNotNull { method ->
            when (method) {
                is AuthMethod.AgentAuth -> SignInMethod(method.id, method.name, method.description)
                is AuthMethod.TerminalAuth -> SignInMethod(
                    id = method.id,
                    name = method.name,
                    description = method.description,
                    terminal = terminalCommand(method._meta as? JsonObject, agent) ?: agent.copy(
                        arguments = agent.arguments + method.args.orEmpty(),
                        environment = agent.environment + method.env.orEmpty(),
                    ),
                )
                else -> null
            }
        }

        private fun terminalCommand(meta: JsonObject?, agent: AgentCommand): AgentCommand? {
            val terminal = meta?.get(TERMINAL_AUTH) as? JsonObject ?: return null
            val program = (terminal["command"] as? JsonPrimitive)?.contentOrNull ?: return null
            return AgentCommand(
                program = program,
                arguments = (terminal["args"] as? JsonArray).orEmpty().mapNotNull { (it as? JsonPrimitive)?.contentOrNull },
                environment = agent.environment + (terminal["env"] as? JsonObject).orEmpty().mapNotNull { (name, value) ->
                    (value as? JsonPrimitive)?.contentOrNull?.let { name to it }
                },
            )
        }

        private const val TERMINAL_AUTH = "terminal-auth"
    }
}
