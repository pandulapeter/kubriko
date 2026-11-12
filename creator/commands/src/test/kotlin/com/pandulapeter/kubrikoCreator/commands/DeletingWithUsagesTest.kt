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
import com.pandulapeter.kubrikoCreator.actors.NumberValue
import com.pandulapeter.kubrikoCreator.actors.PropertyDefinition
import com.pandulapeter.kubrikoCreator.actors.ScreenTextDefinition
import com.pandulapeter.kubrikoCreator.commands.actorTypes.AddProperty
import com.pandulapeter.kubrikoCreator.commands.actorTypes.CreateActorType
import com.pandulapeter.kubrikoCreator.commands.actorTypes.DeleteActorType
import com.pandulapeter.kubrikoCreator.commands.actorTypes.RemoveProperty
import com.pandulapeter.kubrikoCreator.commands.managerTypes.DeleteManagerType
import com.pandulapeter.kubrikoCreator.commands.project.CheckChange
import com.pandulapeter.kubrikoCreator.commands.scenes.DeleteScene
import com.pandulapeter.kubrikoCreator.context.RelationKind
import com.pandulapeter.kubrikoCreator.context.RelationPlace
import com.pandulapeter.kubrikoCreator.context.RelationSubject
import com.pandulapeter.kubrikoCreator.context.Usage
import com.pandulapeter.kubrikoCreator.context.UsageEffect
import com.pandulapeter.kubrikoCreator.context.UsagePlace
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import java.io.File
import java.io.IOException
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull

class DeletingWithUsagesTest {
    private val brick = ActorTypeDefinition(name = "Brick")
    private val ball = ActorTypeDefinition(
        name = "Ball",
        events = listOf(
            EventDefinition(
                EventType.EVERY_FRAME,
                blocks = listOf(
                    BlockDefinition.AddActor("Brick"),
                    BlockDefinition.Stop,
                    BlockDefinition.MoveTowardsNearest("Brick"),
                    BlockDefinition.Kotlin("println(Brick::class)"),
                ),
            ),
            EventDefinition(EventType.COLLISION, otherActorType = "Brick", blocks = listOf(BlockDefinition.RemoveOther)),
            EventDefinition(EventType.COLLISION, otherActorType = "Ball"),
        ),
    )
    private val sceneText = """[{"typeId":"Brick","state":"{}"},{"typeId":"Ball","state":"{}"},{"typeId":"Brick","state":"{}"}]"""

    private fun CommandContext.sceneFile(name: String) = File(project.rootDirectory, "src/commonMain/composeResources/files/scenes/$name.json")

    private fun CommandContext.writeScene(name: String, text: String) {
        sceneFile(name).apply { parentFile.mkdirs() }.writeText(text)
        scenes.refresh()
    }

    private fun CommandContext.writeKotlin(path: String, text: String) = File(project.rootDirectory, path).apply { parentFile.mkdirs() }.writeText(text)

    private fun CommandContext.snapshot() = blockTypes!!.types.value to project.rootDirectory.walkTopDown().filter { it.isFile }.associate { it.path to it.readText() }

    @Test
    fun deletingAnActorTypeCleansUpItsUsagesAndIsUndoneByteForByte() = runTest {
        val context = commandContext(ball, brick)
        context.writeScene("level", sceneText)
        context.writeKotlin("src/commonMain/kotlin/Game.kt", "val first = Brick()")
        val after = context.assertRoundTrip(Invocation(DeleteActorType, DeleteActorType.Input("Brick"))) { context.snapshot() }
        val ballAfter = after.first.single()
        assertEquals(listOf("Ball"), after.first.map { it.name })
        assertEquals(listOf(BlockDefinition.Stop, BlockDefinition.Kotlin("println(Brick::class)")), ballAfter.events[0].blocks)
        assertEquals(listOf(EventType.EVERY_FRAME, EventType.COLLISION), ballAfter.events.map { it.type })
        assertEquals("Ball", ballAfter.events[1].otherActorType)
        assertEquals(listOf("Ball"), context.scenes.actors("level")!!.map { it.typeId })
        assertEquals("val first = Brick()", File(context.project.rootDirectory, "src/commonMain/kotlin/Game.kt").readText())
    }

