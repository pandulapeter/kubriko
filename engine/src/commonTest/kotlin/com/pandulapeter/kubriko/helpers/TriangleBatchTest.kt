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
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class TriangleBatchTest {

    private fun TriangleBatch.fillWithVertices(count: Int) = repeat(count) { addVertex(0f, 0f, 0) }

    @Test
    fun newBatchIsEmpty() {
        assertTrue(TriangleBatch().isEmpty)
    }

    @Test
    fun addedGeometryMakesTheBatchNonEmpty() {
        val batch = TriangleBatch()

        batch.addTriangle(0f, 0f, 1f, 0f, 0f, 1f, argb = -1)

        assertFalse(batch.isEmpty)
    }

    @Test
    fun vertexIndicesAreSequentialFromZero() {
        val batch = TriangleBatch()

        val indices = List(3) { batch.addVertex(it.toFloat(), 0f, 0) }
        val texturedIndex = batch.addTexturedVertex(0f, 1f, 0, u = 1f, v = 1f)

        assertEquals(listOf(0, 1, 2), indices)
        assertEquals(3, texturedIndex)
    }

    @Test
    fun resetEmptiesTheBatchAndRestartsTheIndices() {
        val batch = TriangleBatch()
        batch.fillWithVertices(3)

        batch.reset()

        assertTrue(batch.isEmpty)
        assertEquals(0, batch.addVertex(0f, 0f, 0))
    }

    @Test
    fun resetChangesTheGeneration() {
        val batch = TriangleBatch()
        val generation = batch.generation

        batch.reset()

        assertNotEquals(generation, batch.generation)
    }

    @Test
    fun willOverflowReportsTheVertexLimitExactly() {
        val batch = TriangleBatch()
        batch.fillWithVertices(10)

        assertFalse(batch.willOverflow(TriangleBatch.MAX_INDEXED_VERTICES - 10))
        assertTrue(batch.willOverflow(TriangleBatch.MAX_INDEXED_VERTICES - 9))
    }

    @Test
    fun indexedShapesAddNoVertices() {
        val batch = TriangleBatch()
        val a = batch.addVertex(0f, 0f, 0)
        val b = batch.addVertex(1f, 0f, 0)
        val c = batch.addVertex(1f, 1f, 0)
        val d = batch.addVertex(0f, 1f, 0)

        batch.addTriangle(a, b, c)
        batch.addQuad(a, b, c, d)

        assertFalse(batch.willOverflow(TriangleBatch.MAX_INDEXED_VERTICES - 4))
        assertTrue(batch.willOverflow(TriangleBatch.MAX_INDEXED_VERTICES - 3))
    }

    @Test
    fun addingAVertexPastTheLimitFails() {
        val batch = TriangleBatch()
        batch.fillWithVertices(TriangleBatch.MAX_INDEXED_VERTICES)

        assertFailsWith<IllegalStateException> { batch.addVertex(0f, 0f, 0) }
    }

    @Test
    fun addingAShapePastTheLimitFails() {
        val batch = TriangleBatch()
        batch.fillWithVertices(TriangleBatch.MAX_INDEXED_VERTICES - 2)

        assertFailsWith<IllegalStateException> { batch.addQuad(0f, 0f, 1f, 0f, 1f, 1f, 0f, 1f, argb = -1) }
        assertFailsWith<IllegalStateException> { batch.addLine(0f, 0f, 10f, 0f, halfWidth = 1f, argbA = -1) }
    }

    @Test
    fun aFullBatchAcceptsVerticesAgainAfterReset() {
        val batch = TriangleBatch()
        batch.fillWithVertices(TriangleBatch.MAX_INDEXED_VERTICES)

        batch.reset()

        assertEquals(0, batch.addVertex(0f, 0f, 0))
    }

    @Test
    fun zeroLengthLineAddsNothing() {
        val batch = TriangleBatch()

        batch.addLine(5f, 5f, 5f, 5f, halfWidth = 1f, argbA = -1)
        batch.addLine(5f, 5f, 5f, 5f, halfWidth = 1f, argbA = -1, isAntiAlias = true)

        assertTrue(batch.isEmpty)
    }

    @Test
    fun strokeSectionsReturnTheIndexOfTheirFirstVertex() {
        val batch = TriangleBatch()
        batch.fillWithVertices(5)

        val section = batch.addStrokeSection(0f, 0f, 0f, 1f, halfWidth = 1f, argb = -1, isAntiAlias = true, pixelsPerUnit = 1f)

        assertEquals(5, section)
    }
}
