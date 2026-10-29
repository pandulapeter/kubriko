/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.scenes

import com.pandulapeter.kubrikoCreator.scenes.SceneHitTest.isDrawnAbove
import kotlin.math.floor

/**
 * The bounds of a Scene's Actors in a uniform grid, so that picking the Actor under a click looks at the Actors near it
 * instead of every Actor in the Scene. Bounds are kept by their [SceneHitTest.Bounds.index]. Backgrounds, and Actors
 * covering more than [MAXIMUM_CELLS_PER_ACTOR] cells, are kept apart and checked on every pick.
 */
internal class SceneBoundsGrid(private val cellSize: Float = DEFAULT_CELL_SIZE) {
    private var bounds = arrayOfNulls<SceneHitTest.Bounds>(0)
    private val cells = HashMap<Long, IndexBucket>()
    private val unbucketed = IndexBucket()

    /**
     * Every bounds in the grid, in index order.
     */
    fun all() = bounds.filterNotNull()

    /**
     * Replaces everything in the grid with [all], for a Scene of [size] Actors.
     */
    fun rebuild(all: Collection<SceneHitTest.Bounds>, size: Int = (all.maxOfOrNull { it.index } ?: -1) + 1) {
        bounds = arrayOfNulls(size)
        cells.clear()
        unbucketed.clear()
        all.forEach(::insert)
    }

    /**
     * Replaces the bounds at [index] with [newBounds], or removes them if null.
     */
    fun update(index: Int, newBounds: SceneHitTest.Bounds?) {
        bounds[index]?.let(::remove)
        bounds[index] = null
        newBounds?.let(::insert)
    }

    /**
     * The index of the Actor drawn on top at [x], [y], or null if there is none: the lowest drawing order wins, and
     * of equal ones the Actor added later, which Kubriko draws later. Backgrounds are only picked when nothing else
     * is there.
     */
    fun pick(x: Float, y: Float): Int? {
        var picked: SceneHitTest.Bounds? = null
        fun consider(bucket: IndexBucket) {
            for (position in 0 until bucket.size) {
                val candidate = bounds[bucket[position]] ?: continue
                if (!candidate.contains(x, y)) continue
                val current = picked
                if (current == null || candidate.isDrawnAbove(current)) picked = candidate
            }
        }
        cells[key(cellOf(x), cellOf(y))]?.let(::consider)
        consider(unbucketed)
        return picked?.index
    }

    private fun insert(newBounds: SceneHitTest.Bounds) {
        bounds[newBounds.index] = newBounds
        forEachCell(newBounds, onTooMany = { unbucketed.add(newBounds.index) }) { cell -> cells.getOrPut(cell) { IndexBucket() }.add(newBounds.index) }
    }

    private fun remove(oldBounds: SceneHitTest.Bounds) {
        forEachCell(oldBounds, onTooMany = { unbucketed.remove(oldBounds.index) }) { cell ->
            val bucket = cells[cell] ?: return@forEachCell
            bucket.remove(oldBounds.index)
            if (bucket.size == 0) cells.remove(cell)
        }
    }

    private inline fun forEachCell(bounds: SceneHitTest.Bounds, onTooMany: () -> Unit, action: (Long) -> Unit) {
        val left = cellOf(bounds.left)
        val top = cellOf(bounds.top)
        val right = cellOf(bounds.right)
        val bottom = cellOf(bounds.bottom)
        if (bounds.isBackground || (right.toLong() - left + 1) * (bottom.toLong() - top + 1) > MAXIMUM_CELLS_PER_ACTOR) {
            onTooMany()
        } else {
            for (column in left..right) for (row in top..bottom) action(key(column, row))
        }
    }

    private fun cellOf(coordinate: Float) = floor(coordinate / cellSize).toInt()

    private fun key(column: Int, row: Int) = (column.toLong() shl 32) or (row.toLong() and 0xFFFFFFFFL)

    /**
     * A growable list of indices without boxing, in no particular order.
     */
    private class IndexBucket {
        private var indices = IntArray(4)
        var size = 0
            private set

        operator fun get(position: Int) = indices[position]

        fun add(index: Int) {
            if (size == indices.size) indices = indices.copyOf(size * 2)
            indices[size++] = index
        }

        fun remove(index: Int) {
            for (position in 0 until size) {
                if (indices[position] == index) {
                    indices[position] = indices[--size]
                    return
                }
            }
        }

        fun clear() {
            size = 0
        }
    }

    companion object {
        const val DEFAULT_CELL_SIZE = 128f
        const val MAXIMUM_CELLS_PER_ACTOR = 64L
    }
}
