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

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * [TriangleMeshBuffers] trims a batch's oversized buffers into the arrays Skia derives its counts from. What reaches
 * Skia has to draw exactly the batch's triangles: the real data unchanged, and padding that rasterizes nothing and
 * does not widen the mesh's bounds.
 */
class TriangleMeshBuffersTest {

    private val vertexCount = 5
    private val indexCount = 9
    private val positions = FloatArray(4096) { if (it < vertexCount * 2) it + 1f else -999f }
    private val colors = IntArray(2048) { if (it < vertexCount) it + 1 else -999 }
    private val indices = ShortArray(3072) { if (it < indexCount) (it % vertexCount).toShort() else 99 }
    private val texCoords = FloatArray(4096) { if (it < vertexCount * 2) 100f + it else -999f }

    private fun prepare(texCoords: FloatArray? = null) = TriangleMeshBuffers.prepare(
        positions = positions,
        colors = colors,
        indices = indices,
        vertexCount = vertexCount,
        indexCount = indexCount,
        texCoords = texCoords,
    )

    @Test
    fun realVerticesAndIndicesReachSkiaUnchanged() {
        prepare()

        for (i in 0 until vertexCount * 2) assertEquals(positions[i], TriangleMeshBuffers.positions[i], "position $i")
        for (i in 0 until vertexCount) assertEquals(colors[i], TriangleMeshBuffers.colors[i], "color $i")
        for (i in 0 until indexCount) assertEquals(indices[i], TriangleMeshBuffers.indices[i], "index $i")
    }

    @Test
    fun everyVertexHasOneColorAndTwoCoordinates() {
        prepare()

        assertEquals(TriangleMeshBuffers.colors.size * 2, TriangleMeshBuffers.positions.size)
        assertTrue(TriangleMeshBuffers.colors.size >= vertexCount)
    }

    @Test
    fun paddingVerticesRepeatTheLastRealOne() {
        prepare()

        val lastX = positions[vertexCount * 2 - 2]
        val lastY = positions[vertexCount * 2 - 1]
        var p = vertexCount * 2
        while (p < TriangleMeshBuffers.positions.size) {
            assertEquals(lastX, TriangleMeshBuffers.positions[p++], "padding x")
            assertEquals(lastY, TriangleMeshBuffers.positions[p++], "padding y")
        }
    }

    @Test
    fun paddingIndicesFormOnlyDegenerateTriangles() {
        prepare()

        val preparedIndices = TriangleMeshBuffers.indices
        assertEquals(0, preparedIndices.size % 3)
        for (i in indexCount until preparedIndices.size step 3) {
            assertEquals(preparedIndices[i], preparedIndices[i + 1], "triangle at $i")
            assertEquals(preparedIndices[i], preparedIndices[i + 2], "triangle at $i")
            assertTrue(preparedIndices[i] < vertexCount, "triangle at $i indexes past the real vertices")
        }
    }

    @Test
    fun textureCoordinatesAreTrimmedAlongsideThePositions() {
        prepare(texCoords)

        val preparedTexCoords = assertNotNull(TriangleMeshBuffers.texCoords)
        assertEquals(TriangleMeshBuffers.positions.size, preparedTexCoords.size)
        for (i in 0 until vertexCount * 2) assertEquals(texCoords[i], preparedTexCoords[i], "texture coordinate $i")
    }

    @Test
    fun aColorOnlyMeshHasNoTextureCoordinates() {
        prepare(texCoords)

        prepare()

        assertNull(TriangleMeshBuffers.texCoords)
    }

    @Test
    fun exactlySizedBuffersReachSkiaUnchanged() {
        val exactPositions = floatArrayOf(0f, 0f, 1f, 0f, 0f, 1f)
        val exactColors = intArrayOf(1, 2, 3)
        val exactIndices = shortArrayOf(0, 1, 2)

        TriangleMeshBuffers.prepare(exactPositions, exactColors, exactIndices, vertexCount = 3, indexCount = 3, texCoords = null)

        assertTrue(exactPositions.contentEquals(TriangleMeshBuffers.positions))
        assertTrue(exactColors.contentEquals(TriangleMeshBuffers.colors))
        assertTrue(exactIndices.contentEquals(TriangleMeshBuffers.indices))
    }
}
