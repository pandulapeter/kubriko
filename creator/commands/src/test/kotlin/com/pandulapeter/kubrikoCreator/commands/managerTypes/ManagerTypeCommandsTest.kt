/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.commands.managerTypes

import com.pandulapeter.kubrikoCreator.actors.ActorTypeDefinition
import com.pandulapeter.kubrikoCreator.actors.BlockDefinition
import com.pandulapeter.kubrikoCreator.actors.ConditionDefinition
import com.pandulapeter.kubrikoCreator.actors.EventDefinition
import com.pandulapeter.kubrikoCreator.actors.EventType
import com.pandulapeter.kubrikoCreator.actors.ManagerTypeDefinition
import com.pandulapeter.kubrikoCreator.actors.NameProblem
import com.pandulapeter.kubrikoCreator.actors.PlacementProblem
import com.pandulapeter.kubrikoCreator.actors.PropertyDefinition
import com.pandulapeter.kubrikoCreator.actors.ScreenPosition
import com.pandulapeter.kubrikoCreator.actors.ScreenTextDefinition
import com.pandulapeter.kubrikoCreator.commands.CommandContext
import com.pandulapeter.kubrikoCreator.commands.CommandOutcome
import com.pandulapeter.kubrikoCreator.commands.CommandProblem
import com.pandulapeter.kubrikoCreator.commands.Invocation
import com.pandulapeter.kubrikoCreator.commands.LookupProblem
import com.pandulapeter.kubrikoCreator.commands.Rejection
import com.pandulapeter.kubrikoCreator.commands.actorTypes.AddBlock
import com.pandulapeter.kubrikoCreator.commands.actorTypes.AddEvent
import com.pandulapeter.kubrikoCreator.commands.actorTypes.EditProperty
import com.pandulapeter.kubrikoCreator.commands.actorTypes.RenameActorType
import com.pandulapeter.kubrikoCreator.commands.actorTypes.SetAppearance
import com.pandulapeter.kubrikoCreator.commands.assertRoundTrip
import com.pandulapeter.kubrikoCreator.commands.commandContext
import com.pandulapeter.kubrikoCreator.context.ReadDetail
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

class ManagerTypeCommandsTest {
    private val game = ManagerTypeDefinition(
        name = "Game",
        properties = listOf(PropertyDefinition.Number("score"), PropertyDefinition.Toggle("isOver")),
        events = listOf(EventDefinition(EventType.EVERY_FRAME)),
        texts = listOf(ScreenTextDefinition("Score: {score}")),
    )
    private val brick = ActorTypeDefinition(
        name = "Brick",
        events = listOf(EventDefinition(EventType.COLLISION, otherActorType = "Brick", blocks = listOf(BlockDefinition.ChangeNumber("score", 10f, manager = "Game"), BlockDefinition.Log("{Game.score}")))),
    )

    private fun CommandContext.game() = blockTypes!!.managerType("Game")

    @Test
    fun managerTypesAreCreatedRenamedAndDeletedAndEachIsUndone() = runTest {
        val context = commandContext(brick)
        assertEquals(game, context.assertRoundTrip(Invocation(CreateManagerType, game)) { context.game() })
        val renamed = context.assertRoundTrip(Invocation(RenameManagerType, RenameManagerType.Input("Game", "Match"))) { context.blockTypes!!.types.value }
        assertEquals(listOf("Brick", "Match"), renamed.map { it.name })
        assertEquals(listOf(BlockDefinition.ChangeNumber("score", 10f, manager = "Match"), BlockDefinition.Log("{Match.score}")), (renamed[0] as ActorTypeDefinition).events.single().blocks)
        assertNull(context.assertRoundTrip(Invocation(DeleteManagerType, DeleteManagerType.Input("Match"))) { context.blockTypes!!.managerType("Match") })
    }

