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
import com.pandulapeter.kubrikoCreator.actors.EventDefinition
import com.pandulapeter.kubrikoCreator.actors.EventType
import com.pandulapeter.kubrikoCreator.actors.ManagerTypeDefinition
import com.pandulapeter.kubrikoCreator.actors.PropertyDefinition
import kotlin.test.Test
import kotlin.test.assertEquals

class HealthChecksTest {
    private val ball = ActorTypeDefinition(
        name = "Ball",
        events = listOf(
            EventDefinition(EventType.COLLISION, otherActorType = "Brick", blocks = listOf(BlockDefinition.Log("Score: {Game.score}"))),
            EventDefinition(EventType.COLLISION, otherActorType = "Ghost"),
            EventDefinition(EventType.TIMER_FINISHED, timer = "speedUp"),
            EventDefinition(EventType.TIMER_FINISHED, timer = "spawn"),
            EventDefinition(EventType.ADDED_TO_SCENE, blocks = listOf(BlockDefinition.StartTimer("spawn"), BlockDefinition.GoToScene("level-2"))),
        ),
    )
    private val enemyBase = ActorTypeDefinition(name = "EnemyBase")
    private val bat = ActorTypeDefinition(name = "Bat", parent = "EnemyBase")
    private val brick = ActorTypeDefinition(name = "Brick")
    private val ghost = ActorTypeDefinition(name = "Ghost")
    private val game = ManagerTypeDefinition(
        name = "Game",
        properties = listOf(PropertyDefinition.Number("score"), PropertyDefinition.Number("lives"), PropertyDefinition.Number("combo")),
        events = listOf(EventDefinition(EventType.EVERY_FRAME, blocks = listOf(BlockDefinition.Kotlin("if (lives < 0) println(\"over\")")))),
    )
    private val types = listOf(ball, enemyBase, bat, brick, ghost, game)
    private val scenes = mapOf(
        "level-1" to mapOf("Ball" to 1, "Brick" to 10, "Bat" to 2),
        "level-2" to mapOf("Ball" to 1),
        "secret" to emptyMap(),
    )

    @Test
    fun whatIsWorthALookIsFoundFromTheModel() = assertEquals(
        listOf(
            HealthFinding(HealthFinding.Kind.UNUSED_ACTOR_TYPE, RelationSubject.Type("Ghost")),
            HealthFinding(HealthFinding.Kind.UNREACHED_SCENE, RelationSubject.Scene("level-1")),
            HealthFinding(HealthFinding.Kind.UNREACHED_SCENE, RelationSubject.Scene("secret")),
            HealthFinding(HealthFinding.Kind.UNREAD_PROPERTY, RelationSubject.Property("Game", "combo")),
            HealthFinding(HealthFinding.Kind.TIMER_NEVER_STARTED, RelationSubject.Type("Ball"), "speedUp"),
            HealthFinding(HealthFinding.Kind.COLLISION_WITH_ABSENT_TYPE, RelationSubject.Type("Ball"), "Ghost"),
        ),
        HealthChecks.of(types, ProjectRelations(types, scenes), scenes.keys).sortedBy { it.kind.ordinal },
    )

    @Test
    fun aGameWithOneSceneDoesntReportItAsUnreached() =
        assertEquals(emptyList(), HealthChecks.of(listOf(brick), ProjectRelations(listOf(brick), mapOf("main" to mapOf("Brick" to 1))), listOf("main")))

    @Test
    fun theAiReadsFindingsAsSentences() = assertEquals(
        "Ball: the timerFinished Event of speedUp never fires, since no startTimer Block of its family starts it.",
        HealthFinding(HealthFinding.Kind.TIMER_NEVER_STARTED, RelationSubject.Type("Ball"), "speedUp").toString(),
    )
}
