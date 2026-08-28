/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.collision

import com.pandulapeter.kubriko.collision.mask.CollisionMask
import com.pandulapeter.kubriko.types.SceneUnit
import kotlin.math.floor

/**
 * A uniform spatial hash grid over a set of [CollisionMask]s: the broad phase behind [CollisionManager]'s
 * detection loop, and a reusable index for game code that runs its own bounded queries.
 *
 * A query costs what its local neighborhood costs rather than what the whole scene does, so many actors
 * can each ask "what is near me?" every frame without any of them scanning the full mask set. Cells are
 * hashed rather than stored densely, so the world can grow without the index growing with it; a hash
 * collision only ever adds candidates, which the per-mask bounds check filters back out.
 *
 * Masks are identified by the index [add] returns, which stays valid until the next [clear]. Nothing but
 * the mask itself is stored, so a consumer that needs to resolve a candidate back to an actor, a payload
 * or a filter flag keeps its own array parallel to those indices.
 *
 * Typical use, with the grid rebuilt once per tick and queried many times:
 * ```kotlin
 * grid.clear()
 * for (collidable in collidables) grid.add(collidable.collisionMask)
 * grid.rebuild()
 * // ...
 * val candidateCount = grid.findCandidates(left, top, right, bottom)
 * val candidates = grid.candidateIndices
 * for (index in 0 until candidateCount) {
 *     val mask = grid.maskAt(candidates[index])
 * }
 * ```
 *
 * Steady-state operation allocates nothing: every buffer only ever grows, and results are written into a
 * reusable array. Not thread-safe, and a query in progress must not be interleaved with another.
 */
class SpatialHashGrid {

    private val masks = mutableListOf<CollisionMask>()
    private var bucketHeads = IntArray(INITIAL_BUCKET_COUNT) { NO_ENTRY }
    private var entryMaskIndices = IntArray(INITIAL_ENTRY_CAPACITY)
    private var entryNextIndices = IntArray(INITIAL_ENTRY_CAPACITY)
    private var entryCount = 0

    // Masks covering so many cells that bucketing them would cost more than testing them against every
    // query (a map-edge slab, an arena-sized trigger volume). Held aside and always offered as candidates.
    private var oversizedMaskIndices = IntArray(INITIAL_OVERSIZED_CAPACITY)
    private var oversizedCount = 0

    // A mask spanning several cells is reachable through each of them; stamping by query keeps it from
    // being reported more than once. The counter only ever increments, so a stamp left over from an
    // earlier rebuild can never match the current query and the array needs clearing only when it grows.
    private var queryStamps = IntArray(INITIAL_ENTRY_CAPACITY)
    private var currentStamp = 0

    private var results = IntArray(INITIAL_ENTRY_CAPACITY)
    private var inverseCellSize = 1f

    /**
     * How many masks this grid holds. Every index in `0 until maskCount` resolves through [maskAt].
     */
    val maskCount get() = masks.size

    /**
     * The indices found by the most recent [findCandidates] call, in its first `candidateCount` slots.
     *
     * A shared buffer that the next query overwrites and may replace outright, so read this property
     * after the [findCandidates] call it belongs to and do not hold on to the array.
     */
    val candidateIndices get() = results

    /**
     * Removes every mask, invalidating all previously returned indices.
     */
    fun clear() {
        masks.clear()
        entryCount = 0
        oversizedCount = 0
    }

    /**
     * Adds [mask] to the grid and returns the index that identifies it until the next [clear].
     *
     * The mask is not bucketed until the next [rebuild].
     *
     * @param mask The shape to index.
     */
    fun add(mask: CollisionMask): Int {
        val index = masks.size
        masks.add(mask)
        return index
    }

    /**
     * Returns the mask [add] gave [index] to.
     *
     * @param index An index returned by [add], or read out of [candidateIndices].
     */
    fun maskAt(index: Int) = masks[index]

    /**
     * Re-buckets every mask by the bounds it currently has, and sizes the cells to the masks in it.
     *
     * Call this after adding masks, and once per tick while the masks move; queries in between read the
     * bounds captured here, not live ones.
     */
    fun rebuild() {
        if (queryStamps.size < masks.size) {
            queryStamps = IntArray(masks.size * 2)
        }
        if (results.size < masks.size) {
            results = IntArray(masks.size * 2)
        }
        refreshCellSize()
        ensureBucketCapacity()
        bucketHeads.fill(NO_ENTRY)
        entryCount = 0
        oversizedCount = 0
        val bucketMask = bucketHeads.size - 1
        for (maskIndex in masks.indices) {
            val bounds = masks[maskIndex].axisAlignedBoundingBox
            val minCellX = cellCoordinate(bounds.minXRaw)
            val maxCellX = cellCoordinate(bounds.maxXRaw)
            val minCellY = cellCoordinate(bounds.minYRaw)
            val maxCellY = cellCoordinate(bounds.maxYRaw)
            val cellCount = (maxCellX.toLong() - minCellX + 1) * (maxCellY.toLong() - minCellY + 1)
            if (cellCount > MAXIMUM_CELLS_PER_MASK) {
                addOversized(maskIndex)
                continue
            }
            for (cellY in minCellY..maxCellY) {
                for (cellX in minCellX..maxCellX) {
                    if (entryCount == entryMaskIndices.size) {
                        entryMaskIndices = entryMaskIndices.copyOf(entryCount * 2)
                        entryNextIndices = entryNextIndices.copyOf(entryCount * 2)
                    }
                    val bucket = bucketIndex(cellX, cellY, bucketMask)
                    entryMaskIndices[entryCount] = maskIndex
                    entryNextIndices[entryCount] = bucketHeads[bucket]
                    bucketHeads[bucket] = entryCount
                    entryCount++
                }
            }
        }
    }

