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
import com.pandulapeter.kubrikoCreator.actors.BlockTypeKind
import com.pandulapeter.kubrikoCreator.actors.EventDefinition
import com.pandulapeter.kubrikoCreator.actors.EventType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNotSame
import kotlin.test.assertNull
import kotlin.test.assertTrue

class BlockEventDropTest {
    private val ifBlock = BlockDefinition.If()
    private var events = listOf(
        EventDefinition(EventType.EVERY_FRAME, blocks = listOf(BlockDefinition.Stop, ifBlock, BlockDefinition.RemoveSelf, BlockDefinition.Else, BlockDefinition.End)),
        EventDefinition(EventType.COLLISION, otherActorType = "Ball", blocks = listOf(BlockDefinition.RemoveOther, ifBlock, BlockDefinition.RemoveOther, BlockDefinition.End)),
        EventDefinition(EventType.ADDED_TO_SCENE),
    )
    private var keys = listOf(10L, 11L, 12L)
    private val dropsInto = mutableListOf<Pair<DraggedBlock, Int>>()
    private val successes = mutableListOf<() -> Unit>()
    private val dropsInList = mutableListOf<Pair<DraggedBlock, Int>>()
    private var shownEventIndex: Int? = 0
    private val drop = BlockEventDrop(
        typeKind = { BlockTypeKind.ACTOR },
        events = { events },
        shownEventIndex = { shownEventIndex },
        eventIndexOf = { key -> keys.indexOf(key).takeIf { it >= 0 } },
        onDrop = { block, gap -> dropsInList += block to gap },
        onDropInto = { block, index, onSuccess ->
            dropsInto += block to index
            successes += onSuccess
        },
    ).apply {
        place(10L, Rect(0f, 0f, 100f, 40f))
        place(11L, Rect(0f, 40f, 100f, 80f))
        place(12L, Rect(0f, 80f, 100f, 120f))
    }
    private val stop = DraggedBlock.Existing(0, BlockDefinition.Stop)

    private fun dropAt(block: DraggedBlock, at: Offset) {
        drop.dragState.start(block, Offset(500f, 500f))
        drop.dragState.moveTo(at)
        drop.dragState.end()
    }

    @Test
    fun aBlockLetGoOverAnotherEventsRowGoesIntoItAndLightsItUpOnceMade() {
        drop.dragState.start(stop, Offset(500f, 500f))
        drop.dragState.moveTo(Offset(50f, 100f))
        assertEquals(12L, drop.hoveredKey)
        assertEquals(BlockDropLook.HOVERED, drop.lookOf(12L, 2))
        assertEquals(BlockDropLook.NONE, drop.lookOf(11L, 1))
        assertEquals(BlockDropLook.NONE, drop.lookOf(10L, 0))
        drop.dragState.end()
        assertEquals(listOf<Pair<DraggedBlock, Int>>(stop to 2), dropsInto)
        assertNull(drop.dropped)
        successes.single().invoke()
        val first = assertNotNull(drop.dropped)
        assertEquals(12L, first.key)
        successes.single().invoke()
        assertNotSame(first, drop.dropped)
        assertNull(drop.hoveredKey)
        assertEquals(BlockDropLook.NONE, drop.lookOf(12L, 2))
    }

    @Test
    fun theOutlineFollowsARowThatMovesUnderAStillPointer() {
        drop.dragState.start(stop, Offset(500f, 500f))
        drop.dragState.moveTo(Offset(50f, 100f))
        assertEquals(12L, drop.hoveredKey)
        drop.place(12L, Rect(0f, 120f, 100f, 160f))
        drop.place(11L, Rect(0f, 80f, 100f, 120f))
        assertEquals(11L, drop.hoveredKey)
    }

    @Test
    fun theEventIsFoundByItsKeyWhenTheBlockIsLetGo() {
        events = listOf(EventDefinition(EventType.KEY_PRESSED)) + events
        keys = listOf(9L) + keys
        shownEventIndex = 1
        dropAt(stop, Offset(50f, 100f))
        assertEquals(listOf<Pair<DraggedBlock, Int>>(stop to 3), dropsInto)
    }

    @Test
    fun anIfBlockGoesOnlyWhereEveryBlockOfItsSectionCanAndAnElseOrEndBlockNowhere() {
        shownEventIndex = 1
        drop.dragState.start(DraggedBlock.Existing(1, ifBlock), Offset.Zero)
        assertEquals(BlockDropLook.REFUSED, drop.lookOf(10L, 0))
        drop.dragState.cancel()
        shownEventIndex = 0
        dropAt(DraggedBlock.Existing(1, ifBlock), Offset(50f, 60f))
        dropAt(DraggedBlock.Existing(3, BlockDefinition.Else), Offset(50f, 100f))
        dropAt(DraggedBlock.Existing(4, BlockDefinition.End), Offset(50f, 100f))
        assertEquals(listOf<Pair<DraggedBlock, Int>>(DraggedBlock.Existing(1, ifBlock) to 1), dropsInto)
    }

    @Test
    fun aBlockGoesNowhereOverItsOwnEventAnEventThatDoesNotAllowItOrNoRow() {
        dropAt(stop, Offset(50f, 10f))
        shownEventIndex = 1
        dropAt(DraggedBlock.Existing(0, BlockDefinition.RemoveOther), Offset(50f, 100f))
        dropAt(stop, Offset(50f, 300f))
        assertTrue(dropsInto.isEmpty())
        assertTrue(dropsInList.isEmpty())
    }

    @Test
    fun aNewBlockFromThePaletteGoesIntoAnyEventThatAllowsItTheShownOneToo() {
        dropAt(DraggedBlock.New(BlockKind.STOP), Offset(50f, 10f))
        dropAt(DraggedBlock.New(BlockKind.REMOVE_OTHER), Offset(50f, 60f))
        dropAt(DraggedBlock.New(BlockKind.REMOVE_OTHER), Offset(50f, 100f))
        assertEquals(listOf<Pair<DraggedBlock, Int>>(DraggedBlock.New(BlockKind.STOP) to 0, DraggedBlock.New(BlockKind.REMOVE_OTHER) to 1), dropsInto)
    }

    @Test
    fun aForgottenRowTakesNothing() {
        drop.forget(12L)
        dropAt(stop, Offset(50f, 100f))
        assertNull(drop.hoveredKey)
        assertTrue(dropsInto.isEmpty())
    }

    @Test
    fun whatAnEventMakesOfADraggedBlock() {
        val shown = events[0].blocks
        assertEquals(BlockDrop.SOURCE, blockDropOf(stop, 0, shown, 0, events[0], BlockTypeKind.ACTOR))
        assertEquals(BlockDrop.ACCEPTED, blockDropOf(stop, 0, shown, 1, events[1], BlockTypeKind.ACTOR))
        assertEquals(BlockDrop.REFUSED, blockDropOf(stop, 0, shown, 5, null, BlockTypeKind.ACTOR))
        assertEquals(BlockDrop.REFUSED, blockDropOf(DraggedBlock.Existing(9, BlockDefinition.Stop), 0, shown, 1, events[1], BlockTypeKind.ACTOR))
        assertEquals(BlockDrop.REFUSED, blockDropOf(DraggedBlock.New(BlockKind.STOP), null, emptyList(), 0, events[0], BlockTypeKind.MANAGER))
    }
}