    @Test
    fun namesAreSharedWithActorTypes() = runTest {
        val context = commandContext(brick, game)
        assertEquals(Rejection(NameProblem.TYPE_TAKEN), Invocation(CreateManagerType, ManagerTypeDefinition("BRICK")).validate(context))
        assertEquals(Rejection(NameProblem.TYPE_TAKEN), Invocation(RenameActorType, RenameActorType.Input("Brick", "Game")).validate(context))
        assertEquals(Rejection(NameProblem.TYPE_TAKEN), Invocation(RenameManagerType, RenameManagerType.Input("Game", "Brick")).validate(context))
        assertEquals(Rejection(LookupProblem.MANAGER_TYPE_IS_ACTOR_TYPE, "Brick"), Invocation(DeleteManagerType, DeleteManagerType.Input("Brick")).validate(context))
        assertEquals(Rejection(LookupProblem.ACTOR_TYPE_IS_MANAGER_TYPE, "Game"), Invocation(SetAppearance, SetAppearance.Input("Game", width = 5f)).validate(context))
    }

    @Test
    fun aManagerOnlyGetsEventsAndBlocksThatNeedNoActor() = runTest {
        val context = commandContext(game)
        assertEquals(Rejection(PlacementProblem.EVENT_COLLISION_IN_MANAGER_TYPE), Invocation(AddEvent, AddEvent.Input("Game", EventType.COLLISION)).validate(context))
        assertEquals(Rejection(PlacementProblem.EVENT_ADDED_TO_SCENE_IN_MANAGER_TYPE), Invocation(AddEvent, AddEvent.Input("Game", EventType.ADDED_TO_SCENE)).validate(context))
        assertEquals(Rejection(PlacementProblem.BLOCK_MOVEMENT_IN_MANAGER_TYPE), Invocation(AddBlock, AddBlock.Input("Game", 0, BlockDefinition.SetVelocity())).validate(context))
        assertEquals(Rejection(PlacementProblem.CONDITION_NOT_ALLOWED), Invocation(AddBlock, AddBlock.Input("Game", 0, BlockDefinition.If(ConditionDefinition.Velocity()))).validate(context))
        assertNull(Invocation(AddEvent, AddEvent.Input("Game", EventType.GAME_STARTED, blocks = listOf(BlockDefinition.SetNumber("score")))).validate(context))
        assertNull(Invocation(AddBlock, AddBlock.Input("Game", 0, BlockDefinition.ChangeNumber("score", manager = "Game"))).validate(context))
    }

    @Test
    fun anActorDoesNotGetTheGameStartedEvent() = runTest {
        assertEquals(Rejection(PlacementProblem.EVENT_GAME_STARTED_IN_ACTOR_TYPE), Invocation(AddEvent, AddEvent.Input("Brick", EventType.GAME_STARTED)).validate(commandContext(brick)))
    }

    @Test
    fun blocksMayOnlyUseManagerPropertiesThatExist() = runTest {
        val context = commandContext(brick, game)
        assertNull(Invocation(AddBlock, AddBlock.Input("Brick", 0, BlockDefinition.SetToggle("isOver", manager = "Game"))).validate(context))
        assertNull(Invocation(AddBlock, AddBlock.Input("Brick", 0, BlockDefinition.SetToggle(manager = "Game"))).validate(context))
        assertEquals(Rejection(LookupProblem.MANAGER_TYPE_MISSING, "Level"), Invocation(AddBlock, AddBlock.Input("Brick", 0, BlockDefinition.SetToggle("isOver", manager = "Level"))).validate(context))
        assertEquals(Rejection(LookupProblem.MANAGER_TYPE_IS_ACTOR_TYPE, "Brick"), Invocation(AddBlock, AddBlock.Input("Brick", 0, BlockDefinition.SetToggle("isOver", manager = "Brick"))).validate(context))
        assertEquals(Rejection(PlacementProblem.TOGGLE_PROPERTY_MISSING, "score", "Game"), Invocation(AddBlock, AddBlock.Input("Brick", 0, BlockDefinition.SetToggle("score", manager = "Game"))).validate(context))
    }