    @Test
    fun theResultAndCheckChangeListEveryUsageWithWhatHappensToIt() = runTest {
        val context = commandContext(ball, brick)
        context.writeScene("level", sceneText)
        context.writeKotlin("src/commonMain/kotlin/Game.kt", "val first = Brick()")
        val expected = listOf(
            "Ball#0.0 adds Brick: the Block is removed.",
            "Ball#0.2 moves towards Brick: the Block is removed.",
            "Ball#0.3 names in Kotlin Brick: left as it is, so the build fails until it is changed.",
            "Ball#1 collides with Brick: the Event is removed with its Blocks.",
            "Scene level places Brick (2): its Actors are removed.",
            "src/commonMain/kotlin/Game.kt names in Kotlin Brick: left as it is, so the build fails until it is changed.",
        )
        val check = CheckChange.read(context, CheckChange.Input("delete_actor_type", buildJsonObject { put("actorType", "Brick") }))
        val text = assertIs<CommandOutcome.Success>(check).result.jsonPrimitive.content
        expected.forEach { assertContains(text, it) }
        val details = assertNotNull(DeleteActorType.apply(context, DeleteActorType.Input("Brick")).details)
        val afterwards = expected.map { it.replace("Ball#0.3 names", "Ball#0.1 names") }
        assertEquals(afterwards, details.getValue("usages").jsonArray.map { it.jsonPrimitive.content })
    }

    @Test
    fun anAiHearsWhatItsDeleteCleanedUpBesidesWhatItChanged() = runTest {
        val context = commandContext(ball, brick)
        val executor = CommandExecutor(context = context, scope = testScope())
        val result = assertIs<CommandOutcome.Success>(executor.call("delete_actor_type", buildJsonObject { put("actorType", "Brick") })).result.jsonObject
        assertEquals("delete_actor_type at Brick", result.getValue("changed").jsonPrimitive.content)
        assertEquals("Ball#0.0 adds Brick: the Block is removed.", result.getValue("usages").jsonArray.first().jsonPrimitive.content)
        executor.undo()
        assertEquals(listOf(ball, brick), context.blockTypes!!.types.value)
    }

    @Test
    fun aDeleteWhoseScenesCantBeWrittenChangesNothing() = runTest {
        val context = commandContext(ball, brick, sceneWriteText = { _, _ -> throw IOException("The disk is full.") })
        context.writeScene("level", sceneText)
        val before = context.snapshot()
        val outcome = runCatching { DeleteActorType.apply(context, DeleteActorType.Input("Brick")) }
        assertIs<IOException>(outcome.exceptionOrNull())
        assertEquals(before, context.snapshot())
    }

    @Test
    fun deletingAManagerTypeRemovesWhatUsesItsPropertiesAndKeepsTheirSentences() = runTest {
        val game = ManagerTypeDefinition(name = "Game", properties = listOf(PropertyDefinition.Number("score")), texts = listOf(ScreenTextDefinition(text = "Score: {score}")))
        val hud = ManagerTypeDefinition(name = "Hud", texts = listOf(ScreenTextDefinition(text = "Score: {Game.score}")))
        val player = ActorTypeDefinition(
            name = "Player",
            events = listOf(
                EventDefinition(
                    EventType.EVERY_FRAME,
                    blocks = listOf(
                        BlockDefinition.If(ConditionDefinition.Number("score", value = NumberValue.Fixed(3f), manager = "Game")),
                        BlockDefinition.If(ConditionDefinition.Chance()),
                        BlockDefinition.Stop,
                        BlockDefinition.End,
                        BlockDefinition.Else,
                        BlockDefinition.RemoveSelf,
                        BlockDefinition.End,
                        BlockDefinition.SetNumber("score", NumberValue.Fixed(1f), manager = "Game"),
                        BlockDefinition.Log("Score {Game.score}"),
                        BlockDefinition.Kotlin("gameManager.score += 1"),
                        BlockDefinition.BounceOffEdge,
                    ),
                ),
            ),
        )
        val context = commandContext(game, hud, player)
        val after = context.assertRoundTrip(Invocation(DeleteManagerType, DeleteManagerType.Input("Game"))) { context.snapshot() }.first
        assertEquals(listOf(BlockDefinition.Log("Score score"), BlockDefinition.Kotlin("gameManager.score += 1"), BlockDefinition.BounceOffEdge), after.single { it.name == "Player" }.events[0].blocks)
        assertEquals("Score: score", (after.single { it.name == "Hud" } as ManagerTypeDefinition).texts.single().text)
        assertEquals(emptyList(), context.usagesOf(RelationSubject.Type("Hud")))
    }

