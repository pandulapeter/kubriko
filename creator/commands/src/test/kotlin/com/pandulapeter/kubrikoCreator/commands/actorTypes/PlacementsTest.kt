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
import com.pandulapeter.kubrikoCreator.commands.PlacementProblem
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PlacementsTest {

    @Test
    fun everyEventInEveryTypeHasAProblemExactlyWhenItIsntAvailable() {
        BlockTypeKind.entries.forEach { kind ->
            EventType.entries.forEach { eventType ->
                assertEquals(!eventType.isAvailableIn(kind), eventType.problemIn(kind) != null, "$eventType in $kind")
            }
        }
    }

    @Test
    fun anEventIsRejectedForItsOwnReason() {
        assertEquals(PlacementProblem.EVENT_GAME_STARTED_IN_ACTOR_TYPE, EventType.GAME_STARTED.problemIn(BlockTypeKind.ACTOR))
        assertEquals(PlacementProblem.EVENT_ADDED_TO_SCENE_IN_MANAGER_TYPE, EventType.ADDED_TO_SCENE.problemIn(BlockTypeKind.MANAGER))
        assertEquals(PlacementProblem.EVENT_EDGE_TOUCHED_IN_MANAGER_TYPE, EventType.EDGE_TOUCHED.problemIn(BlockTypeKind.MANAGER))
        assertEquals(PlacementProblem.EVENT_COLLISION_IN_MANAGER_TYPE, EventType.COLLISION.problemIn(BlockTypeKind.MANAGER))
        assertNull(EventType.GAME_STARTED.problemIn(BlockTypeKind.MANAGER))
    }

    @Test
    fun everyBlockInEveryEventOfEveryTypeHasAProblemExactlyWhenItIsntAvailable() {
        BlockTypeKind.entries.forEach { kind ->
            EventType.entries.forEach { eventType ->
                BlockKind.entries.forEach { blockKind ->
                    val problem = blockKind.problemIn(eventType, kind)
                    assertEquals(!blockKind.isAvailableIn(eventType, kind), problem != null, "$blockKind in $eventType of $kind")
                    assertTrue(problem == null || (problem.name.endsWith("IN_MANAGER_TYPE") == (kind == BlockTypeKind.MANAGER && blockKind.needsActor)), "$blockKind in $eventType of $kind")
                }
            }
        }
    }

    @Test
    fun everyBlockHasAReasonOfItsOwnExactlyWhereItNeedsOne() {
        BlockKind.entries.forEach { blockKind ->
            assertEquals(blockKind.needsActor, blockKind.managerTypeProblem != null, "$blockKind in a Manager type")
            assertEquals(blockKind.onlyIn != null, blockKind.otherEventProblem != null, "$blockKind outside its Event")
        }
    }

    @Test
    fun aBlockIsRejectedForItsOwnReasonAndAManagerTypesFirst() {
        assertEquals(PlacementProblem.BLOCK_BOUNCE_OFF_EDGE_OUTSIDE_EDGE_TOUCHED, BlockKind.BOUNCE_OFF_EDGE.problemIn(EventType.EVERY_FRAME, BlockTypeKind.ACTOR))
        assertEquals(PlacementProblem.BLOCK_BOUNCE_OFF_OTHER_OUTSIDE_COLLISION, BlockKind.BOUNCE_OFF_OTHER.problemIn(EventType.EDGE_TOUCHED, BlockTypeKind.ACTOR))
        assertEquals(PlacementProblem.BLOCK_REMOVE_OTHER_OUTSIDE_COLLISION, BlockKind.REMOVE_OTHER.problemIn(EventType.KEY_PRESSED, BlockTypeKind.ACTOR))
        assertEquals(PlacementProblem.BLOCK_MOVEMENT_IN_MANAGER_TYPE, BlockKind.BOUNCE_OFF_EDGE.problemIn(EventType.EVERY_FRAME, BlockTypeKind.MANAGER))
        assertEquals(PlacementProblem.BLOCK_MOVEMENT_IN_MANAGER_TYPE, BlockKind.JUMP_TO.problemIn(EventType.GAME_STARTED, BlockTypeKind.MANAGER))
        assertEquals(PlacementProblem.BLOCK_REMOVE_SELF_IN_MANAGER_TYPE, BlockKind.REMOVE_SELF.problemIn(EventType.EVERY_FRAME, BlockTypeKind.MANAGER))
        assertEquals(PlacementProblem.BLOCK_REMOVE_OTHER_IN_MANAGER_TYPE, BlockKind.REMOVE_OTHER.problemIn(EventType.KEY_PRESSED, BlockTypeKind.MANAGER))
        assertEquals(PlacementProblem.BLOCK_RUN_PARENT_EVENT_IN_MANAGER_TYPE, BlockKind.RUN_PARENT_EVENT.problemIn(EventType.TIMER_FINISHED, BlockTypeKind.MANAGER))
        assertNull(BlockKind.REMOVE_OTHER.problemIn(EventType.COLLISION, BlockTypeKind.ACTOR))
    }

    @Test
    fun theAiReadsTheIdsOfTheEventsAndBlocksAProblemNames() {
        val named = mapOf(
            PlacementProblem.EVENT_GAME_STARTED_IN_ACTOR_TYPE to listOf(EventType.GAME_STARTED.serialName, EventType.ADDED_TO_SCENE.serialName),
            PlacementProblem.EVENT_ADDED_TO_SCENE_IN_MANAGER_TYPE to listOf(EventType.ADDED_TO_SCENE.serialName, EventType.GAME_STARTED.serialName),
            PlacementProblem.EVENT_EDGE_TOUCHED_IN_MANAGER_TYPE to listOf(EventType.EDGE_TOUCHED.serialName),
            PlacementProblem.EVENT_COLLISION_IN_MANAGER_TYPE to listOf(EventType.COLLISION.serialName),
            PlacementProblem.KEY_NOT_ALLOWED to listOf(EventType.KEY_PRESSED.serialName),
            PlacementProblem.OTHER_ACTOR_TYPE_NOT_ALLOWED to listOf(EventType.COLLISION.serialName),
            PlacementProblem.TIMER_NOT_ALLOWED to listOf(EventType.TIMER_FINISHED.serialName),
            PlacementProblem.BLOCK_BOUNCE_OFF_EDGE_OUTSIDE_EDGE_TOUCHED to listOf(BlockKind.BOUNCE_OFF_EDGE.serialName, EventType.EDGE_TOUCHED.serialName),
            PlacementProblem.BLOCK_BOUNCE_OFF_OTHER_OUTSIDE_COLLISION to listOf(BlockKind.BOUNCE_OFF_OTHER.serialName, EventType.COLLISION.serialName),
            PlacementProblem.BLOCK_REMOVE_OTHER_OUTSIDE_COLLISION to listOf(BlockKind.REMOVE_OTHER.serialName, EventType.COLLISION.serialName),
            PlacementProblem.BLOCK_REMOVE_SELF_IN_MANAGER_TYPE to listOf(BlockKind.REMOVE_SELF.serialName),
            PlacementProblem.BLOCK_REMOVE_OTHER_IN_MANAGER_TYPE to listOf(BlockKind.REMOVE_OTHER.serialName),
            PlacementProblem.BLOCK_RUN_PARENT_EVENT_IN_MANAGER_TYPE to listOf(BlockKind.RUN_PARENT_EVENT.serialName),
            PlacementProblem.BLOCK_MOVEMENT_IN_MANAGER_TYPE to BlockKind.entries
                .filter { it.problemIn(EventType.EVERY_FRAME, BlockTypeKind.MANAGER) == PlacementProblem.BLOCK_MOVEMENT_IN_MANAGER_TYPE }
                .map { it.serialName },
        )
        named.forEach { (problem, ids) -> ids.forEach { assertTrue(Regex("\\b$it\\b").containsMatchIn(problem.english), "$problem names $it") } }
    }

    @Test
    fun aMissingPropertyIsNamedByItsKind() {
        assertEquals(
            listOf(PlacementProblem.NUMBER_PROPERTY_MISSING, PlacementProblem.TEXT_PROPERTY_MISSING, PlacementProblem.TOGGLE_PROPERTY_MISSING),
            PropertyKind.entries.map { it.missingProblem },
        )
        PropertyKind.entries.forEach { assertTrue(" ${it.serialName} Property" in it.missingProblem.english, "$it") }
    }
}
