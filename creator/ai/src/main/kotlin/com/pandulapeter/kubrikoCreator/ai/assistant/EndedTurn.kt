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
 * A turn of the assistant panel's conversation that ended, stopped or not, with what keeping the conversation needs.
 *
 * @param id The agent's ID of the conversation.
 * @param title The first thing the user asked, which names the conversation.
 * @param lastAnswer The agent's last answer, if it gave one.
 * @param toolsId The ID of the conversation's tool address, which the summary it noted is kept by
 *   ([com.pandulapeter.kubrikoCreator.commands.ConversationSummaries]).
 * @param planAwaitingApproval The steps of the plan the agent laid out with Kubriko Creator's `set_plan` and waits to have
 *   approved, if it does, which a continued conversation offers again, since the agent's history doesn't hold it.
 * @param messages What the user asked and the agent answered so far, kept for searching ([KeptMessage]).
 */
public data class EndedTurn(
    val id: String,
    val title: String,
    val lastAnswer: String?,
    val toolsId: String?,
    val planAwaitingApproval: List<String> = emptyList(),
    val messages: List<KeptMessage> = emptyList(),
)
