/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.commands.screenplay

import com.pandulapeter.kubrikoCreator.actors.ActorTypeDefinition
import com.pandulapeter.kubrikoCreator.actors.PropertyDefinition
import com.pandulapeter.kubrikoCreator.commands.CommandExecutor
import com.pandulapeter.kubrikoCreator.commands.CommandOutcome
import com.pandulapeter.kubrikoCreator.commands.CommandProblem
import com.pandulapeter.kubrikoCreator.commands.Invocation
import com.pandulapeter.kubrikoCreator.commands.commandContext
import com.pandulapeter.kubrikoCreator.commands.testScope
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class PageStyleTest {
    private val ball = ActorTypeDefinition(name = "Ball", properties = listOf(PropertyDefinition.Number("speed", 240f), PropertyDefinition.Number("lives", 1f)))

    @Test
    fun `the AI's pages keep the writing rules, while the user's own are theirs`() = runTest {
        val context = commandContext(ball)
        val executor = CommandExecutor(context, testScope())
        val narrated = buildJsonObject { put("page", "README"); put("text", "# Game\n\nI added a ball that used to fly.") }
        val rejected = assertIs<CommandOutcome.Rejected>(executor.call("create_page", narrated)).rejection
        assertEquals(CommandProblem.SCREENPLAY_STYLE, rejected.reason)
        assertEquals(
            "line 3, \"used to\": Describe the game as it is, in the present tense, without its history.; " +
                "line 3, \"I added\": Leave out the conversation and the work; say what the game is.",
            rejected.arguments.single(),
        )
        val checked = assertIs<CommandOutcome.Rejected>(executor.call("check_change", buildJsonObject { put("tool", "create_page"); put("arguments", narrated) }))
        assertEquals(CommandProblem.SCREENPLAY_STYLE, checked.rejection.reason)
        assertIs<CommandOutcome.Success>(executor.execute(Invocation(CreatePage, CreatePage.Input("README", "# Game\n\nI added a ball that used to fly."))))
    }

    @Test
    fun `a number of a mentioned type's Property is named by its mention, and a section is checked as a section`() = runTest {
        val context = commandContext(ball)
        val executor = CommandExecutor(context, testScope())
        executor.execute(Invocation(CreatePage, CreatePage.Input("README", "# Game\n\n## Ball\n\nIt bounces.\n")))
        val copied = buildJsonObject { put("page", "README"); put("section", "Ball"); put("text", "## Ball\n\nThe [ball](../.kubriko/actors/Ball.json) moves at 240, with 1 life.") }
        assertEquals("line 3, \"240\": Name this number by a mention of its Property rather than copying it, so it can't drift.", assertIs<CommandOutcome.Rejected>(executor.call("edit_page", copied)).rejection.arguments.single())
        val named = buildJsonObject { put("page", "README"); put("section", "Ball"); put("text", "## Ball\n\nThe [ball](../.kubriko/actors/Ball.json) moves at its [speed](../.kubriko/actors/Ball.json).") }
        assertIs<CommandOutcome.Success>(executor.call("edit_page", named))
    }
}
