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

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import com.pandulapeter.kubrikoCreator.actors.BlockDefinition
import com.pandulapeter.kubrikoCreator.actors.BlockKind
import com.pandulapeter.kubrikoCreator.actors.BlockNesting
import com.pandulapeter.kubrikoCreator.actors.BlockTypeKind
import com.pandulapeter.kubrikoCreator.actors.EventDefinition

/**
 * The Blocks dragged in the type editor ([dragState]): dropped into the shown Event's list, or onto a row of the Event
 * list, which puts the Block (an if or repeat Block with its section) at the end of that Event. It knows where those rows
 * are, by their Events' keys, and which Event a Block just went into, which its row lights up for.
 *
 * @param events The type's Events as they are now.
 * @param shownEventIndex The Event whose Blocks are shown, which those Blocks are dragged from.
 * @param eventIndexOf The index of the Event of a key now, or null if it is gone.
 * @param onDrop Called with a Block dropped into the shown Event's list and the gap it was dropped in.
 * @param onDropInto Called with a Block dropped onto another Event's row that can take it, that Event's index, and what to
 *   call once the change is made.
 */
@Stable
internal class BlockEventDrop(
    private val typeKind: () -> BlockTypeKind,
    private val events: () -> List<EventDefinition>,
    private val shownEventIndex: () -> Int?,
    private val eventIndexOf: (Long) -> Int?,
    onDrop: (DraggedBlock, gap: Int) -> Unit,
    private val onDropInto: (DraggedBlock, eventIndex: Int, onSuccess: () -> Unit) -> Unit,
) {
    private val rows = mutableStateMapOf<Long, Rect>()

    val dragState = BlockDragState(onDrop = onDrop, onDropOutside = ::dropOutside)

    /**
     * The key of the Event the pointer is over while a Block is dragged, or null.
     */
    val hoveredKey by derivedStateOf {
        val pointer = dragState.pointer
        if (dragState.dragged == null) null else keyAt(pointer)
    }

    /**
     * A new value whenever a Block went into an Event, naming it by its key.
     */
    var dropped by mutableStateOf<DroppedInto?>(null)
        private set

    fun place(key: Long, bounds: Rect) {
        if (rows[key] != bounds) rows[key] = bounds
    }

    fun forget(key: Long) {
        rows.remove(key)
    }

    /**
     * How the row of the Event at [index], keyed [key], shows the Block being dragged.
     */
    fun lookOf(key: Long, index: Int): BlockDropLook = when (dropOf(dragState.dragged ?: return BlockDropLook.NONE, index)) {
        BlockDrop.SOURCE -> BlockDropLook.NONE
        BlockDrop.REFUSED -> BlockDropLook.REFUSED
        BlockDrop.ACCEPTED -> if (hoveredKey == key) BlockDropLook.HOVERED else BlockDropLook.NONE
    }

    private fun keyAt(at: Offset) = rows.entries.firstOrNull { it.value.contains(at) }?.key

    private fun dropOf(dragged: DraggedBlock, index: Int): BlockDrop {
        val events = events()
        val shownIndex = shownEventIndex()
        return blockDropOf(dragged, shownIndex, shownIndex?.let(events::getOrNull)?.blocks.orEmpty(), index, events.getOrNull(index), typeKind())
    }

    private fun dropOutside(block: DraggedBlock, at: Offset) {
        val key = keyAt(at) ?: return
        val index = eventIndexOf(key) ?: return
        if (dropOf(block, index) == BlockDrop.ACCEPTED) onDropInto(block, index) { dropped = DroppedInto(key) }
    }

    /**
     * The Event a Block went into; a class rather than a data class, so the same Event lights up again for the next one.
     */
    class DroppedInto(val key: Long)
}

/**
 * How an Event's row shows a Block being dragged: as where it would go, as an Event that can't take it, or as usual.
 */
internal enum class BlockDropLook {
    NONE,
    HOVERED,
    REFUSED,
}

/**
 * What an Event makes of a Block dropped on its row: the Event it comes from, one it can't go into, or one it can.
 */
internal enum class BlockDrop {
    SOURCE,
    REFUSED,
    ACCEPTED,
}

/**
 * What the Event at [index] ([event], null if there is none) of a type of [typeKind] makes of [dragged] while the Event at
 * [shownIndex], holding [shownBlocks], is shown: a Block of the shown Event goes only into another one, an else or end Block
 * nowhere, and only into an Event that allows it and every Block of the section it takes along.
 */
internal fun blockDropOf(dragged: DraggedBlock, shownIndex: Int?, shownBlocks: List<BlockDefinition>, index: Int, event: EventDefinition?, typeKind: BlockTypeKind): BlockDrop {
    if (dragged is DraggedBlock.Existing && index == shownIndex) return BlockDrop.SOURCE
    if (event == null) return BlockDrop.REFUSED
    val kinds = when (dragged) {
        is DraggedBlock.New -> listOf(dragged.kind)
        is DraggedBlock.Existing -> dragged.index.takeIf { it in shownBlocks.indices }
            ?.let { BlockNesting.movingBetweenEvents(shownBlocks, it) }
            ?.map { BlockKind.of(shownBlocks[it]) }
            ?: return BlockDrop.REFUSED
    }
    return if (kinds.all { it.isAvailableIn(event.type, typeKind) }) BlockDrop.ACCEPTED else BlockDrop.REFUSED
}

/**
 * Makes the row of the Event keyed [key] a place [drop]'s Blocks can be dropped on.
 */
@Composable
internal fun Modifier.blockDropRow(drop: BlockEventDrop?, key: Long): Modifier {
    if (drop == null) return this
    DisposableEffect(drop, key) { onDispose { drop.forget(key) } }
    return onGloballyPositioned { drop.place(key, it.boundsInRoot()) }
}