    @Test
    fun aRemovedIfBlockWithoutAnEndTakesTheRestOfItsEvent() = runTest {
        val player = ActorTypeDefinition(
            name = "Player",
            properties = listOf(PropertyDefinition.Number("hp")),
            events = listOf(EventDefinition(EventType.EVERY_FRAME, blocks = listOf(BlockDefinition.Stop, BlockDefinition.If(ConditionDefinition.Number("hp")), BlockDefinition.RemoveSelf))),
        )
        val context = commandContext(player)
        val usages = context.usagesOf(RelationSubject.Property("Player", "hp"))
        assertEquals(listOf(Usage(UsagePlace.InModel(RelationPlace.Type("Player", 0, 1)), RelationKind.READS, RelationSubject.Property("Player", "hp"), UsageEffect.SECTION_REMOVED, blocksInside = 1)), usages)
        val after = context.assertRoundTrip(Invocation(RemoveProperty, RemoveProperty.Input("Player", "hp"))) { context.snapshot() }.first.single()
        assertEquals(listOf(BlockDefinition.Stop), after.events[0].blocks)
        assertEquals(emptyList(), after.properties)
    }

    @Test
    fun removingAPropertyReachesTheFamilyAndKeepsItsSceneValues() = runTest {
        val enemy = ActorTypeDefinition(name = "Enemy", properties = listOf(PropertyDefinition.Number("hp")), events = listOf(EventDefinition(EventType.EVERY_FRAME, blocks = listOf(BlockDefinition.Log("{hp} left")))))
        val boss = ActorTypeDefinition(name = "Boss", parent = "Enemy", events = listOf(EventDefinition(EventType.EVERY_FRAME, blocks = listOf(BlockDefinition.ChangeNumber("hp"), BlockDefinition.Kotlin("hp -= 1")))))
        val other = ActorTypeDefinition(name = "Other", properties = listOf(PropertyDefinition.Number("hp")), events = listOf(EventDefinition(EventType.EVERY_FRAME, blocks = listOf(BlockDefinition.ChangeNumber("hp")))))
        val context = commandContext(enemy, boss, other)
        val sceneText = "[{\"typeId\":\"Boss\",\"state\":\"{\\\"hp\\\":3}\"}]"
        context.writeScene("level", sceneText)
        val after = context.assertRoundTrip(Invocation(RemoveProperty, RemoveProperty.Input("Enemy", "hp"))) { context.snapshot() }.first
        assertEquals(listOf(BlockDefinition.Log("hp left")), after.single { it.name == "Enemy" }.events[0].blocks)
        assertEquals(listOf(BlockDefinition.Kotlin("hp -= 1")), after.single { it.name == "Boss" }.events[0].blocks)
        assertEquals(other, after.single { it.name == "Other" })
        assertEquals(sceneText, context.sceneFile("level").readText())
        assertEquals(UsageEffect.KOTLIN_TO_CHANGE, context.usagesOf(RelationSubject.Property("Enemy", "hp")).last().effect)
    }

