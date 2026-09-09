/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.implementation

import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.ImageBitmap
import com.pandulapeter.kubriko.helpers.TriangleBatch

/**
 * Rasterizes the first [vertexCount] unique vertices and [indexCount] indices in a single native draw call;
 * every three consecutive indices form one triangle.
 *
 * The position/color arrays may be larger than needed — implementations that cannot pass a count (Skia through
 * Skiko) trim through [TriangleMeshBuffers], while Android draws straight from the given arrays so the
 * per-frame hot path never copies or allocates. [isAntiAlias] toggles the paint's anti-aliasing (edge smoothing
 * beyond what [TriangleBatch.addLine]'s geometric fringes provide). With [replace] the triangles use
 * source-replace compositing instead of the default source-over. [texCoords] and [texture] are either both
 * present — the vertex colors are then modulated by the pattern sampled at each vertex's own coordinate, in
 * texels — or both absent, which is the plain color-only path.
 */
internal expect fun drawTriangles(
    canvas: Canvas,
    positions: FloatArray,
    colors: IntArray,
    indices: ShortArray,
    vertexCount: Int,
    indexCount: Int,
    texCoords: FloatArray? = null,
    texture: ImageBitmap? = null,
    replace: Boolean = false,
    isAntiAlias: Boolean = false,
)

/**
 * The trimmed arrays Skia is handed, since it derives the vertex and index counts from the array sizes an
 * oversized batch buffer cannot express. Filled by [prepare] and read straight afterwards, so nothing is
 * allocated per draw once the buckets a scene visits exist.
 *
 * Mirrors are kept one per size bucket and never evicted: a batch's count changes on essentially every frame,
 * and buckets stepping geometrically hold that continuum to a couple of dozen sizes per kind. A pool bounded in
 * slots and matched by exact length thrashes instead — one frame flushing more sizes than it has slots evicts
 * the very size the next flush needs and rebuilds hundreds of kilobytes mid-frame. The padding a bucket leaves
 * is unindexed vertices and whole degenerate triangles, which draw nothing.
 */
internal object TriangleMeshBuffers {

    lateinit var positions: FloatArray
        private set
    lateinit var colors: IntArray
        private set
    lateinit var indices: ShortArray
        private set
    var texCoords: FloatArray? = null
        private set

    private val bucketPositions = arrayOfNulls<FloatArray>(BUCKET_COUNT)
    private val bucketColors = arrayOfNulls<IntArray>(BUCKET_COUNT)
    private val bucketTexCoords = arrayOfNulls<FloatArray>(BUCKET_COUNT)
    private val bucketIndices = arrayOfNulls<ShortArray>(BUCKET_COUNT)

    fun prepare(
        positions: FloatArray,
        colors: IntArray,
        indices: ShortArray,
        vertexCount: Int,
        indexCount: Int,
        texCoords: FloatArray?,
    ) {
        val positionCount = vertexCount * 2
        if (positions.size == positionCount) {
            this.positions = positions
            this.colors = colors
        } else {
            val bucket = bucketIndexFor(vertexCount, MINIMUM_BUCKET_VERTEX_COUNT, MAXIMUM_BUCKET_VERTEX_COUNT)
            val paddedVertexCount = bucketCapacity(bucket, MINIMUM_BUCKET_VERTEX_COUNT, MAXIMUM_BUCKET_VERTEX_COUNT)
            val paddedPositionCount = paddedVertexCount * 2
            val paddedPositions = bucketPositions[bucket] ?: FloatArray(paddedPositionCount).also { bucketPositions[bucket] = it }
            val paddedColors = bucketColors[bucket] ?: IntArray(paddedVertexCount).also { bucketColors[bucket] = it }
            positions.copyInto(paddedPositions, 0, 0, positionCount)
            colors.copyInto(paddedColors, 0, 0, vertexCount)
            // The padding vertices are never indexed, so they rasterize nothing - but Skia takes the mesh's
            // bounds from every position it is handed, so they repeat the last real one rather than sitting
            // at the origin, which would stretch the bounds across the whole scene and defeat quick reject.
            val lastX = paddedPositions[positionCount - 2]
            val lastY = paddedPositions[positionCount - 1]
            var p = positionCount
            while (p < paddedPositionCount) {
                paddedPositions[p++] = lastX
                paddedPositions[p++] = lastY
            }
            this.positions = paddedPositions
            this.colors = paddedColors
        }
        if (indices.size == indexCount) {
            this.indices = indices
        } else {
            val bucket = bucketIndexFor(indexCount, MINIMUM_BUCKET_INDEX_COUNT, Int.MAX_VALUE)
            val paddedIndexCount = bucketCapacity(bucket, MINIMUM_BUCKET_INDEX_COUNT, Int.MAX_VALUE)
            val paddedIndices = bucketIndices[bucket] ?: ShortArray(paddedIndexCount).also { bucketIndices[bucket] = it }
            indices.copyInto(paddedIndices, 0, 0, indexCount)
            // Whole triangles of vertex 0: zero area, so the padding draws nothing.
            paddedIndices.fill(0, indexCount, paddedIndexCount)
            this.indices = paddedIndices
        }
        this.texCoords = if (texCoords == null) null else trimmedTexCoords(texCoords, positionCount, this.positions.size)
    }