    /**
     * Finds every mask whose bounds overlap the given rectangle, writing their indices into
     * [candidateIndices] and returning how many there are.
     *
     * The result is a broad-phase candidate set filtered to an exact bounds overlap: callers still run
     * their own narrow phase, raycast or type filter against it. The order is unspecified — it follows
     * the cell walk, not the order the masks were added in.
     *
     * @param minX The left edge of the rectangle to search.
     * @param minY The top edge of the rectangle to search.
     * @param maxX The right edge of the rectangle to search.
     * @param maxY The bottom edge of the rectangle to search.
     */
    fun findCandidates(
        minX: SceneUnit,
        minY: SceneUnit,
        maxX: SceneUnit,
        maxY: SceneUnit,
    ): Int {
        val minXRaw = minX.raw
        val minYRaw = minY.raw
        val maxXRaw = maxX.raw
        val maxYRaw = maxY.raw
        currentStamp++
        var count = 0
        for (index in 0 until oversizedCount) {
            val maskIndex = oversizedMaskIndices[index]
            queryStamps[maskIndex] = currentStamp
            if (isOverlapping(maskIndex, minXRaw, minYRaw, maxXRaw, maxYRaw)) {
                results[count] = maskIndex
                count++
            }
        }
        val minCellX = cellCoordinate(minXRaw)
        val maxCellX = cellCoordinate(maxXRaw)
        val minCellY = cellCoordinate(minYRaw)
        val maxCellY = cellCoordinate(maxYRaw)
        // A rectangle covering most of the world touches more cells than the grid holds masks, so walking
        // it would cost more than simply testing every mask - and an unbounded one would not terminate.
        if ((maxCellX.toLong() - minCellX + 1) * (maxCellY.toLong() - minCellY + 1) > MAXIMUM_CELLS_PER_QUERY) {
            for (maskIndex in masks.indices) {
                if (queryStamps[maskIndex] != currentStamp && isOverlapping(maskIndex, minXRaw, minYRaw, maxXRaw, maxYRaw)) {
                    results[count] = maskIndex
                    count++
                }
            }
            return count
        }
        val bucketMask = bucketHeads.size - 1
        for (cellY in minCellY..maxCellY) {
            for (cellX in minCellX..maxCellX) {
                var entry = bucketHeads[bucketIndex(cellX, cellY, bucketMask)]
                while (entry != NO_ENTRY) {
                    val maskIndex = entryMaskIndices[entry]
                    entry = entryNextIndices[entry]
                    if (queryStamps[maskIndex] != currentStamp) {
                        queryStamps[maskIndex] = currentStamp
                        if (isOverlapping(maskIndex, minXRaw, minYRaw, maxXRaw, maxYRaw)) {
                            results[count] = maskIndex
                            count++
                        }
                    }
                }
            }
        }
        return count
    }

    private fun isOverlapping(maskIndex: Int, minX: Float, minY: Float, maxX: Float, maxY: Float): Boolean {
        val bounds = masks[maskIndex].axisAlignedBoundingBox
        return bounds.maxXRaw >= minX && bounds.minXRaw <= maxX && bounds.maxYRaw >= minY && bounds.minYRaw <= maxY
    }

    private fun addOversized(maskIndex: Int) {
        if (oversizedCount == oversizedMaskIndices.size) {
            oversizedMaskIndices = oversizedMaskIndices.copyOf(oversizedCount * 2)
        }
        oversizedMaskIndices[oversizedCount] = maskIndex
        oversizedCount++
    }

    // Cells are sized to the masks rather than to a constant the engine cannot know: a cell a couple of
    // average masks wide keeps a typical mask in a handful of buckets while still resolving a crowd.
    private fun refreshCellSize() {
        if (masks.isEmpty()) return
        var extentSum = 0f
        for (index in masks.indices) {
            val bounds = masks[index].axisAlignedBoundingBox
            extentSum += (bounds.maxXRaw - bounds.minXRaw) + (bounds.maxYRaw - bounds.minYRaw)
        }
        val meanExtent = extentSum / (masks.size * 2)
        inverseCellSize = 1f / (meanExtent * CELLS_PER_MEAN_MASK_EXTENT).coerceAtLeast(MINIMUM_CELL_SIZE)
    }

    private fun ensureBucketCapacity() {
        val requiredBuckets = masks.size * BUCKETS_PER_MASK
        if (bucketHeads.size >= requiredBuckets) return
        var newSize = bucketHeads.size
        while (newSize < requiredBuckets) {
            newSize *= 2
        }
        bucketHeads = IntArray(newSize)
    }

    private fun cellCoordinate(value: Float) = floor(value * inverseCellSize).toInt()

    private companion object {
        const val NO_ENTRY = -1
        const val INITIAL_BUCKET_COUNT = 1024
        const val INITIAL_ENTRY_CAPACITY = 256
        const val INITIAL_OVERSIZED_CAPACITY = 16
        const val BUCKETS_PER_MASK = 4
        const val CELLS_PER_MEAN_MASK_EXTENT = 2f
        const val MINIMUM_CELL_SIZE = 1f
        const val MAXIMUM_CELLS_PER_MASK = 64
        const val MAXIMUM_CELLS_PER_QUERY = 4096

        // Large odd primes spread neighboring cells across the pow2-masked bucket range.
        const val HASH_X = 92837111
        const val HASH_Y = 689287499

        fun bucketIndex(cellX: Int, cellY: Int, bucketMask: Int) = ((cellX * HASH_X) xor (cellY * HASH_Y)) and bucketMask
    }
}
