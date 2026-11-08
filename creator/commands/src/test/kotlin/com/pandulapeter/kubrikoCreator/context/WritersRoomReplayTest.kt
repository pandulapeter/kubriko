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

import com.pandulapeter.kubrikoCreator.actors.EventType
import com.pandulapeter.kubrikoCreator.commands.CommandExecutor
import com.pandulapeter.kubrikoCreator.commands.CommandOutcome
import com.pandulapeter.kubrikoCreator.commands.commandContext
import com.pandulapeter.kubrikoCreator.commands.testScope
import com.pandulapeter.kubrikoCreator.foundation.EditorMutation
import com.pandulapeter.kubrikoCreator.scenes.SceneDocument
import com.pandulapeter.kubrikoCreator.screenplay.PageText
import com.pandulapeter.kubrikoCreator.screenplay.ScreenplayStyle
import kotlinx.coroutines.async
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * The tool calls an agent makes in the writers' room, replayed from an idea to its first playable slice on a project made
 * from the Blank template, with ask_user answered as the user would: what the writers' room's rules ask for has to be
 * something the tools accept, pages that keep the writing rules included.
 */
class WritersRoomReplayTest {

    @OptIn(EditorMutation::class)
    @Test
    fun anIdeaBecomesPagesAndThenAFirstPlayableSlice() = runTest {
        val context = commandContext()
        context.scenes.write("main", emptyList())
        val executor = CommandExecutor(context, testScope())
        suspend fun call(tool: String, arguments: String) = assertIs<CommandOutcome.Success>(executor.call(tool, Json.parseToJsonElement(arguments).jsonObject), tool).result

        val asking = async { call("ask_user", """{"question": "How does the paddle move?", "answers": ["With the arrow keys", "With the mouse"]}""") }
        runCurrent()
        val question = context.questions.pending.value.single()
        context.questions.answer(question.id, question.answers.first())
        assertEquals("With the arrow keys", asking.await().jsonObject.getValue("answer").jsonPrimitive.content)

        call(
            "create_page",
            """{"page": "README", "text": "# Pong for one\n\nKeep a ball in play with a paddle for as long as you can.\n\n## The ball\n\nThe ball bounces off the walls and the paddle.\n\n## Words\n\n- The paddle: Paddle\n- The ball: Ball\n\n## Planned\n\n- A ball that bounces\n- A paddle moved with the arrow keys\n- A score for every bounce\n"}""",
        )
        call("create_actor_type", """{"name": "Ball", "shape": "circle", "width": 40, "height": 40}""")
        call("add_event", """{"actorType": "Ball", "type": "addedToScene", "blocks": [{"type": "setVelocity", "x": 240, "y": 180}]}""")
        call("add_event", """{"actorType": "Ball", "type": "edgeTouched", "blocks": [{"type": "bounceOffEdge"}]}""")
        call("place_actor", """{"scene": "main", "actorType": "Ball", "x": 0, "y": 0}""")
        call(
            "edit_page",
            """{"page": "README", "section": "Planned", "text": "## Planned\n\n- A paddle moved with the arrow keys\n- A score for every bounce"}""",
        )
        call(
            "edit_page",
            """{"page": "README", "section": "The ball", "text": "## The ball\n\nThe [ball](../.kubriko/actors/Ball.json) flies off as the game starts and bounces off the walls."}""",
        )

        val page = checkNotNull(context.screenplay.read("README"))
        assertEquals(listOf("A paddle moved with the arrow keys", "A score for every bounce"), PageText.plannedItemsOf(page).map { it.text })
        assertEquals(emptyList(), ScreenplayStyle.problemsOf(page))
        assertEquals(listOf(EventType.ADDED_TO_SCENE, EventType.EDGE_TOUCHED), context.blockTypes!!.actorType("Ball")!!.events.map { it.type })
        assertTrue(SceneDocument.parse(context.scenes.read("main")!!).any { it.typeId == "Ball" })
        assertEquals(listOf("README"), context.mentionIndex().pagesMentioning(".kubriko/actors/Ball.json"))
    }
}
