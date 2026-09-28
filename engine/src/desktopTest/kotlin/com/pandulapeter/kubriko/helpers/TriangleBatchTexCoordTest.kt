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

import kotlin.test.Test
import kotlin.test.assertEquals

class TriangleBatchTexCoordTest {

    private fun TriangleBatch.texCoordsOf(vertexIndex: Int): Pair<Float, Float> {
        val field = TriangleBatch::class.java.getDeclaredField("texCoords").apply { isAccessible = true }
        val texCoords = field.get(this) as FloatArray
        return texCoords[vertexIndex * 2] to texCoords[vertexIndex * 2 + 1]
    }

    private fun TriangleBatch.padTexCoordsTo(end: Int) {
        TriangleBatch::class.java.getDeclaredMethod("padTexCoordsTo", Int::class.javaPrimitiveType)
            .apply { isAccessible = true }
            .invoke(this, end)
    }

    @Test
    fun untexturedVerticesGetBothDefaults() {
        val batch = TriangleBatch().apply {
            defaultU = 3f
            defaultV = 7f
        }
        batch.addVertex(0f, 0f, 0)
        batch.addVertex(1f, 0f, 0)
        batch.addTexturedVertex(0f, 1f, 0, u = 10f, v = 20f)
        assertEquals(3f to 7f, batch.texCoordsOf(0))
        assertEquals(3f to 7f, batch.texCoordsOf(1))
        assertEquals(10f to 20f, batch.texCoordsOf(2))
    }

    @Test
    fun trailingUntexturedVerticesArePadded() {
        val batch = TriangleBatch().apply {
            defaultU = 3f
            defaultV = 7f
        }
        batch.addVertex(0f, 0f, 0)
        batch.addVertex(1f, 0f, 0)
        batch.addTexturedVertex(0f, 1f, 0, u = 10f, v = 20f)
        batch.addVertex(1f, 1f, 0)
        batch.padTexCoordsTo(4)
        assertEquals(3f to 7f, batch.texCoordsOf(3))
    }

    @Test
    fun equalDefaultsStillPad() {
        val batch = TriangleBatch().apply {
            defaultU = 5f
            defaultV = 5f
        }
        batch.addVertex(0f, 0f, 0)
        batch.addVertex(1f, 0f, 0)
        batch.addTexturedVertex(0f, 1f, 0, u = 10f, v = 20f)
        assertEquals(5f to 5f, batch.texCoordsOf(0))
    }
}
