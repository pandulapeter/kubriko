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

import com.agentclientprotocol.client.ClientSession
import com.agentclientprotocol.model.SessionConfigId
import com.agentclientprotocol.model.SessionConfigOption
import com.agentclientprotocol.model.SessionConfigOptionValue
import com.agentclientprotocol.model.SessionConfigSelectOption
import com.agentclientprotocol.model.SessionConfigSelectOptions
import com.agentclientprotocol.model.SessionConfigValueId
import com.agentclientprotocol.model.SessionMode
import com.agentclientprotocol.model.SessionModeId

/**
 * The read-only mode or sandbox an agent offers for a conversation, which keeps it off the files without rejecting what it
 * asks: a value of one of its config options, or one of its session modes, named read-only (Codex's `read-only`, for
 * one). Plan modes don't count, since some (Claude Code's) also refuse MCP tools that change something, Creator's among
 * them.
 */
internal sealed interface ReadOnlyMode {

    /**
     * Switches [session] to this mode.
     */
    suspend fun enter(session: ClientSession)

    data class ConfigOption(
        val id: SessionConfigId,
        val value: SessionConfigValueId,
    ) : ReadOnlyMode {
        override suspend fun enter(session: ClientSession) {
            session.setConfigOption(id, SessionConfigOptionValue.StringValue(value.value))
        }
    }

    data class Mode(
        val id: SessionModeId,
    ) : ReadOnlyMode {
        override suspend fun enter(session: ClientSession) {
            session.setMode(id)
        }
    }

    companion object {

        /**
         * The read-only mode [session] offers and isn't in yet, preferring config options, which supersede modes.
         */
        fun of(session: ClientSession): ReadOnlyMode? = if (isActive(session)) null else configOptionOf(session) ?: modeOf(session)

        /**
         * Whether [session] is in a read-only mode now.
         */
        fun isActive(session: ClientSession): Boolean {
            val isConfigOptionReadOnly = session.selects().any { option ->
                option.choices().any { it.value == option.currentValue && it.isReadOnly() }
            }
            val isModeReadOnly = session.modesSupported && session.availableModes.any { it.id == session.currentMode.value && it.isReadOnly() }
            return isConfigOptionReadOnly || isModeReadOnly
        }

        private fun configOptionOf(session: ClientSession) = session.selects().firstNotNullOfOrNull { option ->
            option.choices().firstOrNull { it.isReadOnly() }?.let { ConfigOption(option.id, it.value) }
        }

        private fun modeOf(session: ClientSession) = session.availableModes.firstOrNull { it.isReadOnly() }?.let { Mode(it.id) }

        private fun SessionConfigSelectOption.isReadOnly() = isReadOnly(value.value, name)

        private fun SessionMode.isReadOnly() = isReadOnly(id.value, name)

        /**
         * Whether a mode or value of [id] and [name] says read-only and neither of them says plan.
         */
        internal fun isReadOnly(id: String, name: String) = (isReadOnly(id) || isReadOnly(name)) && !mentionsPlan(id) && !mentionsPlan(name)

        private fun mentionsPlan(name: String) = PLAN in name.lowercase()

        private fun ClientSession.selects() = if (configOptionsSupported) configOptions.value.filterIsInstance<SessionConfigOption.Select>() else emptyList()

        /**
         * Whether [name] says read-only, however it is written (`read-only`, `readOnly`, `Read only`).
         */
        private fun isReadOnly(name: String) = name.lowercase().filter { it.isLetterOrDigit() }.contains(READ_ONLY)

        private const val READ_ONLY = "readonly"
        private const val PLAN = "plan"
    }
}
