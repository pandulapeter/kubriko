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

import com.pandulapeter.kubrikoCreator.actors.ActorTypeDefinition
import com.pandulapeter.kubrikoCreator.actors.BlockDefinition
import com.pandulapeter.kubrikoCreator.actors.EventDefinition
import com.pandulapeter.kubrikoCreator.actors.EventType
import com.pandulapeter.kubrikoCreator.actors.ManagerTypeDefinition
import com.pandulapeter.kubrikoCreator.actors.PropertyDefinition
import com.pandulapeter.kubrikoCreator.commands.CommandExecutor
import com.pandulapeter.kubrikoCreator.commands.CommandOutcome
import com.pandulapeter.kubrikoCreator.commands.commandContext
import com.pandulapeter.kubrikoCreator.commands.testScope
import com.pandulapeter.kubrikoCreator.context.RelationSubject
import com.pandulapeter.kubrikoCreator.foundation.EditorMutation
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class FindUsagesTest {
    private val ball = ActorTypeDefinition(name = "Ball")
    private val game = ManagerTypeDefinition("Game", properties = listOf(PropertyDefinition.Number("score")))
    private val paddle = ActorTypeDefinition(
        name = "Paddle",
        events = listOf(
            EventDefinition(EventType.COLLISION, otherActorType = "Ball", blocks = listOf(BlockDefinition.ChangeNumber("score", manager = "Game"))),
            EventDefinition(EventType.EDGE_TOUCHED, blocks = listOf(BlockDefinition.GoToScene("over"))),
        ),
    )

    private suspend fun TestScope.executor(): CommandExecutor {
        val context = commandContext(ball, paddle, game)
        @OptIn(EditorMutation::class)
        context.scenes.write("main", """[{"typeId":"Ball","state":"{}"},{"typeId":"Ball","state":"{}"}]""")
        @OptIn(EditorMutation::class)
        context.scenes.write("over", "[]")
        return CommandExecutor(context, testScope())
    }

    private suspend fun CommandExecutor.find(arguments: JsonObject) = assertIs<CommandOutcome.Success>(call("find_usages", arguments)).result

    @Test
    fun aTypesUsagesNameEveryPlaceWithHowManyTimesItRefersToIt() = runTest {
        assertEquals(
            Json.parseToJsonElement("""{"subject":"Ball","usages":["Paddle#0 collides with Ball","Scene main places Ball (2)"]}"""),
            executor().find(buildJsonObject { put("type", "Ball") }),
        )
    }

    @Test
    fun aPropertysAndASceneUsagesAreFoundToo() = runTest {
        val executor = executor()
        assertEquals(
            Json.parseToJsonElement("""{"subject":"Game.score","usages":["Paddle#0.0 changes Game.score"]}"""),
            executor.find(buildJsonObject { put("type", "Game"); put("property", "score") }),
        )
        assertEquals(
            Json.parseToJsonElement("""{"subject":"Scene over","usages":["Paddle#1.0 goes to Scene over"]}"""),
            executor.find(buildJsonObject { put("scene", "over") }),
        )
    }

    @Test
    fun theUserSeesWhatTheAiDoes() = runTest {
        val executor = executor()
        assertEquals(listOf("Paddle#0 collides with Ball", "Scene main places Ball (2)"), executor.findUsages(RelationSubject.Type("Ball"))?.map { it.reference })
        assertEquals(null, executor.findUsages(RelationSubject.Type("Wall")), "a type renamed or deleted since")
        assertEquals(null, executor.findUsages(RelationSubject.Property("Game", "lives")))
        assertEquals(null, executor.findUsages(RelationSubject.Scene("nowhere")))
    }

    @Test
    fun exactlyOneExistingThingIsNamed() = runTest {
        val executor = executor()
        listOf(
            buildJsonObject { },
            buildJsonObject { put("type", "Ball"); put("scene", "main") },
            buildJsonObject { put("scene", "main"); put("property", "score") },
            buildJsonObject { put("type", "Wall") },
            buildJsonObject { put("type", "Game"); put("property", "lives") },
            buildJsonObject { put("scene", "nowhere") },
            buildJsonObject { put("page", "nowhere") },
        ).forEach { arguments -> assertIs<CommandOutcome.Rejected>(executor.call("find_usages", arguments), arguments.toString()) }
    }
}
