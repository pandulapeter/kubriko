/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.context

import com.pandulapeter.kubrikoCreator.actors.ActorTypeDefinition
import com.pandulapeter.kubrikoCreator.actors.BlockDefinition
import com.pandulapeter.kubrikoCreator.actors.EventType
import com.pandulapeter.kubrikoCreator.actors.KeyChoice
import com.pandulapeter.kubrikoCreator.actors.ManagerTypeDefinition
import com.pandulapeter.kubrikoCreator.commands.CommandContext
import com.pandulapeter.kubrikoCreator.commands.CommandExecutor
import com.pandulapeter.kubrikoCreator.commands.CommandOutcome
import com.pandulapeter.kubrikoCreator.commands.EditorMutation
import com.pandulapeter.kubrikoCreator.commands.commandContext
import com.pandulapeter.kubrikoCreator.commands.testScope
import com.pandulapeter.kubrikoCreator.scenes.SceneDocument
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * Realistic requests, each with the tool calls a model made (or would make) for it, replayed against the tools on the
 * Breakout fixture: a change to a tool's name, input, or validation that would break what models do shows up here
 * without spending tokens. Add a request a real session struggled with, with the calls that should have worked.
 */
class EvalSetTest {

    private class Request(
        val text: String,
        val calls: List<Pair<String, String>>,
        val check: (CommandContext) -> Unit,
    )

    private val requests = listOf(
        Request(
            text = "make the Ball faster",
            calls = listOf(
                "read_actor_type" to """{"actorType": "Ball", "detail": "outline"}""",
                "edit_block" to """{"actorType": "Ball", "eventIndex": 0, "blockIndex": 0, "block": {"type": "setVelocity", "x": 300, "y": -450}}""",
            ),
        ) { context -> assertEquals(BlockDefinition.SetVelocity(300f, -450f), context.actorType("Ball").events[0].blocks[0]) },
        Request(
            text = "add a Bat moved with A and D",
            calls = listOf(
                "create_actor_type" to """{"name": "Bat", "shape": "rectangle", "width": 100, "height": 16}""",
                "add_event" to """{"actorType": "Bat", "type": "keyPressed", "key": "A", "blocks": [{"type": "setVelocity", "x": -400}]}""",
                "add_event" to """{"actorType": "Bat", "type": "keyPressed", "key": "D", "blocks": [{"type": "setVelocity", "x": 400}]}""",
                "place_actor" to """{"scene": "level-1", "actorType": "Bat", "x": 400, "y": 560}""",
            ),
        ) { context ->
            assertEquals(listOf(KeyChoice.of("A"), KeyChoice.of("D")), context.actorType("Bat").events.map { it.key })
            assertTrue(SceneDocument.parse(context.scenes.read("level-1")!!).any { it.typeId == "Bat" })
        },
        Request(
            text = "make Bricks give 20 points",
            calls = listOf(
                "get_relations" to """{"type": "Game", "property": "score"}""",
                "edit_block" to """{"actorType": "Ball", "eventIndex": 2, "blockIndex": 2, "block": {"type": "changeNumber", "property": "score", "by": 20, "manager": "Game"}}""",
            ),
        ) { context -> assertEquals(BlockDefinition.ChangeNumber("score", 20f, manager = "Game"), context.actorType("Ball").events[2].blocks[2]) },
        Request(
            text = "log when the game starts",
            calls = listOf(
                "check_change" to """{"tool": "add_event", "arguments": {"actorType": "Game", "type": "gameStarted", "blocks": [{"type": "log", "message": "The game started"}]}}""",
                "add_event" to """{"actorType": "Game", "type": "gameStarted", "blocks": [{"type": "log", "message": "The game started"}]}""",
            ),
        ) { context -> assertTrue(context.managerType("Game").events.any { it.type == EventType.GAME_STARTED && it.blocks == listOf(BlockDefinition.Log("The game started")) }) },
        Request(
            text = "show the score",
            calls = listOf(
                "add_text" to """{"managerType": "Game", "text": {"text": "Score: {score}", "position": "topLeft", "size": 24}}""",
            ),
        ) { context -> assertTrue(context.managerType("Game").texts.any { it.text == "Score: {score}" }) },
    )

    @Test
    fun everyRequestsCallsAreAcceptedAndDoWhatWasAsked() = runTest {
        requests.forEach { request ->
            val (executor, context) = breakout()
            request.calls.forEach { (tool, arguments) ->
                val outcome = executor.call(tool, Json.parseToJsonElement(arguments).jsonObject)
                assertIs<CommandOutcome.Success>(outcome, "\"${request.text}\": $tool was rejected with $outcome")
            }
            request.check(context)
        }
    }

    @OptIn(EditorMutation::class)
    private fun TestScope.breakout(): Pair<CommandExecutor, CommandContext> {
        val context = commandContext(*BreakoutFixture.types.toTypedArray())
        context.scenes.write("level-1", BreakoutFixture.level)
        return CommandExecutor(context, testScope()) to context
    }

    private fun CommandContext.actorType(name: String) = blockTypes!!.get(name) as ActorTypeDefinition

    private fun CommandContext.managerType(name: String) = blockTypes!!.get(name) as ManagerTypeDefinition
}
