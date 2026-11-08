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

import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.TileMode
import com.pandulapeter.kubriko.implementation.samplingKey
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals

class TriangleBatchStorageTest {

    @Test
    fun `appending a batch shifts its indices past the vertices already held`() {
        val target = TriangleBatch().apply { addTriangle(0f, 0f, 1f, 0f, 0f, 1f, argb = 1) }
        val source = TriangleBatch().apply { addQuad(0f, 0f, 2f, 0f, 2f, 2f, 0f, 2f, argb = 2) }

        target.appendFrom(source)

        assertEquals(7, target.vertexCount)
        assertEquals(9, target.indexCount)
        assertContentEquals(shortArrayOf(0, 1, 2, 3, 4, 5, 3, 5, 6), target.indices.copyOf(9))
        assertContentEquals(intArrayOf(1, 1, 1, 2, 2, 2, 2), target.colors.copyOf(7))
        assertContentEquals(floatArrayOf(2f, 0f, 2f, 2f), target.positions.copyOfRange(8, 12))
    }

    @Test
    fun `appending past half the index range keeps indices unsigned`() {
        val target = TriangleBatch()
        repeat(20_000) { target.addVertex(0f, 0f, 0) }
        val source = TriangleBatch().apply {
            repeat(33_000) { addVertex(0f, 0f, 0) }
            addTriangle(32_999, 32_998, 0)
        }

        target.appendFrom(source)

        val appended = target.indices.copyOf(target.indexCount).map { it.toInt() and 0xFFFF }
        assertEquals(listOf(52_999, 52_998, 20_000), appended)
    }

    @Test
    fun `texture coordinates come along and fill the gap before them with the defaults`() {
        val target = TriangleBatch().apply {
            defaultU = 7f
            defaultV = 8f
            addVertex(0f, 0f, 0)
        }
        val source = TriangleBatch().apply { addTexturedVertex(1f, 1f, 0, u = 3f, v = 4f) }

        target.appendFrom(source)

        assertEquals(2, target.texCoordsWritten)
        assertContentEquals(floatArrayOf(7f, 8f, 3f, 4f), target.texCoords.copyOf(4))
    }

    @Test
    fun `an untextured source leaves the appended vertices to the target's defaults`() {
        val target = TriangleBatch()
        target.appendFrom(TriangleBatch().apply { addVertex(1f, 1f, 0) })

        assertEquals(0, target.texCoordsWritten)
    }

    @Test
    fun `vertices are transformed from the given one on`() {
        val batch = TriangleBatch().apply {
            addVertex(1f, 2f, 0)
            addVertex(1f, 2f, 0)
        }

        batch.transformVertices(firstVertex = 1, a = 0f, b = 1f, c = -1f, d = 0f, tx = 10f, ty = 20f)

        assertContentEquals(floatArrayOf(1f, 2f, 8f, 21f), batch.positions.copyOf(4))
    }

    @Test
    fun `every sampling combination has a key of its own`() {
        val tileModes = listOf(TileMode.Clamp, TileMode.Repeated, TileMode.Mirror, TileMode.Decal)
        val filterQualities = listOf(FilterQuality.None, FilterQuality.Low, FilterQuality.Medium, FilterQuality.High)

        val keys = tileModes.flatMap { tileMode -> filterQualities.map { samplingKey(tileMode, it) } }

        assertEquals(16, keys.toSet().size)
    }
}
