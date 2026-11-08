/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.commands.project

import com.pandulapeter.kubrikoCreator.commands.AiStepOrigin
import com.pandulapeter.kubrikoCreator.commands.CommandExecutor
import com.pandulapeter.kubrikoCreator.commands.CommandOutcome
import com.pandulapeter.kubrikoCreator.commands.CommandProblem
import com.pandulapeter.kubrikoCreator.commands.Rejection
import com.pandulapeter.kubrikoCreator.commands.commandContext
import com.pandulapeter.kubrikoCreator.commands.testScope
import kotlinx.coroutines.async
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class AskUserTest {

    @Test
    fun `a question waits for its answer, which the tool returns`() = runTest {
        val context = commandContext()
        val asking = async { AskUser.run(context, AskUser.Input("How does the hero move?", listOf("Runs", " Flies ", "Runs"))) }
        runCurrent()
        val question = context.questions.pending.value.single()
        assertEquals(listOf("Runs", "Flies"), question.answers)
        context.questions.answer(question.id + 1, "Nothing waits for this one")
        context.questions.answer(question.id, "Swims")
        assertEquals("Swims", assertIs<CommandOutcome.Success>(asking.await()).result.jsonObject.getValue("answer").jsonPrimitive.content)
        assertEquals(emptyList(), context.questions.pending.value)
    }

    @Test
    fun `a question asked through the executor names the conversation and the tool call that asked it`() = runTest {
        val context = commandContext()
        val origin = AiStepOrigin(conversationId = "conversation", toolCallId = "toolu_1")
        val executor = CommandExecutor(context, testScope())
        val asking = async { executor.call(AskUser.name, buildJsonObject { put("question", "Which?"); putJsonArray("answers") { add("A"); add("B") } }, origin) }
        runCurrent()
        assertEquals(origin, context.questions.pending.value.single().origin)
        asking.cancel()
    }

    @Test
    fun `a stopped turn takes its question away`() = runTest {
        val context = commandContext()
        val asking = async { AskUser.run(context, AskUser.Input("Which?", listOf("A", "B"))) }
        runCurrent()
        assertEquals(1, context.questions.pending.value.size)
        asking.cancel()
        runCurrent()
        assertEquals(emptyList(), context.questions.pending.value)
    }

    @Test
    fun `a question needs two to four short answers`() = runTest {
        val context = commandContext()
        val invalid = CommandOutcome.Rejected(Rejection(CommandProblem.QUESTION_INVALID, "80"))
        assertEquals(invalid, AskUser.run(context, AskUser.Input("Which?", listOf("Only one", "Only one"))))
        assertEquals(invalid, AskUser.run(context, AskUser.Input("Which?", listOf("A", "B", "C", "D", "E"))))
        assertEquals(invalid, AskUser.run(context, AskUser.Input(" ", listOf("A", "B"))))
        assertEquals(invalid, AskUser.run(context, AskUser.Input("Which?", listOf("A", "x".repeat(81)))))
    }
}
