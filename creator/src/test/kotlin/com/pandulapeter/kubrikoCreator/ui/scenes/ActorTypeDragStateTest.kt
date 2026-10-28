/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.ui.scenes

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ActorTypeDragStateTest {
    private var now = 0L
    private val drops = mutableListOf<Pair<String, Offset>>()
    private val state = ActorTypeDragState(clock = { now }).apply {
        dropTarget = ActorTypeDragState.DropTarget(Rect(100f, 0f, 500f, 400f)) { name, at -> drops += name to at }
    }

    @Test
    fun anActorTypeDroppedOnTheSceneIsPlacedThere() {
        state.start("Ball", Offset(10f, 10f))
        assertFalse(state.isOverDropTarget)
        state.moveTo(Offset(200f, 50f))
        assertTrue(state.isOverDropTarget)
        state.end()
        assertEquals(listOf("Ball" to Offset(200f, 50f)), drops)
        assertNull(state.dragged)
    }

    @Test
    fun anActorTypeDroppedElsewhereIsNotPlaced() {
        state.start("Ball", Offset(10f, 10f))
        state.end()
        state.start("Ball", Offset(200f, 50f))
        state.cancel()
        assertTrue(drops.isEmpty())
    }

    @Test
    fun theClickThatEndsADragIsIgnored() {
        assertFalse(state.isClickAfterDrag)
        state.start("Ball", Offset(10f, 10f))
        state.end()
        assertTrue(state.isClickAfterDrag)
        now += 1_000_000_000L
        assertFalse(state.isClickAfterDrag)
    }
}
