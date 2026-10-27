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
import kotlin.test.assertSame

class ActorTypeEditsTest {
    private val stop = BlockDefinition.Stop
    private val jump = BlockDefinition.JumpTo(1f, 2f)
    private val remove = BlockDefinition.RemoveSelf
    private val definition = ActorTypeDefinition(
        name = "Ball",
        events = listOf(
            EventDefinition(EventType.ADDED_TO_SCENE, blocks = listOf(stop, jump, remove)),
            EventDefinition(EventType.KEY_PRESSED, KeyChoice.LEFT),
        ),
    )

    @Test
    fun addedEventsGoLastAndKeyEventsStartWithAKey() {
        assertEquals(EventDefinition(EventType.EVERY_FRAME), definition.withEventAdded(EventType.EVERY_FRAME).events.last())
        assertEquals(KeyChoice.SPACE, definition.withEventAdded(EventType.KEY_PRESSED).events.last().key)
    }

    @Test
    fun eventsCanBeRemovedAndRekeyed() {
        assertEquals(listOf(EventType.KEY_PRESSED), definition.withEventRemoved(0).events.map { it.type })
        assertEquals(KeyChoice.D, definition.withEventKey(1, KeyChoice.D).events[1].key)
    }

    @Test
    fun blocksAreAddedReplacedAndRemovedWithinTheirEvent() {
        assertEquals(listOf(stop), definition.withBlockAdded(1, stop).events[1].blocks)
        assertEquals(listOf(stop, BlockDefinition.JumpTo(5f, 5f), remove), definition.withBlockReplaced(0, 1, BlockDefinition.JumpTo(5f, 5f)).events[0].blocks)
        assertEquals(listOf(stop, remove), definition.withBlockRemoved(0, 1).events[0].blocks)
        assertEquals(definition.events[1], definition.withBlockRemoved(0, 1).events[1])
    }

    @Test
    fun blocksMoveWithinTheirEventAndStopAtEitherEnd() {
        assertEquals(listOf(jump, stop, remove), definition.withBlockMoved(0, 1, -1).events[0].blocks)
        assertEquals(listOf(stop, remove, jump), definition.withBlockMoved(0, 1, 1).events[0].blocks)
        assertEquals(listOf(jump, stop, remove), definition.withBlockMoved(0, 1, -5).events[0].blocks)
        assertEquals(listOf(stop, jump, remove), definition.withBlockMoved(0, 2, 1).events[0].blocks)
    }

    @Test
    fun outOfRangeIndicesChangeNothing() {
        assertSame(definition, definition.withEventRemoved(5))
        assertSame(definition, definition.withEventKey(-1, KeyChoice.A))
        assertSame(definition, definition.withBlockAdded(2, stop))
        assertEquals(definition, definition.withBlockReplaced(0, 9, stop))
        assertEquals(definition, definition.withBlockMoved(0, 9, 1))
        assertEquals(definition, definition.withBlockRemoved(0, 9))
    }
}
