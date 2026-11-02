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
import com.pandulapeter.kubrikoCreator.actors.ManagerTypeDefinition
import com.pandulapeter.kubrikoCreator.commands.actorTypes.AddBlock
import com.pandulapeter.kubrikoCreator.commands.actorTypes.CreateActorType
import com.pandulapeter.kubrikoCreator.commands.actorTypes.DeleteActorType
import com.pandulapeter.kubrikoCreator.commands.actorTypes.EditBlock
import com.pandulapeter.kubrikoCreator.commands.actorTypes.RemoveBlock
import com.pandulapeter.kubrikoCreator.commands.actorTypes.RemoveEvent
import com.pandulapeter.kubrikoCreator.commands.actorTypes.SetEventOtherActorType
import com.pandulapeter.kubrikoCreator.commands.managerTypes.DeleteManagerType
import com.pandulapeter.kubrikoCreator.commands.scenes.DeleteScene
import com.pandulapeter.kubrikoCreator.foundation.EditorMutation
import com.pandulapeter.kubrikoCreator.scenes.SceneActorEntry
import com.pandulapeter.kubrikoCreator.scenes.SceneDocument
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import kubriko.creator.generated.resources.Res
import kubriko.creator.generated.resources.error_command_actor_type_missing
import kubriko.creator.generated.resources.error_type_name_taken
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

/**
 * Undo and redo restore what was there, which may refer to things deleted since: valid to keep, though not to create.
 */
@OptIn(ExperimentalCoroutinesApi::class, EditorMutation::class)
class RestoringChangesTest {
    private val bat = ActorTypeDefinition(name = "Bat")
    private val player = ActorTypeDefinition(
        name = "Player",
        events = listOf(
            EventDefinition(EventType.COLLISION, otherActorType = "Bat", blocks = listOf(BlockDefinition.RemoveOther)),
            EventDefinition(EventType.EVERY_FRAME, blocks = listOf(BlockDefinition.GoToScene("level2"))),
        ),
    )

    private fun TestScope.executor(context: CommandContext) = CommandExecutor(context = context, scope = testScope())

    private fun CommandContext.names() = blockTypes!!.types.value.map { it.name }

    private suspend fun CommandExecutor.assertSuccess(invocation: Invocation<*>) = assertIs<CommandOutcome.Success>(execute(invocation))

    private suspend fun CommandExecutor.assertStepped(isUndo: Boolean) = assertIs<CommandOutcome.Success>(step(isUndo))

    @Test
    fun aTypeReferringToADeletedTypeComesBackWithItsReferences() = runTest {
        val context = commandContext(bat, player)
        val executor = executor(context)
        executor.assertSuccess(Invocation(DeleteActorType, DeleteActorType.Input("Bat")))
        executor.assertSuccess(Invocation(DeleteActorType, DeleteActorType.Input("Player")))
        executor.assertStepped(isUndo = true)
        assertEquals(player, context.blockTypes!!["Player"])
        executor.assertStepped(isUndo = true)
        assertEquals(listOf("Bat", "Player"), context.names())
        executor.assertStepped(isUndo = false)
        executor.assertStepped(isUndo = false)
        assertEquals(emptyList(), context.names())
    }

    @Test
    fun aManagerTypeReferringToADeletedSceneComesBack() = runTest {
        val score = ManagerTypeDefinition(name = "Score", events = listOf(EventDefinition(EventType.EVERY_FRAME, blocks = listOf(BlockDefinition.GoToScene("level2")))))
        val context = commandContext(score)
        val executor = executor(context)
        executor.assertSuccess(Invocation(DeleteManagerType, DeleteManagerType.Input("Score")))
        executor.assertStepped(isUndo = true)
        assertEquals(score, context.blockTypes!!["Score"])
    }

    @Test
    fun eventsAndBlocksReferringToDeletedThingsComeBack() = runTest {
        val context = commandContext(player)
        val executor = executor(context)
        executor.assertSuccess(Invocation(RemoveEvent, RemoveEvent.Input("Player", 0)))
        executor.assertSuccess(Invocation(RemoveBlock, RemoveBlock.Input("Player", 0, 0)))
        executor.assertStepped(isUndo = true)
        executor.assertStepped(isUndo = true)
        assertEquals(player, context.blockTypes!!["Player"])
        executor.assertSuccess(Invocation(EditBlock, EditBlock.Input("Player", 1, 0, BlockDefinition.RestartScene)))
        executor.assertSuccess(Invocation(SetEventOtherActorType, SetEventOtherActorType.Input("Player", 0, null)))
        executor.assertStepped(isUndo = true)
        executor.assertStepped(isUndo = true)
        assertEquals(player, context.blockTypes!!["Player"])
    }

    @Test
    fun aNewChangeStillNeedsWhatItRefersToToExist() = runTest {
        val context = commandContext(ActorTypeDefinition(name = "Player", events = listOf(EventDefinition(EventType.EVERY_FRAME))))
        val executor = executor(context)
        val rejected = assertIs<CommandOutcome.Rejected>(executor.execute(Invocation(AddBlock, AddBlock.Input("Player", 0, BlockDefinition.AddActor("Bat", 0f, 0f)))))
        assertEquals(Res.string.error_command_actor_type_missing, rejected.rejection.reason)
        assertEquals(Res.string.error_command_actor_type_missing, executor.validate(Invocation(CreateActorType, player.copy(name = "Enemy", events = listOf(player.events.first()))))?.reason)
    }

    @Test
    fun restoringATypeStillNeedsItsNameToBeFree() = runTest {
        val context = commandContext(bat)
        val executor = executor(context)
        executor.assertSuccess(Invocation(DeleteActorType, DeleteActorType.Input("Bat")))
        val restoring = executor.history.value.undoable.last().revert
        context.blockTypes!!.put(bat)
        assertEquals(Res.string.error_type_name_taken, restoring.validate(context)?.reason)
    }

    @Test
    fun aSceneThatDoesNotParseComesBack() = runTest {
        val context = commandContext()
        val content = SceneDocument.format(listOf(SceneActorEntry("Ball", "not an object", "a")))
        context.scenes.write("main", content)
        val executor = executor(context)
        executor.assertSuccess(Invocation(DeleteScene, DeleteScene.Input("main")))
        executor.assertStepped(isUndo = true)
        assertEquals(content, context.scenes.read("main"))
    }
}
