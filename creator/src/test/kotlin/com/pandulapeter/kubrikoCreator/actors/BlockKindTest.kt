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
        assertTrue(BlockKind.BOUNCE_OFF_EDGE.isAvailableIn(EventType.EDGE_TOUCHED))
        (EventType.entries - EventType.EDGE_TOUCHED).forEach { assertFalse(BlockKind.BOUNCE_OFF_EDGE.isAvailableIn(it), it.name) }
    }

    @Test
    fun blocksAboutTheOtherActorAreOnlyOfferedInCollisionEvents() = listOf(BlockKind.BOUNCE_OFF_OTHER, BlockKind.REMOVE_OTHER).forEach { kind ->
        assertTrue(kind.isAvailableIn(EventType.COLLISION))
        (EventType.entries - EventType.COLLISION).forEach { assertFalse(kind.isAvailableIn(it), "${kind.name} in ${it.name}") }
    }

    @Test
    fun everyOtherBlockIsOfferedInEveryEvent() = (BlockKind.entries - BlockKind.BOUNCE_OFF_EDGE - BlockKind.BOUNCE_OFF_OTHER - BlockKind.REMOVE_OTHER).forEach { kind ->
        EventType.entries.forEach { assertTrue(kind.isAvailableIn(it), "${kind.name} in ${it.name}") }
    }
}
