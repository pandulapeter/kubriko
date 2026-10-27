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
import com.pandulapeter.kubrikoCreator.actors.EventDefinition
import com.pandulapeter.kubrikoCreator.actors.EventType
import com.pandulapeter.kubrikoCreator.commands.actorTypes.CreateActorType
import com.pandulapeter.kubrikoCreator.commands.actorTypes.EditBlock
import com.pandulapeter.kubrikoCreator.commands.actorTypes.SetAppearance
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kubriko.creator.generated.resources.Res
import kubriko.creator.generated.resources.command_add_block
import kubriko.creator.generated.resources.command_create_actor_type
import kubriko.creator.generated.resources.command_edit_block
import kubriko.creator.generated.resources.command_read_actor_type
import kubriko.creator.generated.resources.command_set_appearance
import kubriko.creator.generated.resources.error_actor_type_name_taken
import kubriko.creator.generated.resources.error_command_actor_type_missing
import kubriko.creator.generated.resources.error_command_input_invalid
import kubriko.creator.generated.resources.error_command_unknown
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

@OptIn(ExperimentalCoroutinesApi::class)
class CommandExecutorTest {
    private val ball = ActorTypeDefinition(name = "Ball", events = listOf(EventDefinition(EventType.EVERY_FRAME, blocks = listOf(BlockDefinition.SetVelocity(1f, 1f)))))
    private val rejections = mutableListOf<Rejection>()

    private fun TestScope.executor(vararg definitions: ActorTypeDefinition) = commandContext(*definitions).let { context ->
        CommandExecutor(context = context, scope = testScope(), onRejected = { rejections += it }) to context
    }

    private fun CommandContext.names() = actorTypes!!.actorTypes.value.map { it.name }

    private fun width(width: Float) = Invocation(SetAppearance, SetAppearance.Input("Ball", width = width))

    @Test
    fun changesAreUndoneAndRedoneInOrder() = runTest {
        val (executor, context) = executor()
        executor.execute(Invocation(CreateActorType, ActorTypeDefinition("Ball")))
        executor.execute(Invocation(CreateActorType, ActorTypeDefinition("Wall")))
        assertEquals(Res.string.command_create_actor_type, executor.history.value.nextUndoTitle)
        executor.undo()
        assertEquals(listOf("Ball"), context.names())
        executor.undo()
        assertEquals(emptyList(), context.names())
        assertNull(executor.history.value.nextUndoTitle)
        executor.redo()
        assertEquals(listOf("Ball"), context.names())
        executor.redo()
        assertEquals(listOf("Ball", "Wall"), context.names())
        assertNull(executor.history.value.nextRedoTitle)
    }

    @Test
    fun aNewChangeClearsWhatCouldBeRedone() = runTest {
        val (executor, context) = executor(ball)
        executor.execute(width(10f))
        executor.undo()
        assertEquals(Res.string.command_set_appearance, executor.history.value.nextRedoTitle)
        executor.execute(Invocation(CreateActorType, ActorTypeDefinition("Wall")))
        assertNull(executor.history.value.nextRedoTitle)
        assertEquals(listOf("Ball", "Wall"), context.names())
    }

    @Test
    fun consecutiveChangesOfTheSameFieldAreUndoneTogether() = runTest {
        val (executor, context) = executor(ball)
        listOf(1f, 12f, 123f).forEach { executor.execute(width(it)) }
        executor.execute(Invocation(SetAppearance, SetAppearance.Input("Ball", height = 5f)))
        listOf(2f, 3f).forEach { executor.execute(Invocation(EditBlock, EditBlock.Input("Ball", 0, 0, BlockDefinition.SetVelocity(it, 1f)))) }
        assertEquals(3, executor.history.value.undoable.size)
        assertEquals(Res.string.command_edit_block, executor.history.value.nextUndoTitle)
        executor.undo()
        assertEquals(ball.events, context.actorTypes!!["Ball"]!!.events)
        executor.undo()
        executor.undo()
        assertEquals(ball, context.actorTypes["Ball"])
        executor.redo()
        assertEquals(123f, context.actorTypes["Ball"]!!.width)
    }

    @Test
    fun rejectedChangesChangeNothingAndAreNotRecorded() = runTest {
        val (executor, context) = executor(ball)
        val outcome = executor.execute(Invocation(CreateActorType, ActorTypeDefinition("Ball", width = 5f)))
        assertEquals(CommandOutcome.Rejected(Rejection(Res.string.error_actor_type_name_taken)), outcome)
        assertEquals(listOf(ball), context.actorTypes!!.actorTypes.value)
        assertNull(executor.history.value.nextUndoTitle)
    }

