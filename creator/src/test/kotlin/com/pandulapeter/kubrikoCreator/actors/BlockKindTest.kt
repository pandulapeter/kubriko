/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.actors

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BlockKindTest {

    @Test
    fun everyKindCreatesABlockOfItsOwnKind() = BlockKind.entries.forEach { kind ->
        assertEquals(kind, BlockKind.of(kind.createDefault()), kind.name)
    }

    @Test
    fun bouncingIsOnlyOfferedWhereAnEdgeWasTouched() {
        assertTrue(BlockKind.BOUNCE_OFF_EDGE.isAvailableIn(EventType.EDGE_TOUCHED, BlockTypeKind.ACTOR))
        (EventType.entries - EventType.EDGE_TOUCHED).forEach { assertFalse(BlockKind.BOUNCE_OFF_EDGE.isAvailableIn(it, BlockTypeKind.ACTOR), it.name) }
    }

    @Test
    fun blocksAboutTheOtherActorAreOnlyOfferedInCollisionEvents() = listOf(BlockKind.BOUNCE_OFF_OTHER, BlockKind.REMOVE_OTHER).forEach { kind ->
        assertTrue(kind.isAvailableIn(EventType.COLLISION, BlockTypeKind.ACTOR))
        (EventType.entries - EventType.COLLISION).forEach { assertFalse(kind.isAvailableIn(it, BlockTypeKind.ACTOR), "${kind.name} in ${it.name}") }
    }

    @Test
    fun everyOtherBlockIsOfferedInEveryEvent() = (BlockKind.entries - BlockKind.BOUNCE_OFF_EDGE - BlockKind.BOUNCE_OFF_OTHER - BlockKind.REMOVE_OTHER).forEach { kind ->
        EventType.entries.forEach { assertTrue(kind.isAvailableIn(it, BlockTypeKind.ACTOR), "${kind.name} in ${it.name}") }
    }

    @Test
    fun aManagerIsOfferedEveryBlockThatDoesNotNeedAnActor() {
        val offered = BlockKind.entries.filter { kind -> EventType.entries.any { kind.isAvailableIn(it, BlockTypeKind.MANAGER) } }
        assertEquals(BlockKind.entries.filter { !it.needsActor }, offered)
        listOf(BlockKind.SET_VELOCITY, BlockKind.JUMP_TO, BlockKind.REMOVE_SELF, BlockKind.MOVE_TOWARDS_NEAREST, BlockKind.BOUNCE_OFF_EDGE).forEach { assertTrue(it.needsActor, it.name) }
        listOf(BlockKind.ADD_ACTOR, BlockKind.CHANGE_NUMBER, BlockKind.IF, BlockKind.START_TIMER, BlockKind.LOG, BlockKind.KOTLIN).forEach { assertFalse(it.needsActor, it.name) }
    }

    @Test
    fun managersAndActorsHaveTheirOwnStartEvents() {
        assertEquals(listOf(EventType.GAME_STARTED, EventType.EVERY_FRAME, EventType.KEY_PRESSED, EventType.TIMER_FINISHED), EventType.entries.filter { it.isAvailableIn(BlockTypeKind.MANAGER) })
        assertEquals(EventType.entries - EventType.GAME_STARTED, EventType.entries.filter { it.isAvailableIn(BlockTypeKind.ACTOR) })
    }

    @Test
    fun onlyActorsCheckTheirVelocity() {
        assertFalse(ConditionKind.VELOCITY.isAvailableIn(BlockTypeKind.MANAGER))
        (ConditionKind.entries - ConditionKind.VELOCITY).forEach { assertTrue(it.isAvailableIn(BlockTypeKind.MANAGER), it.name) }
    }
}
