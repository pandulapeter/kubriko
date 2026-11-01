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
import com.pandulapeter.kubrikoCreator.actors.ConditionDefinition
import com.pandulapeter.kubrikoCreator.actors.EventDefinition
import com.pandulapeter.kubrikoCreator.actors.EventType
import com.pandulapeter.kubrikoCreator.actors.ManagerTypeDefinition
import com.pandulapeter.kubrikoCreator.actors.PropertyDefinition
import com.pandulapeter.kubrikoCreator.actors.ScreenTextDefinition
import kotlin.test.Test
import kotlin.test.assertEquals

class ProjectRelationsTest {
    private val game = ManagerTypeDefinition(
        name = "Game",
        properties = listOf(PropertyDefinition.Number("score")),
        texts = listOf(ScreenTextDefinition(text = "Score: {score}")),
        kotlinUi = "Text(\"Bricks: ${'$'}{actorManager.allActors.value.count { it is Brick }}\")",
    )
    private val block = ActorTypeDefinition(name = "Block", properties = listOf(PropertyDefinition.Number("points")))
    private val brick = ActorTypeDefinition(name = "Brick", parent = "Block")
    private val ball = ActorTypeDefinition(
        name = "Ball",
        events = listOf(
            EventDefinition(EventType.ADDED_TO_SCENE, blocks = listOf(BlockDefinition.SetVelocity(200f, 200f))),
            EventDefinition(
                type = EventType.COLLISION,
                otherActorType = "Brick",
                blocks = listOf(
                    BlockDefinition.RemoveOther,
                    BlockDefinition.ChangeNumber("score", 10f, manager = "Game"),
                    BlockDefinition.If(ConditionDefinition.Number("score", value = 100f, manager = "Game")),
                    BlockDefinition.GoToScene("won"),
                    BlockDefinition.End,
                    BlockDefinition.Log("Score: {Game.score}"),
                    BlockDefinition.AddActor("Ball"),
                ),
            ),
        ),
    )
    private val relations = ProjectRelations(
        types = listOf(game, block, brick, ball),
        scenes = mapOf("level" to mapOf("Ball" to 1, "Brick" to 40, "SceneSettings" to 1)),
    )

    @Test
    fun whatATypeUsesIsReadFromItsEventsAndBlocks() {
        assertEquals(
            listOf(
                "Ball#1 collides with Brick",
                "Ball#1.1 changes Game.score",
                "Ball#1.2 reads Game.score",
                "Ball#1.3 goes to Scene won",
                "Ball#1.5 reads Game.score",
                "Ball#1.6 adds Ball",
            ),
            relations.from(RelationSubject.Type("Ball")).map { it.toString() },
        )
        assertEquals(listOf("Brick inherits from Block"), relations.from(RelationSubject.Type("Brick")).map { it.toString() })
    }

    @Test
    fun whatRefersToATypeIncludesItsPropertiesAndTheScenesPlacingIt() {
        assertEquals(
            listOf("Game text 0 reads Game.score", "Ball#1.1 changes Game.score", "Ball#1.2 reads Game.score", "Ball#1.5 reads Game.score"),
            relations.to(RelationSubject.Type("Game")).map { it.toString() },
        )
        assertEquals(
            listOf("Game Kotlin UI names in Kotlin Brick", "Ball#1 collides with Brick", "Scene level places Brick (40)"),
            relations.to(RelationSubject.Type("Brick")).map { it.toString() },
        )
        assertEquals(listOf("Ball#1.3 goes to Scene won"), relations.to(RelationSubject.Scene("won")).map { it.toString() })
    }

    @Test
    fun inheritedPropertiesBelongToTheTypeDeclaringThem() {
        assertEquals("Block", relations.ownerOfProperty("Brick", "points"))
        assertEquals("Brick", relations.ownerOfProperty("Brick", "missing"))
        val child = ProjectRelations(listOf(block, brick.copy(events = listOf(EventDefinition(EventType.EVERY_FRAME, blocks = listOf(BlockDefinition.SetNumber("points", 1f)))))), emptyMap())
        assertEquals(listOf("Brick#0.0 changes Block.points"), child.to(RelationSubject.Property("Block", "points")).map { it.toString() })
    }
}