    // Skia sizes the mesh from the arrays it is handed, so the coordinates have to be trimmed to match the
    // positions exactly as those were; the padding repeats the last real one, which is never indexed.
    private fun trimmedTexCoords(texCoords: FloatArray, positionCount: Int, paddedPositionCount: Int): FloatArray {
        if (texCoords.size == paddedPositionCount) return texCoords
        // The positions were padded to a bucket unless the batch happened to be exactly full, in which case the
        // coordinates have to match that one-off length instead.
        val bucket = bucketIndexFor(paddedPositionCount / 2, MINIMUM_BUCKET_VERTEX_COUNT, MAXIMUM_BUCKET_VERTEX_COUNT)
        val trimmed = if (bucketCapacity(bucket, MINIMUM_BUCKET_VERTEX_COUNT, MAXIMUM_BUCKET_VERTEX_COUNT) * 2 != paddedPositionCount) {
            FloatArray(paddedPositionCount)
        } else {
            bucketTexCoords[bucket] ?: FloatArray(paddedPositionCount).also { bucketTexCoords[bucket] = it }
        }
        texCoords.copyInto(trimmed, 0, 0, minOf(positionCount, texCoords.size))
        trimmed.fill(trimmed[positionCount - 2], positionCount, paddedPositionCount)
        return trimmed
    }

    private fun bucketIndexFor(count: Int, minimum: Int, maximum: Int): Int {
        var capacity = minimum
        var bucket = 0
        while (capacity < count) {
            capacity = nextBucketCapacity(capacity, maximum)
            bucket++
        }
        return bucket
    }

    private fun bucketCapacity(bucket: Int, minimum: Int, maximum: Int): Int {
        var capacity = minimum
        repeat(bucket) { capacity = nextBucketCapacity(capacity, maximum) }
        return capacity
    }

    // Buckets grow by a quarter per step, so the padding tops out at a quarter of the count it holds and the
    // whole ladder stays a small multiple of the largest bucket in memory. Kept a multiple of three, so an
    // index bucket's padding is whole triangles.
    private fun nextBucketCapacity(capacity: Int, maximum: Int) = minOf((capacity + capacity / 4 + 2) / 3 * 3, maximum)

    private const val BUCKET_COUNT = 32
    private const val MINIMUM_BUCKET_VERTEX_COUNT = 1024
    private const val MINIMUM_BUCKET_INDEX_COUNT = 1536

    // The vertex ladder ends exactly at what a 16-bit index can address; a batch never holds more than that
    // anyway. @see TriangleBatch.MAX_INDEXED_VERTICES
    private const val MAXIMUM_BUCKET_VERTEX_COUNT = TriangleBatch.MAX_INDEXED_VERTICES
}