    @Test
    fun renamingAManagersPropertyUpdatesEveryTypeThatUsesIt() = runTest {
        val context = commandContext(brick, game)
        val renamed = context.assertRoundTrip(Invocation(EditProperty, EditProperty.Input("Game", "score", PropertyDefinition.Number("points")))) { context.blockTypes!!.types.value }
        assertEquals(listOf(BlockDefinition.ChangeNumber("points", 10f, manager = "Game"), BlockDefinition.Log("{Game.points}")), (renamed[0] as ActorTypeDefinition).events.single().blocks)
        assertEquals("Score: {points}", (renamed[1] as ManagerTypeDefinition).texts.single().text)
    }

    @Test
    fun textsAreAddedEditedAndRemovedAndEachIsUndone() = runTest {
        val context = commandContext(game)
        val lives = ScreenTextDefinition("Lives", ScreenPosition.TOP_RIGHT)
        assertEquals(listOf(lives, game.texts[0]), context.assertRoundTrip(Invocation(AddText, AddText.Input("Game", lives, index = 0))) { context.game()!!.texts })
        assertEquals(listOf(lives.copy(size = 40f), game.texts[0]), context.assertRoundTrip(Invocation(EditText, EditText.Input("Game", 0, lives.copy(size = 40f)))) { context.game()!!.texts })
        assertEquals(listOf(game.texts[0]), context.assertRoundTrip(Invocation(RemoveText, RemoveText.Input("Game", 0))) { context.game()!!.texts })
        assertEquals(Rejection(LookupProblem.TEXT_MISSING, "Game", "3", "0"), Invocation(RemoveText, RemoveText.Input("Game", 3)).validate(context))
        assertEquals(Rejection(CommandProblem.TEXT_SIZE_INVALID), Invocation(AddText, AddText.Input("Game", ScreenTextDefinition(size = 0f))).validate(context))
    }

    @Test
    fun theKotlinUiIsSetTypedAsOneStepAndUndone() = runTest {
        val context = commandContext(brick, game)
        assertEquals("Column {}", context.assertRoundTrip(Invocation(SetKotlinUi, SetKotlinUi.Input("Game", "Column {}"))) { context.game()!!.kotlinUi })
        assertEquals(SetKotlinUi.mergeKey(SetKotlinUi.Input("Game", "a")), SetKotlinUi.mergeKey(SetKotlinUi.Input("Game", "ab")))
        assertNull(context.assertRoundTrip(Invocation(SetKotlinUi, SetKotlinUi.Input("Game", "  \n"))) { context.game()!!.kotlinUi })
        assertEquals(Rejection(LookupProblem.MANAGER_TYPE_IS_ACTOR_TYPE, "Brick"), Invocation(SetKotlinUi, SetKotlinUi.Input("Brick", "")).validate(context))
    }

    @Test
    fun managerTypesAreListedAndRead() = runTest {
        val context = commandContext(brick, game)
        val list = assertIs<CommandOutcome.Success>(ListManagerTypes.read(context, ListManagerTypes.Input())).result as JsonObject
        val game = (list.getValue("managerTypes") as JsonArray).single().jsonObject
        assertEquals("Game", game.getValue("name").jsonPrimitive.content)
        assertEquals("""[{"name":"score","kind":"number"},{"name":"isOver","kind":"toggle"}]""", game.getValue("properties").toString())
        val read = assertIs<CommandOutcome.Success>(ReadManagerType.read(context, ReadManagerType.Input("Game"))).result as JsonObject
        assertEquals("""["score: number = 0","isOver: toggle = off"]""", read.getValue("properties").toString())
        assertEquals("0 \"Score: {score}\" (topLeft, 24, #202020)", (read.getValue("texts") as JsonArray).single().jsonPrimitive.content)
        assertNull(read["isChangedByHand"])
        val json = assertIs<CommandOutcome.Success>(ReadManagerType.read(context, ReadManagerType.Input("Game", detail = ReadDetail.JSON))).result as JsonObject
        assertEquals("Score: {score}", (json.getValue("texts") as JsonArray).single().jsonObject.getValue("text").jsonPrimitive.content)
    }
}