    @Test
    fun stepsThatNoLongerApplyAreReportedAndDropped() = runTest {
        val (executor, context) = executor(ball)
        executor.execute(Invocation(CreateActorType, ActorTypeDefinition("Wall")))
        executor.execute(width(10f))
        @OptIn(EditorMutation::class)
        context.actorTypes!!.delete("Ball")
        executor.undo()
        assertEquals(listOf(Rejection(Res.string.error_command_actor_type_missing, "Ball")), rejections)
        assertEquals(Res.string.command_create_actor_type, executor.history.value.nextUndoTitle)
        assertNull(executor.history.value.nextRedoTitle)
        executor.undo()
        assertEquals(emptyList(), context.names())
    }

    @Test
    fun toolCallsAreDecodedValidatedAndRecorded() = runTest {
        val (executor, context) = executor(ball)
        val outcome = executor.call("set_appearance", buildJsonObject { put("actorType", "Ball"); put("color", "#112233"); put("shape", "rectangle") })
        assertIs<CommandOutcome.Success>(outcome)
        assertEquals(0xFF112233, context.actorTypes!!["Ball"]!!.color)
        assertEquals(Res.string.command_set_appearance, executor.history.value.nextUndoTitle)
        val event = buildJsonObject { put("actorType", "Ball"); put("eventIndex", 0); put("blockIndex", 0); put("block", buildJsonObject { put("type", "jumpTo"); put("x", 5) }) }
        assertIs<CommandOutcome.Success>(executor.call("edit_block", event))
        assertEquals(BlockDefinition.JumpTo(5f, 0f), context.actorTypes["Ball"]!!.events[0].blocks[0])
    }

    @Test
    fun toolCallsAreRecordedAsStepsThatStartRunningAndNameWhatTheyChanged() = runTest {
        val (executor, _) = executor(ball)
        val running = mutableListOf<AiStep>()
        val collecting = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { executor.aiSteps.collect { steps -> steps.lastOrNull()?.takeIf { it.isRunning }?.let(running::add) } }
        executor.call("add_block", buildJsonObject { put("actorType", "Ball"); put("eventIndex", 0); put("block", buildJsonObject { put("type", "stop") }) })
        executor.call("read_actor_type", buildJsonObject { put("actorType", "Wall") })
        executor.call("set_appearance", buildJsonObject { put("width", "wide") })
        collecting.cancel()
        assertEquals(
            listOf(
                AiStep(0, Res.string.command_add_block, CommandTarget.ActorType("Ball", 0, 1), isRunning = false),
                AiStep(1, Res.string.command_read_actor_type, CommandTarget.ActorType("Wall"), isRunning = false, rejection = Rejection(Res.string.error_command_actor_type_missing, "Wall")),
            ),
            executor.aiSteps.value.take(2),
        )
        assertEquals(Res.string.error_command_input_invalid, executor.aiSteps.value[2].rejection?.reason)
        assertEquals(listOf(AiStep(0, Res.string.command_add_block, CommandTarget.ActorType("Ball", 0, 0), isRunning = true)), running.take(1))
    }

    @Test
    fun theUsersOwnChangesAreNotAiSteps() = runTest {
        val (executor, _) = executor(ball)
        executor.execute(width(10f))
        executor.undo()
        assertEquals(emptyList(), executor.aiSteps.value)
    }

    @Test
    fun unknownToolsAndInvalidInputAreRejected() = runTest {
        val (executor, _) = executor(ball)
        assertEquals(CommandOutcome.Rejected(Rejection(Res.string.error_command_unknown, "fly")), executor.call("fly", JsonObject(emptyMap())))
        listOf(
            buildJsonObject { put("actorType", "Ball"); put("shape", "triangle") },
            buildJsonObject { put("actorType", "Ball"); put("speed", 3) },
            buildJsonObject { put("width", 3) },
            JsonObject(mapOf("actorType" to JsonPrimitive(3))),
        ).forEach { arguments ->
            val outcome = assertIs<CommandOutcome.Rejected>(executor.call("set_appearance", arguments))
            assertEquals(Res.string.error_command_input_invalid, outcome.rejection.reason)
        }
        assertNull(executor.history.value.nextUndoTitle)
    }
}
