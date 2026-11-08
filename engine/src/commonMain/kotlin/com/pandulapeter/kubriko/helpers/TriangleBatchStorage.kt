/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.helpers

/**
 * Brings the written run up to [end] with the default coordinates. When both defaults are equal the gap - which is
 * most of the batch - closes with a single fill rather than a strided walk.
 */
internal fun TriangleBatch.padTexCoordsTo(end: Int) {
    if (texCoords.size < vertexCount * 2) texCoords = texCoords.copyOf(maxOf(vertexCount, colors.size) * 2)
    if (texCoordsWritten >= end) return
    if (defaultU == defaultV) {
        texCoords.fill(defaultU, texCoordsWritten * 2, end * 2)
    } else {
        var i = texCoordsWritten * 2
        val stop = end * 2
        while (i < stop) {
            texCoords[i] = defaultU
            texCoords[i + 1] = defaultV
            i += 2
        }
    }
    texCoordsWritten = end
}

internal fun TriangleBatch.writeQuadIndices(base: Int) {
    val indices = indices
    var i = indexCount
    indices[i++] = base.toShort(); indices[i++] = (base + 1).toShort(); indices[i++] = (base + 2).toShort()
    indices[i++] = base.toShort(); indices[i++] = (base + 2).toShort(); indices[i] = (base + 3).toShort()
}

internal fun TriangleBatch.ensureVertexCapacity(requiredVertexCount: Int) {
    check(requiredVertexCount <= TriangleBatch.MAX_INDEXED_VERTICES) {
        "TriangleBatch cannot hold more than ${TriangleBatch.MAX_INDEXED_VERTICES} unique vertices between flushes " +
            "(index range overflow) — the batch owner must flush more often."
    }
    if (colors.size < requiredVertexCount) {
        val newSize = maxOf(requiredVertexCount, colors.size * 2)
        positions = positions.copyOf(newSize * 2)
        colors = colors.copyOf(newSize)
    }
}

internal fun TriangleBatch.ensureIndexCapacity(requiredIndexCount: Int) {
    if (indices.size < requiredIndexCount) {
        indices = indices.copyOf(maxOf(requiredIndexCount, indices.size * 2))
    }
}

/**
 * Appends everything [source] holds after what this batch holds, its indices shifted to match. Its texture
 * coordinates come along when it wrote any (padded with its own defaults); otherwise the appended vertices are left
 * for this batch's defaults, like any untextured geometry. Allocates only while the buffers grow.
 */
internal fun TriangleBatch.appendFrom(source: TriangleBatch) {
    val count = source.vertexCount
    if (count == 0) return
    val base = vertexCount
    ensureVertexCapacity(base + count)
    ensureIndexCapacity(indexCount + source.indexCount)
    source.positions.copyInto(positions, base * 2, 0, count * 2)
    source.colors.copyInto(colors, base, 0, count)
    val sourceIndices = source.indices
    val indices = indices
    var i = indexCount
    for (k in 0 until source.indexCount) {
        // Indices past 32767 are stored as negative shorts; the mask reads them back unsigned.
        indices[i++] = ((sourceIndices[k].toInt() and 0xFFFF) + base).toShort()
    }
    indexCount = i
    vertexCount = base + count
    if (source.texCoordsWritten > 0) {
        source.padTexCoordsTo(count)
        padTexCoordsTo(base)
        source.texCoords.copyInto(texCoords, base * 2, 0, count * 2)
        texCoordsWritten = vertexCount
    }
}

/**
 * Applies the affine transform `x' = a·x + c·y + tx`, `y' = b·x + d·y + ty` to every vertex from [firstVertex] on.
 */
internal fun TriangleBatch.transformVertices(firstVertex: Int, a: Float, b: Float, c: Float, d: Float, tx: Float, ty: Float) {
    val positions = positions
    var p = firstVertex * 2
    val end = vertexCount * 2
    while (p < end) {
        val x = positions[p]
        val y = positions[p + 1]
        positions[p] = a * x + c * y + tx
        positions[p + 1] = b * x + d * y + ty
        p += 2
    }
}
