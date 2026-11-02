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
import com.pandulapeter.kubrikoCreator.commands.AiStepOrigin
import com.pandulapeter.kubrikoCreator.commands.CommandTarget
import kubriko.creator.generated.resources.Res
import kotlin.test.Test
import kotlin.test.assertEquals

class ToolCallStepsTest {
    private fun call(id: String, tool: String) = TranscriptItem.ToolCall(ToolCallId(id), "mcp__kubriko-creator__$tool")

    private fun step(id: Int, tool: String, conversation: String?, toolCallId: String? = null) = AiStep(
        id = id,
        toolName = tool,
        target = CommandTarget.BlockType("Ball"),
        isRunning = false,
        origin = conversation?.let { AiStepOrigin(it, toolCallId) },
    )

    @Test
    fun callsAreTiedToTheStepsCarryingTheirIds() {
        val items = listOf(call("a", "add_block"), call("b", "add_block"))
        val steps = listOf(step(0, "add_block", "one", "b"), step(1, "add_block", "one", "a"), step(2, "add_block", "two", "a"))
        assertEquals(mapOf(ToolCallId("a") to steps[1], ToolCallId("b") to steps[0]), aiStepsOf(items, steps, "one"))
    }

    @Test
    fun withoutIdsCallsAreTiedToTheConversationsStepsOfTheSameToolInOrder() {
        val items = listOf(
            call("a", "add_block"),
            TranscriptItem.ToolCall(ToolCallId("own"), "Read"),
            call("b", "read_scene"),
            call("c", "add_block"),
        )
        val steps = listOf(step(0, "add_block", null), step(1, "add_block", "one"), step(2, "read_scene", "one"), step(3, "add_block", "one"))
        assertEquals(mapOf(ToolCallId("a") to steps[1], ToolCallId("b") to steps[2], ToolCallId("c") to steps[3]), aiStepsOf(items, steps, "one"))
    }

    @Test
    fun aConversationNotStartedYetHasNoSteps() {
        assertEquals(emptyMap(), aiStepsOf(listOf(call("a", "add_block")), listOf(step(0, "add_block", "one", "a")), null))
    }
}
