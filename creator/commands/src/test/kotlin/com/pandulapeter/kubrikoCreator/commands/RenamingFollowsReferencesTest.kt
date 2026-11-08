/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.commands

import com.pandulapeter.kubrikoCreator.actors.ActorTypeDefinition
import com.pandulapeter.kubrikoCreator.actors.BlockDefinition
import com.pandulapeter.kubrikoCreator.actors.ConditionDefinition
import com.pandulapeter.kubrikoCreator.actors.EventDefinition
import com.pandulapeter.kubrikoCreator.actors.EventType
import com.pandulapeter.kubrikoCreator.actors.ManagerTypeDefinition
import com.pandulapeter.kubrikoCreator.actors.PropertyDefinition
import com.pandulapeter.kubrikoCreator.actors.ScreenTextDefinition
import com.pandulapeter.kubrikoCreator.commands.actorTypes.EditProperty
import com.pandulapeter.kubrikoCreator.commands.actorTypes.RenameActorType
import com.pandulapeter.kubrikoCreator.commands.managerTypes.RenameManagerType
import com.pandulapeter.kubrikoCreator.commands.scenes.RenameScene
import com.pandulapeter.kubrikoCreator.commands.screenplay.CreatePage
import com.pandulapeter.kubrikoCreator.context.RelationKind
import com.pandulapeter.kubrikoCreator.context.RelationSubject
import com.pandulapeter.kubrikoCreator.context.Usage
import com.pandulapeter.kubrikoCreator.context.UsagePlace
import kotlinx.coroutines.test.runTest
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Every rename follows what a delete of the same thing would clean up ([com.pandulapeter.kubrikoCreator.context.ProjectReferences]):
 * afterwards the new name has the usages the old one had, and the old one none but hand-written Kotlin, which Kubriko
 * Creator never rewrites. Each project here refers to what is renamed in every way [RelationKind] has for it, so that a
 * kind a rename misses fails here.
 */
class RenamingFollowsReferencesTest {
    private val actorTypeKinds = setOf(
        RelationKind.ADDS,
        RelationKind.MOVES_TOWARDS,
        RelationKind.NAMES_IN_KOTLIN,
        RelationKind.COLLIDES_WITH,
        RelationKind.PLACES,
        RelationKind.INHERITS_FROM,
        RelationKind.MENTIONS,
    )
    private val managerTypeKinds = setOf(RelationKind.CHANGES, RelationKind.READS, RelationKind.MENTIONS)
    private val sceneKinds = setOf(RelationKind.GOES_TO, RelationKind.STARTS_WITH, RelationKind.NAMES_IN_KOTLIN, RelationKind.MENTIONS)
    private val propertyKinds = setOf(RelationKind.READS, RelationKind.CHANGES)

    @Test
    fun everyKindOfReferenceIsCoveredByARename() {
        assertEquals(RelationKind.entries.toSet(), actorTypeKinds + managerTypeKinds + sceneKinds + propertyKinds)
    }

    private fun CommandContext.writeFile(path: String, text: String) = File(project.rootDirectory, path).apply { parentFile.mkdirs() }.writeText(text)

    /**
     * What refers to a subject, apart from hand-written Kotlin: where, how, and how many times.
     */
    private fun List<Usage>.followed() = filterNot { it.place is UsagePlace.KotlinFile }.map { Triple(it.place.toString(), it.kind, it.count) }.sortedBy { it.toString() }

    private suspend fun CommandContext.assertFollowed(before: RelationSubject, after: RelationSubject, kinds: Set<RelationKind>, rename: suspend () -> Unit) {
        val usages = usagesOf(before)
        assertEquals(kinds, usages.map { it.kind }.toSet(), "The project refers to $before in every way it can.")
        rename()
        assertEquals(usages.followed(), usagesOf(after).followed())
        assertTrue(usagesOf(before).all { it.place is UsagePlace.KotlinFile }, "Nothing but hand-written Kotlin still refers to $before.")
    }

