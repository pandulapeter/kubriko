/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.commands

/**
 * Where an AI tool call came from: one of the assistant's conversations, so that its panel can tie the call to the step.
 *
 * @param conversationId The ID of the conversation's tool address (`McpToolServer.Conversation.id`).
 * @param toolCallId The client's own ID for the call, if it sent one.
 */
internal data class AiStepOrigin(
    val conversationId: String,
    val toolCallId: String? = null,
)
