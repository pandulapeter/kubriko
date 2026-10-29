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

import com.agentclientprotocol.model.PermissionOption
import com.agentclientprotocol.model.PermissionOptionKind
import com.agentclientprotocol.model.RequestPermissionOutcome
import com.agentclientprotocol.model.SessionUpdate

/**
 * Answers an agent asking whether it may run a tool call.
 */
internal fun interface PermissionHandler {

    suspend fun answer(toolCall: SessionUpdate.ToolCallUpdate, options: List<PermissionOption>): RequestPermissionOutcome

    companion object {

        /**
         * Rejects every request once, or cancels it if the agent offers no way to reject it.
         */
        val Reject = PermissionHandler { _, options ->
            options.firstOrNull { it.kind == PermissionOptionKind.REJECT_ONCE }
                ?.let { RequestPermissionOutcome.Selected(it.optionId) }
                ?: RequestPermissionOutcome.Cancelled
        }
    }
}
