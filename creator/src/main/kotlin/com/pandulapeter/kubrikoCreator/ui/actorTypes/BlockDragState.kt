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

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import com.pandulapeter.kubrikoCreator.actors.BlockDefinition
import com.pandulapeter.kubrikoCreator.actors.BlockKind

/**
 * A Block being dragged in the Actor type editor: a new one from the palette, or one of the selected Event's.
 */
internal sealed interface DraggedBlock {

    val kind: BlockKind

    data class New(
        override val kind: BlockKind,
    ) : DraggedBlock

    data class Existing(
        val index: Int,
        val block: BlockDefinition,
    ) : DraggedBlock {
        override val kind get() = BlockKind.of(block)
    }
}

/**
 * Where a Block is being dragged, in the editor's root coordinates, and where in the selected Event's Blocks it would
 * land. Drag sources report the pointer; the Block list reports where it and its rows are.
 *
 * @param onDrop Called with the Block and the gap it was dropped in (0 is before the first Block).
 */
@Stable
internal class BlockDragState(
    private val onDrop: (DraggedBlock, gap: Int) -> Unit,
    private val clock: () -> Long = System::nanoTime,
) {
    private var lastDragEndedAt = Long.MIN_VALUE / 2
    var dragged by mutableStateOf<DraggedBlock?>(null)
        private set
    var pointer by mutableStateOf(Offset.Zero)
        private set
    var listBounds: Rect? = null

    /**
     * The bounds of each Block row of the selected Event, by index; read only for the first [blockCount] of them.
     */
    val rowBounds = mutableMapOf<Int, Rect>()
    var blockCount = 0

    /**
     * The gap the Block would land in if it were dropped now, or null if the pointer isn't over the Block list.
     */
    val gap: Int?
        get() {
            if (dragged == null || listBounds?.contains(pointer) != true) return null
            return gapAt(pointer.y, (0 until blockCount).mapNotNull { index -> rowBounds[index]?.let { it.top..it.bottom } })
        }

    fun start(block: DraggedBlock, at: Offset) {
        dragged = block
        pointer = at
    }

    fun moveTo(at: Offset) {
        pointer = at
    }

    fun end() {
        val block = dragged ?: return
        val gap = gap
        cancel()
        if (gap != null) onDrop(block, gap)
    }

    fun cancel() {
        dragged = null
        lastDragEndedAt = clock()
    }

    /**
     * Whether a click now is the release of a drag that just ended, which a drag source with a click of its own ignores.
     */
    val isClickAfterDrag get() = clock() - lastDragEndedAt < CLICK_AFTER_DRAG_IN_NANOSECONDS

    private companion object {
        const val CLICK_AFTER_DRAG_IN_NANOSECONDS = 300_000_000L
    }
}

/**
 * The gap among rows spanning [rows] (top to bottom, in order) that a pointer at [y] points at: before the first row
 * whose middle is below it, or after the last one.
 */
internal fun gapAt(y: Float, rows: List<ClosedFloatingPointRange<Float>>) = rows.indexOfFirst { y < (it.start + it.endInclusive) / 2 }.let { if (it < 0) rows.size else it }

/**
 * The index a Block moved from [index] into [gap] ends up at, or null if that leaves it where it is.
 */
internal fun moveTarget(index: Int, gap: Int) = (if (gap > index) gap - 1 else gap).takeIf { it != index }
