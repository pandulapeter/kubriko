/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.ui.assistant

import com.agentclientprotocol.model.ToolCallId
import com.pandulapeter.kubrikoCreator.ai.assistant.TranscriptItem
import com.pandulapeter.kubrikoCreator.commands.AiStep

/**
 * The AI step each of Kubriko Creator's tool calls in [items] made, by the call's ID: the step of the conversation
 * whose tools are at [toolsId] that carries the call's ID or, for an agent that sends none, the conversation's steps of
 * the same tool in order.
 */
internal fun aiStepsOf(items: List<TranscriptItem>, steps: List<AiStep>, toolsId: String?): Map<ToolCallId, AiStep> {
    if (toolsId == null) return emptyMap()
    val ownSteps = steps.filter { it.origin?.conversationId == toolsId }
    if (ownSteps.isEmpty()) return emptyMap()
    val identified = ownSteps.mapNotNull { step -> step.origin?.toolCallId?.let { it to step } }.toMap()
    val unidentified = ownSteps.filter { it.origin?.toolCallId == null }.groupByTo(mutableMapOf(), { it.toolName }) { it }.mapValues { ArrayDeque(it.value) }
    return buildMap {
        items.forEach { item ->
            if (item !is TranscriptItem.ToolCall) return@forEach
            val toolName = item.creatorToolName ?: return@forEach
            (identified[item.id.value] ?: unidentified[toolName]?.removeFirstOrNull())?.let { put(item.id, it) }
        }
    }
}
