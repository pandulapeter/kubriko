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

import com.pandulapeter.kubrikoCreator.actors.BlockKind
import com.pandulapeter.kubrikoCreator.actors.BlockTypeKind
import com.pandulapeter.kubrikoCreator.actors.EventType
import com.pandulapeter.kubrikoCreator.actors.PropertyKind
import com.pandulapeter.kubrikoCreator.commands.CommandProblem
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PlacementsTest {

    @Test
    fun everyEventInEveryType() {
        BlockTypeKind.entries.forEach { kind ->
            EventType.entries.forEach { eventType ->
                assertEquals(!eventType.isAvailableIn(kind), eventType.problemIn(kind) != null, "$eventType in $kind")
            }
        }
    }

    @Test
    fun anEventIsRejectedForItsOwnReason() {
        assertEquals(CommandProblem.EVENT_GAME_STARTED_IN_ACTOR_TYPE, EventType.GAME_STARTED.problemIn(BlockTypeKind.ACTOR))
        assertEquals(CommandProblem.EVENT_ADDED_TO_SCENE_IN_MANAGER_TYPE, EventType.ADDED_TO_SCENE.problemIn(BlockTypeKind.MANAGER))
        assertEquals(CommandProblem.EVENT_EDGE_TOUCHED_IN_MANAGER_TYPE, EventType.EDGE_TOUCHED.problemIn(BlockTypeKind.MANAGER))
        assertEquals(CommandProblem.EVENT_COLLISION_IN_MANAGER_TYPE, EventType.COLLISION.problemIn(BlockTypeKind.MANAGER))
        assertNull(EventType.GAME_STARTED.problemIn(BlockTypeKind.MANAGER))
    }

    @Test
    fun everyBlockInEveryEventOfEveryType() {
        BlockTypeKind.entries.forEach { kind ->
            EventType.entries.filter { it.isAvailableIn(kind) }.forEach { eventType ->
                BlockKind.entries.forEach { blockKind ->
                    assertEquals(!blockKind.isAvailableIn(eventType, kind), blockKind.problemIn(eventType, kind) != null, "$blockKind in $eventType of $kind")
                }
            }
        }
    }

    @Test
    fun aBlockIsRejectedForItsOwnReason() {
        assertEquals(CommandProblem.BLOCK_BOUNCE_OFF_EDGE_OUTSIDE_EDGE_TOUCHED, BlockKind.BOUNCE_OFF_EDGE.problemIn(EventType.EVERY_FRAME, BlockTypeKind.ACTOR))
        assertEquals(CommandProblem.BLOCK_BOUNCE_OFF_OTHER_OUTSIDE_COLLISION, BlockKind.BOUNCE_OFF_OTHER.problemIn(EventType.EDGE_TOUCHED, BlockTypeKind.ACTOR))
        assertEquals(CommandProblem.BLOCK_REMOVE_OTHER_OUTSIDE_COLLISION, BlockKind.REMOVE_OTHER.problemIn(EventType.KEY_PRESSED, BlockTypeKind.MANAGER))
        assertEquals(CommandProblem.BLOCK_MOVEMENT_IN_MANAGER_TYPE, BlockKind.JUMP_TO.problemIn(EventType.GAME_STARTED, BlockTypeKind.MANAGER))
        assertEquals(CommandProblem.BLOCK_REMOVE_SELF_IN_MANAGER_TYPE, BlockKind.REMOVE_SELF.problemIn(EventType.EVERY_FRAME, BlockTypeKind.MANAGER))
        assertEquals(CommandProblem.BLOCK_RUN_PARENT_EVENT_IN_MANAGER_TYPE, BlockKind.RUN_PARENT_EVENT.problemIn(EventType.TIMER_FINISHED, BlockTypeKind.MANAGER))
        assertNull(BlockKind.REMOVE_OTHER.problemIn(EventType.COLLISION, BlockTypeKind.ACTOR))
    }

    @Test
    fun theAiReadsTheIdsOfTheEventsAndBlocksAProblemNames() {
        val named = mapOf(
            CommandProblem.EVENT_GAME_STARTED_IN_ACTOR_TYPE to listOf(EventType.GAME_STARTED.serialName, EventType.ADDED_TO_SCENE.serialName),
            CommandProblem.EVENT_ADDED_TO_SCENE_IN_MANAGER_TYPE to listOf(EventType.ADDED_TO_SCENE.serialName, EventType.GAME_STARTED.serialName),
            CommandProblem.EVENT_EDGE_TOUCHED_IN_MANAGER_TYPE to listOf(EventType.EDGE_TOUCHED.serialName),
            CommandProblem.EVENT_COLLISION_IN_MANAGER_TYPE to listOf(EventType.COLLISION.serialName),
            CommandProblem.KEY_NOT_ALLOWED to listOf(EventType.KEY_PRESSED.serialName),
            CommandProblem.OTHER_ACTOR_TYPE_NOT_ALLOWED to listOf(EventType.COLLISION.serialName),
            CommandProblem.TIMER_NOT_ALLOWED to listOf(EventType.TIMER_FINISHED.serialName),
            CommandProblem.BLOCK_BOUNCE_OFF_EDGE_OUTSIDE_EDGE_TOUCHED to listOf(BlockKind.BOUNCE_OFF_EDGE.serialName, EventType.EDGE_TOUCHED.serialName),
            CommandProblem.BLOCK_BOUNCE_OFF_OTHER_OUTSIDE_COLLISION to listOf(BlockKind.BOUNCE_OFF_OTHER.serialName, EventType.COLLISION.serialName),
            CommandProblem.BLOCK_REMOVE_OTHER_OUTSIDE_COLLISION to listOf(BlockKind.REMOVE_OTHER.serialName, EventType.COLLISION.serialName),
            CommandProblem.BLOCK_REMOVE_SELF_IN_MANAGER_TYPE to listOf(BlockKind.REMOVE_SELF.serialName),
            CommandProblem.BLOCK_RUN_PARENT_EVENT_IN_MANAGER_TYPE to listOf(BlockKind.RUN_PARENT_EVENT.serialName),
            CommandProblem.BLOCK_MOVEMENT_IN_MANAGER_TYPE to BlockKind.entries
                .filter { it.problemIn(EventType.EVERY_FRAME, BlockTypeKind.MANAGER) == CommandProblem.BLOCK_MOVEMENT_IN_MANAGER_TYPE }
                .map { it.serialName },
        )
        named.forEach { (problem, ids) -> ids.forEach { assertTrue(Regex("\\b$it\\b").containsMatchIn(problem.english), "$problem names $it") } }
    }

    @Test
    fun aMissingPropertyIsNamedByItsKind() {
        assertEquals(
            listOf(CommandProblem.NUMBER_PROPERTY_MISSING, CommandProblem.TEXT_PROPERTY_MISSING, CommandProblem.TOGGLE_PROPERTY_MISSING),
            PropertyKind.entries.map { it.missingProblem },
        )
        PropertyKind.entries.forEach { assertTrue(" ${it.serialName} Property" in it.missingProblem.english, "$it") }
    }
}
