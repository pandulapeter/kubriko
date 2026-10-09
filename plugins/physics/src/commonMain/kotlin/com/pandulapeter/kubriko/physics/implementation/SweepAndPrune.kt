/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.physics.implementation

import com.pandulapeter.kubriko.helpers.extensions.isOverlapping
import com.pandulapeter.kubriko.physics.PhysicsBody

/**
 * Sweep-and-prune broad phase: bodies are kept sorted by the left edge of their bounding box,
 * so each body only needs to be tested against the neighbors whose x-extents can still overlap
 * instead of every other body. Candidate pairs are re-sorted into the (i, j) order of a nested
 * loop, which keeps the solver's arbiter order and so the results deterministic — do not remove
 * the pair sort.
 */
internal class SweepAndPrune {
    private var sweepBodiesSnapshot: List<PhysicsBody>? = null
    private var sweepSortedIndices = IntArray(0)
    private var sweepMinX = FloatArray(0)
    private var sweepMaxX = FloatArray(0)
    private var sweepPairs = LongArray(0)

    /**
     * Finds the overlapping pairs of [bodies] and returns their count; read them with [firstBodyIndexAt] and
     * [secondBodyIndexAt], in the order a nested `(i, j)` loop would produce them.
     */
    fun findPairs(bodies: List<PhysicsBody>): Int {
        val bodyCount = bodies.size
        if (bodyCount < 2) {
            return 0
        }
        if (bodies !== sweepBodiesSnapshot) {
            sweepBodiesSnapshot = bodies
            if (sweepSortedIndices.size < bodyCount) {
                sweepSortedIndices = IntArray(bodyCount)
                sweepMinX = FloatArray(bodyCount)
                sweepMaxX = FloatArray(bodyCount)
            }
            // The previous permutation may not cover 0 until bodyCount anymore; start from identity.
            for (i in 0 until bodyCount) {
                sweepSortedIndices[i] = i
            }
        }
        // One bounding box read per body (the pair loop below would otherwise re-read them O(n²) times).
        for (i in 0 until bodyCount) {
            val aabb = bodies[i].collisionMask.axisAlignedBoundingBox
            val left = aabb.left.raw
            // The sort cannot order NaN, so such a body (which overlaps nothing) goes last instead of splitting it.
            sweepMinX[i] = if (left.isNaN()) Float.POSITIVE_INFINITY else left
            sweepMaxX[i] = aabb.right.raw
        }
        // Insertion sort by minX; nearly sorted from the previous frame.
        val sorted = sweepSortedIndices
        for (k in 1 until bodyCount) {
            val index = sorted[k]
            val key = sweepMinX[index]
            var m = k - 1
            while (m >= 0 && sweepMinX[sorted[m]] > key) {
                sorted[m + 1] = sorted[m]
                m--
            }
            sorted[m + 1] = index
        }
        var pairCount = 0
        for (a in 0 until bodyCount) {
            val i = sorted[a]
            val bodyA = bodies[i]
            val maxXa = sweepMaxX[i]
            for (b in a + 1 until bodyCount) {
                val j = sorted[b]
                // isOverlapping treats touching edges as non-overlapping, so >= prunes exactly the
                // pairs it would reject on the x axis — and every later index sorts even further right.
                if (sweepMinX[j] >= maxXa) {
                    break
                }
                val bodyB = bodies[j]
                if (bodyA.invMass == 0f && bodyB.invMass == 0f || bodyA.isParticle && bodyB.isParticle) {
                    continue
                }
                if (bodyA.collisionMask.axisAlignedBoundingBox.isOverlapping(bodyB.collisionMask.axisAlignedBoundingBox)) {
                    if (pairCount == sweepPairs.size) {
                        sweepPairs = sweepPairs.copyOf(maxOf(16, sweepPairs.size * 2))
                    }
                    sweepPairs[pairCount++] = if (i < j) {
                        (i.toLong() shl 32) or j.toLong()
                    } else {
                        (j.toLong() shl 32) or i.toLong()
                    }
                }
            }
        }
        // Restore the original deterministic pair order before running the narrow phase.
        sweepPairs.sort(fromIndex = 0, toIndex = pairCount)
        return pairCount
    }

    fun firstBodyIndexAt(pairIndex: Int): Int = (sweepPairs[pairIndex] ushr 32).toInt()

    fun secondBodyIndexAt(pairIndex: Int): Int = (sweepPairs[pairIndex] and 0xFFFFFFFFL).toInt()
}
