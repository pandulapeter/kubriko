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

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import com.pandulapeter.kubrikoCreator.actors.BlockDefinition
import com.pandulapeter.kubrikoCreator.actors.BlockKind
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ListDragStateTest {
    private val drops = mutableListOf<Pair<DraggedBlock, Int>>()
    private var now = 0L
    private val state = BlockDragState(onDrop = { block, gap -> drops += block to gap }, clock = { now }).apply {
        listBounds = Rect(0f, 0f, 100f, 300f)
        laidOutRows = { LaidOutRows(0, listOf(0f..40f, 50f..90f)) }
    }

    @Test
    fun theGapIsBeforeTheFirstRowWhoseMiddleIsBelowThePointer() {
        val rows = listOf(0f..40f, 50f..90f)
        assertEquals(0, gapAt(-5f, rows))
        assertEquals(0, gapAt(19f, rows))
        assertEquals(1, gapAt(21f, rows))
        assertEquals(1, gapAt(69f, rows))
        assertEquals(2, gapAt(71f, rows))
        assertEquals(0, gapAt(10f, emptyList()))
    }

    @Test
    fun aBlockMovedIntoAGapAfterItselfEndsUpOneIndexEarlierAndTheGapsAroundItMoveNothing() {
        assertEquals(2, moveTarget(index = 0, gap = 3))
        assertEquals(0, moveTarget(index = 2, gap = 0))
        assertNull(moveTarget(index = 1, gap = 1))
        assertNull(moveTarget(index = 1, gap = 2))
    }

    @Test
    fun aBlockDroppedOverTheListLandsInTheGapUnderThePointer() {
        state.start(DraggedBlock.New(BlockKind.STOP), Offset(500f, 500f))
        assertNull(state.gap)
        state.moveTo(Offset(50f, 60f))
        assertEquals(1, state.gap)
        state.end()
        assertEquals(listOf<Pair<DraggedBlock, Int>>(DraggedBlock.New(BlockKind.STOP) to 1), drops)
        assertNull(state.dragged)
    }

    @Test
    fun rowsScrolledOutOfTheListCountBeforeTheLaidOutOnes() {
        state.laidOutRows = { LaidOutRows(10, listOf(-20f..40f, 50f..90f)) }
        state.start(DraggedBlock.New(BlockKind.STOP), Offset(50f, 5f))
        assertEquals(10, state.gap)
        state.moveTo(Offset(50f, 80f))
        assertEquals(12, state.gap)
        state.laidOutRows = { LaidOutRows.NONE }
        assertEquals(0, state.gap)
    }

    @Test
    fun theListScrollsWhileABlockIsDraggedNearItsEdges() {
        assertEquals(0f, state.autoScrollStep(edge = 40f, maximumStep = 10f), "nothing is dragged")
        state.start(DraggedBlock.New(BlockKind.STOP), Offset(50f, 150f))
        assertEquals(0f, state.autoScrollStep(edge = 40f, maximumStep = 10f))
        state.moveTo(Offset(50f, 0f))
        assertEquals(-10f, state.autoScrollStep(edge = 40f, maximumStep = 10f))
        state.moveTo(Offset(50f, 20f))
        assertEquals(-5f, state.autoScrollStep(edge = 40f, maximumStep = 10f))
        state.moveTo(Offset(50f, 290f))
        assertEquals(7.5f, state.autoScrollStep(edge = 40f, maximumStep = 10f))
        state.moveTo(Offset(150f, 290f))
        assertEquals(0f, state.autoScrollStep(edge = 40f, maximumStep = 10f), "outside the list")
    }

    @Test
    fun aBlockDroppedOutsideTheListOrCancelledLandsNowhere() {
        val block = DraggedBlock.Existing(0, BlockDefinition.Stop)
        state.start(block, Offset(50f, 10f))
        state.moveTo(Offset(150f, 10f))
        state.end()
        state.start(block, Offset(50f, 10f))
        state.cancel()
        assertEquals(emptyList(), drops)
        assertEquals(BlockKind.STOP, block.kind)
    }

    @Test
    fun aClickRightAfterADragIsTheDragsRelease() {
        now = 1_000_000_000L
        assertFalse(state.isClickAfterDrag)
        state.start(DraggedBlock.New(BlockKind.STOP), Offset(150f, 10f))
        state.end()
        assertTrue(state.isClickAfterDrag)
        now += 400_000_000L
        assertFalse(state.isClickAfterDrag)
    }
}
