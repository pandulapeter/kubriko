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
import com.pandulapeter.kubrikoCreator.actors.KeyChoice
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
    fun whatNamesSomethingThatNoLongerExistsIsReportedWhereItIs() {
        val door = ActorTypeDefinition(
            name = "Door",
            parent = "Gone",
            properties = listOf(PropertyDefinition.Number("hp")),
            events = listOf(
                EventDefinition(EventType.COLLISION, otherActorType = "Ghost"),
                EventDefinition(
                    EventType.EVERY_FRAME,
                    blocks = listOf(
                        BlockDefinition.AddActor("Brick"),
                        BlockDefinition.GoToScene("cave"),
                        BlockDefinition.SetNumber("gold", manager = "Game"),
                        BlockDefinition.Log("{hp} {mana}"),
                        BlockDefinition.GoToScene("main"),
                        BlockDefinition.Kotlin("Brick()"),
                    ),
                ),
            ),
        )
        val placed = mapOf("main" to mapOf("Door" to 1, "HandWritten" to 2))
        val missing = HealthChecks.of(listOf(door, game), ProjectRelations(listOf(door, game), placed), placed.keys).filter { it.kind == HealthFinding.Kind.REFERENCE_MISSING }
        assertEquals(
            listOf(
                HealthFinding(HealthFinding.Kind.REFERENCE_MISSING, RelationSubject.Type("Gone"), "Door"),
                HealthFinding(HealthFinding.Kind.REFERENCE_MISSING, RelationSubject.Type("Ghost"), "Door#0"),
                HealthFinding(HealthFinding.Kind.REFERENCE_MISSING, RelationSubject.Type("Brick"), "Door#1.0"),
                HealthFinding(HealthFinding.Kind.REFERENCE_MISSING, RelationSubject.Scene("cave"), "Door#1.1"),
                HealthFinding(HealthFinding.Kind.REFERENCE_MISSING, RelationSubject.Property("Game", "gold"), "Door#1.2"),
                HealthFinding(HealthFinding.Kind.REFERENCE_MISSING, RelationSubject.Property("Door", "mana"), "Door#1.3"),
            ),
            missing,
        )
    }

    @Test
    fun aGameWithOneSceneDoesntReportItAsUnreached() =
        assertEquals(emptyList(), HealthChecks.of(listOf(brick), ProjectRelations(listOf(brick), mapOf("main" to mapOf("Brick" to 1))), listOf("main")))

    @Test
    fun aRedeclaredPropertyIsReadWhereverItsFamilyReadsIt() {
        val health = PropertyDefinition.Number("health")
        val logsHealth = listOf(EventDefinition(EventType.EVERY_FRAME, blocks = listOf(BlockDefinition.Log("{health}"))))
        val parent = ActorTypeDefinition(name = "Parent", properties = listOf(health))
        val child = ActorTypeDefinition(name = "Child", parent = "Parent", properties = listOf(health.copy(initialValue = 3f)))
        val placed = mapOf("main" to mapOf("Parent" to 1, "Child" to 1))
        listOf(listOf(parent.copy(events = logsHealth), child), listOf(parent, child.copy(events = logsHealth))).forEach { types ->
            assertEquals(emptyList(), HealthChecks.of(types, ProjectRelations(types, placed), placed.keys))
        }
    }

    @Test
    fun aKotlinBlockReturningOrUsingARemovedLocalIsReported() {
        val kotlin = { code: String -> BlockDefinition.Kotlin(code) }
        val paddle = ActorTypeDefinition(
            name = "Paddle",
            events = listOf(
                EventDefinition(EventType.EVERY_FRAME, blocks = listOf(BlockDefinition.Stop, kotlin("if (velocity.x > 0f.sceneUnit) return"))),
                EventDefinition(EventType.EDGE_TOUCHED, blocks = listOf(kotlin("body.position = box.min"), kotlin("Logger.log(\"box\") // box"))),
                EventDefinition(EventType.KEY_PRESSED, key = KeyChoice.SPACE, blocks = listOf(kotlin("println(key)"), kotlin("val key = 1\nprintln(key)"))),
                EventDefinition(EventType.TIMER_FINISHED, timer = "spawn", blocks = listOf(kotlin("listOf(1).forEach { return@forEach }"), kotlin("fun skip(): Int { return 1 }\nprintln(skip())"))),
                EventDefinition(EventType.ADDED_TO_SCENE, blocks = listOf(kotlin("return"))),
            ),
        )
        val types = listOf(paddle)
        val placed = mapOf("main" to mapOf("Paddle" to 1))
        assertEquals(
            listOf(
                HealthFinding(HealthFinding.Kind.KOTLIN_BLOCK_SCOPE_CHANGED, RelationSubject.Type("Paddle"), "Paddle#0.1", "return"),
                HealthFinding(HealthFinding.Kind.KOTLIN_BLOCK_SCOPE_CHANGED, RelationSubject.Type("Paddle"), "Paddle#1.0", "box"),
                HealthFinding(HealthFinding.Kind.KOTLIN_BLOCK_SCOPE_CHANGED, RelationSubject.Type("Paddle"), "Paddle#2.0", "key"),
            ),
            HealthChecks.of(types, ProjectRelations(types, placed), placed.keys).filter { it.kind == HealthFinding.Kind.KOTLIN_BLOCK_SCOPE_CHANGED },
        )
    }

    @Test
    fun aFamilysKotlinBlocksAlwaysRanInFunctionsOfTheirOwn() {
        val parent = ActorTypeDefinition(name = "Parent", events = listOf(EventDefinition(EventType.EVERY_FRAME, blocks = listOf(BlockDefinition.Kotlin("return")))))
        val child = ActorTypeDefinition(name = "Child", parent = "Parent")
        assertEquals(emptyList(), KotlinBlockScopeChecks.of(listOf(parent, child)))
    }

    @Test
    fun theAiReadsFindingsAsSentences() = assertEquals(
        "Ball: the timerFinished Event of speedUp never fires, since no startTimer Block of its family starts it.",
        HealthFinding(HealthFinding.Kind.TIMER_NEVER_STARTED, RelationSubject.Type("Ball"), "speedUp").toString(),
    )
}
