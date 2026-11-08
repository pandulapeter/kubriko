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

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * A conversation about a project kept after it ended, so that it can be continued with the same agent later.
 *
 * @param project The project folder's absolute path.
 * @param agentId The [AgentChoice.id] of the agent that held it.
 * @param id The agent's ID of the conversation.
 * @param title The first thing the user asked.
 * @param updatedAt When it was last used, in milliseconds since the epoch.
 * @param lastAnswer The start of the agent's last answer, which the next conversation about the project starts with.
 * @param isWritersRoom Whether it is a conversation in the writers' room, which is continued with its rules.
 */
@Serializable
public data class SavedConversation(
    @SerialName("project") val project: String,
    @SerialName("agentId") val agentId: String,
    @SerialName("id") val id: String,
    @SerialName("title") val title: String,
    @SerialName("updatedAt") val updatedAt: Long,
    @SerialName("lastAnswer") val lastAnswer: String? = null,
    @SerialName("isWritersRoom") val isWritersRoom: Boolean = false,
)
