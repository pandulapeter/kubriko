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
import com.pandulapeter.kubrikoCreator.actors.EventDefinition
import com.pandulapeter.kubrikoCreator.actors.EventType
import com.pandulapeter.kubrikoCreator.actors.KeyChoice
import com.pandulapeter.kubrikoCreator.actors.ManagerTypeDefinition
import com.pandulapeter.kubrikoCreator.actors.PlacementProblem
import com.pandulapeter.kubrikoCreator.commands.actorTypes.AddEvent
import com.pandulapeter.kubrikoCreator.commands.actorTypes.SetEventKey
import com.pandulapeter.kubrikoCreator.context.BlockNotation
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * The key released, key held, and removed from the Scene Events, through the commands.
 */
class KeyAndRemovedEventsTest {
    private val ship = ActorTypeDefinition(name = "Ship")
    private val game = ManagerTypeDefinition(name = "Game")

    @Test
    fun keyEventsStartOnSpacebarAndTakeAnotherKey() = runTest {
        val context = commandContext(ship, game)
        listOf(EventType.KEY_RELEASED, EventType.KEY_HELD).forEachIndexed { index, type ->
            val events = context.assertRoundTrip(Invocation(AddEvent, AddEvent.Input("Ship", type))) { context.blockTypes!!.types.value.single { it.name == "Ship" }.events }
            assertEquals(EventDefinition(type, key = KeyChoice.SPACE), events[index])
            val changed = context.assertRoundTrip(Invocation(SetEventKey, SetEventKey.Input("Ship", index, KeyChoice.A))) { context.blockTypes!!.types.value.single { it.name == "Ship" }.events }
            assertEquals(KeyChoice.A, changed[index].key)
        }
    }

    @Test
    fun onlyKeyEventsTakeAKeyAndOnlyActorTypesAreRemoved() = runTest {
        val context = commandContext(ship.copy(events = listOf(EventDefinition(EventType.REMOVED_FROM_SCENE))), game)
        assertEquals(PlacementProblem.KEY_NOT_ALLOWED, SetEventKey.validate(context, SetEventKey.Input("Ship", 0, KeyChoice.A))?.reason)
        assertEquals(PlacementProblem.KEY_NOT_ALLOWED, AddEvent.validate(context, AddEvent.Input("Ship", EventType.REMOVED_FROM_SCENE, key = KeyChoice.A))?.reason)
        assertEquals(PlacementProblem.EVENT_REMOVED_FROM_SCENE_IN_MANAGER_TYPE, AddEvent.validate(context, AddEvent.Input("Game", EventType.REMOVED_FROM_SCENE))?.reason)
    }

    @Test
    fun theEventsReadAsSentences() {
        assertEquals("When Spacebar is released", BlockNotation.of(EventDefinition(EventType.KEY_RELEASED, key = KeyChoice.SPACE)))
        assertEquals("While A is held", BlockNotation.of(EventDefinition(EventType.KEY_HELD, key = KeyChoice.A)))
        assertEquals("When it is removed from the Scene", BlockNotation.of(EventDefinition(EventType.REMOVED_FROM_SCENE)))
    }
}
