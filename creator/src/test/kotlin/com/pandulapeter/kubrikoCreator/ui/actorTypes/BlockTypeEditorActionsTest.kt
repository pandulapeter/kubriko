/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.ui.actorTypes

import com.pandulapeter.kubrikoCreator.actors.ActorTypeDefinition
import com.pandulapeter.kubrikoCreator.actors.BlockDefinition
import com.pandulapeter.kubrikoCreator.actors.BlockKind
import com.pandulapeter.kubrikoCreator.actors.BlockTypeDefinition
import com.pandulapeter.kubrikoCreator.actors.EventDefinition
import com.pandulapeter.kubrikoCreator.actors.EventType
import com.pandulapeter.kubrikoCreator.actors.KeyChoice
import com.pandulapeter.kubrikoCreator.actors.ManagerTypeDefinition
import com.pandulapeter.kubrikoCreator.actors.ScreenTextDefinition
import com.pandulapeter.kubrikoCreator.commands.Invocation
import com.pandulapeter.kubrikoCreator.commands.actorTypes.AddBlock
import com.pandulapeter.kubrikoCreator.commands.actorTypes.AddEvent
import com.pandulapeter.kubrikoCreator.commands.actorTypes.DeleteActorType
import com.pandulapeter.kubrikoCreator.commands.actorTypes.MoveBlock
import com.pandulapeter.kubrikoCreator.commands.actorTypes.RemoveEvent
import com.pandulapeter.kubrikoCreator.commands.actorTypes.SetEventKey
import com.pandulapeter.kubrikoCreator.commands.actorTypes.SetEventOtherActorType
import com.pandulapeter.kubrikoCreator.commands.actorTypes.SetEventTimer
import com.pandulapeter.kubrikoCreator.commands.managerTypes.AddText
import com.pandulapeter.kubrikoCreator.commands.managerTypes.DeleteManagerType
import kotlin.test.Test
import kotlin.test.assertEquals

class BlockTypeEditorActionsTest {

    private val ball = ActorTypeDefinition("Ball")
    private val dispatched = mutableListOf<Invocation<*>>()

    private val successes = mutableListOf<() -> Unit>()

    private fun actionsOf(definition: BlockTypeDefinition) = BlockTypeEditorActions(definition) { invocation, onSuccess ->
        dispatched += invocation
        successes += onSuccess
    }

    @Test
    fun deletingUsesTheCommandOfTheKind() {
        actionsOf(ball).delete()
        actionsOf(ManagerTypeDefinition("Game")).delete()

        assertEquals<List<Invocation<*>>>(
            listOf(
                Invocation(DeleteActorType, DeleteActorType.Input("Ball")),
                Invocation(DeleteManagerType, DeleteManagerType.Input("Game")),
            ),
            dispatched,
        )
    }

    @Test
    fun eventCommandsNameTheTypeAndEvent() {
        val actions = actionsOf(ball)

        actions.setEventKey(2, KeyChoice.A)
        actions.setEventTimer(1, "spawnBrick")
        actions.setEventOtherActorType(3, "Brick")
        actions.removeEvent(1)

        assertEquals<List<Invocation<*>>>(
            listOf(
                Invocation(SetEventKey, SetEventKey.Input("Ball", 2, KeyChoice.A)),
                Invocation(SetEventTimer, SetEventTimer.Input("Ball", 1, "spawnBrick")),
                Invocation(SetEventOtherActorType, SetEventOtherActorType.Input("Ball", 3, "Brick")),
                Invocation(RemoveEvent, RemoveEvent.Input("Ball", 1)),
            ),
            dispatched,
        )
    }

    @Test
    fun blockCommandsNameTheEventAndBlock() {
        val actions = actionsOf(ball)

        actions.addBlock(0, BlockKind.STOP)
        actions.addBlock(0, BlockKind.STOP, gap = 3)
        actions.moveBlock(0, index = 2, gap = 2)
        actions.moveBlock(0, index = 2, gap = 3)
        actions.moveBlock(0, index = 2, gap = 0)

        assertEquals<List<Invocation<*>>>(
            listOf(
                Invocation(AddBlock, AddBlock.Input("Ball", 0, BlockKind.STOP.createDefault())),
                Invocation(AddBlock, AddBlock.Input("Ball", 0, BlockKind.STOP.createDefault(), index = 3)),
                Invocation(MoveBlock, MoveBlock.Input("Ball", 0, 2, 0)),
            ),
            dispatched,
        )
    }

    @Test
    fun overridingAnEventRunsTheParentsFirst() {
        val inherited = EventDefinition(EventType.COLLISION, otherActorType = "Brick", blocks = listOf(BlockDefinition.Stop))

        var added = 0
        actionsOf(ball).overrideEvent(inherited) { added++ }

        assertEquals<List<Invocation<*>>>(
            listOf(Invocation(AddEvent, AddEvent.Input("Ball", EventType.COLLISION, null, "Brick", null, listOf(BlockDefinition.RunParentEvent)))),
            dispatched,
        )
        assertEquals(0, added)
        successes.single().invoke()
        assertEquals(1, added)
    }

    @Test
    fun addingAnEventReportsOnlyItsSuccess() {
        var added = 0
        actionsOf(ball).addEvent(EventType.GAME_STARTED) { added++ }

        assertEquals<List<Invocation<*>>>(listOf(Invocation(AddEvent, AddEvent.Input("Ball", EventType.GAME_STARTED))), dispatched)
        assertEquals(0, added)
        successes.single().invoke()
        assertEquals(1, added)
    }

    @Test
    fun aNewTextOnAManagerWithoutPropertiesShowsScore() {
        actionsOf(ManagerTypeDefinition("Game")).addText()

        assertEquals<List<Invocation<*>>>(listOf(Invocation(AddText, AddText.Input("Game", ScreenTextDefinition(text = "{score}")))), dispatched)
    }
}
