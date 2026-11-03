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

import com.agentclientprotocol.agent.AgentInfo

/**
 * Whether an agent's process runs and can be talked to.
 */
internal sealed interface AgentState {

    data object Stopped : AgentState

    data object Starting : AgentState

    data class Running(
        val info: AgentInfo,
    ) : AgentState

    /**
     * The agent couldn't be started, or kept crashing. It is started again only when asked.
     *
     * @param exitCode The process's exit code, or null if it never started or is still running (it didn't answer).
     * @param errorOutput The last lines it wrote to its standard error.
     */
    data class Failed(
        val exitCode: Int?,
        val errorOutput: List<String>,
    ) : AgentState
}