    @Test
    fun renamingAnActorTypeFollowsEveryReferenceToIt() = runTest {
        val brick = ActorTypeDefinition(name = "Brick")
        val bigBrick = ActorTypeDefinition(name = "BigBrick", parent = "Brick")
        val ball = ActorTypeDefinition(
            name = "Ball",
            events = listOf(
                EventDefinition(EventType.EVERY_FRAME, blocks = listOf(BlockDefinition.AddActor("Brick"), BlockDefinition.MoveTowardsNearest("Brick"), BlockDefinition.Kotlin("println(Brick::class)"))),
                EventDefinition(EventType.COLLISION, otherActorType = "Brick"),
            ),
        )
        val context = commandContext(ball, brick, bigBrick)
        context.writeFile("src/commonMain/composeResources/files/scenes/level.json", """[{"typeId":"Brick","state":"{}"}]""")
        context.scenes.refresh()
        context.writeFile("src/commonMain/kotlin/Game.kt", "val first = Brick()")
        CreatePage.apply(context, CreatePage.Input("README", "# Breakout\n\nThe ball breaks each [brick](../.kubriko/actors/Brick.json).\n"))
        context.assertFollowed(RelationSubject.Type("Brick"), RelationSubject.Type("Stone"), actorTypeKinds) {
            RenameActorType.apply(context, RenameActorType.Input("Brick", "Stone"))
        }
    }

    @Test
    fun renamingAManagerTypeFollowsEveryReferenceToIt() = runTest {
        val game = ManagerTypeDefinition(name = "Game", properties = listOf(PropertyDefinition.Number("score")))
        val hud = ManagerTypeDefinition(name = "Hud", texts = listOf(ScreenTextDefinition(text = "Score: {Game.score}")))
        val player = ActorTypeDefinition(
            name = "Player",
            events = listOf(EventDefinition(EventType.EVERY_FRAME, blocks = listOf(BlockDefinition.SetNumber("score", 1f, manager = "Game"), BlockDefinition.If(ConditionDefinition.Number("score", value = 3f, manager = "Game")), BlockDefinition.End))),
        )
        val context = commandContext(game, hud, player)
        CreatePage.apply(context, CreatePage.Input("README", "# Game\n\nThe [game](../.kubriko/managers/Game.json) keeps the score.\n"))
        context.assertFollowed(RelationSubject.Type("Game"), RelationSubject.Type("Match"), managerTypeKinds) {
            RenameManagerType.apply(context, RenameManagerType.Input("Game", "Match"))
        }
    }

    @Test
    fun renamingASceneFollowsEveryReferenceToIt() = runTest {
        val door = ActorTypeDefinition(name = "Door", events = listOf(EventDefinition(EventType.EVERY_FRAME, blocks = listOf(BlockDefinition.GoToScene("cave")))))
        val context = commandContext(door)
        context.writeFile("src/commonMain/composeResources/files/scenes/cave.json", "[]")
        context.scenes.refresh()
        context.writeFile("build.gradle.kts", "val gameFirstScene = providers.gradleProperty(\"game.firstScene\")\n")
        context.writeFile("gradle.properties", "game.name=Game\ngame.firstScene=cave\n")
        context.writeFile("src/commonMain/kotlin/Game.kt", "val start = \"cave\"")
        CreatePage.apply(context, CreatePage.Input("README", "# Game\n\nIt ends in the [cave](../src/commonMain/composeResources/files/scenes/cave.json).\n"))
        context.assertFollowed(RelationSubject.Scene("cave"), RelationSubject.Scene("grotto"), sceneKinds) {
            RenameScene.apply(context, RenameScene.Input("cave", "grotto"))
        }
    }

    @Test
    fun renamingAPropertyFollowsEveryReferenceToIt() = runTest {
        val enemy = ActorTypeDefinition(
            name = "Enemy",
            properties = listOf(PropertyDefinition.Number("hp")),
            events = listOf(EventDefinition(EventType.EVERY_FRAME, blocks = listOf(BlockDefinition.Log("{hp} left"), BlockDefinition.ChangeNumber("hp"), BlockDefinition.If(ConditionDefinition.Number("hp")), BlockDefinition.End))),
        )
        val boss = ActorTypeDefinition(name = "Boss", parent = "Enemy", events = listOf(EventDefinition(EventType.EVERY_FRAME, blocks = listOf(BlockDefinition.SetNumber("hp", 5f)))))
        val context = commandContext(enemy, boss)
        context.assertFollowed(RelationSubject.Property("Enemy", "hp"), RelationSubject.Property("Enemy", "health"), propertyKinds) {
            EditProperty.apply(context, EditProperty.Input("Enemy", "hp", PropertyDefinition.Number("health")))
        }
    }
}
