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

/**
 * The rows of a lazy list that are laid out (only those on screen are), as their spans in root coordinates, top to
 * bottom, and the index of the first of them.
 */
internal data class LaidOutRows(
    val firstIndex: Int,
    val spans: List<ClosedFloatingPointRange<Float>>,
) {
    companion object {
        val NONE = LaidOutRows(0, emptyList())
    }
}

/**
 * Where an item ([T]: a Block, or the index of an Event) is being dragged, in the editor's root coordinates, and where
 * in a list it would land. Drag sources ([listDragSource]) report the pointer; the list ([lazyListDropTarget]) reports
 * where it is and which of its rows are laid out, and scrolls by [autoScrollStep] while the pointer is near its edges,
 * so that every gap can be reached.
 *
 * @param onDrop Called with the item and the gap it was dropped in (0 is before the first row).
 */
@Stable
internal class ListDragState<T : Any>(
    private val onDrop: (T, gap: Int) -> Unit,
    private val clock: () -> Long = System::nanoTime,
) {
    private var lastDragEndedAt = Long.MIN_VALUE / 2
    var dragged by mutableStateOf<T?>(null)
        private set
    var pointer by mutableStateOf(Offset.Zero)
        private set
    var listBounds: Rect? = null
    var laidOutRows: () -> LaidOutRows = { LaidOutRows.NONE }

    /**
     * The gap the item would land in if it were dropped now, or null if the pointer isn't over the list.
     */
    val gap: Int?
        get() {
            if (dragged == null || listBounds?.contains(pointer) != true) return null
            val rows = laidOutRows()
            return rows.firstIndex + gapAt(pointer.y, rows.spans)
        }

    /**
     * How far the list should scroll on this frame: towards the edge the pointer is within [edge] of, faster the
     * closer it is, up to [maximumStep]. Zero while nothing is dragged over the list.
     */
    fun autoScrollStep(edge: Float, maximumStep: Float): Float {
        val bounds = listBounds ?: return 0f
        if (dragged == null || !bounds.contains(pointer)) return 0f
        return when {
            pointer.y < bounds.top + edge -> -maximumStep * (bounds.top + edge - pointer.y) / edge
            pointer.y > bounds.bottom - edge -> maximumStep * (pointer.y - bounds.bottom + edge) / edge
            else -> 0f
        }
    }

    fun start(item: T, at: Offset) {
        dragged = item
        pointer = at
    }

    fun moveTo(at: Offset) {
        pointer = at
    }

    fun end() {
        val item = dragged ?: return
        val gap = gap
        cancel()
        if (gap != null) onDrop(item, gap)
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
 * The index a row moved from [index] into [gap] ends up at, or null if that leaves it where it is.
 */
internal fun moveTarget(index: Int, gap: Int) = (if (gap > index) gap - 1 else gap).takeIf { it != index }