    @Test
    fun deletingASceneRemovesWhatGoesToItAndListsTheKotlinNamingIt() = runTest {
        val door = ActorTypeDefinition(name = "Door", events = listOf(EventDefinition(EventType.EVERY_FRAME, blocks = listOf(BlockDefinition.GoToScene("cave"), BlockDefinition.GoToScene("town")))))
        val context = commandContext(door)
        context.writeScene("cave", "[]")
        context.writeScene("town", "[]")
        context.writeKotlin("src/commonMain/kotlin/Game.kt", "val start = \"cave\"")
        val usages = context.usagesOf(RelationSubject.Scene("cave"))
        assertEquals(listOf(UsageEffect.BLOCK_REMOVED, UsageEffect.KOTLIN_TO_CHANGE), usages.map { it.effect })
        val after = context.assertRoundTrip(Invocation(DeleteScene, DeleteScene.Input("cave"))) { context.snapshot() }
        assertEquals(listOf(BlockDefinition.GoToScene("town")), after.first.single().events[0].blocks)
    }

    @Test
    fun theFirstSceneIsNamedAsWhatPreventsDeletingIt() = runTest {
        val door = ActorTypeDefinition(name = "Door", events = listOf(EventDefinition(EventType.EVERY_FRAME, blocks = listOf(BlockDefinition.GoToScene("cave")))))
        val context = commandContext(door)
        context.writeScene("cave", "[]")
        File(context.project.rootDirectory, "build.gradle.kts").writeText("val gameFirstScene = providers.gradleProperty(\"game.firstScene\")\n")
        File(context.project.rootDirectory, "gradle.properties").writeText("game.name=Game\ngame.firstScene=cave\n")
        assertEquals(UsageEffect.PREVENTS_DELETING, context.usagesOf(RelationSubject.Scene("cave")).last().effect)
        assertNotNull(DeleteScene.apply(context, DeleteScene.Input("cave")).failure)
        assertEquals(door, context.blockTypes!!.types.value.single())
    }

    @Test
    fun undoingACreationLeavesWhatReferredToItBeforeAsItWas() = runTest {
        val context = commandContext(ball)
        context.writeScene("level", sceneText)
        val before = context.snapshot()
        val inverse = assertNotNull(CreateActorType.apply(context, brick).inverse).copy(isRestoring = true)
        inverse.apply(context)
        assertEquals(before, context.snapshot())
        val player = ActorTypeDefinition(name = "Player", events = listOf(EventDefinition(EventType.EVERY_FRAME, blocks = listOf(BlockDefinition.Log("{hp}")))))
        val propertyContext = commandContext(player)
        val removal = assertNotNull(AddProperty.apply(propertyContext, AddProperty.Input("Player", PropertyDefinition.Number("hp"))).inverse)
        removal.apply(propertyContext)
        assertEquals(player, propertyContext.blockTypes!!["Player"])
    }

    @Test
    fun aSceneWithAnActorWhoseStateCantBeReadDoesntStopTheDelete() = runTest {
        val context = commandContext(ball, brick)
        val text = """[{"typeId":"Brick","state":"{}"},{"typeId":"Ball","state":"not json"}]"""
        context.writeScene("level", text)
        val after = context.assertRoundTrip(Invocation(DeleteActorType, DeleteActorType.Input("Brick"))) { context.sceneFile("level").readText() }
        assertContains(after, "not json")
        assertEquals(false, "Brick" in after)
    }

    @Test
    fun kotlinNamingADeletedSceneIsListedAndAPlaceholderUsedTwiceOnce() = runTest {
        val door = ActorTypeDefinition(
            name = "Door",
            properties = listOf(PropertyDefinition.Number("hp")),
            events = listOf(EventDefinition(EventType.EVERY_FRAME, blocks = listOf(BlockDefinition.Kotlin("println(\"cave\")"), BlockDefinition.Log("{hp} of {hp}")))),
        )
        val context = commandContext(door)
        context.writeScene("cave", "[]")
        assertEquals(
            listOf(Usage(UsagePlace.InModel(RelationPlace.Type("Door", 0, 0)), RelationKind.NAMES_IN_KOTLIN, RelationSubject.Scene("cave"), UsageEffect.KOTLIN_TO_CHANGE)),
            context.usagesOf(RelationSubject.Scene("cave")),
        )
        assertEquals(1, context.usagesOf(RelationSubject.Property("Door", "hp")).size)
    }
}
