/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.commands.actorTypes

import com.pandulapeter.kubrikoCreator.actors.ActorShape
import com.pandulapeter.kubrikoCreator.actors.ActorTypeDefinition
import com.pandulapeter.kubrikoCreator.actors.BlockDefinition
import com.pandulapeter.kubrikoCreator.actors.ConditionDefinition
import com.pandulapeter.kubrikoCreator.actors.EventDefinition
import com.pandulapeter.kubrikoCreator.actors.EventType
import com.pandulapeter.kubrikoCreator.actors.KeyChoice
import com.pandulapeter.kubrikoCreator.actors.PauseAction
import com.pandulapeter.kubrikoCreator.actors.PropertyDefinition
import com.pandulapeter.kubrikoCreator.commands.CommandContext
import com.pandulapeter.kubrikoCreator.commands.CommandTarget
import com.pandulapeter.kubrikoCreator.commands.Invocation
import com.pandulapeter.kubrikoCreator.commands.Rejection
import com.pandulapeter.kubrikoCreator.commands.WriteCommand
import com.pandulapeter.kubrikoCreator.commands.assertRoundTrip
import com.pandulapeter.kubrikoCreator.commands.blockTypesController
import com.pandulapeter.kubrikoCreator.commands.commandContext
import com.pandulapeter.kubrikoCreator.scenes.SceneStore
import com.pandulapeter.kubrikoCreator.workspace.FakeBlockTypeRepository
import kotlinx.coroutines.test.runTest
import kubriko.creator.generated.resources.Res
import kubriko.creator.generated.resources.error_command_actor_type_missing
import kubriko.creator.generated.resources.error_command_block_missing
import kubriko.creator.generated.resources.error_command_block_not_allowed
import kubriko.creator.generated.resources.error_command_event_missing
import kubriko.creator.generated.resources.error_command_index_invalid
import kubriko.creator.generated.resources.error_command_key_not_allowed
import kubriko.creator.generated.resources.error_command_no_actor_types
import kubriko.creator.generated.resources.error_command_not_changed_by_hand
import kubriko.creator.generated.resources.error_command_number_invalid
import kubriko.creator.generated.resources.error_command_other_actor_type_not_allowed
import kubriko.creator.generated.resources.error_command_property_missing
import kubriko.creator.generated.resources.error_command_scene_missing
import kubriko.creator.generated.resources.error_command_size_invalid
import kubriko.creator.generated.resources.error_command_timer_invalid
import kubriko.creator.generated.resources.error_command_timer_not_allowed
import kubriko.creator.generated.resources.error_property_name_taken
import kubriko.creator.generated.resources.error_type_name_invalid
import kubriko.creator.generated.resources.error_type_name_taken
import java.io.File
import kotlin.io.path.createTempDirectory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ActorTypeCommandsTest {
    private val stop = BlockDefinition.Stop
    private val jump = BlockDefinition.JumpTo(1f, 2f)
    private val ball = ActorTypeDefinition(
        name = "Ball",
        events = listOf(
            EventDefinition(EventType.ADDED_TO_SCENE, blocks = listOf(stop, jump)),
            EventDefinition(EventType.KEY_PRESSED, KeyChoice.LEFT),
            EventDefinition(EventType.COLLISION, otherActorType = "Wall", blocks = listOf(BlockDefinition.RemoveOther)),
        ),
    )
    private val wall = ActorTypeDefinition(name = "Wall", shape = ActorShape.RECTANGLE)

    private fun CommandContext.ball() = blockTypes!!["Ball"]

    private fun CommandContext.all() = blockTypes!!.actorTypes.value

    private fun <I> CommandContext.rejection(command: WriteCommand<I>, input: I) = command.validate(this, input)

    @Test
    fun actorTypesAreCreatedRenamedAndDeleted() = runTest {
        val context = commandContext(wall)
        assertEquals(listOf(ball, wall), context.assertRoundTrip(Invocation(CreateActorType, ball)) { context.all() })
        val renamed = context.assertRoundTrip(Invocation(RenameActorType, RenameActorType.Input("Wall", "Brick"))) { context.all() }
        assertEquals(listOf("Ball", "Brick"), renamed.map { it.name })
        assertEquals("Brick", renamed.first().events[2].otherActorType)
        assertEquals(listOf("Ball"), context.assertRoundTrip(Invocation(DeleteActorType, DeleteActorType.Input("Brick"))) { context.all() }.map { it.name })
    }

    @Test
    fun deletingAnActorTypeIsUndoneWithEverythingItHad() = runTest {
        val context = commandContext(ball, wall)
        val inverse = DeleteActorType.apply(context, DeleteActorType.Input("Ball")).inverse!!
        inverse.apply(context)
        assertEquals(ball, context.ball())
    }

    @Test
    fun namesAndContentsOfNewActorTypesAreValidated() = runTest {
        val context = commandContext(ball, wall)
        assertEquals(Rejection(Res.string.error_type_name_invalid), context.rejection(CreateActorType, ActorTypeDefinition("ball game")))
        assertEquals(Rejection(Res.string.error_type_name_taken), context.rejection(CreateActorType, ActorTypeDefinition("WALL")))
        assertEquals(Rejection(Res.string.error_command_size_invalid), context.rejection(CreateActorType, ActorTypeDefinition("Paddle", width = 0f)))
        assertEquals(Rejection(Res.string.error_command_key_not_allowed), context.rejection(CreateActorType, ActorTypeDefinition("Paddle", events = listOf(EventDefinition(EventType.EVERY_FRAME, key = KeyChoice.A)))))
        assertEquals(
            Rejection(Res.string.error_command_other_actor_type_not_allowed),
            context.rejection(CreateActorType, ActorTypeDefinition("Paddle", events = listOf(EventDefinition(EventType.EVERY_FRAME, otherActorType = "Ball")))),
        )
        assertEquals(
            Rejection(Res.string.error_command_actor_type_missing, "Ghost"),
            context.rejection(CreateActorType, ActorTypeDefinition("Paddle", events = listOf(EventDefinition(EventType.COLLISION, otherActorType = "Ghost")))),
        )
        assertNull(context.rejection(CreateActorType, ActorTypeDefinition("Paddle", events = listOf(EventDefinition(EventType.COLLISION, otherActorType = "Paddle")))))
        assertEquals(
            Rejection(Res.string.error_command_block_not_allowed, "bounceOffEdge", "everyFrame"),
            context.rejection(CreateActorType, ActorTypeDefinition("Paddle", events = listOf(EventDefinition(EventType.EVERY_FRAME, blocks = listOf(BlockDefinition.BounceOffEdge))))),
        )
        assertEquals(
            Rejection(Res.string.error_command_number_invalid),
            context.rejection(CreateActorType, ActorTypeDefinition("Paddle", events = listOf(EventDefinition(EventType.EVERY_FRAME, blocks = listOf(BlockDefinition.SetVelocity(Float.NaN, 0f)))))),
        )
        assertEquals(Rejection(Res.string.error_type_name_taken), context.rejection(RenameActorType, RenameActorType.Input("Ball", "Wall")))
        assertEquals(Rejection(Res.string.error_command_actor_type_missing, "Ghost"), context.rejection(DeleteActorType, DeleteActorType.Input("Ghost")))
    }

    @Test
    fun projectsWithoutBlockBuiltActorTypesRejectActorTypeCommands() = runTest {
        val context = commandContext(blockTypes = null)
        assertEquals(Rejection(Res.string.error_command_no_actor_types), context.rejection(CreateActorType, ball))
        assertEquals(Rejection(Res.string.error_command_no_actor_types), context.rejection(SetAppearance, SetAppearance.Input("Ball", width = 1f)))
    }

    @Test
    fun appearanceChangesOnlyWhatIsGivenAndIsUndoneTheSameWay() = runTest {
        val context = commandContext(ball)
        val changed = context.assertRoundTrip(Invocation(SetAppearance, SetAppearance.Input("Ball", color = 0xFF112233, height = 12f))) { context.ball() }
        assertEquals(ball.copy(color = 0xFF112233, height = 12f), changed)
        assertEquals(Rejection(Res.string.error_command_size_invalid), context.rejection(SetAppearance, SetAppearance.Input("Ball", width = -1f)))
        assertEquals(SetAppearance.mergeKey(SetAppearance.Input("Ball", width = 1f)), SetAppearance.mergeKey(SetAppearance.Input("Ball", width = 2f)))
        assertEquals(false, SetAppearance.mergeKey(SetAppearance.Input("Ball", width = 1f)) == SetAppearance.mergeKey(SetAppearance.Input("Ball", height = 1f)))
    }

    @Test
    fun eventsAreAddedMovedRekeyedAndRemoved() = runTest {
        val context = commandContext(ball, wall)
        val added = context.assertRoundTrip(Invocation(AddEvent, AddEvent.Input("Ball", EventType.KEY_PRESSED, index = 1))) { context.ball() }
        assertEquals(EventDefinition(EventType.KEY_PRESSED, KeyChoice.SPACE), added!!.events[1])
        val moved = context.assertRoundTrip(Invocation(MoveEvent, MoveEvent.Input("Ball", eventIndex = 0, targetIndex = 3))) { context.ball() }
        assertEquals(listOf(EventType.KEY_PRESSED, EventType.KEY_PRESSED, EventType.COLLISION, EventType.ADDED_TO_SCENE), moved!!.events.map { it.type })
        assertEquals(KeyChoice.D, context.assertRoundTrip(Invocation(SetEventKey, SetEventKey.Input("Ball", 0, KeyChoice.D))) { context.ball() }!!.events[0].key)
        assertEquals(null, context.assertRoundTrip(Invocation(SetEventOtherActorType, SetEventOtherActorType.Input("Ball", 2, null))) { context.ball() }!!.events[2].otherActorType)
        assertEquals(3, context.assertRoundTrip(Invocation(RemoveEvent, RemoveEvent.Input("Ball", 3))) { context.ball() }!!.events.size)
    }

    @Test
    fun removingAnEventIsUndoneWithItsBlocksInItsPlace() = runTest {
        val context = commandContext(ball, wall)
        RemoveEvent.apply(context, RemoveEvent.Input("Ball", 0)).inverse!!.apply(context)
        assertEquals(ball, context.ball())
    }

    @Test
    fun eventChangesAreValidated() = runTest {
        val context = commandContext(ball, wall)
        assertEquals(Rejection(Res.string.error_command_event_missing, "Ball", "3"), context.rejection(RemoveEvent, RemoveEvent.Input("Ball", 3)))
        assertEquals(Rejection(Res.string.error_command_index_invalid, "3"), context.rejection(MoveEvent, MoveEvent.Input("Ball", 0, 3)))
        assertEquals(Rejection(Res.string.error_command_key_not_allowed), context.rejection(SetEventKey, SetEventKey.Input("Ball", 0, KeyChoice.A)))
        assertEquals(Rejection(Res.string.error_command_other_actor_type_not_allowed), context.rejection(SetEventOtherActorType, SetEventOtherActorType.Input("Ball", 0, "Wall")))
        assertEquals(Rejection(Res.string.error_command_actor_type_missing, "Ghost"), context.rejection(SetEventOtherActorType, SetEventOtherActorType.Input("Ball", 2, "Ghost")))
        assertEquals(Rejection(Res.string.error_command_key_not_allowed), context.rejection(AddEvent, AddEvent.Input("Ball", EventType.EVERY_FRAME, key = KeyChoice.A)))
    }

    @Test
    fun blocksAreAddedEditedMovedAndRemoved() = runTest {
        val context = commandContext(ball, wall)
        val bounce = BlockDefinition.BounceOffOther
        assertEquals(listOf(bounce, BlockDefinition.RemoveOther), context.assertRoundTrip(Invocation(AddBlock, AddBlock.Input("Ball", 2, bounce, index = 0))) { context.ball() }!!.events[2].blocks)
        assertEquals(listOf(BlockDefinition.SetVelocity(3f, 4f), jump), context.assertRoundTrip(Invocation(EditBlock, EditBlock.Input("Ball", 0, 0, BlockDefinition.SetVelocity(3f, 4f)))) { context.ball() }!!.events[0].blocks)
        assertEquals(listOf(jump, BlockDefinition.SetVelocity(3f, 4f)), context.assertRoundTrip(Invocation(MoveBlock, MoveBlock.Input("Ball", 0, 0, 1))) { context.ball() }!!.events[0].blocks)
        assertEquals(listOf(BlockDefinition.SetVelocity(3f, 4f)), context.assertRoundTrip(Invocation(RemoveBlock, RemoveBlock.Input("Ball", 0, 0))) { context.ball() }!!.events[0].blocks)
    }

    @Test
    fun goToSceneBlocksMayLeaveTheSceneUnchosenButNotNameOneThatDoesNotExist() = runTest {
        val root = createTempDirectory().toFile()
        File(root, "${SceneStore.DEFAULT_SCENE_DIRECTORY}/level-2.json").apply { parentFile.mkdirs() }.writeText("[]")
        val context = commandContext(ball, wall, rootDirectory = root)
        assertNull(context.rejection(AddBlock, AddBlock.Input("Ball", 0, BlockDefinition.GoToScene())))
        assertNull(context.rejection(AddBlock, AddBlock.Input("Ball", 0, BlockDefinition.GoToScene("level-2"))))
        assertEquals(Rejection(Res.string.error_command_scene_missing, "ghost"), context.rejection(AddBlock, AddBlock.Input("Ball", 0, BlockDefinition.GoToScene("ghost"))))
        assertEquals(Rejection(Res.string.error_command_scene_missing, "ghost"), context.rejection(EditBlock, EditBlock.Input("Ball", 0, 0, BlockDefinition.GoToScene("ghost"))))
        assertNull(context.rejection(AddBlock, AddBlock.Input("Ball", 0, BlockDefinition.RestartScene)))
        assertNull(context.rejection(AddBlock, AddBlock.Input("Ball", 0, BlockDefinition.PauseGame(PauseAction.RESUME))))
        root.deleteRecursively()
    }

    @Test
    fun propertyBlocksMayLeaveThePropertyUnchosenButNotNameOneTheActorTypeLacks() = runTest {
        val context = commandContext(ball.copy(properties = listOf(PropertyDefinition.Number("score"), PropertyDefinition.Toggle("isFast"))), wall)
        assertNull(context.rejection(AddBlock, AddBlock.Input("Ball", 0, BlockDefinition.ChangeNumber())))
        assertNull(context.rejection(AddBlock, AddBlock.Input("Ball", 0, BlockDefinition.ChangeNumber("score"))))
        assertEquals(Rejection(Res.string.error_command_property_missing, "lives", "number"), context.rejection(AddBlock, AddBlock.Input("Ball", 0, BlockDefinition.SetNumber("lives"))))
        assertEquals(Rejection(Res.string.error_command_property_missing, "isFast", "text"), context.rejection(EditBlock, EditBlock.Input("Ball", 0, 0, BlockDefinition.SetText("isFast"))))
        assertEquals(Rejection(Res.string.error_command_number_invalid), context.rejection(AddBlock, AddBlock.Input("Ball", 0, BlockDefinition.SetNumber("score", Float.NaN))))
        assertEquals(
            Rejection(Res.string.error_property_name_taken),
            context.rejection(CreateActorType, ActorTypeDefinition("Paddle", properties = listOf(PropertyDefinition.Number("speed"), PropertyDefinition.Text("Speed".lowercase())))),
        )
    }

    @Test
    fun timerEventsStartWithTheFirstTimerAndTheirTimerCanBeChanged() = runTest {
        val context = commandContext(ball, wall)
        val added = context.assertRoundTrip(Invocation(AddEvent, AddEvent.Input("Ball", EventType.TIMER_FINISHED))) { context.ball() }!!.events.last()
        assertEquals(EventDefinition(EventType.TIMER_FINISHED, timer = 1), added)
        assertEquals(5, context.assertRoundTrip(Invocation(SetEventTimer, SetEventTimer.Input("Ball", 3, 5))) { context.ball() }!!.events[3].timer)
        assertEquals(Rejection(Res.string.error_command_timer_not_allowed), context.rejection(SetEventTimer, SetEventTimer.Input("Ball", 0, 2)))
        assertEquals(Rejection(Res.string.error_command_timer_invalid), context.rejection(SetEventTimer, SetEventTimer.Input("Ball", 3, 9)))
        assertEquals(Rejection(Res.string.error_command_timer_not_allowed), context.rejection(AddEvent, AddEvent.Input("Ball", EventType.EVERY_FRAME, timer = 2)))
        assertEquals(Rejection(Res.string.error_command_timer_invalid), context.rejection(AddBlock, AddBlock.Input("Ball", 0, BlockDefinition.StopTimer(timer = 0))))
        assertEquals(Rejection(Res.string.error_command_number_invalid), context.rejection(AddBlock, AddBlock.Input("Ball", 0, BlockDefinition.StartTimer(seconds = -1f))))
        assertNull(context.rejection(AddBlock, AddBlock.Input("Ball", 0, BlockDefinition.StartTimer(timer = 8, seconds = 0f, isRepeating = true))))
    }

    @Test
    fun blocksThatNameAnActorTypeNeedOneThatExistsAndFollowItsRename() = runTest {
        val context = commandContext(ball, wall)
        assertNull(context.rejection(AddBlock, AddBlock.Input("Ball", 0, BlockDefinition.AddActor())))
        assertNull(context.rejection(AddBlock, AddBlock.Input("Ball", 0, BlockDefinition.MoveTowardsNearest("Wall"))))
        assertEquals(Rejection(Res.string.error_command_actor_type_missing, "Brick"), context.rejection(AddBlock, AddBlock.Input("Ball", 0, BlockDefinition.AddActor("Brick"))))
        assertEquals(Rejection(Res.string.error_command_number_invalid), context.rejection(AddBlock, AddBlock.Input("Ball", 0, BlockDefinition.AddActor("Wall", x = Float.NaN))))
        Invocation(AddBlock, AddBlock.Input("Ball", 0, BlockDefinition.AddActor("Wall", 1f, 2f))).apply(context)
        Invocation(RenameActorType, RenameActorType.Input("Wall", "Brick")).apply(context)
        assertEquals(BlockDefinition.AddActor("Brick", 1f, 2f), context.ball()!!.events[0].blocks.last())
    }

    @Test
    fun flowBlocksAreValidated() = runTest {
        val context = commandContext(ball.copy(properties = listOf(PropertyDefinition.Number("score"))), wall)
        assertNull(context.rejection(AddBlock, AddBlock.Input("Ball", 0, BlockDefinition.If(ConditionDefinition.Number("score", value = 3f)))))
        assertNull(context.rejection(AddBlock, AddBlock.Input("Ball", 0, BlockDefinition.Repeat(times = 0))))
        assertEquals(Rejection(Res.string.error_command_property_missing, "lives", "number"), context.rejection(AddBlock, AddBlock.Input("Ball", 0, BlockDefinition.If(ConditionDefinition.Number("lives")))))
        assertEquals(Rejection(Res.string.error_command_number_invalid), context.rejection(AddBlock, AddBlock.Input("Ball", 0, BlockDefinition.If(ConditionDefinition.Chance(101f)))))
        assertEquals(Rejection(Res.string.error_command_number_invalid), context.rejection(AddBlock, AddBlock.Input("Ball", 0, BlockDefinition.If(ConditionDefinition.Velocity(value = Float.POSITIVE_INFINITY)))))
        assertEquals(Rejection(Res.string.error_command_number_invalid), context.rejection(AddBlock, AddBlock.Input("Ball", 0, BlockDefinition.Repeat(times = -1))))
    }

    @Test
    fun blockChangesAreValidated() = runTest {
        val context = commandContext(ball, wall)
        assertEquals(Rejection(Res.string.error_command_block_not_allowed, "removeOther", "addedToScene"), context.rejection(AddBlock, AddBlock.Input("Ball", 0, BlockDefinition.RemoveOther)))
        assertEquals(Rejection(Res.string.error_command_block_not_allowed, "bounceOffEdge", "addedToScene"), context.rejection(EditBlock, EditBlock.Input("Ball", 0, 0, BlockDefinition.BounceOffEdge)))
        assertEquals(Rejection(Res.string.error_command_block_missing, "Ball", "1", "0"), context.rejection(RemoveBlock, RemoveBlock.Input("Ball", 1, 0)))
        assertEquals(Rejection(Res.string.error_command_index_invalid, "2"), context.rejection(MoveBlock, MoveBlock.Input("Ball", 0, 0, 2)))
        assertEquals(EditBlock.mergeKey(EditBlock.Input("Ball", 0, 1, jump)), EditBlock.mergeKey(EditBlock.Input("Ball", 0, 1, stop)))
    }

    @Test
    fun targetsNameWhatACommandCreatedOrMoved() = runTest {
        val context = commandContext(ball, wall)
        val addBlock = AddBlock.Input("Ball", 0, stop)
        AddBlock.apply(context, addBlock)
        assertEquals(CommandTarget.BlockType("Ball", 0, 2), AddBlock.target(context, addBlock))
        assertEquals(CommandTarget.BlockType("Ball", 0, 0), AddBlock.target(context, addBlock.copy(index = 0)))
        assertEquals(CommandTarget.BlockType("Ball", 2), MoveEvent.target(context, MoveEvent.Input("Ball", 0, 2)))
        assertEquals(CommandTarget.BlockType("Ball", 0, 1), MoveBlock.target(context, MoveBlock.Input("Ball", 0, 0, 1)))
        val rename = RenameActorType.Input("Wall", "Brick")
        assertEquals(CommandTarget.BlockType("Wall"), RenameActorType.target(context, rename))
        RenameActorType.apply(context, rename)
        assertEquals(CommandTarget.BlockType("Brick"), RenameActorType.target(context, rename))
    }

    @Test
    fun onlyCodeChangedByHandCanBeDiscardedAndThatCantBeUndone() = runTest {
        val repository = FakeBlockTypeRepository(listOf(ball)).apply { changedByHand = setOf("Ball") }
        val context = commandContext(blockTypes = blockTypesController(repository = repository))
        assertNull(context.rejection(DiscardChangesMadeByHand, DiscardChangesMadeByHand.Input("Ball")))
        assertNull(DiscardChangesMadeByHand.apply(context, DiscardChangesMadeByHand.Input("Ball")).inverse)
        assertEquals("regenerate overwriting Ball", repository.writes.last())
        assertEquals(Rejection(Res.string.error_command_not_changed_by_hand, "Ball"), context.rejection(DiscardChangesMadeByHand, DiscardChangesMadeByHand.Input("Ball")))
    }
}
