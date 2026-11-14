/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.actors.store

import com.pandulapeter.kubrikoCreator.actors.ActorTypeDefinition
import com.pandulapeter.kubrikoCreator.actors.AnimatedValue
import com.pandulapeter.kubrikoCreator.actors.BlockDefinition
import com.pandulapeter.kubrikoCreator.actors.BlockTypeKind
import com.pandulapeter.kubrikoCreator.actors.EventDefinition
import com.pandulapeter.kubrikoCreator.actors.EventType
import com.pandulapeter.kubrikoCreator.actors.ManagerTypeDefinition
import com.pandulapeter.kubrikoCreator.actors.NameProblem
import com.pandulapeter.kubrikoCreator.actors.NumberValue
import com.pandulapeter.kubrikoCreator.actors.PropertyDefinition
import com.pandulapeter.kubrikoCreator.actors.PropertyNaming
import com.pandulapeter.kubrikoCreator.project.ProjectFormat
import com.pandulapeter.kubrikoCreator.project.ProjectFormatStatus
import com.pandulapeter.kubrikoCreator.scenes.SceneActorEntry
import com.pandulapeter.kubrikoCreator.scenes.SceneDocument
import com.pandulapeter.kubrikoCreator.testing.testDirectory
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class ReservedPropertyMigrationTest {
    private val root = testDirectory()
    private val settingsFile = File(root, ".kubriko/project.json").apply {
        parentFile.mkdirs()
        writeText("""{ "packageName": "com.example.game", "formatVersion": 2 }""")
    }
    private val sceneFile = File(root, "src/commonMain/composeResources/files/scenes/level.json")

    private fun everyFrame(vararg blocks: BlockDefinition) = listOf(EventDefinition(EventType.EVERY_FRAME, blocks = blocks.toList()))

    private fun migratedTypes(): Map<String, Any> {
        assertEquals(ProjectFormatStatus.Writable, ProjectFormat(root, creatorVersion = "1.0.0").prepare())
        val loaded = assertNotNull(BlockTypeStore.open(root)).load()
        assertEquals(emptyList(), loaded.unreadableFiles)
        assertTrue("\"formatVersion\": ${ProjectFormat.CURRENT_VERSION}" in settingsFile.readText())
        return loaded.types.associateBy { it.name }
    }

    @Test
    fun lookCanNoLongerBeAPropertysName() {
        assertEquals(NameProblem.PROPERTY_RESERVED, PropertyNaming.validate("look", emptyList(), BlockTypeKind.ACTOR))
    }

    @Test
    fun formatVersionFourReservesWhatPlaysASpritesFrames() {
        listOf("animation", "animationSpeed", "isAnimationLooping").forEach { name ->
            assertEquals(NameProblem.PROPERTY_RESERVED, PropertyNaming.validate(name, emptyList(), BlockTypeKind.ACTOR), name)
        }
        assertNotNull(BlockTypeStore.open(root)).save(
            ActorTypeDefinition(
                name = "Hero",
                properties = listOf(PropertyDefinition.Number("animationSpeed", 2f), PropertyDefinition.Toggle("isAnimationLooping")),
                events = everyFrame(BlockDefinition.Kotlin("animationSpeed += 1f")),
            ),
        )
        sceneFile.apply { parentFile.mkdirs() }.writeText(
            SceneDocument.format(listOf(SceneActorEntry(typeId = "Hero", state = """{"animationSpeed":3.0,"isAnimationLooping":true}""", id = "h"))),
        )
        val hero = migratedTypes().getValue("Hero") as ActorTypeDefinition
        assertEquals(listOf(PropertyDefinition.Number("animationSpeed2", 2f), PropertyDefinition.Toggle("isAnimationLooping2")), hero.properties)
        assertEquals(everyFrame(BlockDefinition.Kotlin("animationSpeed2 += 1f")), hero.events)
        assertEquals("""{"animationSpeed2":3.0,"isAnimationLooping2":true}""", SceneDocument.parse(sceneFile.readText()).single().state)
    }

    @Test
    fun formatVersionElevenReservesTheFunctionMovingWithKeys() {
        assertEquals(NameProblem.PROPERTY_RESERVED, PropertyNaming.validate("velocityFromKeys", emptyList(), BlockTypeKind.ACTOR))
        assertNotNull(BlockTypeStore.open(root)).save(
            ActorTypeDefinition(
                name = "Hero",
                properties = listOf(PropertyDefinition.Number("velocityFromKeys", 150f)),
                events = everyFrame(BlockDefinition.MoveWithKeys(speed = NumberValue.Property("velocityFromKeys"))),
            ),
        )
        val hero = migratedTypes().getValue("Hero") as ActorTypeDefinition
        assertEquals(listOf(PropertyDefinition.Number("velocityFromKeys2", 150f)), hero.properties)
        assertEquals(everyFrame(BlockDefinition.MoveWithKeys(speed = NumberValue.Property("velocityFromKeys2"))), hero.events)
    }

    @Test
    fun formatVersionTenReservesWhatPlaysTheAnimateBlocks() {
        listOf("scaleTweenFrom", "opacityTweenDuration", "colorTweenRemovesActor", "positionTweenEasing", "startColorTween", "tweenProgress", "com").forEach { name ->
            assertEquals(NameProblem.PROPERTY_RESERVED, PropertyNaming.validate(name, emptyList(), BlockTypeKind.ACTOR), name)
        }
        assertNotNull(BlockTypeStore.open(root)).save(
            ActorTypeDefinition(
                name = "Spark",
                properties = listOf(PropertyDefinition.Number("scaleTweenTo", 3f), PropertyDefinition.Toggle("com")),
                events = everyFrame(BlockDefinition.Animate(AnimatedValue.Scale(to = NumberValue.Property("scaleTweenTo")))),
            ),
        )
        sceneFile.apply { parentFile.mkdirs() }.writeText(
            SceneDocument.format(listOf(SceneActorEntry(typeId = "Spark", state = """{"scaleTweenTo":4.0,"com":true}""", id = "s"))),
        )
        val spark = migratedTypes().getValue("Spark") as ActorTypeDefinition
        assertEquals(listOf(PropertyDefinition.Number("scaleTweenTo2", 3f), PropertyDefinition.Toggle("com2")), spark.properties)
        assertEquals(everyFrame(BlockDefinition.Animate(AnimatedValue.Scale(to = NumberValue.Property("scaleTweenTo2")))), spark.events)
        assertEquals("""{"scaleTweenTo2":4.0,"com2":true}""", SceneDocument.parse(sceneFile.readText()).single().state)
    }

    @Test
    fun formatVersionNineReservesTheOpacity() {
        assertEquals(NameProblem.PROPERTY_RESERVED, PropertyNaming.validate("opacity", emptyList(), BlockTypeKind.ACTOR))
        assertNotNull(BlockTypeStore.open(root)).save(
            ActorTypeDefinition(
                name = "Ghost",
                properties = listOf(PropertyDefinition.Number("opacity", 40f)),
                events = everyFrame(BlockDefinition.SetOpacity(NumberValue.Property("opacity"))),
            ),
        )
        val ghost = migratedTypes().getValue("Ghost") as ActorTypeDefinition
        assertEquals(listOf(PropertyDefinition.Number("opacity2", 40f)), ghost.properties)
        assertEquals(everyFrame(BlockDefinition.SetOpacity(NumberValue.Property("opacity2"))), ghost.events)
    }

    @Test
    fun formatVersionEightReservesTheFunctionMovingInADirection() {
        assertEquals(NameProblem.PROPERTY_RESERVED, PropertyNaming.validate("velocityInDirection", emptyList(), BlockTypeKind.ACTOR))
        assertNotNull(BlockTypeStore.open(root)).save(
            ActorTypeDefinition(
                name = "Debris",
                properties = listOf(PropertyDefinition.Number("velocityInDirection", 90f)),
                events = everyFrame(BlockDefinition.MoveInDirection(direction = NumberValue.Property("velocityInDirection"))),
            ),
        )
        val debris = migratedTypes().getValue("Debris") as ActorTypeDefinition
        assertEquals(listOf(PropertyDefinition.Number("velocityInDirection2", 90f)), debris.properties)
        assertEquals(everyFrame(BlockDefinition.MoveInDirection(direction = NumberValue.Property("velocityInDirection2"))), debris.events)
    }

    @Test
    fun anActorFamilysLookIsRenamedWhereItIsDeclaredUsedAndKeptInScenes() {
        val store = assertNotNull(BlockTypeStore.open(root))
        store.save(
            ActorTypeDefinition(
                name = "Enemy",
                properties = listOf(PropertyDefinition.Number("look", 2f), PropertyDefinition.Number("look2")),
                events = everyFrame(BlockDefinition.ChangeNumber(property = "look"), BlockDefinition.Kotlin("look += look2")),
            ),
        )
        store.save(ActorTypeDefinition(name = "Ghost", parent = "Enemy", events = everyFrame(BlockDefinition.Kotlin("look = 0f"))))
        store.save(ActorTypeDefinition(name = "Bat", parent = "Enemy", properties = listOf(PropertyDefinition.Number("look", 5f))))
        store.save(ActorTypeDefinition(name = "Ball", events = everyFrame(BlockDefinition.Kotlin("val look = 1f"))))
        sceneFile.apply { parentFile.mkdirs() }.writeText(
            SceneDocument.format(
                listOf(
                    SceneActorEntry(typeId = "Ghost", state = """{"look":3.0,"look2":1.0}""", id = "a"),
                    SceneActorEntry(typeId = "Ball", state = """{"look":4.0}""", id = "b"),
                ),
            ),
        )
        val types = migratedTypes()
        val enemy = types.getValue("Enemy") as ActorTypeDefinition
        assertEquals(listOf(PropertyDefinition.Number("look3", 2f), PropertyDefinition.Number("look2")), enemy.properties)
        assertEquals(everyFrame(BlockDefinition.ChangeNumber(property = "look3"), BlockDefinition.Kotlin("look3 += look2")), enemy.events)
        assertEquals(everyFrame(BlockDefinition.Kotlin("look3 = 0f")), (types.getValue("Ghost") as ActorTypeDefinition).events)
        assertEquals(listOf(PropertyDefinition.Number("look3", 5f)), (types.getValue("Bat") as ActorTypeDefinition).properties)
        assertEquals(everyFrame(BlockDefinition.Kotlin("val look = 1f")), (types.getValue("Ball") as ActorTypeDefinition).events)
        assertEquals(
            listOf("""{"look3":3.0,"look2":1.0}""", """{"look":4.0}"""),
            SceneDocument.parse(sceneFile.readText()).map { it.state },
        )
    }

    @Test
    fun aRunAfterTheUsersWereSavedButNotTheDeclarerRenamesItTheSameWay() {
        val store = assertNotNull(BlockTypeStore.open(root))
        store.save(ActorTypeDefinition(name = "Enemy", properties = listOf(PropertyDefinition.Number("look"))))
        store.save(ActorTypeDefinition(name = "Ghost", parent = "Enemy", events = everyFrame(BlockDefinition.Kotlin("look2 = 0f"))))
        val types = migratedTypes()
        assertEquals(listOf(PropertyDefinition.Number("look2")), (types.getValue("Enemy") as ActorTypeDefinition).properties)
        assertEquals(everyFrame(BlockDefinition.Kotlin("look2 = 0f")), (types.getValue("Ghost") as ActorTypeDefinition).events)
    }

    @Test
    fun aManagerTypeKeepsTheNamesOnlyActorTypesReserve() {
        val store = assertNotNull(BlockTypeStore.open(root))
        val game = ManagerTypeDefinition("Game", properties = listOf(PropertyDefinition.Number("look"), PropertyDefinition.Number("velocityFromKeys")))
        store.save(game)
        val ball = ActorTypeDefinition(
            name = "Ball",
            events = everyFrame(BlockDefinition.SetNumber(manager = "Game", property = "look", value = NumberValue.Fixed(1f)), BlockDefinition.Kotlin("gameManager.look += 1f")),
        )
        store.save(ball)
        val types = migratedTypes()
        assertEquals(game.properties, (types.getValue("Game") as ManagerTypeDefinition).properties)
        assertEquals(ball.events, (types.getValue("Ball") as ActorTypeDefinition).events)
        assertEquals(null, PropertyNaming.validate("look", emptyList(), BlockTypeKind.MANAGER))
        assertEquals(NameProblem.PROPERTY_RESERVED, PropertyNaming.validate("look", emptyList(), BlockTypeKind.ACTOR))
        assertEquals(NameProblem.PROPERTY_RESERVED, PropertyNaming.validate("velocity", emptyList(), BlockTypeKind.MANAGER))
    }

    @Test
    fun formatVersionTwelveReservesTheCountingMembersEachInTheKindOfTypeThatDeclaresThem() {
        val store = assertNotNull(BlockTypeStore.open(root))
        val game = ManagerTypeDefinition(
            "Game",
            properties = listOf(PropertyDefinition.Number("actorsAdded"), PropertyDefinition.Toggle("kotlin"), PropertyDefinition.Number("actorCountGuard")),
        )
        store.save(game)
        val ball = ActorTypeDefinition(
            name = "Ball",
            properties = listOf(PropertyDefinition.Number("actorsRemoved"), PropertyDefinition.Number("actorCountGuard")),
            events = everyFrame(BlockDefinition.ChangeNumber(manager = "Game", property = "actorsAdded"), BlockDefinition.ChangeNumber(property = "actorCountGuard")),
        )
        store.save(ball)
        val types = migratedTypes()
        assertEquals(
            listOf(PropertyDefinition.Number("actorsAdded2"), PropertyDefinition.Toggle("kotlin2"), PropertyDefinition.Number("actorCountGuard")),
            (types.getValue("Game") as ManagerTypeDefinition).properties,
        )
        val migratedBall = types.getValue("Ball") as ActorTypeDefinition
        assertEquals(listOf(PropertyDefinition.Number("actorsRemoved"), PropertyDefinition.Number("actorCountGuard2")), migratedBall.properties)
        assertEquals(everyFrame(BlockDefinition.ChangeNumber(manager = "Game", property = "actorsAdded2"), BlockDefinition.ChangeNumber(property = "actorCountGuard2")), migratedBall.events)
        assertEquals(null, PropertyNaming.validate("kotlin", emptyList(), BlockTypeKind.ACTOR))
        assertEquals(NameProblem.PROPERTY_RESERVED, PropertyNaming.validate("sceneChangesWhenAdded", emptyList(), BlockTypeKind.ACTOR))
        assertEquals(null, PropertyNaming.validate("sceneChangesWhenAdded", emptyList(), BlockTypeKind.MANAGER))
    }

    @Test
    fun aProjectWithoutALookPropertyIsLeftAsItWas() {
        val store = assertNotNull(BlockTypeStore.open(root))
        store.save(ActorTypeDefinition(name = "Ball", properties = listOf(PropertyDefinition.Number("speed"))))
        val file = File(root, ".kubriko/actors/Ball.json")
        val text = file.readText()
        migratedTypes()
        assertEquals(text, file.readText())
    }
}
